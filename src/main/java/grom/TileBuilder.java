package grom;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder/factory for creating Tile instances.
 * Use TileBuilder.builder().color("G").dots(1).build() for fluent creation.
 */
public class TileBuilder {
    private List<String> colors = new ArrayList<>();
    private int dots = 0;

    private TileBuilder() {}

    public static TileBuilder builder() {
        return new TileBuilder();
    }

    public TileBuilder color(String color) {
        this.colors.add(color);
        return this;
    }

    public TileBuilder colors(List<String> colors) {
        this.colors = new ArrayList<>(colors);
        return this;
    }

    public TileBuilder dots(int dots) {
        this.dots = dots;
        return this;
    }

    public Tile build() {
        return new Tile(new ArrayList<>(this.colors), this.dots);
    }

    public static Tile empty() {
        return new EmptyTile();
    }

    public static Tile wall() {
        return new WallTile();
    }
}
