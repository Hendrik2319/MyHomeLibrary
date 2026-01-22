package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.BorderLayout;
import java.util.Set;

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
import net.schwarzbaer.java.tools.myhomelibrary.data.Notifier;
import net.schwarzbaer.java.tools.myhomelibrary.data.Publisher;

class BooksTab extends JSplitPane
{
	private static final long serialVersionUID = 4723650064707625510L;
	private final MyHomeLibrary main;
	private final BooksTable table;
	private final BookPanel bookPanel;

	BooksTab(MyHomeLibrary main)
	{
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		this.main = main;
		
		table = new BooksTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bookPanel = new BookPanel(this.main);
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.add(Tools.createButton("Add Book", true, GrayCommandIcons.IconGroup.Add, e -> {
			this.main.bookStorage.createBook();
			table.tableModel.setData(this.main.bookStorage.getListOfBooks());
			this.main.bookStorage.writeToFile();
		}));
		// TODO: add more elements to BooksTab.toolBar (remove book. Books of Author, Books of Series, ...)
		
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
			@Override public void authorAdded(Object source, Author author) {}
			@Override public void publisherAdded(Object source, Publisher publisher) {}
		});
	}
}
