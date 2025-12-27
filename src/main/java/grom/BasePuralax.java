package grom;

import java.util.List;
import java.util.ArrayList;

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
            throw new IllegalArgumentException("Invalid move. Tile indexes outside of board.");}

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
        } else if (toTile.isWall()) {return;}                           // Ignore the move if the destination is a wall

        if (toTile.isEmpty()) {                                 // If the destination is empty
            gameBoard.get(toRow).set(toCol, fromTile);                  // Only move the initial tile
            gameBoard.get(fromRow).set(fromCol, new EmptyTile());
        } else {
            paintTiles(toTile, fromTile.getFirstColor(), toTile.getFirstColor(), toRow, toCol);      // Propogate the painting along same-colored tiles
        }
        fromTile.useDot();                                              // And use up a dot if a move was able to be played
    }

    /**
     * If a tile should be painted, paints the tile and continues propagating in all directions
     * @param tileToPaint the target tile that should be painted if it is the color of propagateAlong
     * @param newColor the new color, if the tile is to be painted
     * @param propagateAlong the color to be painted over
     * @param rowLoc the row index of the current tile
     * @param colLoc the column index of the current tile
     */
    private void paintTiles(Tile tileToPaint, String newColor, String propagateAlong,int rowLoc, int colLoc) {
        // Only paint tiles that match the original color being propagated and that aren't already the new color
        if (tileToPaint == null) { return; }
        String firstColor = tileToPaint.getFirstColor();
        if (firstColor == null) { return; }
        if (!firstColor.equals(propagateAlong)) { return; }
        if (firstColor.equals(newColor)) { return; }

        // Paint this tile
        tileToPaint.paint(newColor, propagateAlong);

        // Recurse only into neighbors that match the propagateAlong color (prevents cycling)
        if (rowLoc < numRows - 1) {
            Tile down = getTileAt(rowLoc + 1, colLoc);
            if (down.getFirstColor().equals(propagateAlong)) {
                paintTiles(down, newColor, propagateAlong, rowLoc + 1, colLoc);
            }
        }
        if (rowLoc > 0) {
            Tile up = getTileAt(rowLoc - 1, colLoc);
            if (up.getFirstColor().equals(propagateAlong)) {
                paintTiles(up, newColor, propagateAlong, rowLoc - 1, colLoc);
            }
        }
        if (colLoc < numCols - 1) {
            Tile right = getTileAt(rowLoc, colLoc + 1);
            if (right.getFirstColor().equals(propagateAlong)) {
                paintTiles(right, newColor, propagateAlong, rowLoc, colLoc + 1);
            }
        }
        if (colLoc > 0) {
            Tile left = getTileAt(rowLoc, colLoc - 1);
            if (left.getFirstColor().equals(propagateAlong)) {
                paintTiles(left, newColor, propagateAlong, rowLoc, colLoc - 1);
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
}
