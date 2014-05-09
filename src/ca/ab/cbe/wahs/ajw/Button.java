package ca.ab.cbe.wahs.ajw;

import java.awt.Color;

public class Button {
	
	int x, y, width, height;
	boolean hover = false;
	Color colour, hoverColour;
	String text;
	
	public Button(int x, int y, int width, int height, Color colour, Color hoverColour, String text) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.colour = colour;
		this.hoverColour = hoverColour;
		this.text = text;
	}
	
	public boolean mouseInBounds(Input input) {
		return (input.x > this.x && input.x < this.x + this.width && input.y > this.y && input.y < this.y + this.height);
	}
	
}
