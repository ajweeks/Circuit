package ca.ab.cbe.wahs.ajw;

public class Tile {
	
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
		direction = Direction.NULL;
		powered = (type == TileType.POWER ? true : false);
		neighbours = new boolean[] { false, false, false, false };
	}
	
	public static Tile newBlankTile() {
		return new Tile(TileType.BLANK, Direction.NULL);
	}
	
	public Tile copy() {
		return new Tile(this.type, this.direction, this.neighbours, this.powered);
	}
}
