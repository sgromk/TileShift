# Puralax

A Java implementation of the puzzle game Puralax, featuring procedurally generated levels and a clean MVC architecture. Built with Java Swing for the GUI and includes a sophisticated level generator with solvability guarantees.

## Game Rules

Puralax is a color-based puzzle game where the objective is to paint all tiles on the board to match a target color:

- **Colored tiles** can have dots indicating remaining moves
- **Click a tile** to select it, then click an adjacent tile to move
- **Moving to empty space**: The tile relocates and consumes one dot
- **Moving to a different color**: Triggers flood-fill painting, converting all connected tiles of that color to your tile's color
- **Win condition**: All colored tiles match the goal color
- **Lose condition**: No legal moves remain and the board is not solved

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+

### Building and Running

```bash
# Compile the project
mvn clean compile

# Run the game
mvn exec:java

# Run tests
mvn test
```

### Playing

1. Launch the application to see the main menu
2. Choose "Play Level" to select from pre-made levels
3. Choose "Level Creator" to design custom levels
4. Choose "Generate Level" to create procedural puzzles (2x2 to 7x7 grids)

## Architecture

### MVC Design Pattern

- **Model** ([BasePuralax.java](src/main/java/grom/BasePuralax.java)): Core game logic, state management, move validation, and win/loss detection
- **View** ([PuralaxGraphicalView.java](src/main/java/grom/PuralaxGraphicalView.java)): Java Swing GUI with multiple screens (title, level select, creator, gameplay)
- **Controller** ([GameController.java](src/main/java/grom/GameController.java)): Mediates user input, coordinates model updates and view refreshes

### Key Components

**Tile Hierarchy** ([ITile.java](src/main/java/grom/ITile.java), [TileBuilder.java](src/main/java/grom/TileBuilder.java))
- Interface-based design with `ColorTile`, `EmptyTile`, and `WallTile`
- Builder pattern for flexible tile construction

**Level System**
- [LevelLoader.java](src/main/java/grom/LevelLoader.java): JSON-based level loading using Gson
- [LevelGenerator.java](src/main/java/grom/LevelGenerator.java): Procedural generation with BFS solvability verification
- [levels.json](src/main/resources/levels.json): Hand-crafted level collection

**Game Logic**
- Iterative flood-fill painting (BFS-based, avoids stack overflow)
- Deep copying for move simulation and level replay
- Comprehensive move validation

## Testing

Test suite includes:
- **MovementTests**: Core game mechanics and tile movement
- **PaintPropagationTest**: Flood-fill correctness
- **ModelBuilderTest**: Tile construction patterns
- **SolvabilityTest**: Generated level validation

## Level Generation

The procedural generator uses a sophisticated approach:

1. **Reverse-move strategy**: Starts with goal-colored tiles, applies reverse paint operations to create solvable puzzles
2. **BFS verification**: Every generated level is validated for solvability (explores up to 100k game states)
3. **Difficulty scoring**: Heuristic based on walls, colored tiles, dots, and spatial distribution
4. **Constraint validation**: Ensures minimum complexity, legal moves, and playability

## Dependencies

- **Gson 2.10.1**: JSON serialization for level data
- **JUnit 4.11**: Unit testing framework

## Technical Highlights

- **Guaranteed solvable levels**: BFS forward solver verifies every generated puzzle
- **Stack-safe flood-fill**: Iterative implementation handles large connected components
- **Seeded RNG**: Reproducible level generation for debugging and sharing
- **Clean separation of concerns**: MVC architecture with minimal coupling
