package ca.ab.cbe.wahs.ajw;

import static ca.ab.cbe.wahs.ajw.Circuit.tileSize;

import java.awt.Graphics;
import java.io.Serializable;

public class Tile implements Serializable {
	private static final long serialVersionUID = 2L;
	
	/** index 0 = N, 1 = E, 2 = S, 3 = W */
	public boolean[] neighbours;
	public TileType type;
	public Direction direction;
	public boolean powered;
	
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
			switch (direction) {
			case NORTH:
				if (this.powered) g.drawImage(Circuit.inverterN_ON, x, y - tileSize, null);
				else g.drawImage(Circuit.inverterN_OFF, x, y - tileSize, null);
				
				g.setColor(this.powered ? colour.darkRed : colour.lightRed);
				if (this.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2 - 5); //N
				g.setColor(this.powered ? colour.lightRed : colour.darkRed);
				if (this.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2), 5, tileSize / 2); //S
				break;
			case EAST:
				if (this.powered) g.drawImage(Circuit.inverterE_ON, x, y - tileSize, null);
				else g.drawImage(Circuit.inverterE_OFF, x, y - tileSize, null);
				
				g.setColor(this.powered ? colour.darkRed : colour.lightRed);
				if (this.neighbours[1]) g.fillRect(x + (tileSize / 2) + 4, y - (tileSize / 2) - 1, (tileSize / 2) - 4, 5); //E
				g.setColor(this.powered ? colour.lightRed : colour.darkRed);
				if (this.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //W
				break;
			case SOUTH:
				if (this.powered) g.drawImage(Circuit.inverterS_ON, x, y - tileSize, null);
				else g.drawImage(Circuit.inverterS_OFF, x, y - tileSize, null);
				
				g.setColor(this.powered ? colour.lightRed : colour.darkRed);
				if (this.neighbours[0]) g.fillRect(x + (tileSize / 2) - 1, y - tileSize, 5, tileSize / 2); //N
				g.setColor(this.powered ? colour.darkRed : colour.lightRed);
				if (this.neighbours[2]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) + 5, 5, tileSize / 2 - 5); //S
				break;
			case WEST:
				if (this.powered) g.drawImage(Circuit.inverterW_ON, x, y - tileSize, null);
				else g.drawImage(Circuit.inverterW_OFF, x, y - tileSize, null);
				
				g.setColor(this.powered ? colour.lightRed : colour.darkRed);
				if (this.neighbours[1]) g.fillRect(x + (tileSize / 2) - 1, y - (tileSize / 2) - 1, (tileSize / 2) + 1, 5); //E
				g.setColor(this.powered ? colour.darkRed : colour.lightRed);
				if (this.neighbours[3]) g.fillRect(x, y - (tileSize / 2) - 1, (tileSize / 2) - 5, 5); //W
				break;
			default:
				new IllegalStateException("Inverter has an illegal direction: " + direction + " @ x:" + x + ",y: " + y).printStackTrace();
				return;
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
	
	public Tile copy() {
		return new Tile(this.type, this.direction, this.neighbours.clone(), this.powered);
	}
}
