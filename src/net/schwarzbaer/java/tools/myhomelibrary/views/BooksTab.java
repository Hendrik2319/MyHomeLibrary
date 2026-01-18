package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;

class BooksTab extends JSplitPane
{
	private static final long serialVersionUID = 4723650064707625510L;
	private final BooksTable table;
	private final BookPanel bookPanel;

	BooksTab(MyHomeLibrary main)
	{
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		
		table = new BooksTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bookPanel = new BookPanel(main.bookStorage);
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.add(Tools.createButton("Add Book", true, GrayCommandIcons.IconGroup.Add, e -> {
			main.bookStorage.createBook();
			table.tableModel.setData(main.bookStorage.getListOfBooks());
		}));
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(table.tableScrollPane, BorderLayout.CENTER);
		leftPanel.add(toolBar, BorderLayout.PAGE_START);
		
		setLeftComponent(leftPanel);
		setRightComponent(bookPanel);
		
		table.getSelectionModel().addListSelectionListener(e -> {
			int rowV = table.getSelectedRow();
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			Book book = table.tableModel.getRow(rowM);
			bookPanel.setBook(book);
		});
	}
}
