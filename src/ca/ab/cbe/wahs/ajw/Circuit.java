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
	
	public static int boardSize = 18; //Number of tiles wide and tall the board is
	public static int tileSize = 36; //Number of pixels per tile
	public volatile boolean running = false;
	public volatile boolean paused = false;
	
	private Button clearBoard;
	private Button saveGame;
	private Button loadGame;
	private Button help;
	
	private Image icon;
	
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
			hoverTileType.render(hoverTileX * tileSize, hoverTileY * tileSize + tileSize, g, colour);
			g.setColor(new Color(20, 20, 20, 100));
			g.fillRect(hoverTileX * tileSize + 1, hoverTileY * tileSize + 1, tileSize - 1, tileSize - 1);
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
		
		//TODO re-enable focus manager
		//if (!canvas.hasFocus()) paused = true; //Automatically pause the game if the user has clicked on another window
		
		if (paused) {
			//Translucent gray over entire screen
			g.setColor(new Color(65, 75, 75, 160));
			g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
			
			g.setFont(font.deriveFont(32f));
			g.setColor(Color.white);
			g.drawString("PAUSED", (canvas.getWidth() / 2) - (getFontMetrics(g.getFont()).stringWidth("PAUSED") / 2), 200);
			
			g.setFont(font.deriveFont(20f));
			g.drawString("(esc to unpause)", (canvas.getWidth() / 2) - (getFontMetrics(g.getFont()).stringWidth("(esc to unpause)") / 2), 250);
		}
		
		g.dispose();
		buffer.show();
	}
	
	private void update() {
		//Update game grid
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				if (grid.tiles[y * grid.width + x].type == TileType.BLANK || grid.tiles[y * grid.width + x].type == TileType.NULL) continue; //No need updating blank or null tiles
				//grid.tiles[y * grid.width + x].powered = checkPowered(x, y);
				grid.tiles[y * grid.width + x].neighbours = updateConnections(x, y);
			}
		}
		for (int i = 0; i < boardSize; i++) { //TODO yeah...
			updatePower();
		}
	}
	
	private void updatePower() {
		boolean[] checked = new boolean[boardSize * boardSize];
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				if (grid.tiles[y * grid.width + x].type == TileType.INVERTER) {
					switch (grid.tiles[y * grid.width + x].direction) {
					case NORTH:
						Tile tN = getTileAt(x, y + 1);
						if (tN.type == TileType.NULL) continue;
						else if (tN.type == TileType.INVERTER) grid.tiles[y * grid.width + x].powered = !tN.powered;
						else grid.tiles[y * grid.width + x].powered = tN.powered;
						break;
					case EAST:
						Tile tE = getTileAt(x - 1, y);
						if (tE.type == TileType.NULL) continue;
						else if (tE.type == TileType.INVERTER) grid.tiles[y * grid.width + x].powered = !tE.powered;
						else grid.tiles[y * grid.width + x].powered = tE.powered;
						break;
					case SOUTH:
						Tile tS = getTileAt(x, y + 1);
						if (tS.type == TileType.NULL) continue;
						else if (tS.type == TileType.INVERTER) grid.tiles[y * grid.width + x].powered = !tS.powered;
						else grid.tiles[y * grid.width + x].powered = tS.powered;
						break;
					case WEST:
						Tile tW = getTileAt(x, y + 1);
						if (tW.type == TileType.NULL) continue;
						else if (tW.type == TileType.INVERTER) grid.tiles[y * grid.width + x].powered = !tW.powered;
						else grid.tiles[y * grid.width + x].powered = tW.powered;
						break;
					default:
						break;
					}
				} else if (grid.tiles[y * grid.width + x].type == TileType.POWER) checkNeighbours(grid.tiles[y * grid.width + x], x, y, checked);
			}
		}
	}
	
	private void checkNeighbours(Tile t, int x, int y, boolean[] checked) {
		if (t.powered) {
			powerUp(t, x, y, checked);
			powerRight(t, x, y, checked);
			powerDown(t, x, y, checked);
			powerLeft(t, x, y, checked);
		} else { //not powered
			unPowerUp(t, x, y, checked);
			unPowerRight(t, x, y, checked);
			unPowerDown(t, x, y, checked);
			unPowerLeft(t, x, y, checked);
		}
	}
	
	private void powerUp(Tile t, int x, int y, boolean[] checked) {
		if (y - 1 < 0) return;
		if (checked[y * boardSize + x]) return;
		else checked[y * boardSize + x] = true;
		if (getTileAt(x, y - 1).type == TileType.NULL) return;
		switch (getTileAt(x, y - 1).type) {
		case BLANK:
		case NULL:
		case POWER:
			return;
		case WIRE:
			grid.tiles[(y - 1) * grid.width + x].powered = true;
			checkNeighbours(grid.tiles[(y - 1) * grid.width + x], x, y - 1, checked);
		case INVERTER:
			if (grid.tiles[(y - 1) * grid.width + x].direction == Direction.NORTH) {
				grid.tiles[(y - 1) * grid.width + x].powered = true;
				checkNeighbours(grid.tiles[(y - 1) * grid.width + x], x, y - 1, checked);
			}
		}
	}
	
	private void powerRight(Tile t, int x, int y, boolean[] checked) {
		if (x + 1 > grid.width) return;
		if (checked[y * boardSize + x + 1]) return;
		else checked[y * boardSize + x + 1] = true;
		Tile t2 = getTileAt(x + 1, y);
		switch (t2.type) {
		case BLANK:
		case NULL:
		case POWER:
			return;
		case WIRE:
			t2.powered = true;
		case INVERTER:
			if (t2.direction == Direction.EAST) t2.powered = true;
		}
		grid.tiles[y * grid.width + x + 1].powered = t2.powered;
	}
	
	private void powerDown(Tile t, int x, int y, boolean[] checked) {
		//		if (y + 1 > grid.height) return;
		//		if (checked[(y + 1) * boardSize + x]) return;
		//		else checked[(y + 1) * boardSize + x] = true;
		//		Tile t2 = getTileAt(x, y + 1);
		//		switch (t2.type) {
		//		case BLANK:
		//		case NULL:
		//		case POWER:
		//			return;
		//		case WIRE:
		//		case INVERTER:
		//		}
		//		grid.tiles[(y + 1) * grid.width + x].powered = t2.powered;
	}
	
	private void powerLeft(Tile t, int x, int y, boolean[] checked) {
		//		if (x - 1 < 0) return;
		//		if (checked[y * boardSize + x - 1]) return;
		//		else checked[y * boardSize + x - 1] = true;
		//		Tile t2 = getTileAt(x - 1, y);
		//		switch (t2.type) {
		//		case BLANK:
		//		case NULL:
		//		case POWER:
		//			return;
		//		case WIRE:
		//		case INVERTER:
		//		}
		//		grid.tiles[y * grid.width + x - 1].powered = t2.powered;
	}
	
	private void unPowerUp(Tile t, int x, int y, boolean[] checked) {
		//		if (y - 1 < 0) return;
		//		if (checked[y * boardSize + x]) return;
		//		else checked[y * boardSize + x] = true;
		//		Tile t2 = getTileAt(x, y - 1);
		//		switch (t2.type) {
		//		case BLANK:
		//		case NULL:
		//		case POWER:
		//			return;
		//		case WIRE:
		//		case INVERTER:
		//		}
		//		grid.tiles[(y - 1) * grid.width + x].powered = t2.powered;
	}
	
	private void unPowerRight(Tile t, int x, int y, boolean[] checked) {
		if (x > grid.width) return;
		
	}
	
	private void unPowerDown(Tile t, int x, int y, boolean[] checked) {
		if (y > grid.height) return;
		
	}
	
	private void unPowerLeft(Tile t, int x, int y, boolean[] checked) {
		if (x < 0) return;
		
	}
	
	/*
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
	*/
	
	/** @return Whether <b>tile</b> is giving power towards <b>direction</b> 
	 * @param tile - the tile which you are checking
	 *  @param direction - the direction towards the current tile (only affects inverters) */
	/*
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
	*/
	
	private boolean[] updateConnections(int x, int y) {
		boolean[] newNeighbours = new boolean[] { false, false, false, false };
		Tile curTile = grid.tiles[y * grid.width + x];
		if (curTile.type == TileType.BLANK || curTile.type == TileType.NULL) return newNeighbours; //These tiles don't care about neighbours
			
		Tile above = getTileAt(x, y - 1);
		Tile below = getTileAt(x, y + 1);
		Tile right = getTileAt(x + 1, y);
		Tile left = getTileAt(x - 1, y);
		
		if (connects(curTile, above, Direction.SOUTH)) newNeighbours[0] = true;
		if (connects(curTile, right, Direction.WEST)) newNeighbours[1] = true;
		if (connects(curTile, below, Direction.NORTH)) newNeighbours[2] = true;
		if (connects(curTile, left, Direction.EAST)) newNeighbours[3] = true;
		
		return newNeighbours;
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
						grid.tiles[y * grid.width + x].direction = Tile.rotateCW(grid, x, y);
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
	
	/** Overwrites the existing saveBoard file */
	private void saveBoard() {
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
