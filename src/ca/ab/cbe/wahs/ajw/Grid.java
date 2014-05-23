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
}
