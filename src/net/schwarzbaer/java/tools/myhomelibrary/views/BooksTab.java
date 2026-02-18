package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
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
	private static final Comparator<Book> COMPARATOR__BY_NAME = Tools.createComparatorByName(b -> Tools.getIfNotNull(b.title, "<unnamed>"));
	
	private final MyHomeLibrary main;
	private final BooksTable table;
	private final BookPanel bookPanel;
	private final UpperToolBar upperToolBar;
	private final LowerToolBar lowerToolBar;
	
	private ListType currentListType;
	private Selector currentSelector;
	private RowOrder currentRowOrder;

	BooksTab(MyHomeLibrary main)
	{
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		this.main = main;
		currentListType = ListType.AllBooks;
		currentSelector = null;
		currentRowOrder = RowOrder.Name;
		
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
			@Override public void added  (Object source, Book book) {}
			@Override public void deleted(Object source, Book book) {}
		});
		
		this.main.notifier.bookSeries.addListener(new Notifier.BookSeriesChangeListener() {
			@Override public void fieldChanged (Object source,      BookSeries  bookSeries,     BookSeries.Field  field ) {}
			@Override public void fieldsChanged(Object source,  Set<BookSeries> bookSeries, Set<BookSeries.Field> fields) {}
			@Override public void added        (Object source,      BookSeries  bookSeries) { updateCmbbxSelector(); }
			@Override public void deleted      (Object source, List<BookSeries> bookSeries) { updateCmbbxSelector(); }
			
			private void updateCmbbxSelector()
			{
				if (currentListType==ListType.BooksOfSeries)
					upperToolBar.updateCmbbxSelector(BooksTab.this.main.bookStorage.getListOfBookSeries());
			}
		});
		
		this.main.notifier.authors.addListener(new Notifier.AuthorChangeListener() {
			@Override public void added  (Object source,      Author  author ) { updateCmbbxSelector(); }
			@Override public void deleted(Object source, List<Author> authors) { updateCmbbxSelector(); }
			
			private void updateCmbbxSelector()
			{
				if (currentListType==ListType.BooksOfAuthor)
					upperToolBar.updateCmbbxSelector(BooksTab.this.main.bookStorage.getListOfKnownAuthors());
			}
		});
		
		this.main.notifier.publishers.addListener(new Notifier.PublisherChangeListener() {
			@Override public void added  (Object source,      Publisher  publisher ) { updateCmbbxSelector(); }
			@Override public void deleted(Object source, List<Publisher> publishers) { updateCmbbxSelector(); }
			
			private void updateCmbbxSelector()
			{
				if (currentListType==ListType.BooksOfPublisher)
					upperToolBar.updateCmbbxSelector(BooksTab.this.main.bookStorage.getListOfKnownPublishers());
			}
		});
	}

	private void updateTableContent()
	{
		List<Book> list = currentSelector==null ? null : currentSelector.getBooks.get();
		if (currentRowOrder!=null && currentRowOrder.comparator!=null && list!=null)
		{
			list = new ArrayList<>(list);
			list.sort(currentRowOrder.comparator);
		}
		table.tableModel.setData( list );
	}

	private enum ListType
	{
		AllBooks         ("All Books"),
		NewBooks         ("Recently Added Books"),
		BooksOfAuthor    ("Books of Author"),
		BooksOfSeries    ("Books of Series"),
		BooksOfPublisher ("Books of Publisher"),
		NotReadBooks     ("Not Read Books"),
		NotOwnedBooks    ("Not Owned Books"),
		IncompleteData   ("Books with incomplete data"),
		IncompleteCover  ("Books with incomplete cover"),
		;
		private final String label;
		ListType(String label) { this.label = label; }
		@Override public String toString() { return label; }
	}
	
	private record Selector(BookStorage bookStorage, Author author, BookSeries bookSeries, Publisher publisher, Supplier<List<Book>> getBooks)
	{
		static class Factory
		{
			private final BookStorage bookStorage;
			Factory(BookStorage bookStorage) { this.bookStorage = bookStorage; }
			
			Selector createFor(Author author)
			{
				return new Selector(bookStorage, author, null, null, () -> bookStorage.getListOfBooks(b -> b.authors.contains(author)));
			}
			
			Selector createFor(BookSeries bookSeries)
			{
				return new Selector(bookStorage, null, bookSeries, null, () -> bookSeries.books);
			}
			
			Selector createFor(Publisher publisher)
			{
				return new Selector(bookStorage, null, null, publisher, () -> bookStorage.getListOfBooks(b -> b.publisher == publisher));
			}

			Selector createForAll()
			{
				return new Selector(bookStorage, null, null, null, () -> bookStorage.getListOfBooks());
			}

			Selector createForPredicate(Predicate<Book> filter)
			{
				return new Selector(bookStorage, null, null, null, () -> bookStorage.getListOfBooks(filter));
			}
		}

		void initNewBook(Book newBook)
		{
			if (author!=null)
				newBook.authors.add(author);
			if (bookSeries!=null)
				bookStorage.assign(newBook, bookSeries);
			if (publisher!=null)
				newBook.publisher = publisher;
		}
	}
	
	private enum RowOrder
	{
		Original ("Original Order", null),
		Name     ("Order by Name",
				COMPARATOR__BY_NAME
		),
		CatalogID("Order by Catalog ID", Comparator
				.<Book,String>comparing(b -> b.catalogID, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(COMPARATOR__BY_NAME)
		),
		Release  ("Order by Release", Comparator
				.<Book,String>comparing(b -> b.release, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(COMPARATOR__BY_NAME)
		),
		BookSeries ("Order by Book Series", Comparator
				.<Book,String>comparing(b -> Tools.getIfNotNull(b.bookSeries, null, bs -> bs.toString()), Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(b -> Tools.getIfNotNull(b.bookSeries, null, bs -> bs.books.indexOf(b)), Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(COMPARATOR__BY_NAME)
		),
		;
		private final String label;
		private final Comparator<Book> comparator;
		RowOrder(String label, Comparator<Book> comparator) { this.label = label; this.comparator = comparator; }
		@Override public String toString() { return label; }
	}

	private class UpperToolBar extends JToolBar
	{
		private static final long serialVersionUID = -747144168387260872L;
		
		private final Selector.Factory selectorFactory;
		private final JComboBox<ListType> cmbbxListType;
		private final JComboBox<RowOrder> cmbbxRowOrder;
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
			cmbbxListType.setSelectedItem(currentListType);
			cmbbxListType.addActionListener(e -> listTypeChanged());
			
			labSelector = new JLabel();
			
			cmbbxSelectorModel = new DefaultComboBoxModel<>();
			cmbbxSelector = new JComboBox<>(cmbbxSelectorModel);
			cmbbxSelector.addActionListener(e -> selectorChanged());
			
			cmbbxRowOrder = new JComboBox<>(RowOrder.values());
			cmbbxRowOrder.setMaximumSize(cmbbxRowOrder.getPreferredSize());
			cmbbxRowOrder.setSelectedItem(currentRowOrder);
			cmbbxRowOrder.addActionListener(e -> rowOrderChanged());
			
			add(cmbbxListType);
			add(labSelector);
			add(cmbbxSelector);
			add(cmbbxRowOrder);
		}

		private void rowOrderChanged()
		{
			currentRowOrder = cmbbxRowOrder.getItemAt(cmbbxRowOrder.getSelectedIndex());
			updateTableContent();
			lowerToolBar.updateElements();
		}

		private void selectorChanged()
		{
			if (ignoreSelectorChanges) return;
			
			Object selectedObj = cmbbxSelector.getSelectedItem();
			
			if (currentListType != null)
				switch (currentListType)
				{
				case AllBooks:
				case NewBooks:
				case NotReadBooks:
				case NotOwnedBooks:
				case IncompleteData:
				case IncompleteCover:
					break;
					
				case BooksOfAuthor:
					if (selectedObj instanceof Author author)
						currentSelector = selectorFactory.createFor(author);
					break;
					
				case BooksOfSeries:
					if (selectedObj instanceof BookSeries bs)
						currentSelector = selectorFactory.createFor(bs);
					break;
					
				case BooksOfPublisher:
					if (selectedObj instanceof Publisher p)
						currentSelector = selectorFactory.createFor(p);
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
					
				case NewBooks:
					currentSelector = selectorFactory.createForPredicate(b -> b.recentlyCreated);
					break;
					
				case NotReadBooks:
					currentSelector = selectorFactory.createForPredicate(b -> !b.read);
					break;
					
				case NotOwnedBooks:
					currentSelector = selectorFactory.createForPredicate(b -> !b.owned);
					break;
					
				case BooksOfAuthor:
					labSelectorText = "  Author: ";
					selectors = main.bookStorage.getListOfKnownAuthors();
					break;
					
				case BooksOfSeries:
					labSelectorText = "  Book Series: ";
					selectors = main.bookStorage.getListOfBookSeries();
					break;
					
				case BooksOfPublisher:
					labSelectorText = "  Publisher: ";
					selectors = main.bookStorage.getListOfKnownPublishers();
					break;
					
				case IncompleteData:
					currentSelector = selectorFactory.createForPredicate( Book::hasIncompleteData );
					break;
					
				case IncompleteCover:
					currentSelector = selectorFactory.createForPredicate( b -> b.frontCover==null || b.spineCover==null || b.backCover==null );
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
		
		void updateCmbbxSelector(List<? extends Object> selectors)
		{
			ignoreSelectorChanges = true;
			Object selectedObj = cmbbxSelector.getSelectedItem();
			if (!selectors.contains(selectedObj)) selectedObj = null;
			cmbbxSelectorModel.removeAllElements();
			cmbbxSelectorModel.addAll(selectors);
			cmbbxSelector.setSelectedItem(selectedObj);
			ignoreSelectorChanges = false;
		}
	}

	private class LowerToolBar extends JToolBar
	{
		private static final long serialVersionUID = -8128704594216506559L;
		private final JButton btnAdd;
		private final JButton btnRemove;
//		private final JButton btnResetRowOrder;

		LowerToolBar()
		{
			setFloatable(false);
			
			add(btnAdd = Tools.createButton("Add Book", true, GrayCommandIcons.IconGroup.Add, e -> {
				Book newBook = main.bookStorage.createBook();
				if (currentSelector!=null)
					currentSelector.initNewBook(newBook);
				updateTableContent();
				main.bookStorage.writeToFile();
				int rowM = table.tableModel.getRowIndex(newBook);
				int rowV = rowM<0 ? -1 : table.convertRowIndexToView(rowM);
				if (rowV>=0) table.setRowSelectionInterval(rowV, rowV);
				main.notifier.books.added(this, newBook);
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
				
				main.notifier.books.deleted(this, row);
				if (row.bookSeries!=null)
					main.notifier.bookSeries.fieldChanged(this, row.bookSeries, BookSeries.Field.Books);
			}));
			
//			addSeparator();
//			
//			add(btnResetRowOrder = Tools.createButton("Reset Row Order", true, GrayCommandIcons.IconGroup.Reload, e -> {
//				table.tableRowSorter.resetSortOrder();
//				table.repaint();
//			}));
		}

		public void updateElements()
		{
			btnAdd          .setEnabled(currentSelector!=null);
			btnRemove       .setEnabled(currentSelector!=null && table.getSelectedRowCount() > 0);
//			btnResetRowOrder.setEnabled(currentSelector!=null);
		}
	}
}
