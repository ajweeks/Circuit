package ca.ab.cbe.wahs.ajw;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Circuit extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;
	
	public Font font;
	public final Dimension SIZE = new Dimension(780, 639);
	
	public int boardSize = 18; //Number of tiles wide and tall the board is
	public int tileSize = 36; //Number of pixels per tile
	public volatile boolean running = false;
	public volatile boolean paused = false;
	
	private Button clearBoard;
	private Button saveGame;
	private Button loadGame;
	private Button help;
	
	private Image icon;
	private Image inverterN_ON;
	private Image inverterN_OFF;
	private Image inverterE_ON;
	private Image inverterE_OFF;
	private Image inverterS_ON;
	private Image inverterS_OFF;
	private Image inverterW_ON;
	private Image inverterW_OFF;
	
	private File savesDirectory;
	private Canvas canvas;
	private Tile[] selectionGrid;
	private int selectedTile; //Index in selection grid of the current selected tile
	private Colour colour;
	private Input input;
	private int hoverTileX, hoverTileY; //X and Y coordinates of the current tile under the mouse
	private Tile hoverTileType = new Tile(TileType.BLANK);
	private Grid grid; //Main game board (width & height = boardSize)
	
	public Circuit() {
		super("Circuit");
		
		colour = new Colour();
		font = new Font("Arial", Font.BOLD, 36);
		
		clearBoard = new Button(695, 35, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Clear Board");
		saveGame = new Button(695, 65, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Save");
		loadGame = new Button(695, 95, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Load");
		help = new Button(695, 125, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Help");
		
		grid = new Grid(boardSize, boardSize);
		selectionGrid = new Tile[] { new Tile(TileType.BLANK), new Tile(TileType.WIRE), new Tile(TileType.INVERTER, Direction.NORTH),
				new Tile(TileType.POWER) };
		selectedTile = 0;
		
		savesDirectory = new File("saves");
		
		inverterN_ON = new ImageIcon("res/inverterN_ON.png").getImage();
		inverterN_OFF = new ImageIcon("res/inverterN_OFF.png").getImage();
		inverterE_ON = new ImageIcon("res/inverterE_ON.png").getImage();
		inverterE_OFF = new ImageIcon("res/inverterE_OFF.png").getImage();
		inverterS_ON = new ImageIcon("res/inverterS_ON.png").getImage();
		inverterS_OFF = new ImageIcon("res/inverterS_OFF.png").getImage();
		inverterW_ON = new ImageIcon("res/inverterW_ON.png").getImage();
		inverterW_OFF = new ImageIcon("res/inverterW_OFF.png").getImage();
		
		icon = new ImageIcon("res/icon.png").getImage();
		
		canvas = new Canvas();
		input = new Input(canvas);
		canvas.setMinimumSize(SIZE);
		canvas.setMaximumSize(SIZE);
		canvas.setPreferredSize(SIZE);
		canvas.setFont(font);
		canvas.setFocusable(true);
		canvas.requestFocus();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(canvas);
		pack();
		setLocationRelativeTo(null);
		setResizable(false);
		setIconImage(icon);
		setVisible(true);
	}
	
	private void loop() {
		while (running) {
			pollInput();
			update();
			render();
			try {
				Thread.sleep(1000 / 60); //60 updates / second (ish)
			} catch (InterruptedException io) {
				io.printStackTrace();
			}
		}
		dispose();
	}
	
	private void render() {
		BufferStrategy buffer = canvas.getBufferStrategy();
		if (buffer == null) {
			canvas.createBufferStrategy(2);
			return;
		}
		Graphics g = buffer.getDrawGraphics();
		
		g.setFont(canvas.getFont());
		
		//Clear screen
		g.setColor(Color.white);
		g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		
		//Tile selection background
		g.setColor(colour.gray);
		g.fillRect(0, 0, tileSize, tileSize * selectionGrid.length);
		
		//Game board background
		g.setColor(colour.darkGray);
		g.fillRect(tileSize, 0, tileSize * grid.width, tileSize * grid.height);
		
		//Main game board grid
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width + 1; x++) {
				g.setColor(colour.lightGray);
				g.drawRect(x * tileSize, y * tileSize, tileSize, tileSize);
			}
		}
		
		//Hover tile 
		if (hoverTileX > 0 && hoverTileX <= grid.tiles.length && hoverTileY >= 0 && hoverTileY <= grid.tiles.length
				&& grid.tiles[hoverTileY * grid.width + hoverTileX - 1].type == TileType.BLANK) {
			renderTile(hoverTileX * tileSize, hoverTileY * tileSize + tileSize, g, hoverTileType);
			g.setColor(new Color(20, 20, 20, 100));
			g.fillRect(hoverTileX * tileSize + 1, hoverTileY * tileSize + 1, tileSize - 1, tileSize - 1);
		}
		
		//Main game board tiles
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				renderTile(x * tileSize + tileSize, y * tileSize + tileSize, g, grid.tiles[y * grid.width + x]);
			}
		}
		
		//Outline selected tile
		g.setColor(Color.ORANGE);
		g.drawRect(0, selectedTile * tileSize, tileSize, tileSize);
		
		//Render selection tiles
		for (int y = 0; y < selectionGrid.length; y++) {
			renderTile(0, (y + 1) * tileSize, g, selectionGrid[y]);
		}
		
		//Render buttons
		renderButton(clearBoard, g);
		renderButton(saveGame, g);
		renderButton(loadGame, g);
		renderButton(help, g);
		
		//TODO re-enable focus manager
		//if (!canvas.hasFocus()) paused = true; //Automatically pause the game if the user has clicked on another window
		
		if (paused) {
			//Render translucent gray over entire screen
			g.setColor(new Color(65, 75, 75, 160));
			g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
			
			g.setFont(font.deriveFont(32f));
			g.setColor(Color.white);
			g.drawString("PAUSED", (canvas.getWidth() / 2) - (getFontMetrics(g.getFont()).stringWidth("PAUSED") / 2), 200);
			
			g.setFont(font.deriveFont(20f));
			g.drawString("(esc to unpause)", (canvas.getWidth() / 2) - (getFontMetrics(g.getFont()).stringWidth("(esc to unpause)") / 2),
					250);
		}
		
		g.dispose();
		buffer.show();
	}
	
	private void renderButton(Button button, Graphics g) {
		if (button.hover) g.setColor(button.hoverColour);
		else g.setColor(button.colour);
		
		g.fillRect(button.x, button.y, button.width, button.height);
		
		g.setFont(font.deriveFont(12f));
		g.setColor(Color.WHITE);
		g.drawString(button.text, button.x + 10, button.y + 15);
	}
	
	/** @param y - x position on screen
	    @param x - y position on screen
	     */
	private void renderTile(int x, int y, Graphics g, Tile tile) {
		g.setColor(tile.powered ? colour.lightRed : colour.darkRed);
		
		switch (tile.type) {
		case WIRE:
			if (tile.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2); //N
			if (tile.neighbours[1]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //E
			if (tile.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2), 5, tileSize / 2); //S
			if (tile.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) + 4, 5); //W
				
			if (!tile.neighbours[0] && !tile.neighbours[1] && !tile.neighbours[2] && !tile.neighbours[3]) { //There are no neighbors
				g.fillRect(x + (tileSize / 2) - 1, (int) (y - (tileSize * 0.7)), 5, tileSize / 2); //V
				g.fillRect((int) (x + tileSize * 0.3), y - (tileSize / 2) - 1, tileSize / 2, 5); //H
			}
			break;
		case INVERTER:
			if (tile.direction == Direction.NORTH) {
				if (tile.powered) g.drawImage(inverterN_ON, x, y - tileSize, null);
				else g.drawImage(inverterN_OFF, x, y - tileSize, null);
				
				g.setColor(tile.powered ? colour.darkRed : colour.lightRed);
				if (tile.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2 - 5); //N
				g.setColor(tile.powered ? colour.lightRed : colour.darkRed);
				if (tile.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2), 5, tileSize / 2); //S
					
			} else if (tile.direction == Direction.EAST) {
				if (tile.powered) g.drawImage(inverterE_ON, x, y - tileSize, null);
				else g.drawImage(inverterE_OFF, x, y - tileSize, null);
				
				g.setColor(tile.powered ? colour.darkRed : colour.lightRed);
				if (tile.neighbours[1]) g.fillRect(x + (tileSize / 2) + 4, y - (tileSize / 2) - 1, (tileSize / 2) - 4, 5); //E
				g.setColor(tile.powered ? colour.lightRed : colour.darkRed);
				if (tile.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //W
					
			} else if (tile.direction == Direction.SOUTH) {
				if (tile.powered) g.drawImage(inverterS_ON, x, y - tileSize, null);
				else g.drawImage(inverterS_OFF, x, y - tileSize, null);
				
				g.setColor(tile.powered ? colour.lightRed : colour.darkRed);
				if (tile.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2); //N
				g.setColor(tile.powered ? colour.darkRed : colour.lightRed);
				if (tile.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) + 5, 5, tileSize / 2 - 5); //S
					
			} else if (tile.direction == Direction.WEST) {
				if (tile.powered) g.drawImage(inverterW_ON, x, y - tileSize, null);
				else g.drawImage(inverterW_OFF, x, y - tileSize, null);
				
				g.setColor(tile.powered ? colour.lightRed : colour.darkRed);
				if (tile.neighbours[1]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //E
				g.setColor(tile.powered ? colour.darkRed : colour.lightRed);
				if (tile.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) - 5, 5); //W
					
			} else { //problems
				g.setColor(Color.RED);
				g.fillRect(x + 1, y - tileSize + 1, tileSize - 1, tileSize - 1);
			}
			break;
		case POWER:
			g.setColor(tile.powered ? colour.lightRed : colour.darkRed);
			g.fillOval(x + (tileSize / 2) - 5, y - (tileSize / 2) - 6, (tileSize / 3), (tileSize / 3));
			
			if (tile.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2); //N
			if (tile.neighbours[1]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //E
			if (tile.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2), 5, tileSize / 2); //S
			if (tile.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) + 4, 5); //W
			break;
		default:
			break;
		}
	}
	
	private void update() {
		//Update game grid
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				if (grid.tiles[y * grid.width + x].type == TileType.BLANK || grid.tiles[y * grid.width + x].type == TileType.NULL)
					continue; //No need updating blank or null tiles
					
				grid.tiles[y * grid.width + x].powered = checkPowered(x, y);
				grid.tiles[y * grid.width + x].neighbours = updateConnections(x, y);
			}
		}
	}
	
	private boolean[] updateConnections(int x, int y) {
		boolean[] newNeighbors = new boolean[] { false, false, false, false };
		Tile curTile = grid.tiles[y * grid.width + x];
		if (curTile.type == TileType.BLANK || curTile.type == TileType.NULL) return newNeighbors; //These tiles don't care about neighbors
			
		Tile above = getTileAt(x, y - 1);
		Tile below = getTileAt(x, y + 1);
		Tile right = getTileAt(x + 1, y);
		Tile left = getTileAt(x - 1, y);
		
		if (connects(curTile, above, Direction.SOUTH)) newNeighbors[0] = true;
		if (connects(curTile, right, Direction.WEST)) newNeighbors[1] = true;
		if (connects(curTile, below, Direction.NORTH)) newNeighbors[2] = true;
		if (connects(curTile, left, Direction.EAST)) newNeighbors[3] = true;
		
		return newNeighbors;
	}
	
	/** @param tile - the tile which you are checking
	 *  @param curTile - the tile who's neighbors are being updated currently
	 *  @param direction - the direction from tile towards curTile
	 *  @return Whether or not curTile connects to tile*/
	private boolean connects(Tile curTile, Tile tile, Direction direction) {
		if (tile.type == TileType.BLANK || tile.type == TileType.NULL || curTile.type == TileType.BLANK || curTile.type == TileType.NULL)
			return false;
		switch (curTile.type) {
		case INVERTER:
			if (tile.type == TileType.INVERTER) return curTile.direction == tile.direction; //Inverters only connect if they're facing the same way
			if (curTile.direction == direction || curTile.direction == direction.opposite()) { //Only update connections to the front and back
				if (tile.type == TileType.WIRE) if (!tile.powered) return true; //update all connections, even though we don't render sideways ones
					
				if (tile.type == TileType.POWER) {
					return curTile.direction == direction; //power tile is behind inverter
				} else if (tile.direction == direction || tile.direction == direction.opposite()) return true;
				
				if (direction == curTile.direction) return curTile.powered ? tile.powered : !tile.powered;
				else if (direction == curTile.direction.opposite()) return curTile.powered ? !tile.powered : tile.powered;
			} else return false;
		case POWER:
			if (tile.type == TileType.INVERTER) return (tile.direction == direction.opposite());
			else if (tile.type == TileType.WIRE) return true;
			return curTile.type != TileType.POWER; //power tiles don't connect to other power tiles
		case WIRE:
			if (tile.type == TileType.INVERTER) {
				if (tile.direction == direction) { //facing towards (output side of converter)
					if (tile.powered != curTile.powered) return true; //Only connect if 
				} else if (tile.direction == direction.opposite()) { //facing away (input side of converter)
					return true;
				}
			} else if (tile.type == TileType.POWER || tile.type == TileType.WIRE) return true;
		default:
			return false;
		}
	}
	
	private boolean checkPowered(int x, int y) {
		Tile curTile = grid.tiles[y * grid.width + x];
		if (curTile.type == TileType.BLANK) return false;
		if (curTile.type == TileType.POWER) return curTile.powered;
		
		Tile above = getTileAt(x, y - 1);
		Tile below = getTileAt(x, y + 1);
		Tile right = getTileAt(x + 1, y);
		Tile left = getTileAt(x - 1, y);
		
		if (curTile.type == TileType.INVERTER) { //Inverters are only affected by the tile behind them (north facing tiles by the tile below, east facing tiles by the tile to the left)
			switch (curTile.direction) {
			case NORTH:
				if (givingPower(below, Direction.NORTH)) return true;
				break;
			case EAST:
				if (givingPower(left, Direction.EAST)) return true;
				break;
			case SOUTH:
				if (givingPower(above, Direction.SOUTH)) return true;
				break;
			case WEST:
				if (givingPower(right, Direction.WEST)) return true;
				break;
			default:
				System.err.println("Invalid inverter direction @ x: " + x + " ,y: " + y);
				return false;
			}
			return false;
		}
		
		if (above.type != TileType.NULL) { //type will be null if the current tile is at the top of the grid
			if (givingPower(above, Direction.SOUTH)) return true;
		}
		
		if (below.type != TileType.NULL) { //type will be null if the current tile is at the bottom of the grid
			if (givingPower(below, Direction.NORTH)) return true;
		}
		
		if (right.type != TileType.NULL) { //type will be null if the current tile is at the right side of the grid
			if (givingPower(right, Direction.WEST)) return true;
		}
		
		if (left.type != TileType.NULL) { //type will be null if the current tile is at the left side of the grid
			if (givingPower(left, Direction.EAST)) return true;
		}
		
		return false;
	}
	
	/** @return Whether <b>tile</b> is giving power towards <b>direction</b> 
	 * @param tile - the tile which you are checking
	 *  @param direction - the direction towards the current tile (only affects inverters) */
	private boolean givingPower(Tile tile, Direction direction) {
		//FIXME redo entire power system
		switch (tile.type) {
		case INVERTER:
			if (tile.direction == direction) return !tile.powered;
			else return false;
		case POWER:
			return tile.powered;
		case WIRE:
			return tile.powered;
		case NULL:
		case BLANK:
			return false;
		default:
			return false;
		}
	}
	
	/** @return the tile with x and y coordinates, or a Tile with type NULL if x or y are out of range */
	private Tile getTileAt(int x, int y) {
		if (x < 0 || x >= grid.width || y < 0 || y >= grid.height) return new Tile(TileType.NULL);
		return grid.tiles[y * grid.width + x];
	}
	
	private void pollInput() {
		if (input.escape) paused = !paused;
		if (paused) {
			input.releaseAll();
			return;
		}
		
		if (input.num != -1 && input.num < selectionGrid.length) selectedTile = input.num;
		
		int x = getMouseColumn(input.x);
		int y = getMouseRow(input.y);
		
		// 0 = tile selection area, 18 = rightmost column
		hoverTileY = y;
		hoverTileX = x;
		hoverTileType = selectionGrid[selectedTile];
		
		if (y != -1 && x != -1) { //Mouse is not in game board or tile selection area
			updateGrid(x, y);
		}
		
		//Clear Screen Button
		if (clearBoard.mouseInBounds(input)) {
			clearBoard.hover = true;
			if (input.leftDown || input.rightDown) {
				grid = clearBoard(boardSize, boardSize);
			}
		} else clearBoard.hover = false;
		
		//Save Game Button
		if (saveGame.mouseInBounds(input)) {
			saveGame.hover = true;
			if (input.leftDown || input.rightDown) {
				saveBoard();
			}
		} else saveGame.hover = false;
		
		//Load Game Button
		if (loadGame.mouseInBounds(input)) {
			loadGame.hover = true;
			if (input.leftDown || input.rightDown) {
				loadBoard();
			}
		} else loadGame.hover = false;
		
		//Help Button
		if (help.mouseInBounds(input)) {
			help.hover = true;
			if (input.leftDown || input.rightDown) {
				JTextArea textArea = new JTextArea(
						"Circuit is a virtual electronic circuit builder/tester made by AJ Weeks in April 2014.\r\n"
								+ "-Left click to place/roatate objects on the grid.\r\n" + "-Right click to clear a spot on the grid.\r\n"
								+ "-Hold down Ctrl while clicking and dragging the mouse to draw.\r\n"
								+ "-Use the number keys to quickly select different tile tile types.\r\n" + "-Hit esc to pause/unpause");
				textArea.setEditable(false);
				textArea.setColumns(25);
				textArea.setRows(60);
				textArea.setLineWrap(true);
				textArea.setWrapStyleWord(true);
				textArea.setFont(new Font("Consolas", Font.BOLD, 16));
				textArea.setAlignmentX(JTextArea.CENTER_ALIGNMENT);
				JDialog dialog = new JDialog(getOwner(), "Help");
				dialog.add(textArea);
				dialog.setSize(580, 300);
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);
			}
		} else help.hover = false;
		
		input.releaseAll();
	}
	
	/** @param x - the x coordinate of the tile currently under the mouse
	 *  @param y - the y coordinate of the tile currently under the mouse  */
	private void updateGrid(int x, int y) {
		x--; //Offset to account for tile selection column
		if (input.leftDown) { //Left click in game board or tile selection area
			if (x == -1) { //Mouse is in leftmost column (tile selection area)
				if (y + 1 <= selectionGrid.length) selectedTile = y; //Check if the selected tile has a tile to select
			} else { //Click in the game board
				if (grid.tiles[y * grid.width + x].type != selectionGrid[selectedTile].type) { //If the tile not the selected tile
					grid.tiles[y * grid.width + x] = Tile.copy(selectionGrid[selectedTile]);
				} else { //The selected tile is the same type as the tile being clicked, so rotate it
					switch (grid.tiles[y * grid.width + x].type) {
					case INVERTER:
						grid.tiles[y * grid.width + x].direction = rotateCW(grid, x, y);
					case POWER:
						grid.tiles[y * grid.width + x].powered = !grid.tiles[y * grid.width + x].powered;
						break;
					default:
						break;
					}
				}
			}
		} else if (input.rightDown && x >= 0 && x < grid.width) { //Right click clears the tile (except in the tile selection area)
			grid.tiles[y * grid.width + x] = Tile.newBlankTile();
		}
	}
	
	//--------------Helper methods------------------------
	
	/** @return New blank board */
	public static Grid clearBoard(int width, int height) {
		return new Grid(width, height);
	}
	
	/** @return The direction of the tile at <b>grid[y][x]</b> rotated clockwise once */
	public static Direction rotateCW(Grid grid, int x, int y) {
		switch (grid.tiles[y * grid.width + x].direction) {
		case NORTH:
			return Direction.EAST;
		case EAST:
			return Direction.SOUTH;
		case SOUTH:
			return Direction.WEST;
		case WEST:
			return Direction.NORTH;
		default:
			System.out.println("invalid direction!");
			return Direction.NONE;
		}
	}
	
	/** Overwrites the existing saveBoard file */
	private void saveBoard() {
		if (!savesDirectory.exists()) {
			try {
				savesDirectory.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		JFileChooser chooser = new JFileChooser(savesDirectory);
		chooser.setFileFilter(new FileNameExtensionFilter("*.ser", "ser"));
		chooser.setSelectedFile(new File("save.ser"));
		if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			if (!chooser.getSelectedFile().getName().endsWith(".ser")) {
				JOptionPane.showMessageDialog(null, "Must be saved as a .ser file! Please try again.", "Invalid file type!",
						JOptionPane.DEFAULT_OPTION);
				chooser.getSelectedFile().delete();
				return;
			}
			File save = chooser.getSelectedFile().getAbsoluteFile();
			if (save.exists()) {
				int i = 0;
				if ((i = JOptionPane.showConfirmDialog(null, chooser.getSelectedFile().getName()
						+ " already exists! Would you like to overwrite it?", "File exists", JOptionPane.YES_NO_OPTION)) == JOptionPane.YES_OPTION) {
					save.delete();
				} else if (i == JOptionPane.NO_OPTION) return;
			}
			try {
				save.createNewFile();
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(save.getAbsoluteFile()));
				out.writeObject(grid);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadBoard() {
		//LATER add save check to prevent overwritten files
		if (!savesDirectory.exists()) {
			try {
				savesDirectory.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		JFileChooser chooser = new JFileChooser(savesDirectory.getAbsoluteFile());
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileFilter(new FileNameExtensionFilter("*.ser", "ser"));
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			if (!chooser.getSelectedFile().getName().endsWith(".ser")) {
				JOptionPane.showMessageDialog(null, "Must be a .ser file!", "Invalid file type!", JOptionPane.DEFAULT_OPTION);
				return;
			}
			try {
				FileInputStream fileInput = new FileInputStream(chooser.getSelectedFile().getAbsolutePath());
				if (fileInput.available() > 0) {
					ObjectInputStream in = new ObjectInputStream(fileInput);
					Grid newGrid = new Grid(boardSize, boardSize);
					newGrid = (Grid) in.readObject();
					in.close();
					grid = newGrid;
				} else System.err.println("uhoh " + chooser.getSelectedFile().getAbsolutePath());
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** @param y - the y position of the mouse on screen 
	 *  @return the row the mouse is in on the game grid */
	public int getMouseRow(int y) {
		return Math.max(0, Math.min((y - 1) / tileSize, boardSize - 1));
	}
	
	/** @param x - the x position of the mouse on screen 
	 *  @return the column the mouse is in on the game grid OR -1 if mouse is outside of game grid */
	public int getMouseColumn(int x) {
		if (x > boardSize * tileSize + tileSize) return -1;
		return Math.min((x - 1) / tileSize, boardSize);
	}
	
	public static void main(String[] args) {
		new Thread(new Circuit()).start();
	}
	
	@Override
	public void run() {
		running = true;
		loop();
	}
}
