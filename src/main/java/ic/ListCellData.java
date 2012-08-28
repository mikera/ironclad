package ic;

import javax.swing.Icon;

public class ListCellData {
	public Icon icon;
	public String text;
	public Object value;
	
	public ListCellData(Icon icon, String text, Object value) {
		this.icon=icon;
		this.text=text;
		this.value=value;
	}
}
