package ca.ab.cbe.wahs.ajw;

import java.io.Serializable;

public class Grid implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Tile[][] tiles;
	public int height, width;
	
	public Grid(int width, int height) {
		this.height = height;
		this.width = width;
		
		tiles = new Tile[height][width];
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				tiles[y][x] = new Tile(Type.BLANK);
			}
		}
	}
	
	public Grid clearBoard(Grid grid, int width, int height) {
		return new Grid(width, height);
	}
	
	/** Rotates the direction of the tile at grid[y][x] clockwise once */
	public void rotateCW(int x, int y) {
		switch (this.tiles[y][x].direction) {
		case NORTH:
			this.tiles[y][x].direction = Direction.EAST;
			break;
		case EAST:
			this.tiles[y][x].direction = Direction.SOUTH;
			break;
		case SOUTH:
			this.tiles[y][x].direction = Direction.WEST;
			break;
		case WEST:
			this.tiles[y][x].direction = Direction.NORTH;
			break;
		case NULL:
			break;
		}
	}
}
