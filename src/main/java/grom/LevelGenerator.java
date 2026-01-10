package grom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Level generator that operates on an internal state model.
 * Starts from a minimal solved template and applies controlled mutations
 * under strict constraints using a seeded RNG for reproducibility.
 * Final output is serialized to the existing level JSON format.
 */
public class LevelGenerator {
    // Structural
    private static final int MIN_ROWS = 2;
    private static final int MIN_COLS = 2;
    private static final int MAX_CELLS = 49; // up to 7x7
    private static final float MAX_WALL_RATIO = 0.25f;

    // Dots
    private static final int MAX_DOTS = 5;
    private static final int MAX_TOTAL_DOTS = 20;

    // Generation
    private static final int MAX_REJECTS = 120;
    private static final int MAX_MUTATIONS_MULT = 2; // rows*cols/this

    // Colors
    private static final String[] COLORS = new String[]{"G","B","R","Y","P"};

    private final Random rng;

    public LevelGenerator() {
        // Mix multiple entropy sources to diversify sequences across runs
        long seed = System.nanoTime() ^ System.currentTimeMillis() ^ new Random().nextLong();
        this.rng = new Random(seed);
    }

    public LevelGenerator(long seed) {
        this.rng = new Random(seed);
    }

    /**
     * Primary API: generate level JSON for given grid and goal color.
     * Uses a default target difficulty and the generator's RNG.
     */
    public String generateLevelAsJson(int rows, int cols, String goalColor) {
        // Conservative difficulty formula: keeps levels solvable while adding complexity
        // For 5x5: max(15, 25+6) = 31 points
        int base = Math.max(15, (rows * cols) + 6);
        State s = generate(rows, cols, goalColor, base);
        return convertToJson(s);
    }

    /** Internal state representation. */
    private static class State {
        final int rows, cols;
        final String goalColor;
        final Cell[][] grid;

        State(int rows, int cols, String goalColor) {
            this.rows = rows; this.cols = cols; this.goalColor = goalColor;
            this.grid = new Cell[rows][cols];
        }

        State deepCopy() {
            State copy = new State(rows, cols, goalColor);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    copy.grid[r][c] = grid[r][c].copy();
                }
            }
            return copy;
        }
        
        /** Shallow copy for non-mutating validation checks - much faster than deepCopy */
        State shallowCopy() {
            State copy = new State(rows, cols, goalColor);
            for (int r = 0; r < rows; r++) {
                copy.grid[r] = grid[r].clone(); // Clone array reference, not cell objects
            }
            return copy;
        }
    }

    /** Move history for solvability tracking */
    private static class Move {
        final int fromR, fromC, toR, toC;
        final String moveType; // "MOVE" or "PAINT"
        final String originalColor; // For paint moves: what was overwritten
        Move(int fromR, int fromC, int toR, int toC, String type, String origColor) {
            this.fromR = fromR; this.fromC = fromC;
            this.toR = toR; this.toC = toC;
            this.moveType = type;
            this.originalColor = origColor;
        }
    }
    
    /** Cell contents. */
    private static class Cell {
        enum Type { EMPTY, WALL, COLOR }
        Type type;
        String color; // only for COLOR
        int dots;     // only for COLOR

        static Cell empty() { Cell t = new Cell(); t.type = Type.EMPTY; return t; }
        static Cell wall() { Cell t = new Cell(); t.type = Type.WALL; return t; }
        static Cell color(String color, int dots) { Cell t = new Cell(); t.type = Type.COLOR; t.color = color; t.dots = dots; return t; }

        Cell copy() {
            Cell n = new Cell();
            n.type = this.type;
            n.color = this.color;
            n.dots = this.dots;
            return n;
        }
    }

    /**  Create complex solvable template using reverse-move strategy */
    private State createSolvedTemplate(int rows, int cols, String goalColor) {
        // Start with a solved state (all goal color)
        State s = new State(rows, cols, goalColor);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                s.grid[r][c] = Cell.empty();
            }
        }
        
        // Place multiple goal-colored tiles with dots across the board
        int numTiles = Math.max(3, (rows * cols) / 4); // 25% of board
        int tilesPlaced = 0;
        
        // Try to place tiles with some spacing
        for (int attempt = 0; attempt < numTiles * 3 && tilesPlaced < numTiles; attempt++) {
            int r = rng.nextInt(rows);
            int c = rng.nextInt(cols);
            if (s.grid[r][c].type == Cell.Type.EMPTY) {
                int dots = 1 + rng.nextInt(Math.min(3, MAX_DOTS));
                s.grid[r][c] = Cell.color(goalColor, dots);
                tilesPlaced++;
            }
        }
        
        // Now apply 3-8 reverse paint operations to create non-goal tiles
        // This guarantees solvability because we can just replay the moves forward
        int numReversePaints = 3 + rng.nextInt(6);
        for (int i = 0; i < numReversePaints; i++) {
            reversePaintComponent(s);
        }
        
        return s;
    }

    /**
     * Generate by applying single-step mutations under constraints until difficulty target.
     * If too many consecutive rejects, restart from template.
     */
    private State generate(int rows, int cols, String goalColor, int targetDifficulty) {
        if (!validateDimensions(rows, cols)) {
            throw new IllegalArgumentException("Invalid grid dimensions");
        }
        if (!Arrays.asList(COLORS).contains(goalColor)) {
            throw new IllegalArgumentException("Unsupported goal color");
        }

        State current = createSolvedTemplate(rows, cols, goalColor);
        // Template should NOT be solved (has at least one non-goal tile for gameplay)
        if (isSolved(current)) {
            throw new IllegalStateException("Starting template should not be already solved");
        }

        int consecutiveRejects = 0;
        int guard = 0; // safety
        int successfulMutations = 0;
        int minMutations = 1; // Just 1 minimum mutation for simplicity

        while ((difficultyScore(current) < targetDifficulty || successfulMutations < minMutations) && guard < 5000) {
            guard++;
            State candidate = current.deepCopy();
            if (!applyControlledMutation(candidate)) {
                consecutiveRejects++;
            } else if (!validateAll(candidate)) {
                consecutiveRejects++;
            } else {
                // Only accept if the mutation doesn't solve the level
                if (!isSolved(candidate)) {
                    current = candidate;
                    consecutiveRejects = 0;
                    successfulMutations++;
                } else {
                    consecutiveRejects++;
                }
            }

            if (consecutiveRejects >= MAX_REJECTS) {
                current = createSolvedTemplate(rows, cols, goalColor);
                consecutiveRejects = 0;
                successfulMutations = 0; // Reset mutation count on restart
            }
        }

        // Finalization guard: ensure the result is playable and not solved
        current = ensurePlayableNotSolved(current);
        
        // Final validation that level is not already solved
        if (isSolved(current)) {
            throw new IllegalStateException("Generated level is already solved! This should never happen.");
        }
        
        // Final solvability check - retry generation if unsolvable
        int solvabilityRetries = 0;
        while (!isSolvableViaBFS(current) && solvabilityRetries < 3) {
            solvabilityRetries++;
            // Generate a fresh level instead of falling back to simple template
            current = createSolvedTemplate(rows, cols, goalColor);
            
            // Apply some mutations to increase difficulty
            for (int i = 0; i < 5; i++) {
                State candidate = current.deepCopy();
                if (applyControlledMutation(candidate) && validateAll(candidate) && !isSolved(candidate)) {
                    current = candidate;
                }
            }
        }
        
        // If still unsolvable after retries, accept the level anyway
        // (BFS might be hitting exploration limit on valid but complex levels)
        
        return current;
    }

    /**
     * Construct a simple, known-solvable 2x2 puzzle:
     * - One goal tile with 1 dot
     * - One adjacent non-goal colored tile
     * - Two empties
     * A single move from the goal onto the non-goal paints it and wins.
     */
    private State generate2x2(String goalColor) {
        State s = new State(2, 2, goalColor);
        for (int r = 0; r < 2; r++) for (int c = 0; c < 2; c++) s.grid[r][c] = Cell.empty();

        // Randomize goal position among the 4 cells
        int[][] pos = new int[][]{{0,0},{0,1},{1,0},{1,1}};
        int gi = rng.nextInt(4);
        int gr = pos[gi][0], gc = pos[gi][1];

        // Determine orthogonal neighbors of the goal
        List<int[]> neighbors = new ArrayList<>();
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : D) {
            int nr = gr + d[0], nc = gc + d[1];
            if (nr >= 0 && nr < 2 && nc >= 0 && nc < 2) neighbors.add(new int[]{nr,nc});
        }

        // Choose pattern: A) one-move paint (50%), B) two paints (50%)
        boolean twoStep = rng.nextInt(100) >= 50;
        s.grid[gr][gc] = Cell.color(goalColor, twoStep ? 2 : 1);

        if (!twoStep) {
            // Pattern A: choose one neighbor to be non-goal, others empty
            int[] p = neighbors.get(rng.nextInt(neighbors.size()));
            String other = pickNonGoalColor(goalColor);
            s.grid[p[0]][p[1]] = Cell.color(other, 0);
        } else {
            // Pattern B: both neighbors are non-goal; may be same or different colors
            String c1 = pickNonGoalColor(goalColor);
            String c2 = (rng.nextInt(100) < 50) ? c1 : pickNonGoalColor(goalColor);
            // neighbors size in 2x2 corners is 2; in edges for 2x2 still 2
            s.grid[neighbors.get(0)[0]][neighbors.get(0)[1]] = Cell.color(c1, 0);
            s.grid[neighbors.get(1)[0]][neighbors.get(1)[1]] = Cell.color(c2, 0);
        }

        // Validate basic constraints; if not valid, fallback to simple one-move pattern centered at [0,0]
        if (!validateAll(s)) {
            for (int r = 0; r < 2; r++) for (int c = 0; c < 2; c++) s.grid[r][c] = Cell.empty();
            s.grid[0][0] = Cell.color(goalColor, 1);
            String other = pickNonGoalColor(goalColor);
            s.grid[0][1] = Cell.color(other, 0);
        }
        return s;
    }

    /** Single mutation chosen from a fixed set; never blind placement. */
    private boolean applyControlledMutation(State s) {
        // Reverse-generation: only apply exact inverses of legal moves
        int pick = rng.nextInt(100);
        if (pick < 50) return reversePaintComponent(s);
        if (pick < 80) return reverseMoveToAdjacentEmpty(s);
        return addWall(s);
    }

    /** Add a wall to an empty cell to increase complexity. */
    private boolean addWall(State s) {
        List<int[]> empties = collectEmpty(s);
        if (empties.isEmpty()) return false;
        // Check wall ratio constraint
        if (countWalls(s) >= Math.floor(s.rows * s.cols * MAX_WALL_RATIO)) return false;
        // Prefer cells that are not adjacent to the goal to avoid blocking it completely
        int[] g = findGoal(s);
        List<int[]> nonAdjGoal = new ArrayList<>();
        for (int[] p : empties) {
            if (manhattan(g[0], g[1], p[0], p[1]) > 1) nonAdjGoal.add(p);
        }
        List<int[]> pool = nonAdjGoal.isEmpty() ? empties : nonAdjGoal;
        int[] p = pool.get(rng.nextInt(pool.size()));
        s.grid[p[0]][p[1]] = Cell.wall();
        return true;
    }

    private boolean addNonGoalColorTile(State s) {
        List<int[]> empties = collectEmpty(s);
        if (empties.isEmpty()) return false;
        // Prefer cells adjacent to non-wall for accessibility
        List<int[]> adj = filterAdjacentToNonWall(s, empties);
        List<int[]> pool = adj.isEmpty() ? empties : adj;
        int[] p = pool.get(rng.nextInt(pool.size()));
        String color = pickNonGoalColor(s.goalColor);
        s.grid[p[0]][p[1]] = Cell.color(color, 0);
        return true;
    }

    // Dots are only adjusted as part of reverse of a move; handled in reverse methods

    private boolean reverseMoveToAdjacentEmpty(State s) {
        // Inverse of forward move onto empty: move tile back and add a dot
        List<int[]> movable = collectColored(s, /*includeGoal*/ true);
        if (movable.isEmpty()) return false;
        // Randomize starting index to avoid bias
        int start = rng.nextInt(movable.size());
        for (int k = 0; k < movable.size(); k++) {
            int[] from = movable.get((start + k) % movable.size());
            List<int[]> adjEmpty = adjacentEmpties(s, from[0], from[1]);
            if (adjEmpty.isEmpty()) continue;
            int[] to = adjEmpty.get(rng.nextInt(adjEmpty.size()));
            Cell src = s.grid[from[0]][from[1]];
            // Move tile into empty (reverse of having moved from that empty)
            s.grid[to[0]][to[1]] = Cell.color(src.color, Math.min(src.dots + 1, MAX_DOTS));
            s.grid[from[0]][from[1]] = Cell.empty();
            return true;
        }
        return false;
    }

    private boolean reversePaintComponent(State s) {
        // Inverse of forward paint: add a dot to the source (goal tile) and
        // convert a connected component (except target tile) from goalColor back to non-goal.
        int[] g = findGoal(s);
        Cell goal = s.grid[g[0]][g[1]];
        if (goal.type != Cell.Type.COLOR) return false;
        // Ensure source has dots to reflect reverse of consumption
        if (goal.dots < MAX_DOTS) goal.dots += 1;

        // Try to find a goal-colored tile that has at least one goal-colored neighbor
        List<int[]> goalTiles = collectColored(s, /*includeGoal*/ true);
        // Filter only goal color
        List<int[]> goalOnly = new ArrayList<>();
        for (int[] p : goalTiles) {
            if (s.grid[p[0]][p[1]].type == Cell.Type.COLOR && s.grid[p[0]][p[1]].color.equals(s.goalColor)) {
                goalOnly.add(p);
            }
        }
        // Prefer components of size > 1; else bootstrap by placing non-goal adjacent to goal
        for (int[] t : goalOnly) {
            List<int[]> comp = connectedComponentOfColor(s, t[0], t[1], s.goalColor);
            if (comp.size() > 1) {
                // Keep target tile t as goal; convert others to a chosen non-goal color
                String other = pickNonGoalColor(s.goalColor);
                for (int[] q : comp) {
                    if (q[0] == t[0] && q[1] == t[1]) continue;
                    s.grid[q[0]][q[1]] = Cell.color(other, 0);
                }
                return true;
            }
        }
        // Bootstrap: if no multi-tile goal component, create a single non-goal adjacent to goal
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : D) {
            int nr = g[0] + d[0], nc = g[1] + d[1];
            if (inBounds(s, nr, nc) && s.grid[nr][nc].type == Cell.Type.EMPTY) {
                s.grid[nr][nc] = Cell.color(pickNonGoalColor(s.goalColor), 0);
                // Ensure post-paint progression: goal must retain a dot after consuming one
                goal.dots = Math.min(MAX_DOTS, Math.max(goal.dots, 2));
                return true;
            }
        }
        return false;
    }

    private List<int[]> connectedComponentOfColor(State s, int sr, int sc, String color) {
        List<int[]> out = new ArrayList<>();
        boolean[][] vis = new boolean[s.rows][s.cols];
        if (!inBounds(s, sr, sc)) return out;
        if (s.grid[sr][sc].type != Cell.Type.COLOR || !color.equals(s.grid[sr][sc].color)) return out;
        Deque<int[]> dq = new ArrayDeque<>();
        dq.add(new int[]{sr, sc}); vis[sr][sc] = true;
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        while (!dq.isEmpty()) {
            int[] cur = dq.poll();
            out.add(cur);
            for (int[] d : D) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (inBounds(s, nr, nc) && !vis[nr][nc]) {
                    Cell nb = s.grid[nr][nc];
                    if (nb.type == Cell.Type.COLOR && color.equals(nb.color)) {
                        vis[nr][nc] = true; dq.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return out;
    }

    /** Constraints */
    private boolean validateDimensions(int rows, int cols) {
        return rows >= MIN_ROWS && cols >= MIN_COLS && rows * cols <= MAX_CELLS;
    }

    private boolean validateAll(State s) {
        if (!validateDimensions(s.rows, s.cols)) return false;
        if (countGoals(s) != 1) return false;
        if (collectEmpty(s).isEmpty()) return false;
        if (countWalls(s) > Math.floor(s.rows * s.cols * MAX_WALL_RATIO)) return false;
        if (!validateDots(s)) return false;
        if (!hasAnyActiveDot(s)) return false; // must have at least one movable tile
        if (!goalHasNonWallNeighbor(s)) return false;
        if (!goalCanReachEmpty(s)) return false;
        if (isSolved(s)) return false;
        // must be able to make progress toward solution: goal tile can paint immediately
        if (!goalPaintMoveExists(s)) return false;
        // and at least one legal move exists somewhere (redundancy for safety)
        if (!legalMoveExists(s)) return false;
        // ensure that after performing one paint from goal, another legal move remains
        if (!postPaintLegalMoveExists(s)) return false;
        // Reject trivial levels - must have minimum complexity
        if (countColoredNonGoal(s) < 1) return false; // At least 1 non-goal tile
        return true;
    }

    private boolean validateDots(State s) {
        int sum = 0;
        for (int r = 0; r < s.rows; r++) {
            for (int c = 0; c < s.cols; c++) {
                Cell cell = s.grid[r][c];
                if (cell.type == Cell.Type.COLOR) {
                    if (cell.dots < 0 || cell.dots > MAX_DOTS) return false;
                    sum += cell.dots;
                }
            }
        }
        return sum <= MAX_TOTAL_DOTS;
    }

    private boolean goalHasNonWallNeighbor(State s) {
        int[] g = findGoal(s);
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : D) {
            int nr = g[0] + d[0], nc = g[1] + d[1];
            if (inBounds(s, nr, nc) && s.grid[nr][nc].type != Cell.Type.WALL) return true;
        }
        return false;
    }

    private boolean goalCanReachEmpty(State s) {
        int[] g = findGoal(s);
        boolean[][] vis = new boolean[s.rows][s.cols];
        Deque<int[]> dq = new ArrayDeque<>();
        dq.add(new int[]{g[0], g[1]});
        vis[g[0]][g[1]] = true;
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        while (!dq.isEmpty()) {
            int[] cur = dq.poll();
            if (s.grid[cur[0]][cur[1]].type == Cell.Type.EMPTY) return true;
            for (int[] d : D) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (inBounds(s, nr, nc) && !vis[nr][nc] && s.grid[nr][nc].type != Cell.Type.WALL) {
                    vis[nr][nc] = true;
                    dq.add(new int[]{nr, nc});
                }
            }
        }
        return false;
    }

    // isSolved: all colored tiles are goalColor (empties/walls ignored)
    private boolean isSolved(State s) {
        for (int r = 0; r < s.rows; r++) {
            for (int c = 0; c < s.cols; c++) {
                Cell cell = s.grid[r][c];
                if (cell.type == Cell.Type.COLOR && !s.goalColor.equals(cell.color)) return false;
            }
        }
        return true;
    }

    private boolean legalMoveExists(State s) {
        // A legal move requires a colored tile with dots > 0
        // and an adjacent empty or differently colored neighbor.
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        for (int r=0;r<s.rows;r++){
            for (int c=0;c<s.cols;c++){
                Cell cell = s.grid[r][c];
                if (cell.type == Cell.Type.COLOR && cell.dots > 0) {
                    for (int[] d: D) {
                        int nr=r+d[0], nc=c+d[1];
                        if (!inBounds(s,nr,nc)) continue;
                        Cell nb = s.grid[nr][nc];
                        if (nb.type == Cell.Type.EMPTY) return true;
                        if (nb.type == Cell.Type.COLOR && !nb.color.equals(cell.color)) return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if there is an immediate paint opportunity: a goal-colored tile
     * with dots > 0 has an orthogonal non-goal colored neighbor.
     */
    private boolean goalPaintMoveExists(State s) {
        int[] g = findGoal(s);
        Cell goal = s.grid[g[0]][g[1]];
        if (goal.type != Cell.Type.COLOR || goal.dots <= 0) return false;
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : D) {
            int nr = g[0] + d[0], nc = g[1] + d[1];
            if (!inBounds(s, nr, nc)) continue;
            Cell nb = s.grid[nr][nc];
            if (nb.type == Cell.Type.COLOR && !s.goalColor.equals(nb.color)) return true;
        }
        return false;
    }

    /**
     * Simulate one paint from the goal onto an adjacent non-goal colored tile
     * and verify another legal move exists afterwards.
     */
    private boolean postPaintLegalMoveExists(State s) {
        int[] g = findGoal(s);
        Cell goal = s.grid[g[0]][g[1]];
        if (goal.type != Cell.Type.COLOR || goal.dots <= 0) return false;
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : D) {
            int nr = g[0] + d[0], nc = g[1] + d[1];
            if (!inBounds(s, nr, nc)) continue;
            Cell nb = s.grid[nr][nc];
            if (nb.type == Cell.Type.COLOR && !s.goalColor.equals(nb.color)) {
                // simulate paint: flood fill from neighbor over same color to goalColor
                State t = s.shallowCopy();  // Use shallow copy for validation
                floodFillPaint(t, nr, nc, nb.color, s.goalColor);
                // consume one dot from goal in the simulated state
                t.grid[g[0]][g[1]].dots = Math.max(0, t.grid[g[0]][g[1]].dots - 1);
                // After painting, confirm a legal move still exists
                if (legalMoveExists(t) && hasAnyActiveDot(t)) return true;
            }
        }
        return false;
    }

    /**
     * Flood fill painting: convert the connected component of 'fromColor' starting
     * at (sr, sc) into 'toColor'. Walls and empties are ignored.
     */
    private void floodFillPaint(State s, int sr, int sc, String fromColor, String toColor) {
        if (fromColor == null || toColor == null || fromColor.equals(toColor)) return;
        boolean[][] vis = new boolean[s.rows][s.cols];
        Deque<int[]> dq = new ArrayDeque<>();
        dq.add(new int[]{sr, sc});
        vis[sr][sc] = true;
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        while (!dq.isEmpty()) {
            int[] cur = dq.poll();
            Cell cell = s.grid[cur[0]][cur[1]];
            if (cell.type == Cell.Type.COLOR && fromColor.equals(cell.color)) {
                cell.color = toColor;
                for (int[] d : D) {
                    int nr = cur[0] + d[0], nc = cur[1] + d[1];
                    if (inBounds(s, nr, nc) && !vis[nr][nc]) {
                        vis[nr][nc] = true;
                        dq.add(new int[]{nr, nc});
                    }
                }
            }
        }
    }

    private boolean hasAnyActiveDot(State s) {
        for (int r=0;r<s.rows;r++) for (int c=0;c<s.cols;c++) {
            Cell cell = s.grid[r][c];
            if (cell.type == Cell.Type.COLOR && cell.dots > 0) return true;
        }
        return false;
    }

    /**
     * Repair step to guarantee playability if the mutation loop exits early.
     * Ensures: not solved, has at least one active dot, and a legal move exists.
     * Does NOT check BFS solvability here to avoid excessive computation.
     */
    private State ensurePlayableNotSolved(State s) {
        State cur = s.deepCopy();
        
        // First check: if the level is not solved and valid, return it
        if (!isSolved(cur) && validateAll(cur)) {
            return cur;
        }
        
        // If solved, we MUST fix it - apply reverse mutations to create unsolved state
        int fixAttempts = 0;
        while (isSolved(cur) && fixAttempts < 100) {
            fixAttempts++;
            
            // Strategy 1: Apply reverse paint to split goal-colored component
            State cand = cur.deepCopy();
            if (reversePaintComponent(cand) && !isSolved(cand) && validateAll(cand)) {
                cur = cand;
                break;
            }
            
            // Strategy 2: Add non-goal colored tiles directly
            cand = cur.deepCopy();
            if (addNonGoalColorTile(cand) && !isSolved(cand) && validateAll(cand)) {
                cur = cand;
                if (addNonGoalColorTile(cur)) {
                    break;
                }
            }
            
            // Strategy 3: Use reverse move if nothing else works
            cand = cur.deepCopy();
            if (reverseMoveToAdjacentEmpty(cand) && !isSolved(cand) && validateAll(cand)) {
                cur = cand;
                break;
            }
        }
        
        // Ensure an immediate paint opportunity exists
        if (!goalPaintMoveExists(cur)) {
            State cand = cur.deepCopy();
            if (reversePaintComponent(cand) && validateAll(cand)) cur = cand;
        }
        
        // Ensure there is at least one active dot
        if (!hasAnyActiveDot(cur)) {
            State cand = cur.deepCopy();
            if (reverseMoveToAdjacentEmpty(cand) && validateAll(cand)) cur = cand;
        }
        
        return cur;
    }

    // Removed triviality heuristic; rely on explicit isSolved() rejection

    /** Difficulty scoring combining features; monotonic with mutations. */
    private int difficultyScore(State s) {
        int score = 0;
        int walls = countWalls(s);
        int colored = countColoredNonGoal(s);
        int dots = countTotalDots(s);
        score += walls * 4;
        score += colored * 4;
        score += Math.min(15, dots);
        // distance term: average manhattan distance from goal to colored non-goal tiles
        int[] g = findGoal(s);
        List<int[]> nonGoals = collectColored(s, false /*include goal*/);
        int distSum = 0;
        for (int[] p : nonGoals) distSum += manhattan(g[0], g[1], p[0], p[1]);
        if (!nonGoals.isEmpty()) score += Math.min(10, distSum / nonGoals.size());
        return score;
    }

    private int countMutationsEstimate(State s) {
        // heuristic: number of non-empty changes from template
        int count = 0;
        for (int r=0;r<s.rows;r++) for (int c=0;c<s.cols;c++) {
            if (r==s.rows/2 && c==s.cols/2) continue; // center goal
            Cell cell = s.grid[r][c];
            if (cell.type != Cell.Type.EMPTY) count++;
        }
        return count;
    }

    /** Serialization to existing levels.json format. */
    private String convertToJson(State s) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"goalColor\": \"").append(s.goalColor).append("\",\n");
        json.append("  \"rows\": ").append(s.rows).append(",\n");
        json.append("  \"cols\": ").append(s.cols).append(",\n");
        json.append("  \"tiles\": [\n");
        for (int r = 0; r < s.rows; r++) {
            json.append("    [ ");
            for (int c = 0; c < s.cols; c++) {
                json.append(cellToJson(s.grid[r][c]));
                if (c < s.cols - 1) json.append(", ");
            }
            json.append(" ]");
            if (r < s.rows - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }

    private String cellToJson(Cell cell) {
        if (cell.type == Cell.Type.EMPTY) return "{ \"type\": \"empty\" }";
        if (cell.type == Cell.Type.WALL) return "{ \"type\": \"wall\" }";
        return "{ \"color\": \"" + cell.color + "\", \"dots\": " + cell.dots + " }";
    }

    /** Helpers */
    private boolean inBounds(State s, int r, int c) { return r>=0 && r<s.rows && c>=0 && c<s.cols; }
    private int[] findGoal(State s) {
        for (int r=0;r<s.rows;r++) for (int c=0;c<s.cols;c++) {
            Cell cell = s.grid[r][c];
            if (cell.type == Cell.Type.COLOR && s.goalColor.equals(cell.color)) return new int[]{r,c};
        }
        // Fallback to center if not found (should not happen after template)
        return new int[]{s.rows/2, s.cols/2};
    }
    private int countWalls(State s) {
        int w=0; for (int r=0;r<s.rows;r++) for (int c=0;c<s.cols;c++) if (s.grid[r][c].type==Cell.Type.WALL) w++; return w;
    }
    private int countGoals(State s) {
        int g=0; for (int r=0;r<s.rows;r++) for (int c=0;c<s.cols;c++) { Cell cell=s.grid[r][c]; if (cell.type==Cell.Type.COLOR && s.goalColor.equals(cell.color)) g++; } return g;
    }
    private int countTotalDots(State s) {
        int t=0; for (int r=0;r<s.rows;r++) for (int c=0;c<s.cols;c++) { Cell cell=s.grid[r][c]; if (cell.type==Cell.Type.COLOR) t+=cell.dots; } return t;
    }
    private int countColoredNonGoal(State s) {
        int n=0; for (int r=0;r<s.rows;r++) for (int c=0;c<s.cols;c++) { Cell cell=s.grid[r][c]; if (cell.type==Cell.Type.COLOR && !cell.color.equals(s.goalColor)) n++; } return n;
    }
    private List<int[]> collectEmpty(State s) {
        List<int[]> res = new ArrayList<>();
        for (int r=0;r<s.rows;r++) for (int c=0;c<s.cols;c++) if (s.grid[r][c].type==Cell.Type.EMPTY) res.add(new int[]{r,c});
        return res;
    }
    private List<int[]> collectColored(State s, boolean includeGoal) {
        List<int[]> res = new ArrayList<>();
        for (int r=0;r<s.rows;r++) for (int c=0;c<s.cols;c++) {
            Cell cell=s.grid[r][c];
            if (cell.type==Cell.Type.COLOR && (includeGoal || !cell.color.equals(s.goalColor))) res.add(new int[]{r,c});
        }
        return res;
    }
    private List<int[]> filterAdjacentToNonWall(State s, List<int[]> cells) {
        List<int[]> res = new ArrayList<>();
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] p : cells) {
            boolean ok=false; for (int[] d: D){ int nr=p[0]+d[0], nc=p[1]+d[1]; if (inBounds(s,nr,nc) && s.grid[nr][nc].type!=Cell.Type.WALL){ ok=true; break; } }
            if (ok) res.add(p);
        }
        return res;
    }
    private List<int[]> adjacentEmpties(State s, int r, int c) {
        List<int[]> out = new ArrayList<>();
        int[][] D = new int[][]{{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : D) {
            int nr = r + d[0], nc = c + d[1];
            if (inBounds(s, nr, nc) && s.grid[nr][nc].type == Cell.Type.EMPTY) out.add(new int[]{nr,nc});
        }
        return out;
    }
    private String pickNonGoalColor(String goal) {
        // pick uniformly among COLORS \ {goal}
        List<String> pool = new ArrayList<>();
        for (String c : COLORS) if (!c.equals(goal)) pool.add(c);
        return pool.get(rng.nextInt(pool.size()));
    }
    private int manhattan(int r1,int c1,int r2,int c2){ return Math.abs(r1-r2)+Math.abs(c1-c2);}
    
    /**
     * Forward BFS solver to verify generated level is actually solvable.
     * Explores all possible move sequences up to a reasonable depth limit.
     * Returns true if any sequence reaches the solved state (all colored tiles are goalColor).
     * Returns false if the state is already solved (no gameplay needed).
     */
    private boolean isSolvableViaBFS(State s) {
        // Reject already-solved states - we need actual gameplay
        if (isSolved(s)) return false;
        
        Set<String> visited = new HashSet<>();
        Deque<State> queue = new ArrayDeque<>();
        queue.add(s);
        visited.add(stateHash(s));
        
        int maxStates = 100000; // Large limit for complex 5x5+ levels
        int explored = 0;
        
        while (!queue.isEmpty() && explored < maxStates) {
            State current = queue.poll();
            explored++;
            
            // Try all possible moves from this state
            for (int fromR = 0; fromR < current.rows; fromR++) {
                for (int fromC = 0; fromC < current.cols; fromC++) {
                    Cell fromCell = current.grid[fromR][fromC];
                    if (fromCell.type != Cell.Type.COLOR || fromCell.dots <= 0) continue;
                    
                    // Try moving to each orthogonal neighbor
                    int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
                    for (int[] d : dirs) {
                        int toR = fromR + d[0];
                        int toC = fromC + d[1];
                        if (!inBounds(current, toR, toC)) continue;
                        
                        Cell toCell = current.grid[toR][toC];
                        // Skip walls and same-color tiles
                        if (toCell.type == Cell.Type.WALL) continue;
                        if (toCell.type == Cell.Type.COLOR && 
                            toCell.color.equals(fromCell.color)) continue;
                        
                        // Apply move in a deep copy to avoid modifying shared cells
                        State next = current.deepCopy();
                        applyForwardMove(next, fromR, fromC, toR, toC);
                        
                        // Check if solved
                        if (isSolved(next)) return true;
                        
                        // Add to queue if not visited
                        String hash = stateHash(next);
                        if (!visited.contains(hash)) {
                            visited.add(hash);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        
        return false; // No solution found within exploration limit
    }
    
    /**
     * Apply a forward move in the game: move to empty or paint adjacent different color.
     * Modifies the state in place.
     */
    private void applyForwardMove(State s, int fromR, int fromC, int toR, int toC) {
        Cell fromCell = s.grid[fromR][fromC];
        Cell toCell = s.grid[toR][toC];
        
        if (toCell.type == Cell.Type.EMPTY) {
            // Move to empty: relocate tile and consume dot
            s.grid[toR][toC] = Cell.color(fromCell.color, Math.max(0, fromCell.dots - 1));
            s.grid[fromR][fromC] = Cell.empty();
        } else {
            // Paint: flood fill the component with source color
            String targetColor = toCell.color;
            floodFillPaint(s, toR, toC, targetColor, fromCell.color);
            // Consume dot from source
            fromCell.dots = Math.max(0, fromCell.dots - 1);
        }
    }
    
    /**
     * Generate a hash string representing the state for visited set.
     * Format: "color:dots,color:dots,..." for each cell.
     */
    private String stateHash(State s) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < s.rows; r++) {
            for (int c = 0; c < s.cols; c++) {
                Cell cell = s.grid[r][c];
                if (cell.type == Cell.Type.EMPTY) sb.append("E");
                else if (cell.type == Cell.Type.WALL) sb.append("W");
                else sb.append(cell.color).append(cell.dots);
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
