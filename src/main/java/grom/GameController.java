package grom;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

// TODO: Add a 'select tile' visual (maybe dots disappear)
// TODO: Refactor, reintroduce builder, improve separation of tasks between controller - builder - gui
// TODO: Move ALL logic handling from GUI to controller -> GUI needs to know NOTHING about the game


public class GameController {
    private Scanner scanner;
    private BasePuralax game;
    private PuralaxGraphicalView graphicalView;
    private boolean hasQuit;
    public int currentLevelRows;
    public int currentLevelCols;
    public List<List<Tile>> currentLevel;

    GameController() {
        this.scanner = new Scanner(System.in);
        this.game = new BasePuralax();
        this.hasQuit = false;
        this.currentLevel = new ArrayList<>();
        initializeEmptyLevel();
        this.currentLevelRows = 2;
        this.currentLevelCols = 2;
        this.graphicalView = new PuralaxGraphicalView(game, this);
    }

    public void playGame() {
        // TODO: get goal color

        System.out.println("Game starting");
        
        // PuralaxBuilder builder = new PuralaxBuilder();        // Create a level builder
        // List<List<Tile>> currentLevel = builder.buildBoard(userLevel, userRows, userCols); // Build the level

        // playGameHelper(currentLevel, currentLevelRows, currentLevelCols, "G");    // After getting inputs, call the game with a helper to allow
                                                                            // retrying to level on failure
    }

    public void startGame() {
        playGameHelper(currentLevel, currentLevelRows, currentLevelCols, "G");    // After getting inputs, call the game with a helper to allow
                                                                            // retrying to level on failure
    }

    /**
     * Given a constructed level, continues prompting the user for moves until the game is over.
     * The game is over if the player has won, or if there are no more moves and the player has
     * lost. Makes a copy of the given level to avoid mutating the original level, to allow for
     * replaying if the player loses. 
     * @param currentLevel The given level to be played, as a List<List<Tile>>
     * @param userRows The number of rows specified for the level
     * @param userCols The number of columns specified for the level
     * @param userGoalColor The goal color to win
     */
    private void playGameHelper(List<List<Tile>> currentLevel, int userRows, int userCols, String userGoalColor) {
        List<List<Tile>> copyLevel = copyBoard(currentLevel);
        game.startGame(copyLevel, userRows, userCols, userGoalColor);    // Start the game with the created level

        graphicalView.updateView();
        //while (!game.isGameOver() && !hasQuit) {            // Keep playing until the game ends or the user quits
        //    graphicalView.updateView();                     // Render the board graphical view
        //    String test = scanner.next();
       // }

        if (game.isGameOver() && !hasQuit) {                // If the user failed the level
            graphicalView.updateView();
            if (!game.isGameWon()) {
                System.out.println("Try the level again? Y/N:");    // Ask to replay the level if they lost
                String tryAgain = scanner.next();
                if (tryAgain.equals("Y") || tryAgain.equals("y")) {
                    playGameHelper(currentLevel, userRows, userCols, userGoalColor);    // Start a new game
                }
            } else {System.out.println("Thanks for playing.");
                    System.exit(0);
                }
        }
    }

    /**
     * Prints a quit message and exits the level.
     */
    private void quitGame() {
        hasQuit = true;
        System.out.println("Game aborted.");
        System.exit(0);
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
        GameController controller = new GameController();
        controller.playGame();
    }
}
