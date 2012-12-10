package ic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import mikera.ui.JIcon;

@SuppressWarnings({ "serial", "rawtypes" })
public class ListCellRenderer extends JPanel implements
		javax.swing.ListCellRenderer {

	public ListCellRenderer() {
		
	}
	
//	private static MigLayout layout=new MigLayout(
//			"wrap 2, insets 0 0 0 0",
//			"[48!][48::]",
//			"[48!]"
//			);
	
	private static final Border iconBorder=new EmptyBorder(3,3,3,3);
	
	private static BorderLayout layout=new BorderLayout();
	
	private ListCellData data;
	
	public ListCellRenderer(ListCellData value) {
		data=value;
	}

	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {

		ListCellRenderer r= new ListCellRenderer((ListCellData)value);
		r.setLayout(layout);
		r.setBorder(mikera.ui.steampunk.PanelBorder.FILLED_BORDER);
		
//		JLabel label=new JLabel(r.data.icon);
//		label.setText(r.data.text);
//		r.add(label,BorderLayout.WEST);

		if (r.data.icon!=null) {
			JIcon icon=new JIcon(r.data.icon);
			icon.setBorder(iconBorder);
			r.add(icon,BorderLayout.WEST);
		}

		JTextArea textArea=new JTextArea(r.data.text,0,0);
		if (isSelected) {
			textArea.setForeground(Color.WHITE);				
		} else {
			textArea.setForeground(Color.BLACK);	
		}
		r.add(textArea,BorderLayout.CENTER);
				
//		r.setPreferredSize(new Dimension(150,150));
		
		return r;
	}

}
