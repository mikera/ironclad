package ic;

import javax.swing.JApplet;

import mikera.cljutils.Clojure;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Label;

@SuppressWarnings("serial")
public class IronClad extends JApplet {

	public static JApplet applet=null;

	public static boolean JAVA_LAUNCHED=false;
	public static boolean START_SCREEN=true;
	public static boolean DEBUG_MODE=true;
		
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
		JAVA_LAUNCHED=true;
		START_SCREEN=true;
		DEBUG_MODE=false;
		System.setSecurityManager(null);
		
		Clojure.require("ic.main");
		Clojure.eval("(ic.main/main)");
	}
}
