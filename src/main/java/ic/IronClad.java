package ic;

import javax.swing.JApplet;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Label;


@SuppressWarnings("serial")
public class IronClad extends JApplet {

	public static JApplet applet=null;

	public static boolean START_SCREEN=true;
	
	
	public IronClad() {
		applet=this;
		
	}
	
	public void init() {
		setBackground ( Color.black ); 
		try {
			main(null);
		} catch (Exception e) {
			getContentPane().add(new Label(e.toString()));
		}
	}
	
	public void update(Graphics g) {
        paint(g);
    }
	
	public void paint(Graphics g) {
        super.paint(g);
    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		START_SCREEN=true;
		System.setSecurityManager(null);

	}

}
