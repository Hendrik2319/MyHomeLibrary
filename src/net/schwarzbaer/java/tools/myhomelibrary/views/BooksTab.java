package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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
import net.schwarzbaer.java.tools.myhomelibrary.data.BookStorage;
import net.schwarzbaer.java.tools.myhomelibrary.data.Notifier;
import net.schwarzbaer.java.tools.myhomelibrary.data.Publisher;

class BooksTab extends JSplitPane
{
	private static final long serialVersionUID = 4723650064707625510L;
	
	private final MyHomeLibrary main;
	private final BooksTable table;
	private final BookPanel bookPanel;
	private final UpperToolBar upperToolBar;
	private final LowerToolBar lowerToolBar;
	
	private ListType currentListType;
	private Selector currentSelector;

	BooksTab(MyHomeLibrary main)
	{
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		this.main = main;
		currentListType = null;
		currentSelector = null;
		
		table = new BooksTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bookPanel = new BookPanel(this.main);
		
		upperToolBar = new UpperToolBar();
		lowerToolBar = new LowerToolBar();
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(upperToolBar, BorderLayout.PAGE_START);
		leftPanel.add(table.tableScrollPane, BorderLayout.CENTER);
		leftPanel.add(lowerToolBar, BorderLayout.PAGE_END);
		
		setLeftComponent(leftPanel);
		setRightComponent(bookPanel);
		
		upperToolBar.listTypeChanged();
		
		table.getSelectionModel().addListSelectionListener(e -> {
			int rowV = table.getSelectedRow();
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			Book book = table.tableModel.getRow(rowM);
			bookPanel.setBook(book);
			lowerToolBar.updateElements();
		});
		
		this.main.notifier.storages.addListener(new Notifier.StorageListener() {
			@Override public void bookStorageLoaded(Object source)
			{
				updateTableContent();
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

	private void updateTableContent()
	{
		table.tableModel.setData(
				currentSelector==null
					? null
					: currentSelector.getBooks.get()
		);
	}

	private enum ListType
	{
		AllBooks     ("All Books"),
		//NewBooks     ("Recently Added Books"),
		BooksOfAuthor("Books of Author"),
		BooksOfSeries("Books of Series"),
		;
		private final String label;
		ListType(String label) { this.label = label; }
		@Override public String toString() { return label; }
	}
	
	private record Selector(BookStorage bookStorage, Author author, BookSeries bs, Supplier<List<Book>> getBooks)
	{
		static class Factory
		{
			private final BookStorage bookStorage;
			Factory(BookStorage bookStorage) { this.bookStorage = bookStorage; }
			
			Selector createFor(Author author)
			{
				return new Selector(bookStorage, author, null, () -> bookStorage.getListOfBooks(author));
			}
			
			Selector createFor(BookSeries bs)
			{
				return new Selector(bookStorage, null, bs, () -> bs.books);
			}
			
			Selector createForAll()
			{
				return new Selector(bookStorage, null, null, () -> bookStorage.getListOfBooks());
			}
		}

		void initNewBook(Book newBook)
		{
			if (author!=null)
				newBook.authors.add(author);
			if (bs!=null)
				bookStorage.assign(newBook, bs);
		}
	}

	private class UpperToolBar extends JToolBar
	{
		private static final long serialVersionUID = -747144168387260872L;
		
		private final Selector.Factory selectorFactory;
		private final JComboBox<ListType> cmbbxListType;
		private final JComboBox<Object> cmbbxSelector;
		private final DefaultComboBoxModel<Object> cmbbxSelectorModel;
		private final JLabel labSelector;
		private boolean ignoreSelectorChanges;
		
		UpperToolBar()
		{
			setFloatable(false);
			ignoreSelectorChanges = false;
			selectorFactory = new Selector.Factory(main.bookStorage);
			
			cmbbxListType = new JComboBox<>(ListType.values());
			cmbbxListType.setMaximumSize(cmbbxListType.getPreferredSize());
			cmbbxListType.setSelectedItem(ListType.AllBooks);
			cmbbxListType.addActionListener(e -> listTypeChanged());
			
			labSelector = new JLabel();
			
			cmbbxSelectorModel = new DefaultComboBoxModel<>();
			cmbbxSelector = new JComboBox<>(cmbbxSelectorModel);
			cmbbxSelector.addActionListener(e -> selectorChanged());
			
			add(cmbbxListType);
			add(labSelector);
			add(cmbbxSelector);
		}

		private void selectorChanged()
		{
			if (ignoreSelectorChanges) return;
			
			Object selectedObj = cmbbxSelector.getSelectedItem();
			
			if (currentListType != null)
				switch (currentListType)
				{
				case AllBooks:
					break;
					
				case BooksOfAuthor:
					if (selectedObj instanceof Author author)
						currentSelector = selectorFactory.createFor(author);
					break;
					
				case BooksOfSeries:
					if (selectedObj instanceof BookSeries bs)
						currentSelector = selectorFactory.createFor(bs);
					break;
				}
			
			updateTableContent();
			lowerToolBar.updateElements();
		}

		private void listTypeChanged()
		{
			ignoreSelectorChanges = true;
			
			currentListType = cmbbxListType.getItemAt(cmbbxListType.getSelectedIndex());
			cmbbxSelectorModel.removeAllElements();
			
			String labSelectorText = "";
			List<? extends Object> selectors = null;
			currentSelector = null;
			
			if (currentListType != null)
				switch (currentListType)
				{
				case AllBooks:
					currentSelector = selectorFactory.createForAll();
					break;
					
				case BooksOfAuthor:
					labSelectorText = "  Author: ";
					selectors = main.bookStorage.getListOfKnownAuthors();
					break;
					
				case BooksOfSeries:
					labSelectorText = "  Book Series: ";
					selectors = main.bookStorage.getListOfBookSeries();
					break;
				}
			
			labSelector  .setText(labSelectorText);
			labSelector  .setEnabled(selectors!=null);
			cmbbxSelector.setEnabled(selectors!=null);
			if (selectors!=null)
			{
				cmbbxSelectorModel.addAll(selectors);
				cmbbxSelector.setSelectedItem(null);
			}
			updateTableContent();
			lowerToolBar.updateElements();
			
			ignoreSelectorChanges = false;
		}
	}

	private class LowerToolBar extends JToolBar
	{
		private static final long serialVersionUID = -8128704594216506559L;
		private final JButton btnAdd;
		private final JButton btnRemove;

		LowerToolBar()
		{
			setFloatable(false);
			
			add(btnAdd = Tools.createButton("Add Book", true, GrayCommandIcons.IconGroup.Add, e -> {
				Book newBook = main.bookStorage.createBook();
				if (currentSelector!=null)
					currentSelector.initNewBook(newBook);
				updateTableContent();
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
				updateTableContent();
				main.bookStorage.writeToFile();
				
				main.notifier.books.bookRemoved(this, row);
				if (row.bookSeries!=null)
					main.notifier.bookSeries.fieldChanged(this, row.bookSeries, BookSeries.Field.Books);
			}));
		}

		public void updateElements()
		{
			btnAdd   .setEnabled(currentSelector!=null);
			btnRemove.setEnabled(currentSelector!=null && table.getSelectedRowCount() > 0);
		}
	}
}
