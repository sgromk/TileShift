package grom;

import java.util.List;
import java.util.ArrayList;


/**
 * Constructs the level by parsing the user textual input
 */
class PuralaxBuilder {
    private TileFactory tileFactory;

    public PuralaxBuilder() {
        this.tileFactory = new TileFactory();
    }

    // Create the nested ArrayList board data structure by parsing the user input
    public List<List<Tile>> buildBoard(String input, int numRows, int numCols) {
        boolean builtSuccessfully = false;
        List<List<Tile>> board = new ArrayList<>();
        while (!builtSuccessfully) {
            board = new ArrayList<>();
            String[] rows = input.split("\n");      // Separate user input into rows

            if (rows.length != numRows) {
                System.out.println("Invalid input. Row count mismatch.");
                break;
            }

            for (int i = 0; i < numRows; i++) {
                String[] cells = rows[i].split("\\s+");
                if (cells.length != numCols) {
                    System.out.println("Invalid input. Column count mismatch.");
                    break;
                }

                List<Tile> row = new ArrayList<>();             // Read the user input and add each tile to the row
                for (String cell : cells) {
                    Tile tile = parseTile(cell);
                    row.add(tile);
                }
                board.add(row);     // Add each row to the board
            }
            builtSuccessfully = true;   // Flag to re-try level input on fail
    }
        return board;       // Return the board
    }

    /**
     * Determines what kind of tile should be created
     * @param cell the user string describing the tile to create
     * @return a Tile object, parsed from the user input
     */
    private Tile parseTile(String cell) {
        if (cell.trim().isEmpty() || cell.equals("_")) {
            return tileFactory.createEmpty();               // Identify empty tiles, wall tiles, or regular tiles
        } else if (cell.equals("|")) {
            return tileFactory.createWall();
        }

        String color = cell.substring(0, 1);
        int numDots = 0;

        if (cell.length() > 1 && cell.charAt(1) == '(') {           // For regular tiles, identify if there are any dots
            int endIndex= cell.indexOf(')');
            numDots = Integer.parseInt(cell.substring(2, endIndex));
        }

        return tileFactory.createTile(color, numDots);
    }
}


// TODO: Add support for nested tiles
/**
 * Handles actual tile object creation
 */
class TileFactory {
    public Tile createTile(String color, int numDots) {
        if (color == null || color.isEmpty()) {
            return new EmptyTile();
        }
        return new Tile(new ArrayList<>(List.of(color)), numDots);
    }

    public Tile createEmpty() {
        return new EmptyTile();
    }

    public Tile createWall() {
        return new WallTile();
    }
}