/**
 * Loads game levels from a JSON file (levels.json) on the classpath.
 * Each level contains a goal color, board size, and a 2D array of tile definitions.
 * Used by GameController to provide predesigned levels for the level select UI.
 *
 * Example JSON structure:
 * [
 *   {
 *     "goalColor": "G",
 *     "rows": 3,
 *     "cols": 3,
 *     "tiles": [
 *       [ { "color": "G", "dots": 1 }, { "color": "B", "dots": 0 }, { "type": "empty" } ],
 *       ...
 *     ]
 *   }, ...
 * ]
 */
package grom;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.*;
import com.google.gson.*;

/**
 * Loader for levels.json. Provides access to all levels and conversion to in-game board.
 */
public class LevelLoader {
    /**
     * Internal representation of a tile in JSON.
     * If type is "wall" or "empty", color/dots are ignored.
     */
    private static class TileDef {
        String color;
        Integer dots;
        String type;
    }
    /**
     * Internal representation of a level in JSON.
     */
    private static class LevelDef {
        String goalColor;
        int rows;
        int cols;
        TileDef[][] tiles;
    }

    private List<LevelDef> levels = new ArrayList<>();

    /**
     * Loads all levels from /levels.json on the classpath.
     * Throws RuntimeException if loading or parsing fails.
     */
    public LevelLoader() {
        try (InputStream in = getClass().getResourceAsStream("/levels.json")) {
            if (in == null) throw new RuntimeException("levels.json not found");
            InputStreamReader reader = new InputStreamReader(in);
            Gson gson = new Gson();
            LevelDef[] arr = gson.fromJson(reader, LevelDef[].class);
            levels.addAll(Arrays.asList(arr));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load levels.json", e);
        }
    }

    /**
     * @return the number of levels loaded from levels.json
     */
    public int getLevelCount() { return levels.size(); }

    /**
     * Returns the loaded level at the given index (0-based).
     * @param idx the level index (0-based)
     * @return LoadedLevel containing board, goal color, and size
     * @throws IndexOutOfBoundsException if idx is invalid
     */
    public LoadedLevel getLevel(int idx) {
        LevelDef def = levels.get(idx);
        List<List<Tile>> board = new ArrayList<>();
        for (int r = 0; r < def.rows; r++) {
            List<Tile> row = new ArrayList<>();
            for (int c = 0; c < def.cols; c++) {
                TileDef t = def.tiles[r][c];
                if (t == null || "empty".equals(t.type)) {
                    row.add(TileBuilder.empty());
                } else if ("wall".equals(t.type)) {
                    row.add(TileBuilder.wall());
                } else {
                    row.add(TileBuilder.builder().color(t.color).dots(t.dots == null ? 0 : t.dots).build());
                }
            }
            board.add(row);
        }
        return new LoadedLevel(def.goalColor, def.rows, def.cols, board);
    }

    /**
     * Represents a fully loaded level: board, goal color, and dimensions.
     */
    public static class LoadedLevel {
        public final String goalColor;
        public final int rows, cols;
        public final List<List<Tile>> board;
        public LoadedLevel(String goalColor, int rows, int cols, List<List<Tile>> board) {
            this.goalColor = goalColor;
            this.rows = rows;
            this.cols = cols;
            this.board = board;
        }
    }
}