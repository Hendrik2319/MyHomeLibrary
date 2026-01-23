package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.BorderLayout;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Author;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book.Field;
import net.schwarzbaer.java.tools.myhomelibrary.data.BookSeries;
import net.schwarzbaer.java.tools.myhomelibrary.data.Notifier;
import net.schwarzbaer.java.tools.myhomelibrary.data.Publisher;

class BooksTab extends JSplitPane
{
	private static final long serialVersionUID = 4723650064707625510L;
	private final MyHomeLibrary main;
	private final BooksTable table;
	private final BookPanel bookPanel;
	private final ToolBar toolBar;

	BooksTab(MyHomeLibrary main)
	{
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		this.main = main;
		
		table = new BooksTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bookPanel = new BookPanel(this.main);
		
		toolBar = new ToolBar();
		
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
			toolBar.updateElements();
		});
		
		this.main.notifier.storages.addListener(new Notifier.StorageListener() {
			@Override public void bookStorageLoaded(Object source)
			{
				table.tableModel.setData(BooksTab.this.main.bookStorage.getListOfBooks());
				bookPanel.updateAfterBookStorageLoaded();
			}
		});
		
		this.main.notifier.books.addListener(new Notifier.BookChangeListener() {
			@Override public void fieldChanged(Object source, Book book, Field field)
			{
				table.tableModel.fireColumnUpdateForField(field);
				BooksTab.this.main.bookStorage.writeToFile();
			}
			@Override public void fieldsChanged(Object source, Set<Book> books, Set<Field> fields)
			{
				table.tableModel.fireColumnUpdateForFields(fields);
				BooksTab.this.main.bookStorage.writeToFile();
			}
			@Override public void authorAdded   (Object source, Author    author   ) {}
			@Override public void publisherAdded(Object source, Publisher publisher) {}
			@Override public void bookRemoved   (Object source, Book      book     ) {}
		});
	}

	private class ToolBar extends JToolBar
	{
		private static final long serialVersionUID = -8128704594216506559L;
		@SuppressWarnings("unused")
		private final JButton btnAdd;
		private final JButton btnRemove;

		ToolBar()
		{
			setFloatable(false);
			
			add(btnAdd = Tools.createButton("Add Book", true, GrayCommandIcons.IconGroup.Add, e -> {
				main.bookStorage.createBook();
				table.tableModel.setData(main.bookStorage.getListOfBooks());
				main.bookStorage.writeToFile();
			}));
			
			add(btnRemove = Tools.createButton("Remove Book", true, GrayCommandIcons.IconGroup.Delete, e -> {
				if (table.getSelectedRowCount() != 1) return;
				int rowV = table.getSelectedRow();
				int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				Book row = table.tableModel.getRow(rowM);
				if (row==null) return;
				
				String title = "Are you sure?";
				String msg = "Do you really want to remove book %s from database?".formatted( row.getTitle() );
				if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(main.mainWindow, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE))
					return;
				
				main.bookStorage.removeBook(row);
				table.tableModel.setData(main.bookStorage.getListOfBooks());
				main.bookStorage.writeToFile();
				
				main.notifier.books.bookRemoved(this, row);
				if (row.bookSeries!=null)
					main.notifier.bookSeries.fieldChanged(this, row.bookSeries, BookSeries.Field.Books);
			}));
			
			// TODO: add more elements to BooksTab.toolBar (Books of Author, Books of Series, ...)
		}

		public void updateElements()
		{
			btnRemove.setEnabled(table.getSelectedRowCount() > 0);
		}
	}
}
