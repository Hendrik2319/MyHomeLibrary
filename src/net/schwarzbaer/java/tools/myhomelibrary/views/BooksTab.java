package net.schwarzbaer.java.tools.myhomelibrary.views;

import javax.swing.JLabel;
import javax.swing.JSplitPane;

class BooksTab extends JSplitPane
{
	private static final long serialVersionUID = 4723650064707625510L;
	private final BooksTable table;

	BooksTab()
	{
		table = new BooksTable();
		setLeftComponent(table.tableScrollPane);
		setRightComponent(new JLabel("< Book View >"));
	}
}
