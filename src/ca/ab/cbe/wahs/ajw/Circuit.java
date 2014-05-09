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

public class Circuit extends JFrame {
	private static final long serialVersionUID = 1L;
	
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
	
	public File curDir = new File("");
	public Canvas canvas;
	public Grid grid; //Main game board (width & height = boardSize)
	public Tile[] selectionGrid;
	public int selectedTile; //Index in selection grid of the current selected tile
	public Colour colour;
	public Input input;
	public Font font;
	public int hoverTileX, hoverTileY; //X and Y coordinates of the current tile under the mouse
			
	public final Dimension SIZE = new Dimension(780, 639);
	
	public int boardSize = 18; //Number of tiles wide and tall the board is
	public int tileSize = 36; //Number of pixels per tile
	public boolean running = false;
	public boolean paused = false;
	
	public Circuit() {
		super("Circuit");
		
		colour = new Colour();
		input = new Input();
		font = new Font("Arial", Font.BOLD, 36);
		
		clearBoard = new Button(695, 35, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Clear Board");
		saveGame = new Button(695, 65, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Save");
		loadGame = new Button(695, 95, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Load");
		help = new Button(695, 125, 85, 25, colour.buttonColour, colour.buttonHoverColour, "Help");
		
		grid = new Grid(boardSize, boardSize);
		selectionGrid = new Tile[] { new Tile(Tile.Type.BLANK), new Tile(Tile.Type.WIRE), new Tile(Tile.Type.INVERTER),
				new Tile(Tile.Type.POWER) };
		selectedTile = 0;
		
		inverterN = new ImageIcon("res/inverterN.png").getImage();
		inverterE = new ImageIcon("res/inverterE.png").getImage();
		inverterS = new ImageIcon("res/inverterS.png").getImage();
		inverterW = new ImageIcon("res/inverterW.png").getImage();
		
		canvas = new Canvas();
		canvas.setMinimumSize(SIZE);
		canvas.setMaximumSize(SIZE);
		canvas.setPreferredSize(SIZE);
		canvas.addMouseListener(input);
		canvas.addMouseMotionListener(input);
		canvas.addKeyListener(input);
		canvas.setFont(font);
		canvas.setFocusable(true);
		canvas.requestFocus();
		
		add(canvas);
		pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
		
		running = true;
		loop();
	}
	
	private void loop() {
		while (running) {
			pollInput();
			update();
			render();
			try {
				Thread.sleep(1000 / 60); //30 updates / second (ish)
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
		
		//Hover tile background
		if (hoverTileX != -1 && hoverTileY != -1) {
			g.setColor(colour.hover);
			g.fillRect(hoverTileX * tileSize, hoverTileY * tileSize, tileSize, tileSize);
		}
		
		//Main game board grid
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width + 1; x++) {
				g.setColor(colour.lightGray);
				g.drawRect(x * tileSize, y * tileSize, tileSize, tileSize);
			}
		}
		
		//Main game board tiles
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				renderTile(x * tileSize + tileSize, y * tileSize + tileSize, g, grid.tiles[y][x]);
			}
		}
		
		//Outline selected tile
		g.setColor(Color.ORANGE);
		g.drawRect(0, selectedTile * tileSize, tileSize, tileSize);
		
		//Render selection tiles
		for (int y = 0; y < selectionGrid.length; y++) {
			renderTile((y + 1) * tileSize, 0, g, selectionGrid[y]);
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
		
		buffer.show();
		g.dispose();
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
	private void renderTile(int y, int x, Graphics g, Tile tile) {
		g.setColor(tile.powered ? colour.lightRed : colour.darkRed);
		
		switch (tile.type) {
		case WIRE:
			if (tile.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2); //N
			if (tile.neighbours[1]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //E
			if (tile.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize / 2, 5, tileSize / 2); //S
			if (tile.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) + 4, 5); //W
			
			if (!tile.neighbours[0] && !tile.neighbours[1] && !tile.neighbours[2] && !tile.neighbours[3]) { //There are no neighbours
				g.fillRect(x + (tileSize / 2) - 1, (int) (y - (tileSize * 0.7)), 5, tileSize / 2); //V
				g.fillRect((int) (x + tileSize * 0.3), y - (tileSize / 2) - 1, tileSize / 2, 5); //H
			}
			break;
		case INVERTER:
			if (tile.direction == Tile.Direction.NORTH) {
				if (tile.powered) g.drawImage(inverterS, x, y - tileSize, null);
				else g.drawImage(inverterN, x, y - tileSize, null);
			} else if (tile.direction == Tile.Direction.EAST) {
				if (tile.powered) g.drawImage(inverterW, x, y - tileSize, null);
				else g.drawImage(inverterE, x, y - tileSize, null);
			} else if (tile.direction == Tile.Direction.SOUTH) {
				if (tile.powered) g.drawImage(inverterN, x, y - tileSize, null);
				else g.drawImage(inverterS, x, y - tileSize, null);
			} else {
				if (tile.powered) g.drawImage(inverterE, x, y - tileSize, null);
				else g.drawImage(inverterW, x, y - tileSize, null);
			}
			break;
		case POWER:
			g.setColor(colour.lightRed);
			g.fillOval(x + (tileSize / 2) - 7, y - (tileSize / 2) - 6, (tileSize / 3), (tileSize / 3));
			break;
		default:
			break;
		}
	}
	
	private void update() {
		//Update game grid
		for (int y = 0; y < grid.height; y++) {
			for (int x = 0; x < grid.width; x++) {
				if (grid.tiles[y][x].type == Tile.Type.POWER) {
					grid.tiles[y][x].powered = true;
					continue;
				}
				grid.tiles[y][x].powered = checkPowered(x, y);
				updateConnections();
				
			}
		}
	}
	
	private void updateConnections() {
		
	}
	
	private boolean checkPowered(int xpos, int ypos) {
		if (grid.tiles[Math.max(0, ypos - 1)][xpos].powered || grid.tiles[ypos][Math.max(0, xpos - 1)].powered
				|| grid.tiles[ypos][Math.min(grid.tiles[ypos].length - 1, xpos + 1)].powered
				|| grid.tiles[Math.min(0, ypos + 1)][xpos].powered) {
			if (grid.tiles[ypos][xpos].type != Tile.Type.BLANK) return true;
		}
		return false;
	}
	
	private void pollInput() {
		if (input.escape) paused = !paused;
		if (paused) {
			input.releaseAll();
			return;
		}
		
		if (input.num != -1 && input.num < selectionGrid.length) selectedTile = input.num;
		
		int row = getMouseRow(input.y);
		int column = getMouseColumn(input.x);
		
		//Hover tile
		hoverTileX = column;
		hoverTileY = row;
		
		if (row != -1 && column != -1) { //Mouse is in game board or tile selection area
		
			if (input.leftDown) { //Left click in game board or tile selection area
				if (column == 0) { //Mouse is in leftmost column (tile selection area)
					if (selectionGrid.length >= row + 1) selectedTile = row; //Check if the selected tile has a tile to select
				} else { //Click in the game board
					if (grid.tiles[column - 1][row].type.equals(Tile.Type.BLANK)
							|| !grid.tiles[column - 1][row].equals(selectionGrid[selectedTile])) { //If the tile is blank, or a different tile...
						grid.tiles[column - 1][row] = selectionGrid[selectedTile];
					} else { //Cycle through rotations
						switch (grid.tiles[column - 1][row].type) {
						case INVERTER:
							grid.rotateCW(column, row);
						default:
							break;
						}
					}
				}
			} else if (input.rightDown && column > 0) { //Right click clears the tile (except in the tile selection area)
				grid.tiles[column - 1][row].type = Tile.Type.BLANK;
				grid.tiles[column - 1][row].direction = Tile.Direction.NULL;
				grid.tiles[column - 1][row].powered = false;
			}
		}
		
		//Clear Screen Button
		if (clearBoard.mouseInBounds(input)) {
			clearBoard.hover = true;
			if (input.leftDown || input.rightDown) {
				clearBoard();
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
				String message = "Circuit is an electronic circuit builder/tester made by AJ Weeks in April 2014.\r\n"
						+ "Left click to place/roatate objects on the grid.\r\n"
						+ "Right click to clear a spot on the grid.\r\n"
						+ "Use the number keys to quickly select different tile types.\r\n"
						+ "Hit esc to pause/unpause";
				JOptionPane.showMessageDialog(this, message, "Circuit", JOptionPane.PLAIN_MESSAGE);
			}
		} else help.hover = false;
		
		input.releaseAll();
	}
	
	//--------------Helper methods------------------------
	
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
		int returnVal = chooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
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
		int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
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
	
	private void clearBoard() {
		grid = new Grid(boardSize, boardSize);
	}
	
	/** @returns the row the mouse is in on the game grid */
	public int getMouseRow(int y) {
		return Math.min((y - 1) / tileSize, boardSize - 1);
	}
	
	/** @returns the column the mouse is in on the game grid OR -1 if mouse is outside of game grid */
	public int getMouseColumn(int x) {
		if (x > boardSize * tileSize + tileSize) return -1;
		return Math.min((x - 1) / tileSize, boardSize);
	}
	
	public static void main(String[] args) {
		new Circuit();
	}
}
