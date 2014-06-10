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
	
	/** @return A new grid who's tiles all have type <code>BLANK</code> */
	public static Grid clearBoard(int width, int height) {
		return new Grid(width, height);
	}
	
	/** @return A new grid who's tiles all have type <code>WIRE</code> */
	public static Grid allWires(int width, int height) {
		Grid grid = new Grid(width, height);
		for (int i = 0; i < grid.tiles.length; i++) {
			grid.tiles[i].type = TileType.WIRE;
			grid.tiles[i].direction = Direction.NONE;
		}
		return grid;
	}
	
	/** @return <code>true</code> if all tiles have type <code>BLANK</code> */
	public boolean isEmpty() {
		for (int i = 0; i < this.width * this.height; i++) {
			if (this.tiles[i].type != TileType.BLANK) return false;
		}
		return true;
	}
	
}
