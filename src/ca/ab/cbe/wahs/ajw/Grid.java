package ca.ab.cbe.wahs.ajw;

import java.io.Serializable;

public class Grid implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Tile[] tiles;
	public int height, width;
	
	public Grid(int width, int height) {
		this.height = height;
		this.width = width;
		
		tiles = new Tile[width * height];
		
		for (int i = 0; i < height * width; i++) {
			tiles[i] = new Tile(TileType.BLANK);
		}
	}
	
	public Grid clearBoard(Grid grid, int width, int height) {
		return new Grid(width, height);
	}
	
	/** @returns the direction of the tile at grid[y][x] rotated clockwise once */
	public Direction rotateCW(int x, int y) {
		switch (this.tiles[y * width + x].direction) {
		case NORTH:
			return Direction.EAST;
		case EAST:
			return Direction.SOUTH;
		case SOUTH:
			return Direction.WEST;
		case WEST:
			return Direction.NORTH;
		case NULL:
			System.out.println("rotating null!");
			return Direction.NULL;
		default:
			System.out.println("invalid direction!");
			return Direction.NULL;
		}
	}
}
