package ca.ab.cbe.wahs.ajw;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Copyright (c) 2014 AJ Weeks. All rights reserved. */

public class Circuit extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;
	public static final String GAME_TITLE = "Circuit";
	
	public static boolean DEBUG = false;
	
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
	private Button resume;
	
	private Image icon;
	
	private Font font12;
	private Font font36;
	private File savesDirectory;
	private Canvas canvas;
	private Tile[] selectionGrid;
	private int selectedTile; //Index in selection grid of the current selected tile
	private Input input;
	private int hoverTileX, hoverTileY; //X and Y coordinates of the current tile under the mouse
	private Tile hoverTileType = new Tile(TileType.BLANK);
	private Grid grid; //Main game board (width & height = boardSize)
	
	protected static Image inverterN_ON = new ImageIcon("res/inverterN_ON.png").getImage();
	protected static Image inverterN_OFF = new ImageIcon("res/inverterN_OFF.png").getImage();
	protected static Image inverterE_ON = new ImageIcon("res/inverterE_ON.png").getImage();
	protected static Image inverterE_OFF = new ImageIcon("res/inverterE_OFF.png").getImage();
	protected static Image inverterS_ON = new ImageIcon("res/inverterS_ON.png").getImage();
	protected static Image inverterS_OFF = new ImageIcon("res/inverterS_OFF.png").getImage();
	protected static Image inverterW_ON = new ImageIcon("res/inverterW_ON.png").getImage();
	protected static Image inverterW_OFF = new ImageIcon("res/inverterW_OFF.png").getImage();
	
	private int fps = 0;
	private int frames = 0;
	private int ticks = 0;
	private boolean saved = true;
	
	public Circuit() {
		super(GAME_TITLE);
		
		font36 = new Font("Arial", Font.BOLD, 36);
		font12 = new Font("Arial", Font.BOLD, 12);
		
		clearBoard = new Button(695, 35, 85, 25, Colour.buttonColour, Colour.buttonHoverColour, "Clear Board");
		saveGame = new Button(695, 65, 85, 25, Colour.buttonColour, Colour.buttonHoverColour, "Save");
		loadGame = new Button(695, 95, 85, 25, Colour.buttonColour, Colour.buttonHoverColour, "Load");
		help = new Button(695, 125, 85, 25, Colour.buttonColour, Colour.buttonHoverColour, "Help");
		quit = new Button(695, 155, 85, 25, Colour.buttonColour, Colour.buttonHoverColour, "Quit");
		
		resume = new Button(SIZE.width / 2 - 250 / 2, 250, 250, 100, Colour.buttonColour, Colour.buttonHoverColour, "Resume");
		
		grid = new Grid(boardSize, boardSize);
		selectionGrid = new Tile[] { new Tile(TileType.BLANK), new Tile(TileType.WIRE),
				new Tile(TileType.INVERTER, Direction.NORTH), new Tile(TileType.POWER) };
		selectedTile = 0;
		
		savesDirectory = new File("saves");
		
		icon = new ImageIcon("res/icon.png").getImage();
		
		canvas = new Canvas();
		input = new Input(canvas);
		canvas.setMinimumSize(SIZE);
		canvas.setMaximumSize(SIZE);
		canvas.setPreferredSize(SIZE);
		canvas.setFont(font36);
		canvas.setFocusable(true);
		canvas.requestFocus();
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowListener() {
			public void windowClosing(WindowEvent e) {
				exit();
			}
			
			public void windowOpened(WindowEvent e) {}
			
			public void windowIconified(WindowEvent e) {}
			
			public void windowDeiconified(WindowEvent e) {}
			
			public void windowDeactivated(WindowEvent e) {}
			
			public void windowClosed(WindowEvent e) {}
			
			public void windowActivated(WindowEvent e) {}
		});
		add(canvas);
		pack();
		setLocationRelativeTo(null);
		setResizable(false);
		setIconImage(icon);
		setVisible(true);
	}
	
	/** Checks if the current game grid has been saved, and alerts the user if it hasn't.<br/> To be called whenever the user requests the close of the application. */
	private void exit() {
		if (!saved && !grid.isEmpty()) {
			if (JOptionPane.showConfirmDialog(null, "Warning! You haven't saved! Quit anyway?", "Unsaved Work!",
					JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				if (!running) {
					running = true;
					loop();
				}
				return;
			}
		}
		System.exit(0);
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
		exit();
	}
	
	private void render() {
		BufferStrategy buffer = canvas.getBufferStrategy();
		if (buffer == null) {
			canvas.createBufferStrategy(3);
			return;
		}
		Graphics g = buffer.getDrawGraphics();
		
		g.setFont(canvas.getFont());
		
		//Clear screen
		g.setColor(Color.white);
		g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		
		//Tile selection background
		g.setColor(Colour.gray);
		g.fillRect(0, 0, tileSize, tileSize * selectionGrid.length);
		
		//Extra tiles selection background
		g.setColor(Colour.lightGray);
		g.fillRect(0, selectionGrid.length * tileSize, tileSize, (tileSize * grid.height) - (selectedTile * tileSize));
		
		//Selected tile background
		g.setColor(Colour.translucentYellow);
		g.fillRect(0, selectedTile * tileSize, tileSize, tileSize);
		
		//Game board background
		g.setColor(Colour.darkGray);
		g.fillRect(tileSize, 0, tileSize * grid.width, tileSize * grid.height);
		
		//Button area background
		g.setColor(Colour.lightGray);
		g.fillRect(tileSize * grid.width + tileSize + 1, 0, SIZE.width - (tileSize * grid.width), tileSize * grid.height);
		
		//Main game board grid
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width + 1; x++) {
				g.setColor(Colour.lightGray);
				g.drawRect(x * tileSize, y * tileSize, tileSize, tileSize);
			}
		}
		
		//Tile selection grid
		for (int y = 0; y < grid.height; y++) {
			g.setColor(Color.BLACK);
			g.drawRect(0, y * tileSize, tileSize, tileSize);
		}
		
		//Hover tile 
		if (hoverTileX > -1 && hoverTileX <= grid.width && hoverTileY > -1 && hoverTileY <= grid.height) {
			if (grid.tiles[hoverTileY * grid.width + hoverTileX].type == TileType.BLANK) {
				hoverTileType.render(hoverTileX * tileSize + tileSize, hoverTileY * tileSize + tileSize, g);
			}
			g.setColor(new Color(20, 20, 20, 100));
			g.fillRect(hoverTileX * tileSize + tileSize + 1, hoverTileY * tileSize + 1, tileSize - 1, tileSize - 1);
		}
		
		//Main game board tiles
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				grid.tiles[y * grid.width + x].render(x * tileSize + tileSize, y * tileSize + tileSize, g);
			}
		}
		
		//Outline selected tile
		g.setColor(Color.ORANGE);
		g.drawRect(0, selectedTile * tileSize, tileSize, tileSize);
		
		//Selection tiles
		for (int y = 0; y < selectionGrid.length; y++) {
			selectionGrid[y].render(0, (y + 1) * tileSize, g);
		}
		
		//Selection tiles numbers
		for (int y = 0; y < selectionGrid.length + 1; y++) {
			g.setColor(Colour.translucentGray);
			g.setFont(font12);
			g.drawString(Integer.toString(y - 1), 28, y * tileSize - 1);
		}
		
		//Buttons
		clearBoard.render(g, font12, 10, 15);
		saveGame.render(g, font12, 10, 15);
		loadGame.render(g, font12, 10, 15);
		help.render(g, font12, 10, 15);
		quit.render(g, font12, 10, 15);
		
		if (!paused) {
			saveGame.renderButtonHoverText("(crtl-s)", g, 45, 16, input);
			loadGame.renderButtonHoverText("(crtl-o)", g, 45, 16, input);
			quit.renderButtonHoverText("(crtl-q)", g, 45, 16, input);
		}
		
		if (input.x / tileSize == 0) renderSelectionTileText(g);
		
		if (!DEBUG && !canvas.hasFocus()) paused = true; //Automatically pause the game if the user has clicked on another window (unless we're debugging)
			
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
				g.drawString("powered: " + String.valueOf(grid.tiles[hoverTileY * grid.width + hoverTileX].powered), input.x
						+ xoff, input.y - 2 + yoff);
				g.drawString(Arrays.toString(grid.tiles[hoverTileY * grid.width + hoverTileX].neighbours), input.x + xoff,
						input.y + 10 + yoff);
				g.drawString("direction: " + grid.tiles[hoverTileY * grid.width + hoverTileX].direction.toString(), input.x
						+ xoff, input.y + 22 + yoff);
				g.drawString("x: " + input.x + " y: " + input.y, input.x + xoff, input.y + 34 + yoff);
				g.drawString("x: " + hoverTileX + " y: " + hoverTileY, input.x + xoff, input.y + 46 + yoff);
			}
		}
		
		if (paused) {
			//Translucent gray over entire screen
			g.setColor(Colour.translucentGray);
			g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
			
			g.setFont(font36);
			g.setColor(Color.white);
			g.drawString("PAUSED", (canvas.getWidth() / 2) - (getFontMetrics(g.getFont()).stringWidth("PAUSED") / 2), 200);
			
			resume.render(g, font36.deriveFont(30f), 70, 55);
			resume.renderButtonHoverText("(esc)", g, 92, 85, input);
		}
		
		g.dispose();
		buffer.show();
		frames++;
	}
	
	private void renderSelectionTileText(Graphics g) {
		int i = input.y / tileSize;
		
		if (!input.mouseOnScreen || paused) return;
		
		if (i == selectedTile) g.setColor(new Color(60, 250, 60));
		else g.setColor(Color.WHITE);
		g.setFont(font12);
		
		switch (i) {
		case 0:
			if (input.y >= tileSize * i && input.y < tileSize * i + tileSize) {
				int yoff = 0;
				if (input.y < 10) yoff = 30;
				g.drawString("Blank", input.x, input.y + yoff);
			}
			break;
		case 1:
			if (input.y >= tileSize * i && input.y < tileSize * i + tileSize) {
				g.drawString("Wire", input.x, input.y);
			}
			break;
		case 2:
			if (input.y >= tileSize * i && input.y < tileSize * i + tileSize) {
				g.drawString("Inverter", input.x, input.y);
			}
			break;
		case 3:
			if (input.y >= tileSize * i && input.y < tileSize * i + tileSize) {
				g.drawString("Power", input.x, input.y);
			}
			break;
		}
	}
	
	private void update() {
		if (paused) return;
		updateConnections();
		if (ticks++ < 12) return;
		ticks = 0;
		resetPower();
		floodFillGrid();
		
		if (getTitle().equals(GAME_TITLE)) {
			if (!saved) setTitle("*" + GAME_TITLE);
		} else if (saved) setTitle(GAME_TITLE);
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
		if (tile.type == TileType.BLANK || tile.type == TileType.NULL || curTile.type == TileType.BLANK
				|| curTile.type == TileType.NULL) return false;
		switch (curTile.type) {
		case INVERTER:
			if (direction != curTile.direction && direction != curTile.direction.opposite()) return false; //Must be either in front, or behind us to affect us
			switch (tile.type) {
			case INVERTER:
				if (tile.direction == curTile.direction || tile.direction == curTile.direction.opposite()) return curTile.direction == tile.direction; //Inverters only connect if they're facing the same way
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
			else if (tile.type == TileType.WIRE) return tile.powered == curTile.powered;
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
			} else if (tile.type == TileType.POWER) {
				return tile.powered == curTile.powered;
			} else if (tile.type == TileType.WIRE) return true;
			return false;
		default:
			return false;
		}
	}
	
	/** Sets all inverters' power state to whatever the tile on their input side is <br/>Then sets all wires' power state to off */
	private void resetPower() {
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				if (grid.tiles[y * grid.width + x].type == TileType.INVERTER) {
					Tile t = new Tile(TileType.NULL);
					switch (grid.tiles[y * grid.width + x].direction) {
					case NORTH:
						t = getTileAt(x, y + 1);
						break;
					case EAST:
						t = getTileAt(x - 1, y);
						break;
					case SOUTH:
						t = getTileAt(x, y - 1);
						break;
					case WEST:
						t = getTileAt(x + 1, y);
						break;
					default:
						new IllegalStateException("Inverter tile has invalid direction: "
								+ grid.tiles[y * grid.width + x].direction + " @ x: " + x + ",y: " + y).printStackTrace();
					}
					
					if (t.type != TileType.NULL) {
						if (t.type == TileType.INVERTER && t.direction == grid.tiles[y * grid.width + x].direction) {
							grid.tiles[y * grid.width + x].powered = !t.powered;
						} else {
							grid.tiles[y * grid.width + x].powered = t.powered;
						}
					}
				}
			}
		}
		
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				Tile t = getTileAt(x, y);
				if (t.type == TileType.WIRE) grid.tiles[y * grid.width + x].powered = false;
			}
		}
	}
	
	/** Calls floodFillAllExcept on every power source in the grid */
	private void floodFillGrid() {
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				if ((grid.tiles[y * grid.width + x].type == TileType.POWER && grid.tiles[y * grid.width + x].powered)
						|| (grid.tiles[y * grid.width + x].type == TileType.INVERTER && !grid.tiles[y * grid.width + x].powered)) {
					floodFillAllExcept(x, y, Direction.NONE); //Start flood filling at power sources which are giving out power
				}
			}
		}
	}
	
	/** Calls floodFill on all this tile's neighbours (except the one towards direction comingFrom). */
	private void floodFillAllExcept(int x, int y, Direction comingFrom) {
		if (getTileAt(x, y).type == TileType.INVERTER) { //Inverters only power one direction
			if (getTileAt(x, y).powered) return;
			switch (getTileAt(x, y).direction) {
			case NORTH:
				if (comingFrom != Direction.NORTH) floodFill(x, y - 1, Direction.SOUTH);
				break;
			case EAST:
				if (comingFrom != Direction.EAST) floodFill(x + 1, y, Direction.WEST);
				break;
			case SOUTH:
				if (comingFrom != Direction.SOUTH) floodFill(x, y + 1, Direction.NORTH);
				break;
			case WEST:
				if (comingFrom != Direction.WEST) floodFill(x - 1, y, Direction.EAST);
				break;
			default:
				break;
			}
		} else { //Power tiles power all directions
			if (comingFrom != Direction.NORTH) floodFill(x, y - 1, Direction.SOUTH);
			if (comingFrom != Direction.EAST) floodFill(x + 1, y, Direction.WEST);
			if (comingFrom != Direction.SOUTH) floodFill(x, y + 1, Direction.NORTH);
			if (comingFrom != Direction.WEST) floodFill(x - 1, y, Direction.EAST);
		}
	}
	
	private void floodFill(int x, int y, Direction comingFrom) {
		Tile t = getTileAt(x, y);
		if (t.type != TileType.WIRE) return; //only update wires
		if (grid.tiles[y * grid.width + x].powered) return;
		grid.tiles[y * grid.width + x].powered = true;
		
		checkDirection(t, x, y, 0, comingFrom, 0, -1); //N
		checkDirection(t, x, y, 1, comingFrom, 1, 0); //E
		checkDirection(t, x, y, 2, comingFrom, 0, 1); //S
		checkDirection(t, x, y, 3, comingFrom, -1, 0); //W
	}
	
	private void checkDirection(Tile t, int x, int y, int direction, Direction comingFrom, int xoff, int yoff) {
		Direction d = Direction.NONE;
		if (direction == 0) d = Direction.NORTH;
		else if (direction == 1) d = Direction.EAST;
		else if (direction == 2) d = Direction.SOUTH;
		else if (direction == 3) d = Direction.WEST;
		else new IllegalArgumentException("Illegal direction passed to checkDirection: " + direction + " @ x: " + x + ", y: " + y)
				.printStackTrace();
		
		if (t.neighbours[direction] && !comingFrom.equals(d)) {
			Tile n = getTileAt(x + xoff, y + yoff); //neighbour towards Direction d
			if (n.type != TileType.NULL) {
				if (n.type == TileType.INVERTER || n.type == TileType.POWER) {
					return;
				} else {
					grid.tiles[(y + yoff) * grid.width + x + xoff].powered = true;
				}
				
				floodFillAllExcept(x + xoff, y + yoff, d.opposite());
			}
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
		
		//Resume Button
		if (paused && resume.mouseInBounds(input)) {
			resume.hover = true;
			if (input.leftDown || input.rightDown) {
				paused = false;
				input.releaseAll(); //to prevent placing a tile when resuming the game
				return;
			}
		} else resume.hover = false;
		
		if (paused) {
			input.releaseAll();
			hoverTileX = -2;
			hoverTileY = -2;
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
				input.releaseAll();
				saveBoard();
			}
		} else saveGame.hover = false;
		
		//Load Game Button
		if (loadGame.mouseInBounds(input)) {
			loadGame.hover = true;
			if (input.leftDown || input.rightDown) {
				input.releaseAll();
				loadBoard();
			}
		} else loadGame.hover = false;
		
		//Help Button
		if (help.mouseInBounds(input)) {
			help.hover = true;
			if (input.leftDown || input.rightDown) {
				input.releaseAll();
				JOptionPane.showMessageDialog(null,
						"Circuit is a virtual electronic circuit builder/tester made by AJ Weeks in April 2014.\r\n"
								+ "-Left click to place/roatate objects on the grid.\r\n"
								+ "-Right click to clear a spot on the grid.\r\n"
								+ "-Hold down Ctrl while clicking and dragging the mouse to draw.\r\n"
								+ "-Use the number keys to quickly select different tile types.\r\n"
								+ "-Hit esc to pause/unpause", "Help", JOptionPane.PLAIN_MESSAGE);
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
				if (y + 1 <= selectionGrid.length) {
					selectedTile = y; //Check if the selected tile has a tile to select
				}
			} else { //Click in the game board
				if (grid.tiles[y * grid.width + x].type != selectionGrid[selectedTile].type) { //If the tile clicked is different than selected tile
					if (saved) saved = false;
					grid.tiles[y * grid.width + x] = selectionGrid[selectedTile].copy();
				} else { //The selected tile is the same type as the tile being clicked, so rotate it
					switch (grid.tiles[y * grid.width + x].type) {
					case INVERTER:
						grid.tiles[y * grid.width + x].direction = Tile.rotateCW(grid, x, y);
						if (saved) saved = false;
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
			if (x == -1) return;
			if (grid.tiles[y * grid.width + x].type == TileType.BLANK) return;
			if (saved) saved = false;
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
				chooser.setSelectedFile(new File(savesDirectory.getName() + "/" + chooser.getSelectedFile().getName() + ".ser"));
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
				saved = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void loadBoard() {
		if (!saved && !grid.isEmpty()) {
			if (JOptionPane.showConfirmDialog(null, "You haven't saved! All work will be lost! Continue?", "Unsaved Work!",
					JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
		}
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
					saved = true;
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
