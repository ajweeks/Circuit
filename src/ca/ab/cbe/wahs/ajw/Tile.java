package ca.ab.cbe.wahs.ajw;

public class Tile {
	
	public TileType type;
	public Direction direction;
	public boolean powered;
	/** index 0 = N, 1 = E, 2 = S, 3 = W */
	public boolean[] neighbours;
	
	public Tile(TileType type, Direction direction) {
		this.type = type;
		this.direction = direction;
		powered = false;
		neighbours = new boolean[] { false, false, false, false };
	}
	
	public Tile(TileType type) {
		this.type = type;
		direction = Direction.NULL;
		powered = false;
		neighbours = new boolean[] { false, false, false, false };
	}
}
