package ca.ab.cbe.wahs.ajw;

import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class Input implements MouseMotionListener, MouseListener, KeyListener {
	
	public int x, y; //X and Y coordinates of the user's mouse on screen
	public boolean leftDown = false; //Whether or not the user's left mouse button is down
	public boolean rightDown = false; //Whether or not the user's right mouse button is down
	public boolean escape = false; //Whether or not the user is hitting the esc button
	public int num = -1; //Current selected tile
	
	public Input(Canvas canvas) {
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addKeyListener(this);
	}
	
	public void releaseAll() {
		leftDown = false;
		rightDown = false;
		escape = false;
	}
	
	public void mouseMoved(MouseEvent e) {
		x = e.getX();
		y = e.getY();
	}
	
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) leftDown = true;
		if (e.getButton() == MouseEvent.BUTTON3) rightDown = true;
		num = -1; //Incase the player selected a tile with the mouse
	}
	
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_ESCAPE:
			escape = true;
			break;
		case KeyEvent.VK_0:
		case KeyEvent.VK_NUMPAD0:
			num = 0;
			break;
		case KeyEvent.VK_1:
		case KeyEvent.VK_NUMPAD1:
			num = 1;
			break;
		case KeyEvent.VK_2:
		case KeyEvent.VK_NUMPAD2:
			num = 2;
			break;
		case KeyEvent.VK_3:
		case KeyEvent.VK_NUMPAD3:
			num = 3;
			break;
		case KeyEvent.VK_4:
		case KeyEvent.VK_NUMPAD4:
			num = 4;
			break;
		case KeyEvent.VK_5:
		case KeyEvent.VK_NUMPAD5:
			num = 5;
			break;
		case KeyEvent.VK_6:
		case KeyEvent.VK_NUMPAD6:
			num = 6;
			break;
		case KeyEvent.VK_7:
		case KeyEvent.VK_NUMPAD7:
			num = 7;
			break;
		case KeyEvent.VK_8:
		case KeyEvent.VK_NUMPAD8:
			num = 8;
			break;
		case KeyEvent.VK_9:
		case KeyEvent.VK_NUMPAD9:
			num = 9;
			break;
		default:
			num = -1;
			break;
		}
	}
	
	public void mouseDragged(MouseEvent e) {
		x = e.getX();
		y = e.getY();
		//TODO add right click-ctrl dragging
		if (e.isControlDown()) leftDown = true;
	}
	
	public void keyReleased(KeyEvent e) {}
	
	public void keyTyped(KeyEvent e) {}
	
	public void mouseClicked(MouseEvent e) {}
	
	public void mouseEntered(MouseEvent e) {}
	
	public void mouseExited(MouseEvent e) {}
	
	public void mouseReleased(MouseEvent e) {}
	
}
