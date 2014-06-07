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
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Circuit extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;
	
	public static boolean DEBUG = true;
	
	public static final Dimension SIZE = new Dimension(780, 639);
	public static int boardSize = 18; //Number of tiles wide and tall the board is
	public static int tileSize = 36; //Number of pixels per tile
	public volatile boolean running = false;
	public volatile boolean paused = false;
	
	private Button clearBoard;
	private Button saveGame;
	private Button loadGame;
	private Button help;
	private Button quit;
	
	private Image icon;
	
	private Font font;
	private File savesDirectory;
	private Canvas canvas;
	private Tile[] selectionGrid;
	private int selectedTile; //Index in selection grid of the current selected tile
	private Colour colour;
	private Input input;
	private int hoverTileX, hoverTileY; //X and Y coordinates of the current tile under the mouse
	private Tile hoverTileType = new Tile(TileType.BLANK);
	private Grid grid; //Main game board (width & height = boardSize)
	
	private int fps = 0;
	private int frames = 0;
	
	public Circuit() {
		super("Circuit");
		
		colour = new Colour();
		font = new Font("Arial", Font.BOLD, 36);
		
		clearBoard = new Button(695, 35, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Clear Board");
		saveGame = new Button(695, 65, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Save");
		loadGame = new Button(695, 95, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Load");
		help = new Button(695, 125, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Help");
		quit = new Button(695, 155, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Quit");
		
		grid = new Grid(boardSize, boardSize);
		selectionGrid = new Tile[] { new Tile(TileType.BLANK), new Tile(TileType.WIRE), new Tile(TileType.INVERTER, Direction.NORTH),
				new Tile(TileType.POWER) };
		selectedTile = 0;
		
		savesDirectory = new File("saves");
		
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
		long before = System.currentTimeMillis();
		long seconds = 0;
		while (running) {
			pollInput();
			update();
			render();
			try {
				Thread.sleep(1000 / 60); //60 updates / second (ish)
			} catch (InterruptedException io) {
				io.printStackTrace();
			}
			seconds += (System.currentTimeMillis() - before);
			if (seconds >= 1000) {
				fps = frames;
				seconds = 0;
				frames = 0;
			}
			before = System.currentTimeMillis();
		}
		dispose();
		System.exit(0);
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
		if (hoverTileX > -1 && hoverTileX <= grid.width && hoverTileY > -1 && hoverTileY <= grid.height
				&& grid.tiles[hoverTileY * grid.width + hoverTileX].type == TileType.BLANK) {
			hoverTileType.render(hoverTileX * tileSize + tileSize, hoverTileY * tileSize + tileSize, g, colour);
			g.setColor(new Color(20, 20, 20, 100));
			g.fillRect(hoverTileX * tileSize + tileSize + 1, hoverTileY * tileSize + 1, tileSize - 1, tileSize - 1);
		}
		
		//Main game board tiles
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				grid.tiles[y * grid.width + x].render(x * tileSize + tileSize, y * tileSize + tileSize, g, colour);
			}
		}
		
		//Outline selected tile
		g.setColor(Color.ORANGE);
		g.drawRect(0, selectedTile * tileSize, tileSize, tileSize);
		
		//Selection tiles
		for (int y = 0; y < selectionGrid.length; y++) {
			selectionGrid[y].render(0, (y + 1) * tileSize, g, colour);
		}
		
		//Buttons
		clearBoard.render(g, font);
		saveGame.render(g, font);
		loadGame.render(g, font);
		help.render(g, font);
		quit.render(g, font);
		
		if (!paused) {
			renderButtonHoverText(saveGame, "(crtl-s)", g);
			renderButtonHoverText(loadGame, "(crtl-o)", g);
			renderButtonHoverText(quit, "(crtl-q)", g);
		}
		
		if (!DEBUG && !canvas.hasFocus()) paused = true; //Automatically pause the game if the user has clicked on another window
			
		if (DEBUG) {
			g.setColor(Color.BLACK);
			g.fillRect(715, 5, 50, 20);
			
			g.setColor(Color.WHITE);
			g.drawString(fps + " FPS", 720, 20);
			
			if (hoverTileX > -1 && hoverTileY > -1 && !paused) {
				int xoff = hoverTileX > 8 ? -150 : 15;
				int yoff = hoverTileY > 8 ? -60 : 0;
				g.setColor(new Color(25, 25, 25, 115));
				g.fillRect(input.x + xoff - 5, input.y - 15 + yoff, 150, 67);
				g.setColor(Color.WHITE);
				g.drawString("powered: " + String.valueOf(grid.tiles[hoverTileY * grid.width + hoverTileX].powered), input.x + xoff, input.y - 2
						+ yoff);
				g.drawString(Arrays.toString(grid.tiles[hoverTileY * grid.width + hoverTileX].neighbours), input.x + xoff, input.y + 10 + yoff);
				g.drawString("direction: " + grid.tiles[hoverTileY * grid.width + hoverTileX].direction.toString(), input.x + xoff, input.y + 22
						+ yoff);
				g.drawString("x: " + input.x + " y: " + input.y, input.x + xoff, input.y + 34 + yoff);
				g.drawString("x: " + hoverTileX + " y: " + hoverTileY, input.x + xoff, input.y + 46 + yoff);
			}
		}
		
		if (paused) {
			//Translucent gray over entire screen
			g.setColor(colour.translucentGray);
			g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
			
			g.setFont(font.deriveFont(32f));
			g.setColor(Color.white);
			g.drawString("PAUSED", (canvas.getWidth() / 2) - (getFontMetrics(g.getFont()).stringWidth("PAUSED") / 2), 200);
			
			g.setFont(font.deriveFont(20f));
			g.drawString("(esc to unpause)", (canvas.getWidth() / 2) - (getFontMetrics(g.getFont()).stringWidth("(esc to unpause)") / 2), 250);
		}
		
		g.dispose();
		buffer.show();
		frames++;
	}
	
	private void renderButtonHoverText(Button button, String text, Graphics g) {
		if (input.x > button.x && input.x < button.x + button.width && input.y > button.y && input.y < button.y + button.height) {
			g.setColor(new Color(230, 230, 230, 230)); //translucent off white
			g.drawString(text, button.x + 45, button.y + 16);
		}
	}
	
	private void update() {
		updateConnections();
		updatePower();
	}
	
	private void updateConnections() {
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				Tile curTile = getTileAt(x, y);
				if (curTile.type == TileType.BLANK || curTile.type == TileType.NULL) continue; //No need updating blank or null tiles
				boolean[] newNeighbours = new boolean[] { false, false, false, false };
				
				Tile above = getTileAt(x, y - 1);
				Tile below = getTileAt(x, y + 1);
				Tile right = getTileAt(x + 1, y);
				Tile left = getTileAt(x - 1, y);
				
				newNeighbours[0] = connects(curTile, above, Direction.SOUTH);
				newNeighbours[1] = connects(curTile, right, Direction.WEST);
				newNeighbours[2] = connects(curTile, below, Direction.NORTH);
				newNeighbours[3] = connects(curTile, left, Direction.EAST);
				
				grid.tiles[y * grid.width + x].neighbours = newNeighbours;
			}
		}
	}
	
	/** @param tile - the tile which you are checking
	 *  @param curTile - the tile who's neighbours are being updated currently
	 *  @param direction - the direction from tile towards curTile
	 *  @return Whether or not curTile connects to tile*/
	private boolean connects(Tile curTile, Tile tile, Direction direction) {
		if (tile.type == TileType.BLANK || tile.type == TileType.NULL || curTile.type == TileType.BLANK || curTile.type == TileType.NULL)
			return false;
		switch (curTile.type) {
		case INVERTER:
			if (direction != curTile.direction && direction != curTile.direction.opposite()) return false; //Must be either in front, or behind us to affect us
			switch (tile.type) {
			case INVERTER:
				if (tile.direction == curTile.direction || tile.direction == curTile.direction.opposite()) return (curTile.direction == tile.direction || curTile.direction == tile.direction
						.opposite()); //Inverters only connect if they're facing the same way, or opposite ways
				else return false;
			case WIRE:
				if (curTile.direction == direction) return true; //the wire is on the input side of the inverter
				else if (curTile.direction == direction.opposite()) { //the wire is on the output side of the inverter
					if (curTile.powered) return tile.powered != curTile.powered;
					else return true;
				}
				return false;
			case POWER:
				return curTile.direction == direction; //power tile is behind inverter
			default:
				return false;
			}
		case POWER:
			if (tile.type == TileType.INVERTER) return (tile.direction == direction.opposite()); //Connect if it is facing away from you
			else if (tile.type == TileType.WIRE) return true;
			else return curTile.type != TileType.POWER; //power tiles don't connect to other power tiles
		case WIRE:
			if (tile.type == TileType.INVERTER) {
				if (tile.direction == direction) { //facing towards (output side of converter)
					if (curTile.powered) {
						if (!tile.powered) return true;
					} else return true;
				} else if (tile.direction == direction.opposite()) { //facing away (input side of converter)
					return true;
				}
			} else if (tile.type == TileType.POWER || tile.type == TileType.WIRE) return true;
			return false;
		default:
			return false;
		}
	}
	
	private void updatePower() {
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				Tile curTile = getTileAt(x, y);
				
				Tile north = getTileAt(x, y - 1);
				Tile south = getTileAt(x, y + 1);
				Tile east = getTileAt(x + 1, y);
				Tile west = getTileAt(x - 1, y);
				
				//First pass
				switch (curTile.type) {
				case BLANK:
				case NULL:
					continue;
				case INVERTER:
					switch (curTile.direction) {
					case NORTH:
						if (south.type != TileType.NULL) curTile.powered = south.powered;
						break;
					case EAST:
						if (west.type != TileType.NULL) curTile.powered = west.powered;
						break;
					case SOUTH:
						if (north.type != TileType.NULL) curTile.powered = north.powered;
						break;
					case WEST:
						if (east.type != TileType.NULL) curTile.powered = east.powered;
						break;
					default:
						new IllegalStateException("Inverter tile has invalid direction: " + curTile.direction + " @ x: " + x + ",y: " + y)
								.printStackTrace();
					}
					break;
				case WIRE:
					curTile.powered = false;
					break;
				case POWER: //Power tiles don't need updating
					break;
				}
				
				//Second pass 
				if ((curTile.type == TileType.POWER && curTile.powered) || (curTile.type == TileType.INVERTER && !curTile.powered)) {
					floodfillAllExcept(x, y, Direction.NONE); //Start flood filling at power sources which are giving out power
				}
				
				grid.tiles[y * grid.width + x] = curTile.copy();
			}
		}
	}
	
	/** Calls floodfill on all this tile's neighbours (except the one towards direction comingFrom). <br/> If you want to update all neighbours, pass Direction.NONE */
	private void floodfillAllExcept(int x, int y, Direction comingFrom) {
		if (comingFrom != Direction.NORTH) floodfill(x, y - 1, Direction.SOUTH);
		if (comingFrom != Direction.EAST) floodfill(x + 1, y, Direction.WEST);
		if (comingFrom != Direction.SOUTH) floodfill(x, y + 1, Direction.NORTH);
		if (comingFrom != Direction.WEST) floodfill(x - 1, y, Direction.EAST);
	}
	
	private void floodfill(int x, int y, Direction comingFrom) {
		Tile t = getTileAt(x, y);
		if (t.type != TileType.WIRE) return; //only update wires
		if (grid.tiles[y * grid.width + x].powered) return;
		grid.tiles[y * grid.width + x].powered = true;
		
		checkNorth(t, x, y, comingFrom);
		checkEast(t, x, y, comingFrom);
		checkSouth(t, x, y, comingFrom);
		checkWest(t, x, y, comingFrom);
	}
	
	private void checkNorth(Tile t, int x, int y, Direction comingFrom) {
		if (t.neighbours[0] && comingFrom != Direction.NORTH) {
			if (getTileAt(x, y - 1).type != TileType.NULL) {
				grid.tiles[(y - 1) * grid.width + x].powered = true;
			}
			floodfillAllExcept(x, y - 1, Direction.SOUTH);
		}
	}
	
	private void checkEast(Tile t, int x, int y, Direction comingFrom) {
		if (t.neighbours[1] && comingFrom != Direction.EAST) {
			if (getTileAt(x + 1, y).type != TileType.NULL) {
				grid.tiles[y * grid.width + x + 1].powered = true;
			}
			floodfillAllExcept(x + 1, y, Direction.WEST);
		}
	}
	
	private void checkSouth(Tile t, int x, int y, Direction comingFrom) {
		if (t.neighbours[2] && comingFrom != Direction.SOUTH) {
			if (getTileAt(x, y + 1).type != TileType.NULL) {
				grid.tiles[(y + 1) * grid.width + x].powered = true;
			}
			floodfillAllExcept(x, y + 1, Direction.NORTH);
		}
	}
	
	private void checkWest(Tile t, int x, int y, Direction comingFrom) {
		if (t.neighbours[3] && comingFrom != Direction.WEST) {
			if (getTileAt(x - 1, y).type != TileType.NULL) {
				grid.tiles[y * grid.width + x - 1].powered = true;
			}
			floodfillAllExcept(x - 1, y, Direction.EAST);
		}
	}
	
	/** @return the tile with x and y coordinates, or a Tile with type NULL if x or y are out of range */
	private Tile getTileAt(int x, int y) {
		if (x < 0 || x >= grid.width || y < 0 || y >= grid.height) return new Tile(TileType.NULL);
		return grid.tiles[y * grid.width + x];
	}
	
	private void pollInput() {
		if (input.escape) paused = !paused;
		
		if (input.quit) {
			input.quit = false;
			running = false;
		}
		
		if (paused) {
			input.releaseAll();
			return;
		}
		
		if (input.F3) {
			input.F3 = false;
			DEBUG = !DEBUG;
		}
		
		if (input.save) {
			input.save = false;
			saveBoard();
		}
		
		if (input.open) {
			input.open = false;
			loadBoard();
		}
		
		if (input.wires) {
			input.wires = false;
			grid = Grid.allWires(boardSize, boardSize);
		}
		
		if (input.num != -1 && input.num < selectionGrid.length) selectedTile = input.num;
		
		int x = getMouseColumn(input.x);
		int y = getMouseRow(input.y);
		
		// 0 = tile selection area, 18 = rightmost column
		hoverTileY = y;
		hoverTileX = x;
		hoverTileType = selectionGrid[selectedTile];
		
		if (hoverTileY != -2 && hoverTileX != -2) { //Mouse is not in game board or tile selection area
			updateGridWithInput(x, y);
		}
		
		//Clear Screen Button
		if (clearBoard.mouseInBounds(input)) {
			clearBoard.hover = true;
			if (input.leftDown || input.rightDown) {
				grid = Grid.clearBoard(boardSize, boardSize);
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
				JTextArea textArea = new JTextArea("Circuit is a virtual electronic circuit builder/tester made by AJ Weeks in April 2014.\r\n"
						+ "-Left click to place/roatate objects on the grid.\r\n" + "-Right click to clear a spot on the grid.\r\n"
						+ "-Hold down Ctrl while clicking and dragging the mouse to draw.\r\n"
						+ "-Use the number keys to quickly select different tile types.\r\n" + "-Hit esc to pause/unpause");
				textArea.setEditable(false);
				textArea.setColumns(25);
				textArea.setRows(60);
				textArea.setLineWrap(true);
				textArea.setWrapStyleWord(true);
				textArea.setFont(new Font("Consolas", Font.BOLD, 16));
				textArea.setAlignmentX(JTextArea.CENTER_ALIGNMENT);
				textArea.setBackground(Color.LIGHT_GRAY);
				JDialog dialog = new JDialog(getOwner(), "Help");
				dialog.add(textArea);
				dialog.setSize(580, 300);
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);
			}
		} else help.hover = false;
		
		//Quit Button
		if (quit.mouseInBounds(input)) {
			quit.hover = true;
			if (input.leftDown || input.rightDown) {
				running = false;
			}
		} else quit.hover = false;
		
		input.releaseAll();
	}
	
	/** @param x - the x coordinate of the tile currently under the mouse
	 *  @param y - the y coordinate of the tile currently under the mouse  */
	private void updateGridWithInput(int x, int y) {
		if (input.leftDown) { //Left click in game board or tile selection area
			if (x == -1) { //Mouse is in leftmost column (tile selection area)
				if (y + 1 <= selectionGrid.length) selectedTile = y; //Check if the selected tile has a tile to select
			} else { //Click in the game board
				if (grid.tiles[y * grid.width + x].type != selectionGrid[selectedTile].type) { //If the tile not the selected tile
					grid.tiles[y * grid.width + x] = selectionGrid[selectedTile].copy();
				} else { //The selected tile is the same type as the tile being clicked, so rotate it
					switch (grid.tiles[y * grid.width + x].type) {
					case INVERTER:
						grid.tiles[y * grid.width + x].direction = Tile.rotateCW(grid, x, y);
						break;
					case POWER:
						grid.tiles[y * grid.width + x].powered = !grid.tiles[y * grid.width + x].powered;
						break;
					default:
						break;
					}
				}
			}
		} else if (input.rightDown && x >= -1 && x < grid.width) { //Right click clears the tile (except in the tile selection area)
			grid.tiles[y * grid.width + x] = Tile.newBlankTile();
		}
	}
	
	//--------------Helper methods------------------------
	
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
				if ((i = JOptionPane.showConfirmDialog(null,
						chooser.getSelectedFile().getName() + " already exists! Would you like to overwrite it?", "File exists",
						JOptionPane.YES_NO_OPTION)) == JOptionPane.YES_OPTION) {
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
	 *  @return the column the mouse is in on the game grid OR -1 if mouse is outside of game grid (0 = tile selection col, 1 - 18 = main grid)*/
	public int getMouseColumn(int x) {
		if (x > boardSize * tileSize + tileSize) return -2;
		return Math.min((x - 1) / tileSize - 1, boardSize);
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
