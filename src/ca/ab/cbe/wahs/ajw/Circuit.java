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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
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
	
	/** This tile is RECIEVING power from the North */
	private Image inverterN;
	/** This tile is RECIEVING power from the East */
	private Image inverterE;
	/** This tile is RECIEVING power from the South */
	private Image inverterS;
	/** This tile is RECIEVING power from the West*/
	private Image inverterW;
	
	private File curDir = new File("");
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
		selectionGrid = new Tile[] { new Tile(TileType.BLANK), new Tile(TileType.WIRE),
				new Tile(TileType.INVERTER, Direction.NORTH), new Tile(TileType.POWER) };
		selectedTile = 0;
		
		inverterN = new ImageIcon("res/inverterN.png").getImage();
		inverterE = new ImageIcon("res/inverterE.png").getImage();
		inverterS = new ImageIcon("res/inverterS.png").getImage();
		inverterW = new ImageIcon("res/inverterW.png").getImage();
		
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
		
		if (!canvas.hasFocus()) paused = true; //Automatically pause the game if the user has clicked on another window
		
		if (paused) {
			//Render translucent gray over entire screen
			g.setColor(new Color(65, 75, 75, 160));
			g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
			
			g.setFont(font.deriveFont(32f));
			g.setColor(Color.white);
			g.drawString("PAUSED", (canvas.getWidth() / 2) - (getFontMetrics(g.getFont()).stringWidth("PAUSED") / 2),
					200);
			
			g.setFont(font.deriveFont(20f));
			g.drawString("(esc to unpause)",
					(canvas.getWidth() / 2) - (getFontMetrics(g.getFont()).stringWidth("(esc to unpause)") / 2), 250);
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
				if (tile.powered) g.drawImage(inverterS, x, y - tileSize, null);
				else g.drawImage(inverterN, x, y - tileSize, null);
			} else if (tile.direction == Direction.EAST) {
				if (tile.powered) g.drawImage(inverterE, x, y - tileSize, null);
				else g.drawImage(inverterW, x, y - tileSize, null);
			} else { //problems
				g.setColor(Color.RED);
				g.fillRect(x + 1, y - tileSize + 1, tileSize - 1, tileSize - 1);
			}
			break;
		case POWER:
			g.setColor(colour.lightRed);
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
				if (grid.tiles[y * grid.width + x].type == TileType.BLANK) continue; //No need updating blank tiles
				
				grid.tiles[y * grid.width + x].powered = checkPowered(x, y);
				grid.tiles[y * grid.width + x].neighbours = updateConnections(x, y);
			}
		}
	}
	
	private boolean[] updateConnections(int x, int y) {
		boolean[] newNeighbors = new boolean[] { false, false, false, false };
		if (grid.tiles[y * grid.width + x].type == TileType.BLANK) return newNeighbors;
		
		Tile above = getTileAt(x, y - 1);
		Tile below = getTileAt(x, y + 1);
		Tile right = getTileAt(x + 1, y);
		Tile left = getTileAt(x - 1, y);
		
		if (above.type == TileType.NULL) newNeighbors[0] = false; //type is null if current tile is at the top of the grid
		else {
			if (above.type == TileType.BLANK) newNeighbors[0] = false; //Above
			else if (above.type == TileType.INVERTER) newNeighbors[0] = (above.direction == Direction.NORTH);
			else newNeighbors[0] = true;
		}
		
		if (right.type == TileType.NULL) newNeighbors[1] = false; //type is null if current tile is at the right side of the grid
		else {
			if (right.type == TileType.BLANK) newNeighbors[1] = false; //Right
			else if (right.type == TileType.INVERTER) newNeighbors[1] = (right.direction == Direction.EAST);
			else newNeighbors[1] = true;
		}
		
		if (below.type == TileType.NULL) newNeighbors[2] = false; //type is null if current tile is at the bottom of the grid
		else {
			if (below.type == TileType.BLANK) newNeighbors[2] = false; //Below
			else if (below.type == TileType.INVERTER) newNeighbors[2] = (below.direction == Direction.NORTH);
			else newNeighbors[2] = true;
		}
		
		if (left.type == TileType.NULL) newNeighbors[3] = false; //type is null if current tile is at the right side of the grid
		else {
			if (left.type == TileType.BLANK) newNeighbors[3] = false; //Left
			else if (left.type == TileType.INVERTER) newNeighbors[3] = (left.direction == Direction.EAST);
			else newNeighbors[3] = true;
		}
		
		return newNeighbors;
	}
	
	private boolean checkPowered(int x, int y) {
		if (grid.tiles[y * grid.width + x].type == TileType.BLANK) return false;
		if (grid.tiles[y * grid.width + x].type == TileType.POWER) return true;
		
		Tile above = getTileAt(x, y - 1);
		Tile below = getTileAt(x, y + 1);
		Tile right = getTileAt(x + 1, y);
		Tile left = getTileAt(x - 1, y);
		
		if (above.type != TileType.NULL) { //type will be null if the current tile is at the top of the grid
			if (above.direction == Direction.NONE) if (above.powered) return true;
			if (above.type == TileType.INVERTER) {
				if (above.direction == Direction.NORTH) if (above.powered) return true;
			}
		}
		
		if (below.type != TileType.NULL) { //type will be null if the current tile is at the bottom of the grid
			if (below.direction == Direction.NONE) if (below.powered) return true;
			if (below.type == TileType.INVERTER) {
				if (below.direction == Direction.NORTH) if (below.powered) return true;
			}
		}
		
		if (right.type != TileType.NULL) { //type will be null if the current tile is at the right side of the grid
			if (right.direction == Direction.NONE) if (right.powered) return true;
			if (right.type == TileType.INVERTER) {
				if (right.direction == Direction.EAST) if (right.powered) return true;
			}
		}
		
		if (left.type != TileType.NULL) { //type will be null if the current tile is at the left side of the grid
			if (left.direction == Direction.NONE) if (left.powered) return true;
			if (left.type == TileType.INVERTER) {
				if (left.direction == Direction.EAST) if (left.powered) return true;
			}
		}
		
		return false;
	}
	
	/** @returns the tile with x and y coordinates, or a Tile with type NULL if x or y are out of range */
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
				grid = clearBoard(grid, boardSize, boardSize);
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
				String message = "Circuit is a virtual electronic circuit builder/tester made by AJ Weeks in April 2014.\r\n"
						+ "Left click to place/roatate objects on the grid.\r\n"
						+ "Right click to clear a spot on the grid.\r\n"
						+ "Hold down Ctrl while clicking and dragging the mouse to draw.\r\n"
						+ "Use the number keys to quickly select different tile tile types.\r\n"
						+ "Hit esc to pause/unpause";
				JOptionPane.showMessageDialog(this, message, "Circuit", JOptionPane.PLAIN_MESSAGE);
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
						grid.tiles[y * grid.width + x].direction = rotateInverterCW(grid, x, y);
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
	
	/** @returns new blank board */
	public static Grid clearBoard(Grid grid, int width, int height) {
		return new Grid(width, height);
	}
	
	/** @returns EAST if direction is NORTH, or NORTH if direction is EAST */
	public static Direction rotateInverterCW(Grid grid, int x, int y) {
		switch (grid.tiles[y * grid.width + x].direction) {
		case NORTH:
			return Direction.EAST;
		case EAST:
			return Direction.NORTH;
		default:
			System.err.println("Invalid inverter direction! x: " + x + " y: " + y);
			return Direction.NONE;
		}
		
	}
	
	/** @returns the direction of the tile at grid[y][x] rotated clockwise once */
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
		JFileChooser chooser = new JFileChooser();
		try {
			chooser.setCurrentDirectory(curDir.getCanonicalFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
		chooser.setFileFilter(new FileNameExtensionFilter("*.ser", "ser"));
		chooser.setSelectedFile(new File("save.ser"));
		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File save = new File(chooser.getSelectedFile().getName());
			if (save.exists()) save.delete();
			
			try {
				save.createNewFile();
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(chooser.getSelectedFile()
						.getName()));
				out.writeObject(grid);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void loadBoard() {
		JFileChooser chooser = new JFileChooser();
		try {
			chooser.setCurrentDirectory(curDir.getCanonicalFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileFilter(new FileNameExtensionFilter("*.ser", "ser"));
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				FileInputStream fileInput = new FileInputStream(chooser.getSelectedFile().getName());
				ObjectInputStream in = new ObjectInputStream(fileInput);
				Grid newGrid = new Grid(boardSize, boardSize);
				newGrid = (Grid) in.readObject();
				in.close();
				
				grid = newGrid;
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** @param y - the y position of the mouse on screen 
	 *  @returns the row the mouse is in on the game grid */
	public int getMouseRow(int y) {
		return Math.max(0, Math.min((y - 1) / tileSize, boardSize - 1));
	}
	
	/** @param x - the x position of the mouse on screen 
	 *  @returns the column the mouse is in on the game grid OR -1 if mouse is outside of game grid */
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
