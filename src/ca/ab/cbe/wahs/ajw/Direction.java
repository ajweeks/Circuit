package ca.ab.cbe.wahs.ajw;

/**  */
public enum Direction {
	NONE, NORTH, EAST, SOUTH, WEST;
	
	public Direction opposite(Direction direction) {
		switch (direction) {
		case NORTH:
			return SOUTH;
		case EAST:
			return WEST;
		case SOUTH:
			return NORTH;
		case WEST:
			return EAST;
		case NONE:
			return NONE;
		default:
			System.err.println("Invalid direction passed to opposite, " + direction.toString());
			return NONE;
		}
	}
}
