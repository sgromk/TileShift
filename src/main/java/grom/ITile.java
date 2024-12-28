package grom;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

public interface ITile {
    int getNumDots();
    boolean matchingFirstColor(String matchColor);  // Checks if a given color matches the tiles first color
    String getFirstColor(); // Returns the first color of the tile
    void useDot();          // Reduces the number of dots by 1
    Tile copy();            // Returns a deep copy of the given List<List<Tile>>
    boolean isWall();       // Returns true if the tile is a WallTile
    boolean isPaintable();  // Returns true if the tile is paintable (i.e., it is not an Empty or Wall tile)
    boolean isEmpty();      // Returns true if the tile is an EmptyTile
    void paint(String newColor, String paintingAlong);  // If the conditions are met, paints over the old color and any nested colors that match
}


/**
 * The tile that builds the Puralax level, it has list of strings representing colors
 * and an int representing 0 or more dots
 */
class Tile implements ITile {
    private List<String> colors;
    private int numDots;

    public Tile(List<String> colors, int numDots) {
        if (numDots < 0) {
            throw new IllegalArgumentException("Number of dots cannot be negative.");
        } else if (colors.size() < 1) {
            throw new IllegalArgumentException("Must have at least one color.");
        }

        this.colors = colors;
        this.numDots = numDots;
    }

    /**
     * Return the number of dots or -1 for an empty tile
     * @return the number of dots in the tile
     */
    public int getNumDots() {
        return numDots;
    }

    /**
     * Returns true if the first color of the tile matches the given color
     * @param matchColor the color to be matched against
     * @return true if the first color of the tile and the given color are the same, false otherwise
     */
    public boolean matchingFirstColor(String matchColor) {
        return matchColor.equals(colors.get(0));
    }

    public String getFirstColor() {
        return colors.get(0);
    }

    /**
     * If the tile has dots, subtracts one
     */
    public void useDot() {
        if (numDots < 1) {
            throw new IllegalArgumentException("Using move on tile with no dots.");
        } else {numDots -= 1;}
    }

    public Tile copy() {
        return new Tile(this.colors, this.numDots);
    }

    public boolean isWall() {
        return false;
    }

    public boolean isPaintable() {
        return true;
    }

    public boolean isEmpty() {
        return false;
    }

    /**
     * If the tile is the color being painted along, and it does not already have the
     * new color, paint itself then repeat for any nested colors
     * @param newColor The color to be painted to
     * @param paintingAlong The color to be painted from
     */
    public void paint(String newColor, String paintingAlong) {
        if (getFirstColor().equals(newColor) || !getFirstColor().equals(paintingAlong)) {
            return;
        } else {
            for (int i = 0; i < colors.size(); i++) {
                if (colors.get(i).equals(paintingAlong)) {  // Paints as many nested colors deep as
                    colors.set(i, newColor);                // matches the paintingAlong color
                } else {break;}
            }
        };
    }

    @Override
    /**
     * toString method that returns either the color or the color 
     * with the number of dots in parantheses
     */
    public String toString() {
        if (numDots > 0) {
            return colors + "(" + numDots + ") ";
        } else {return colors + "    ";}
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (! (obj instanceof Tile)) {return false;}
        else {
            Tile that = (Tile) obj;
            return this.colors.equals(that.colors)
                && this.numDots == that.numDots;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(colors, numDots);
    }
}

/**
 * Representing an empty tile, with no color and no dots
 */
class EmptyTile extends Tile {
    EmptyTile() {
        super(new ArrayList<>(List.of("LG")), 0);   // Light gray
    }

    @Override
    public String toString() {
        return "_    ";
    }

    @Override
    public Tile copy() {
        return new EmptyTile();
    }

    @Override
    public boolean isPaintable() {
        return false;
    }

    @Override
    // Used to determine if another tile can move to this position
    public boolean isEmpty() {
        return true;
    }

    @Override
    // Empty tiles are not paintable
    public void paint(String newColor, String paintingAlong) {
        return;
    }

    @Override
    public boolean matchingFirstColor(String matchColor) {
        return false;
    }
}

/**
 * Representing a non-traversable, non-paintable tile
 */
class WallTile extends Tile {
    WallTile() {
        super(new ArrayList<>(List.of("DG")), 0);  // Dark gray
    }

    @Override
    public String toString() {
        return "|   ";
    }

    @Override
    public Tile copy() {
        return new WallTile();
    }

    @Override
    public boolean isWall() {
        return true;
    }

    @Override
    public boolean isPaintable() {
        return false;
    }
    
    @Override
    // Wall tiles are not paintable
    public void paint(String newColor, String paintingAlong) {
        return;
    }

    @Override
    public boolean matchingFirstColor(String matchColor) {
        return false;
    }
}