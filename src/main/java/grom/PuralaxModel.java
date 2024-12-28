package grom;

import java.util.List;

// TODO: Come back and figure out if any more methods need to be added here

/**
 * The model for playing a game of Puralax: this maintains
 * the state and enforces rules of gameplay. 
 * @param <T> the type of tiles this model uses
 */
public interface PuralaxModel<T> {
    /**
     * Starts a new game of Puralax.
     * The tiles to build the level are specified by the given tiles list. 
     * Levels are build from left-to-right and top-to-bottom.
     * 
     * <p>This method first verifies that the number of rows and columns are valid, 
     * then that the correct number of tiles are provided.
     * 
     * <p>The game ends when the player has won or there are no possible
     * moves left
     * @param tiles the type of Tiles used to build the level
     * @param numRows the number of rows in the level
     * @param numColumns the number of columns in the level
     * @throws IllegalArgumentException if the rows or columns are negative, or if
     * there are not enough or too many tiles
     */
    void startGame(List<List<T>> tiles, int numRows, int numColumns, String goalColor);

    /**
     * <p>Performs a move from an initial tile to a destination file. 
     * If the destination is empty, move the initial tile and use a dot. If the destination is the
     * same color, do nothing. Otherwise, paint the destination tile and connected same-color tiles
     * and use a dot. 
     * 
     * <p>Does nothing if the initial tile has no dots
     * @param fromRow initial tile row (0-indexed)
     * @param fromCol initial tile column (0-indexed)
     * @param toRow destination tile row (0-indexed)
     * @param toCol destination tile column (0-indexed)
     */
    void moveTile(int fromRow, int fromCol, int toRow, int toCol);

    /**
     * Returns the tile at the specified coorindates.
     * @param row row of the desired tile (0-indexed from the top)
     * @param column column of the desire tile (0-indexed from the left)
     * @return the tile at the given position, or <code>null</code> if an empty tile
     * @throws IllegalStateException if the game hasn't been started yet
     * @throws IllegalArgumentException if the coordinates are invalid
     */
    T getTileAt(int row, int column) throws IllegalStateException;

    /**
     * Returns the number of rows originally in the level, or -1 if the game
     * hasn't been started
     * @return the height of the level, or -1
     */
    int getNumRows();

    /**
     * Returns the number of columns originally in the level, or -1 if the game
     * hasn't been started
     * @return the width of the level, or -1
     */
    int getNumCols();

    /**
     * Signal if the game is over or not, either from the player winning
     * or if ther are no playable moves left and the game is not won
     * @return true if the game is over, false otherwise
     * @throws IllegalStateException if the game hasn't been started yet
     */
    boolean isGameOver() throws IllegalStateException;
}
// TODO: Check if any methods from BasePuralax should be defined here