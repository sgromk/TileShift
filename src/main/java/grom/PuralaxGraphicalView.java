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
    private final Map<SelectedTileState, JPanel> paletteMap = new HashMap<>();

    public PuralaxGraphicalView(BasePuralax model, GameController gameController) {
        this.model = model;
        this.gameController = gameController;
        this.currentSelectedState = SelectedTileState.EMPTY_TILE;
        this.currentSelectedDots = 0;

        // Set up board
        setTitle("TileShift");
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

    /**
     * Renders the level select page, with a back button and a grid of 9 selectable levels.
     */
    private void showLevelSelectPage() {
        resetPage();

        boardPanel.setLayout(new BorderLayout());
        boardPanel.setBackground(PuralaxConstants.COLOR_CREAM);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(PuralaxConstants.COLOR_CREAM);
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton back = levelCreatorButton("Back", PuralaxConstants.COLOR_ORANGE);
        back.addActionListener(e -> showTitlePage());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.setOpaque(false);
        left.add(back);
        topPanel.add(left, BorderLayout.WEST);

        JLabel title = new JLabel("Select a Level", SwingConstants.CENTER);
        title.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(45f));
        title.setForeground(PuralaxConstants.COLOR_MEDIUM_GRAY);
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER));
        center.setOpaque(false);
        center.add(title);
        topPanel.add(center, BorderLayout.CENTER);

        // Add a right placeholder to visually balance the left back button so the title is centered
        Dimension backSize = back.getPreferredSize();
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(backSize.width + 20, backSize.height));
        topPanel.add(right, BorderLayout.EAST);

        boardPanel.add(topPanel, BorderLayout.NORTH);

        // Center: 3x3 grid for levels 1..9
        JPanel grid = new JPanel(new GridLayout(3, 3, 20, 20));
        grid.setBorder(new EmptyBorder(20, 40, 20, 40));
        grid.setBackground(PuralaxConstants.COLOR_CREAM);

        for (int i = 1; i <= 9; i++) {
            int idx = i; // capture
            JButton levelBtn = new JButton(String.valueOf(i));
            levelBtn.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(22f));
            // Larger preferred square size; button will be centered inside its grid cell
            Dimension square = new Dimension(140, 140);
            levelBtn.setPreferredSize(square);
            levelBtn.setBackground(PuralaxConstants.COLOR_LIGHT_GRAY);
            levelBtn.setFocusPainted(false);
            levelBtn.setMargin(new Insets(0, 0, 0, 0));
            levelBtn.addActionListener(e -> gameController.startPreset(idx));

            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(false);
            cell.add(levelBtn);
            grid.add(cell);
        }

        boardPanel.add(grid, BorderLayout.CENTER);
        revalidate();
        repaint();
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
        boardPanel.setLayout(new BorderLayout());
        boardPanel.setBackground(PuralaxConstants.COLOR_CREAM);

        // Top: show the current target color
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 6));
        topPanel.setOpaque(false);
        JLabel targetLabel = new JLabel("Target ");
        targetLabel.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(18f));
        topPanel.add(targetLabel);

        JPanel targetTile = new JPanel();
        targetTile.setPreferredSize(new Dimension(36, 36));
        targetTile.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        String goal = model.getGoalColor();
        if (goal == null || goal.isEmpty()) { goal = "G"; }
        targetTile.setBackground(TileColors.getColor(goal));
        topPanel.add(targetTile);

        boardPanel.add(topPanel, BorderLayout.NORTH);

        // Center: tile grid panel
        JPanel tileGridPanel = new JPanel();
        tileGridPanel.setBackground(PuralaxConstants.COLOR_CREAM);
        // Ensure the tile grid has a known preferred size so populateTileGrid can compute tile sizes
        tileGridPanel.setPreferredSize(new Dimension(PuralaxConstants.WIDTH, PuralaxConstants.HEIGHT - 80));
        populateTileGrid(model.getNumRows(), model.getNumCols(), tileGridPanel, model.getBoard()); // Display the entire screen as a level
        boardPanel.add(tileGridPanel, BorderLayout.CENTER);

        // Refresh the board with the added tiles
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    /**
     * Calls renderBoardGraphic and decides if an end of game message should be displayed
     */
    public void updateView() {
        renderPlayableLevel();

        // Only show end-of-game UI when a game has actually been started
        if (model.isGameStarted() && model.isGameOver()) {
            String endGameMsg = model.isGameWon() ? "You win!" : "Level failed.";

            // Create a modal dialog with vertically stacked buttons for Main Menu and Level Select
            JDialog dialog = new JDialog(this, "Game Over", true);
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(new EmptyBorder(10, 10, 10, 10));

            JLabel msgLabel = new JLabel(endGameMsg, SwingConstants.CENTER);
            msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            msgLabel.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(16f));
            content.add(msgLabel);
            content.add(Box.createRigidArea(new Dimension(0, 12)));

            // Buttons panel (vertical)
            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
            buttons.setOpaque(false);

            JButton mainMenu = new JButton("Main Menu");
            mainMenu.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainMenu.addActionListener(e -> {
                dialog.dispose();
                showTitlePage();
            });
            buttons.add(mainMenu);
            buttons.add(Box.createRigidArea(new Dimension(0, 8)));

            JButton levelSelect = new JButton("Level Select");
            levelSelect.setAlignmentX(Component.CENTER_ALIGNMENT);
            levelSelect.addActionListener(e -> {
                dialog.dispose();
                showLevelSelectPage();
            });
            buttons.add(levelSelect);

            content.add(buttons);

            dialog.setContentPane(content);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
    }

    /**
     * Show an informational dialog to the user.
     */
    public void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Show an error dialog to the user.
     */
    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void populateTileGrid(int numRows, int numCols, JPanel tileGridPanel, List<List<Tile>> graphicalLevel) {
        tileGridPanel.removeAll();
        tileGridPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2,2,2,2);

        Dimension size = tileGridPanel.getSize();
        // If the panel hasn't been laid out yet, fall back to preferred size
        if (size.width <= 0 || size.height <= 0) {
            size = tileGridPanel.getPreferredSize();
        }
        // If still zero (no preferred size), fall back to constants
        if (size.width <= 0 || size.height <= 0) {
            size = new Dimension(PuralaxConstants.WIDTH, PuralaxConstants.HEIGHT - 80);
        }
        // Determine the tile dimension by the smallest dimension available
        int tileWidth = Math.max(1, size.width / Math.max(1, numCols));
        int tileHeight = Math.max(1, size.height / Math.max(1, numRows));
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

        JPanel tilePanel;
        if (currentTile.isWall()) {
            // Custom panel that draws diagonal hatch lines to indicate a wall
            tilePanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int w = getWidth();
                    int h = getHeight();
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        // Fill background (super.paintComponent already fills when opaque)
                        g2.setColor(getBackground());
                        g2.fillRect(0, 0, w, h);

                        // Draw diagonal hatch lines in a lighter gray
                        g2.setColor(new Color(120, 120, 120));
                        g2.setStroke(new BasicStroke(2f));
                        int spacing = 12;
                        // Draw lines from left-top towards bottom-right across the panel
                        for (int x = -h; x < w; x += spacing) {
                            g2.drawLine(x, 0, x + h, h);
                        }
                    } finally {
                        g2.dispose();
                    }
                }
            };
            tilePanel.setBackground(PuralaxConstants.COLOR_DARK_GRAY);
            tilePanel.setOpaque(true);
        } else {
            tilePanel = new JPanel(new BorderLayout());
            tilePanel.setBackground(TileColors.getColor(tileColor));
        }
        // If this tile is currently selected in play mode, show a thin black border to highlight it
        if (gameController.isTileSelected(row, col)) {
            tilePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        } else {
            tilePanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        }

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
                if (model.isGameStarted()) {
                    // In play mode, route clicks to controller for selection/move handling
                    gameController.onTileClicked(row, col);
                } else {
                    // In level-creator mode, replace the tile with the currently selected palette tile
                    replaceTileInGrid(row, col, tileGridPanel);
                }
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
                replacementTile = TileBuilder.builder().color("G").dots(currentSelectedDots).build();
                break;
            case BLUE_TILE:
                replacementTile = TileBuilder.builder().color("B").dots(currentSelectedDots).build();
                break;
            case YELLOW_TILE:
                replacementTile = TileBuilder.builder().color("Y").dots(currentSelectedDots).build();
                break;
            case RED_TILE:
                replacementTile = TileBuilder.builder().color("R").dots(currentSelectedDots).build();
                break;
            case PURPLE_TILE:
                replacementTile = TileBuilder.builder().color("P").dots(currentSelectedDots).build();
                break;
            case WALL_TILE:
                replacementTile = new WallTile();
                break;
        }
        // Route the change through the controller so it owns model updates
        gameController.setTileAt(row, col, replacementTile);

        // Re-populate the level-creator tile grid so edits are immediately visible
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
        JLabel titleLabel = new JLabel("TileShift", SwingConstants.CENTER);
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
        JButton levelSelectButton = createTitlePageButton("Level Select", PuralaxConstants.COLOR_BLUE, e -> showLevelSelectPage());
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
        // Ensure model is not in a started/finished game state when entering level creator
        gameController.resetGameState();
        // Reset the controller's currentLevel to a 3x3 empty grid
        gameController.currentLevelRows = 3;
        gameController.currentLevelCols = 3;
        gameController.currentLevel = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            List<Tile> row = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                row.add(new EmptyTile());
            }
            gameController.currentLevel.add(row);
        }

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

        // Target selector (center top)
        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        targetPanel.setOpaque(false);
        JLabel targetLabel = new JLabel("Target ");
        targetLabel.setFont(PuralaxConstants.BUTTON_FONT.deriveFont(25f));
        targetPanel.add(targetLabel);

        // Small tile showing current target color (slightly larger than palette tiles)
        JPanel targetTile = new JPanel();
        targetTile.setPreferredSize(new Dimension(40, 40));
        targetTile.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        // default target is green
        targetTile.setBackground(PuralaxConstants.COLOR_GREEN);

        targetTile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Only change target to a paintable color (no empty or wall)
                if (currentSelectedState == SelectedTileState.EMPTY_TILE || currentSelectedState == SelectedTileState.WALL_TILE) {
                    return;
                }
                // Map SelectedTileState to color code
                String code = switch (currentSelectedState) {
                    case GREEN_TILE -> "G";
                    case BLUE_TILE -> "B";
                    case YELLOW_TILE -> "Y";
                    case RED_TILE -> "R";
                    case PURPLE_TILE -> "P";
                    default -> null;
                };
                if (code != null) {
                    // Update visual and inform controller
                    targetTile.setBackground(TileColors.getColor(code));
                    gameController.setGoalColor(code);
                }
            }
        });

        targetPanel.add(targetTile);
        topPanel.add(targetPanel);

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

            // Start with a thin border for all palette tiles
            paletteTile.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            // Add a mouse listener to select the tile and update visuals
            paletteTile.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    currentSelectedState = COLOR_TO_STATE_MAP.get(tileColor.color);
                    updatePaletteSelection();
                }
            });

            // Keep a reference so we can update its border when selection changes
            paletteMap.put(COLOR_TO_STATE_MAP.get(tileColor.color), paletteTile);

            inputPanel.add(paletteTile); // Add them before rows and cols
        }
        // Initialize palette visuals according to the currentSelectedState
        updatePaletteSelection();
        
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

    // Update palette tile borders to show which palette color is selected
    private void updatePaletteSelection() {
        for (Map.Entry<SelectedTileState, JPanel> entry : paletteMap.entrySet()) {
            SelectedTileState state = entry.getKey();
            JPanel panel = entry.getValue();
            if (state == currentSelectedState) {
                panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 4));
            } else {
                panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            }
            panel.revalidate();
            panel.repaint();
        }
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
