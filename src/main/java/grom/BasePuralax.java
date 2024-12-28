package grom;

import java.util.List;
import java.util.ArrayList;

// TODO: look for optimizations in BaseGame code
// TODO: Add an interface that the View will implement, that requires a modelChanged() method
//       so that the view knows to update itself


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
                if (currentTile.isEmpty()) {continue;}                    // Move on if it is an empty tile
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

        Tile fromTile = getTileAt(fromRow, fromCol);
        Tile toTile = getTileAt(toRow, toCol);
        if (fromTile.getNumDots() < 1) {
            System.out.println("Initial tile is empty and/or has no dots.");
            return;
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
        tileToPaint.paint(newColor, propagateAlong);     // Tile will know whether or not to paint itslf

        if (rowLoc < numRows - 1) {
            paintTiles(getTileAt(rowLoc + 1, colLoc), newColor, propagateAlong, rowLoc + 1, colLoc); // Paint downwards
        }
        if (rowLoc > 0) {
            paintTiles(getTileAt(rowLoc - 1, colLoc), newColor, propagateAlong, rowLoc - 1, colLoc); // Paint upwards
        } 
        if (colLoc < numCols - 1) {
            paintTiles(getTileAt(rowLoc, colLoc + 1), newColor, propagateAlong, rowLoc, colLoc + 1); // Paint rightwards
        }
        if (colLoc > 0) {
            paintTiles(getTileAt(rowLoc, colLoc - 1), newColor, propagateAlong, rowLoc, colLoc - 1); // Paint leftwards
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
