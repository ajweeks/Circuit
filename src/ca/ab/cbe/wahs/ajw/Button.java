package ca.ab.cbe.wahs.ajw;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

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
	
	public void render(Graphics g, Font font, int xoff, int yoff) {
		if (this.hover) {
			g.setColor(this.hoverColour);
			g.fillRect(this.x + 1, this.y + 1, this.width, this.height);
		} else {
			g.setColor(this.colour);
			g.fillRect(this.x, this.y, this.width, this.height);
		}
		
		g.setFont(font);
		g.setColor(Color.WHITE);
		g.drawString(this.text, this.x + xoff + (this.hover ? 1 : 0), this.y + yoff + (this.hover ? 1 : 0));
	}
	
	public void renderButtonHoverText(String text, Graphics g, int xoff, int yoff, Input input) {
		if (input.x > this.x && input.x < this.x + this.width && input.y > this.y && input.y < this.y + this.height) {
			g.setColor(new Color(230, 230, 230, 230)); //translucent off white
			g.drawString(text, this.x + xoff, this.y + yoff);
		}
	}
}
