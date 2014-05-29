package ca.ab.cbe.wahs.ajw;

import static ca.ab.cbe.wahs.ajw.Circuit.tileSize;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.io.Serializable;

import javax.swing.ImageIcon;

public class Tile implements Serializable {
	private static final long serialVersionUID = 2L;
	
	private Image inverterN_ON;
	private Image inverterN_OFF;
	private Image inverterE_ON;
	private Image inverterE_OFF;
	private Image inverterS_ON;
	private Image inverterS_OFF;
	private Image inverterW_ON;
	private Image inverterW_OFF;
	
	/** index 0 = N, 1 = E, 2 = S, 3 = W */
	public boolean[] neighbours;
	public TileType type;
	public Direction direction;
	public boolean powered;
	
	//Image initialization
	{
		inverterN_ON = new ImageIcon("res/inverterN_ON.png").getImage();
		inverterN_OFF = new ImageIcon("res/inverterN_OFF.png").getImage();
		inverterE_ON = new ImageIcon("res/inverterE_ON.png").getImage();
		inverterE_OFF = new ImageIcon("res/inverterE_OFF.png").getImage();
		inverterS_ON = new ImageIcon("res/inverterS_ON.png").getImage();
		inverterS_OFF = new ImageIcon("res/inverterS_OFF.png").getImage();
		inverterW_ON = new ImageIcon("res/inverterW_ON.png").getImage();
		inverterW_OFF = new ImageIcon("res/inverterW_OFF.png").getImage();
	}
	
	public Tile(TileType type, Direction direction, boolean[] neighbors, boolean powered) {
		this.type = type;
		this.direction = direction;
		this.powered = powered;
		this.neighbours = neighbors;
	}
	
	public Tile(TileType type, Direction direction) {
		this.type = type;
		this.direction = direction;
		powered = (type == TileType.POWER ? true : false);
		neighbours = new boolean[] { false, false, false, false };
	}
	
	public Tile(TileType type) {
		this.type = type;
		direction = Direction.NONE;
		powered = (type == TileType.POWER ? true : false);
		neighbours = new boolean[] { false, false, false, false };
	}
	
	/** @param y - x position on screen
	    @param x - y position on screen
	     */
	public void render(int x, int y, Graphics g, Colour colour) {
		g.setColor(this.powered ? colour.lightRed : colour.darkRed);
		
		switch (this.type) {
		case WIRE:
			if (this.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2); //N
			if (this.neighbours[1]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //E
			if (this.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2), 5, tileSize / 2); //S
			if (this.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) + 4, 5); //W
				
			if (!this.neighbours[0] && !this.neighbours[1] && !this.neighbours[2] && !this.neighbours[3]) { //There are no neighbours
				g.fillRect(x + (tileSize / 2) - 1, (int) (y - (tileSize * 0.7)), 5, tileSize / 2); //V
				g.fillRect((int) (x + tileSize * 0.3), y - (tileSize / 2) - 1, tileSize / 2, 5); //H
			}
			break;
		case INVERTER:
			if (this.direction == Direction.NORTH) {
				if (this.powered) g.drawImage(inverterN_ON, x, y - tileSize, null);
				else g.drawImage(inverterN_OFF, x, y - tileSize, null);
				
				g.setColor(this.powered ? colour.darkRed : colour.lightRed);
				if (this.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2 - 5); //N
				g.setColor(this.powered ? colour.lightRed : colour.darkRed);
				if (this.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2), 5, tileSize / 2); //S
					
			} else if (this.direction == Direction.EAST) {
				if (this.powered) g.drawImage(inverterE_ON, x, y - tileSize, null);
				else g.drawImage(inverterE_OFF, x, y - tileSize, null);
				
				g.setColor(this.powered ? colour.darkRed : colour.lightRed);
				if (this.neighbours[1]) g.fillRect(x + (tileSize / 2) + 4, y - (tileSize / 2) - 1, (tileSize / 2) - 4, 5); //E
				g.setColor(this.powered ? colour.lightRed : colour.darkRed);
				if (this.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //W
					
			} else if (this.direction == Direction.SOUTH) {
				if (this.powered) g.drawImage(inverterS_ON, x, y - tileSize, null);
				else g.drawImage(inverterS_OFF, x, y - tileSize, null);
				
				g.setColor(this.powered ? colour.lightRed : colour.darkRed);
				if (this.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2); //N
				g.setColor(this.powered ? colour.darkRed : colour.lightRed);
				if (this.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) + 5, 5, tileSize / 2 - 5); //S
					
			} else if (this.direction == Direction.WEST) {
				if (this.powered) g.drawImage(inverterW_ON, x, y - tileSize, null);
				else g.drawImage(inverterW_OFF, x, y - tileSize, null);
				
				g.setColor(this.powered ? colour.lightRed : colour.darkRed);
				if (this.neighbours[1]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //E
				g.setColor(this.powered ? colour.darkRed : colour.lightRed);
				if (this.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) - 5, 5); //W
					
			} else { //problems
				g.setColor(Color.RED);
				g.fillRect(x + 1, y - tileSize + 1, tileSize - 1, tileSize - 1);
			}
			break;
		case POWER:
			g.setColor(this.powered ? colour.lightRed : colour.darkRed);
			g.fillOval(x + (tileSize / 2) - 5, y - (tileSize / 2) - 6, (tileSize / 3), (tileSize / 3));
			
			if (this.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2); //N
			if (this.neighbours[1]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //E
			if (this.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2), 5, tileSize / 2); //S
			if (this.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) + 4, 5); //W
			break;
		default:
			break;
		}
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
	
	public static Tile newBlankTile() {
		return new Tile(TileType.BLANK);
	}
	
	public static Tile copy(Tile tile) {
		return new Tile(tile.type, tile.direction, tile.neighbours, tile.powered);
	}
}
