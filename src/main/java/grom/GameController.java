package grom;

import java.util.ArrayList;
import java.util.List;

public class GameController {
    private BasePuralax game;
    private PuralaxGraphicalView graphicalView;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private String goalColor = "G"; // default target color
    private LevelLoader levelLoader = new LevelLoader();

    public boolean isTileSelected(int row, int col) {
        return selectedRow == row && selectedCol == col;
    }
    public int currentLevelRows;
    public int currentLevelCols;
    public List<List<Tile>> currentLevel;

    GameController() {
        this.game = new BasePuralax();
        this.currentLevel = new ArrayList<>();
        initializeEmptyLevel();
        this.currentLevelRows = 2;
        this.currentLevelCols = 2;
        this.graphicalView = new PuralaxGraphicalView(game, this);
    }

    public void startGame() {
        // After getting inputs, call the game with the selected goal color
        playGameHelper(currentLevel, currentLevelRows, currentLevelCols, goalColor);
    }

    /**
     * Start a game using a deep copy of the provided level and refresh the view.
     * This makes a copy to avoid mutating the held level and allows replaying the
     * original level data later.
     * @param currentLevel The level to play as a List<List<Tile>>
     * @param userRows The number of rows for the level
     * @param userCols The number of columns for the level
     * @param userGoalColor The goal color to win
     */
    private void playGameHelper(List<List<Tile>> currentLevel, int userRows, int userCols, String userGoalColor) {
        List<List<Tile>> copyLevel = copyBoard(currentLevel);
        game.startGame(copyLevel, userRows, userCols, userGoalColor);
        graphicalView.updateView();
    }

    /**
     * Copy method to create a deep copy of the game board, to avoid mutating the original level.
     * @param originalBoard the {@code ListList<Tile>>} representing the level
     * @return A deep copy of the level
     */
    private List<List<Tile>> copyBoard(List<List<Tile>> originalBoard) {
        List<List<Tile>> newBoard = new ArrayList<>();
        for (List<Tile> row : originalBoard) {
            List<Tile> newRow = new ArrayList<>();
            for (Tile tile : row) {
                newRow.add(tile.copy());
            }
            newBoard.add(newRow);
        }
        return newBoard;
    }

    /**
     * Sets the default held level to a 2x2 board of {@code EmptyTile}
     */
    private void initializeEmptyLevel() {
        currentLevel.add(new ArrayList<>(List.of(new EmptyTile(), new EmptyTile())));
        currentLevel.add(new ArrayList<>(List.of(new EmptyTile(), new EmptyTile())));
    }

    /**
     * Main method to begin the game. Initializes the GameController and calls the
     * playGame method.
     * @param args
     */
    public static void main(String[] args) {
        new GameController();
    }

    /**
     * Called by the view when the player chooses whether to replay a failed level.
     * @param tryAgain true if player wants to retry the current level, false to exit
     */
    public void onReplayChoice(boolean tryAgain) {
        if (tryAgain) {
            playGameHelper(currentLevel, currentLevelRows, currentLevelCols, goalColor);
        } else {
            graphicalView.dispose();
            System.exit(0);
        }
    }

    /**
     * Set the desired goal color for the next game start (e.g. "G", "B").
     * Ignored if null or empty.
     */
    public void setGoalColor(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) { return; }
        this.goalColor = colorCode;
    }

    /**
     * Called by the view to replace a tile in the held current level.
     * The controller owns the level data structure and will update the view.
     */
    public void setTileAt(int row, int col, Tile tile) {
        if (row < 0 || row >= currentLevelRows || col < 0 || col >= currentLevelCols) {
            throw new IllegalArgumentException("Tile coordinates out of range");
        }
        currentLevel.get(row).set(col, tile);
    }

    /**
     * Start a placeholder preset level by index (1-based). This creates a small
     * board to allow the UI to start a game; you can replace with real levels later.
     */
    public void startPreset(int index) {
        // index is 1-based from UI, but LevelLoader is 0-based
        LevelLoader.LoadedLevel lvl = levelLoader.getLevel(index - 1);
        this.currentLevel = lvl.board;
        this.currentLevelRows = lvl.rows;
        this.currentLevelCols = lvl.cols;
        this.goalColor = lvl.goalColor;
        playGameHelper(currentLevel, currentLevelRows, currentLevelCols, goalColor);
    }

    /**
     * Handle a tile click during gameplay (select → move).
     * - Selects a non-empty source when none is selected.
     * - Deselects if the same tile is clicked.
     * - Attempts an orthogonal move when a source is selected (invalid moves ignored).
     * - Clears selection and refreshes the view after the action.
     */
    public void onTileClicked(int row, int col) {
        // If no source selected yet, select this tile as source
        if (!game.isGameStarted()) { return; }

        if (selectedRow == -1) {
            // Do not allow selecting empty tiles during gameplay
            Tile clicked = game.getTileAt(row, col);
            if (clicked.isEmpty()) { return; }
            selectedRow = row;
            selectedCol = col;
            graphicalView.updateView();
            return;
        }

        // If the same tile clicked again, deselect
        if (selectedRow == row && selectedCol == col) {
            selectedRow = -1;
            selectedCol = -1;
            graphicalView.updateView();
            return;
        }

        // Capture board state before move for animation
        List<List<Tile>> beforeBoard = copyBoard(game.getBoard());
        int fromRow = selectedRow, fromCol = selectedCol;
        int toRow = row, toCol = col;

        // Otherwise, attempt to move from selected -> clicked
        try {
            game.moveTile(fromRow, fromCol, toRow, toCol);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Invalid move (e.g., no dots or invalid indices)
        }

        // Capture board state after move
        List<List<Tile>> afterBoard = copyBoard(game.getBoard());

        // Clear selection
        selectedRow = -1;
        selectedCol = -1;

        // Compute paint propagation for animation, or update view directly if no animation needed
        graphicalView.animateOrUpdateView(beforeBoard, afterBoard, fromRow, fromCol, toRow, toCol, game.getNumRows(), game.getNumCols());
    }

    /**
     * Resets the model's game state and clears any selection in the controller.
     * Use this when switching out of gameplay (for example, entering level creator).
     */
    public void resetGameState() {
        if (this.game != null) {
            this.game.resetGameState();
        }
        this.selectedRow = -1;
        this.selectedCol = -1;
    }
}
