package grom;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

    // Have related paletteTile "highlight" based on currentSelectedState
    // Make the base model trigger an event modelChanged() to tell View to refresh
    // All listeners should forward the action to a controller method - NOT a viewer method

/**
 * Creates a graphical view of the game using Java AWT and Swing. The pages include a title page,
 * a level select, level creator, and active-game page. The board updates every time a move is played.
 */
public class PuralaxGraphicalView extends JFrame {
    private final BasePuralax model;
    private JPanel boardPanel;
    private final GameController gameController;
    private SelectedTileState currentSelectedState;
    private int currentSelectedDots;

    public PuralaxGraphicalView(BasePuralax model, GameController gameController) {
        this.model = model;
        this.gameController = gameController;
        this.currentSelectedState = SelectedTileState.EMPTY_TILE;
        this.currentSelectedDots = 0;

        // Set up board
        setTitle("Puralax");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        boardPanel = new JPanel();
        boardPanel.setPreferredSize(new Dimension(PuralaxConstants.WIDTH, PuralaxConstants.HEIGHT));
        add(boardPanel, BorderLayout.CENTER);

        // Rendering the board right after initialization. Opens to title page.
        showTitlePage();

        pack();
        setVisible(true);
    }

    // Enum to define tile colors from 
    public enum TileColors {
        GREEN("G", PuralaxConstants.COLOR_GREEN),
        BLUE("B", PuralaxConstants.COLOR_BLUE),
        YELLOW("Y", PuralaxConstants.COLOR_YELLOW),
        RED("R", PuralaxConstants.COLOR_RED),
        PURPLE("P", PuralaxConstants.COLOR_PURPLE),
        LIGHT_GRAY("LG", PuralaxConstants.COLOR_LIGHT_GRAY),
        DARK_GRAY("DG", PuralaxConstants.COLOR_DARK_GRAY);

        private final String colorCode;
        private final Color color;

        TileColors(String colorCode, Color color) {
            this.colorCode = colorCode;
            this.color = color;
        }

        public static Color getColor(String colorCode) {
            for (TileColors tileColor : values()) {
                if (tileColor.colorCode.equals(colorCode)) {
                    return tileColor.color;
                }
            }
            return Color.BLACK; // Default color if not found in enum
        }
    }

    private enum SelectedTileState {
        EMPTY_TILE,
        GREEN_TILE,
        BLUE_TILE,
        YELLOW_TILE,
        RED_TILE,
        PURPLE_TILE,
        WALL_TILE;
    }

    private static final Map<Color, SelectedTileState> COLOR_TO_STATE_MAP = new HashMap<>();
    static {
        COLOR_TO_STATE_MAP.put(PuralaxConstants.COLOR_GREEN, SelectedTileState.GREEN_TILE);
        COLOR_TO_STATE_MAP.put(PuralaxConstants.COLOR_BLUE, SelectedTileState.BLUE_TILE);
        COLOR_TO_STATE_MAP.put(PuralaxConstants.COLOR_YELLOW, SelectedTileState.YELLOW_TILE);
        COLOR_TO_STATE_MAP.put(PuralaxConstants.COLOR_RED, SelectedTileState.RED_TILE);
        COLOR_TO_STATE_MAP.put(PuralaxConstants.COLOR_PURPLE, SelectedTileState.PURPLE_TILE);
        COLOR_TO_STATE_MAP.put(PuralaxConstants.COLOR_LIGHT_GRAY, SelectedTileState.EMPTY_TILE);
        COLOR_TO_STATE_MAP.put(PuralaxConstants.COLOR_DARK_GRAY, SelectedTileState.WALL_TILE);
    }

    private void renderPlayableLevel() {
        boardPanel.removeAll();
        populateTileGrid(model.getNumRows(), model.getNumCols(), boardPanel, model.getBoard()); // Display the entire screen as a level

        // Refresh the board with the added tiles
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    /**
     * Calls renderBoardGraphic and decides if an end of game message should be displayed
     */
    public void updateView() {
        renderPlayableLevel();

        if (model.isGameOver()) {
            if (model.isGameOver()) {
                String endGameMsg = model.isGameWon() ? "You win!" : "Level failed.";
                JOptionPane.showMessageDialog(this, endGameMsg, "Game Over", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void populateTileGrid(int numRows, int numCols, JPanel tileGridPanel, List<List<Tile>> graphicalLevel) {
        tileGridPanel.removeAll();
        tileGridPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2,2,2,2);

        Dimension size = tileGridPanel.getSize();
        // Determine the tile dimension by the smallest dimension available
        int tileWidth = size.width / numCols;
        int tileHeight = size.height / numRows;
        int tileSize = Math.min(tileWidth, tileHeight);
        int reducedSize = (int) (tileSize / 1.4);

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                Tile currentTile = graphicalLevel.get(i).get(j);
                JPanel tilePanel = createTilePanel(currentTile, i, j, tileGridPanel);
                tilePanel.setPreferredSize(new Dimension(tileSize, tileSize));
                tilePanel.setMinimumSize(new Dimension(reducedSize, reducedSize));
                tilePanel.setMaximumSize(new Dimension(tileSize, tileSize));

                gbc.gridx = j;
                gbc.gridy = i;
                gbc.weightx = 1.0; // Give equal weight horizontally
                gbc.weighty = 1.0; // Give equal weight vertically

                tileGridPanel.add(tilePanel, gbc);
            }
        }

        tileGridPanel.revalidate();
        tileGridPanel.repaint();
    }

    private JPanel createTilePanel(Tile currentTile, int row, int col, JPanel tileGridPanel) {
        String tileColor = currentTile.getFirstColor();
        int tileNumDots = currentTile.getNumDots();

        JPanel tilePanel = new JPanel(new BorderLayout());
        tilePanel.setBackground(TileColors.getColor(tileColor));
        //tilePanel.setBorder(BorderFactory.createLineBorder(PuralaxConstants.COLOR_MEDIUM_GRAY));

        if (tileNumDots > 0) {
            String repeatedDots = "• ".repeat(tileNumDots);
            JLabel dotLabel = new JLabel(repeatedDots, SwingConstants.RIGHT);
            dotLabel.setFont(new Font("Franklin Gothic Medium", Font.PLAIN, 30));
            dotLabel.setForeground(Color.BLACK);
            tilePanel.add(dotLabel, BorderLayout.NORTH);
        }

        tilePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                replaceTileInGrid(row, col, tileGridPanel);
            }
        });

        return tilePanel;
    }

    /**
     * Changes the selected tile to the tile specified by {@code currentSelectedState}
     * @param row The tile's row index
     * @param col The tile's column index
     */
    private void replaceTileInGrid(int row, int col, JPanel tileGridPanel) {
        Tile replacementTile = new EmptyTile();
        switch (currentSelectedState) {
            case EMPTY_TILE:
                break;
            case GREEN_TILE:
                replacementTile = new Tile(List.of("G"), currentSelectedDots);
                break;
            case BLUE_TILE:
                replacementTile = new Tile(List.of("B"), currentSelectedDots);
                break;
            case YELLOW_TILE:
                replacementTile = new Tile(List.of("Y"), currentSelectedDots);
                break;
            case RED_TILE:
                replacementTile = new Tile(List.of("R"), currentSelectedDots);
                break;
            case PURPLE_TILE:
                replacementTile = new Tile(List.of("P"), currentSelectedDots);
                break;
            case WALL_TILE:
                replacementTile = new WallTile();
                break;
        }
        gameController.currentLevel.get(row).set(col, replacementTile);

        populateTileGrid(gameController.currentLevelRows, gameController.currentLevelCols, 
                        tileGridPanel, gameController.currentLevel);
    }

    /**
     * Renders the game title page, with the game name in the upper center, and two buttons for
     * going to the level creator page or the level select page.
     */
    private void showTitlePage() {
        // Reset the page
        resetPage();

        boardPanel.setLayout(new BoxLayout(boardPanel, BoxLayout.Y_AXIS));
        boardPanel.setBackground(PuralaxConstants.COLOR_CREAM);

        // Adding padding at the top
        boardPanel.add(Box.createRigidArea(new Dimension(0, PuralaxConstants.HEIGHT / 6)));  // One-third down the title page

        // Add the main game name label
        JLabel titleLabel = new JLabel("PURALAX", SwingConstants.CENTER);
        titleLabel.setFont(PuralaxConstants.TITLE_FONT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center horizontally
        titleLabel.setForeground(PuralaxConstants.COLOR_TITLE_PAGE); // Pastel purple
        boardPanel.add(titleLabel);

        // Space padding below title
        boardPanel.add(Box.createRigidArea(new Dimension(0, 100)));

        // Level Creator button, with style parameters and alignment
        JButton levelCreatorButton = createTitlePageButton("Level Creator", PuralaxConstants.COLOR_GREEN, e -> showLevelCreatorPage());
        boardPanel.add(levelCreatorButton);

        // Spacing between buttons
        boardPanel.add(Box.createRigidArea(new Dimension(0, 100)));

        // Level Select button, with style parameters and alignment
        JButton levelSelectButton = createTitlePageButton("Level Select", PuralaxConstants.COLOR_BLUE, null);
        boardPanel.add(levelSelectButton);

        // Add the title panel to the frame
        add(boardPanel);

        // Refresh the frame
        revalidate();
        repaint();
    }

    // Helper function to place similar buttons on title page
    private JButton createTitlePageButton(String buttonText, Color bgColor, ActionListener buttonClick) {
        JButton button = new JButton(buttonText);
        button.setFont(PuralaxConstants.BUTTON_FONT);
        button.setForeground(PuralaxConstants.COLOR_MEDIUM_GRAY);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));
        button.setBorder(new EmptyBorder(20, 40, 20, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT); // Center horizontally
        button.addActionListener(buttonClick);
        button.setFocusPainted(false);                  // Prevent undesired focus box on button
        return button;
    }

    /**
     * Returns the panel to the base state. Removes all components, sets to vertical axis,
     * set background color
     */
    private void resetPage() {
        boardPanel.removeAll();
        boardPanel.setLayout(new BoxLayout(boardPanel, BoxLayout.Y_AXIS));
        boardPanel.setBackground(PuralaxConstants.COLOR_CREAM);
    }


    private void showLevelCreatorPage() {
        // Reset and configure boardPanel
        resetPage();

        // Create a panel to hold the back button and add it to the top of the boardPanel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setBackground(PuralaxConstants.COLOR_CREAM);
        topPanel.setBorder(new EmptyBorder(20, 20, 0, 20));
        
        // Create a back button and add it to the top left of the topPanel
        JButton backButton = levelCreatorButton("Back", PuralaxConstants.COLOR_ORANGE);
        backButton.addActionListener(e -> showTitlePage());
        topPanel.add(backButton);

        topPanel.add(Box.createHorizontalGlue());

        JButton playButton = levelCreatorButton("Play", PuralaxConstants.COLOR_GREEN);
        playButton.addActionListener(e -> gameController.startGame());
        topPanel.add(playButton);

        boardPanel.add(topPanel, BorderLayout.NORTH);   // Add panel to the top

        // Center panel to hold the interactive tile placing level builder
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(PuralaxConstants.COLOR_CREAM);

        JLabel tileBuilderLabel = new JLabel("Tile Builder Visual");
        tileBuilderLabel.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(18f));
        tileBuilderLabel.setHorizontalAlignment(JLabel.CENTER);
        
        centerPanel.add(tileBuilderLabel);

        boardPanel.add(centerPanel, BorderLayout.CENTER);

        // Create a panel for the inputs
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10)); 
        inputPanel.setBackground(PuralaxConstants.COLOR_CREAM);

        for (TileColors tileColor : TileColors.values()) {
            JPanel paletteTile = new JPanel();
            paletteTile.setBackground(tileColor.color);
            paletteTile.setPreferredSize(new Dimension(30, 30));
            paletteTile.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            // Add a mouse listener if needed to select the tile:
            paletteTile.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    currentSelectedState = COLOR_TO_STATE_MAP.get(tileColor.color);
                }
            });

            inputPanel.add(paletteTile); // Add them before rows and cols
        }
        
        // Create the "Rows" label and Spinner
        JLabel rowsLabel = new JLabel("Rows");
        rowsLabel.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(14f));
        JSpinner rowsSpinner = levelCreatorSpinner();
        setRowSpinnerAction(rowsSpinner, centerPanel);

        JPanel rowsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        rowsPanel.setBackground(PuralaxConstants.COLOR_CREAM);
        rowsPanel.add(rowsLabel);
        rowsPanel.add(rowsSpinner);

        // Create the "Columns" label and Spinner
        JLabel columnsLabel = new JLabel("Columns");
        columnsLabel.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(14f));
        JSpinner columnsSpinner = levelCreatorSpinner();
        setColSpinnerAction(columnsSpinner, centerPanel);

        JPanel columnsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        columnsPanel.setBackground(PuralaxConstants.COLOR_CREAM);
        columnsPanel.add(columnsLabel);
        columnsPanel.add(columnsSpinner);

        // Create the "Dots" lavel and Spinner
        JLabel dotsLabel = new JLabel("Dots");
        dotsLabel.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(14f));
        JSpinner dotsSpinner = levelCreatorSpinner();
        SpinnerNumberModel dotModel = (SpinnerNumberModel) dotsSpinner.getModel();
        dotModel.setMinimum(0);
        dotModel.setMaximum(5);
        setDotSpinnerAction(dotsSpinner);

        JPanel dotsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        dotsPanel.setBackground(PuralaxConstants.COLOR_CREAM);
        dotsPanel.add(dotsLabel);
        dotsPanel.add(dotsSpinner);

        // Add components to the input panel
        inputPanel.add(rowsPanel);
        inputPanel.add(columnsPanel);
        inputPanel.add(dotsPanel);

        boardPanel.add(inputPanel, BorderLayout.SOUTH);

        // Populate then repopulate the tile grid to ensure the tiles are sized correctly
        // This avoids using a timer that may cause the user to see the tile population happen
        this.revalidate();
        populateTileGrid(gameController.currentLevelRows, gameController.currentLevelCols, 
                        centerPanel, gameController.currentLevel);
        this.revalidate();
        populateTileGrid(gameController.currentLevelRows, gameController.currentLevelCols, 
                        centerPanel, gameController.currentLevel);
    }

    private JButton levelCreatorButton(String msg, Color buttonColor) {
        JButton backButton = new JButton(msg);
        backButton.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(22f));
        backButton.setForeground(PuralaxConstants.COLOR_MEDIUM_GRAY);
        backButton.setBackground(buttonColor);
        backButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        backButton.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));
        backButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        backButton.setFocusPainted(false);
        return backButton;
    }

    private JSpinner levelCreatorSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(2, 2, 7, 1));
        spinner.setPreferredSize(new Dimension(40, 32));

        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setEditable(false);
        editor.getTextField().setFocusable(false);
        return spinner;
    }

    private void setRowSpinnerAction(JSpinner rowsSpinner, JPanel tileGrid) {
        SpinnerNumberModel spinnerModel = (SpinnerNumberModel) rowsSpinner.getModel();
        spinnerModel.setValue(gameController.currentLevelRows);
        final int[] previousValue = {spinnerModel.getNumber().intValue()}; // To track the previous value

        rowsSpinner.addChangeListener(e -> {
            int currentValue = spinnerModel.getNumber().intValue();
            if (currentValue > previousValue[0]) {
                handleRowSpinnerIncrease();
            } else if (currentValue < previousValue[0]) {
                handleRowSpinnerDecrease();
            }
            previousValue[0] = currentValue; // Update the previous value
            populateTileGrid(gameController.currentLevelRows, gameController.currentLevelCols, 
                            tileGrid, gameController.currentLevel);
        });
    }

    private void setColSpinnerAction(JSpinner colsSpinner, JPanel tileGrid) {
        SpinnerNumberModel spinnerModel = (SpinnerNumberModel) colsSpinner.getModel();
        spinnerModel.setValue(gameController.currentLevelCols);
        final int[] previousValue = {spinnerModel.getNumber().intValue()}; // To track the previous value

        colsSpinner.addChangeListener(e -> {
            int currentValue = spinnerModel.getNumber().intValue();
            if (currentValue > previousValue[0]) {
                handleColSpinnerIncrease();
            } else if (currentValue < previousValue[0]) {
                handleColSpinnerDecrease();
            }
            previousValue[0] = currentValue; // Update the previous value
            populateTileGrid(gameController.currentLevelRows, gameController.currentLevelCols, 
                            tileGrid, gameController.currentLevel);
        });
    }

    private void setDotSpinnerAction(JSpinner dotsSpinner) {
        SpinnerNumberModel spinnerModel = (SpinnerNumberModel) dotsSpinner.getModel();
        spinnerModel.setValue(currentSelectedDots);

        dotsSpinner.addChangeListener(e -> {
            currentSelectedDots = spinnerModel.getNumber().intValue();
        });
    }

    /**
     * Adds a new row of EmptyTile, matching the number of columns in the first row
     */
    private void handleRowSpinnerIncrease() {
        List<Tile> newRow = new ArrayList<>();
        for (int i = 0; i < gameController.currentLevel.get(0).size(); i++) {
            newRow.add(new EmptyTile());
        }
        gameController.currentLevel.add(newRow);
        gameController.currentLevelRows += 1;
    }

    /**
     * Removes the last row
     */
    private void handleRowSpinnerDecrease() {
        gameController.currentLevel.remove(gameController.currentLevel.size() - 1);
        gameController.currentLevelRows -= 1;
    }

    /**
     * Adds a new column to each existing row
     */
    private void handleColSpinnerIncrease() {
        for (List<Tile> row : gameController.currentLevel) {
            row.add(new EmptyTile());
        }
        gameController.currentLevelCols += 1;
    }

    /**
     * Removes a columns from each existing row
     */
    private void handleColSpinnerDecrease() {
        for (List<Tile> row : gameController.currentLevel) {
            row.remove(row.size() - 1);
        }
        gameController.currentLevelCols -= 1;
    }


}
