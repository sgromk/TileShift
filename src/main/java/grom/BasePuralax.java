package grom;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;

public class BasePuralax implements PuralaxModel<Tile> {
    private List<List<Tile>> gameBoard;
    private int numRows;
    private int numCols;
    private boolean gameStarted;
    private String goalColor;
    
    BasePuralax() {
        this.gameBoard = new ArrayList<>();
        this.numRows = -1;
        this.numCols = -1;
        this.gameStarted = false;
        this.goalColor = null;
    }

    public int getNumRows() {
        return numRows;
    }

    public int getNumCols() {
        return numCols;
    }

    public List<List<Tile>> getBoard() {
        return this.gameBoard;
    }

    public Tile getTileAt(int row, int column) {
        if (isValidIndex(row, column)) {
            return gameBoard.get(row).get(column);
        } else {throw new IllegalArgumentException("Invalid move. Invalid tile coorindates.");}
    }

    /**
     * Determines if the game is over, by counting if there are any dots remaining
     * or if all the tiles are the goal color
     */
    public boolean isGameOver() {
        return !isDotsRemaining() || isGameWon();
    }

    /**
     * Determines if the game is won by checking that all tiles in the board are empty or 
     * the goal color
     * @return
     */
    public boolean isGameWon() {
        for (int i = 0; i < numRows; i++) {                             // For each row
            for (int j = 0; j < numCols; j++) {                         // For each column
                Tile currentTile = getTileAt(i, j);                     // Get the tile
                if (currentTile.isEmpty() || currentTile.isWall()) {continue;}                    // Move on if it is an empty tile or a wall
                else if (!currentTile.matchingFirstColor(goalColor)) {   // Check if the color matches the goal
                    return false;                                       // If any tiles are the wrong color,
                                                                        // then the game is not won
                }
            }
        }
        return true;                                                    // Otherwise the game is won
    }

    /**
     * Determines if there are any dots, i.e. playable moves, remaining on the board
     * @return true if there are any dots, false otherwise
     */
    private boolean isDotsRemaining() {
        for (int i = 0; i < numRows; i++) {                 // For each row
            for (int j = 0; j < numCols; j++) {             // For each column
                Tile currentTile = getTileAt(i, j);         // Get the tile
                if (currentTile == null) {continue;}        // If the tile is empty, move on
                else if (currentTile.getNumDots() > 0) {    // If there are any dots, return true
                    return true;
                }
            }
        }
        return false;
    }

    public void moveTile(int fromRow, int fromCol, int toRow, int toCol) {
        if (!isValidIndex(fromRow, fromCol) || !isValidIndex(toRow, toCol)) {
            throw new IllegalArgumentException("Invalid move. Tile indexes outside of board.");
        }

        // Only allow orthogonal moves of one tile (no diagonal or long-range moves)
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);
        if (!((rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1))) {
            throw new IllegalArgumentException("Invalid move. Tiles must be adjacent orthogonally.");
        }

        Tile fromTile = getTileAt(fromRow, fromCol);
        Tile toTile = getTileAt(toRow, toCol);
        if (fromTile.getNumDots() < 1) {
            throw new IllegalStateException("Initial tile is empty and/or has no dots.");
        } else if (toTile.isWall()) {
            return; // Ignore the move if the destination is a wall
        }

        // If trying to paint a tile with the same color as the source, do nothing
        if (!toTile.isEmpty() && toTile.getFirstColor().equals(fromTile.getFirstColor())) {
            return;
        }

        if (toTile.isEmpty()) {                                 // If the destination is empty
            gameBoard.get(toRow).set(toCol, fromTile);                  // Only move the initial tile
            gameBoard.get(fromRow).set(fromCol, new EmptyTile());
            fromTile.useDot();
        } else {
            paintTiles(toTile, fromTile.getFirstColor(), toTile.getFirstColor(), toRow, toCol);      // Propagate the painting along same-colored tiles
            fromTile.useDot();
        }
    }

    /**
     * Paint tiles using iterative BFS flood-fill to avoid stack overflow.
     * If a tile should be painted, paints the tile and continues propagating in all directions.
     * @param tileToPaint the target tile that should be painted if it is the color of propagateAlong
     * @param newColor the new color, if the tile is to be painted
     * @param propagateAlong the color to be painted over
     * @param rowLoc the row index of the current tile
     * @param colLoc the column index of the current tile
     */
    private void paintTiles(Tile tileToPaint, String newColor, String propagateAlong, int rowLoc, int colLoc) {
        // Initial validation
        if (tileToPaint == null) return;
        String firstColor = tileToPaint.getFirstColor();
        if (firstColor == null || !firstColor.equals(propagateAlong) || firstColor.equals(newColor)) {
            return;
        }

        // Use BFS with queue to avoid recursion/stack overflow
        Queue<int[]> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        
        queue.add(new int[]{rowLoc, colLoc});
        visited.add(rowLoc + "," + colLoc);
        
        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int r = pos[0];
            int c = pos[1];
            
            Tile tile = getTileAt(r, c);
            String tileColor = tile.getFirstColor();
            
            // Only paint if it matches the propagate color and isn't already the new color
            if (tileColor == null || !tileColor.equals(propagateAlong) || tileColor.equals(newColor)) {
                continue;
            }
            
            // Paint this tile
            tile.paint(newColor, propagateAlong);
            
            // Add orthogonal neighbors to queue if they match propagateAlong color
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : directions) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                String key = nr + "," + nc;
                
                if (isValidIndex(nr, nc) && !visited.contains(key)) {
                    Tile neighbor = getTileAt(nr, nc);
                    if (neighbor.getFirstColor() != null && neighbor.getFirstColor().equals(propagateAlong)) {
                        visited.add(key);
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
    }

    /**
     * Determines if the given indexes are valid for the level
     * @param row the row of the tile in the board
     * @param col the column of the tile in the board
     * @return true if the index is valid, false otherwise
     */
    private boolean isValidIndex(int row, int col) {
        return row >= 0 
            && row < (numRows) 
            && col >= 0 
            && col < (numCols);
    }

    public void startGame(List<List<Tile>> gameBoard, int numRows, int numColumns, String goalColor) {
        this.numRows = numRows;
        this.numCols = numColumns;
        this.gameStarted = true;
        this.goalColor = goalColor;
        this.gameBoard = gameBoard;

        checkValidStartGame(numRows, numColumns);

    }

    /**
     * Returns the currently set goal color code for the active game.
     * May be null if no goal has been set.
     */
    public String getGoalColor() {
        return this.goalColor;
    }

    /**
     * Determines if the game has been started
     * @return true if the game is started, false otherwise
     */
    public boolean isGameStarted() {
        return gameStarted;
    }

    /**
     * Determines if the starting rows and columns are valid
     * @param rows the rows to start the game
     * @param cols the columns to start the game
     */
    private void checkValidStartGame(int rows, int cols) {
        if (numRows < 1) {
            throw new IllegalArgumentException("Invalid parameters. Must be at least one row.");
        } else if (numCols < 1) {
            throw new IllegalArgumentException("Invalid parameters. Must be at least one column.");
        }
    }
    /**
     * Resets the game state so isGameStarted() returns false and no game is active.
     */
    public void resetGameState() {
        this.gameStarted = false;
    }
}
