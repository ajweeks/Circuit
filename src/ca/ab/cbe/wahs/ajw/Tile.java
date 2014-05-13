package ca.ab.cbe.wahs.ajw;

public class Tile {
	
	public Type type;
	public Direction direction;
	public boolean powered;
	/** index 0 = N, 1 = E, 2 = S, 3 = W */
	public boolean[] neighbours;
	
	public Tile(Type type) {
		this.type = type;
		direction = Direction.NULL;
		powered = false;
		neighbours = new boolean[] { false, false, false, false };
	}
}
