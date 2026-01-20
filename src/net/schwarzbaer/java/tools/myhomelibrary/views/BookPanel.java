package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.ImageView;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO.FileIOException;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Author;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book.Field;
import net.schwarzbaer.java.tools.myhomelibrary.data.BookSeries;
import net.schwarzbaer.java.tools.myhomelibrary.data.Publisher;

class BookPanel extends JPanel
{
	private static final long serialVersionUID = 7734255246870700253L;
	private static final String FORMAT_LABEL_ID = "   ID:  %s";
	
	private final MyHomeLibrary main;
	
	private final JTextField fldTitle;
	private final JTextField fldAuthors;
	private final JTextField fldBookSeriesPos;
	private final JTextField fldReleaseYear;
	private final JTextField fldCatalogID;
	private final JButton btnAddAuthor;
	private final JButton btnEditAuthors;
	private final JButton btnChangeBSPos;
	private final BookSeriesComboBox cmbbxBookSeries;
	private final PublisherComboBox cmbbxPublisher;
	private final JLabel labID;
	private final JLabel labTitle;
	private final JLabel labAuthors;
	private final JLabel labBookSeries;
	private final JLabel labReleaseYear;
	private final JLabel labPublisher;
	private final JLabel labCatalogID;
	private final CoverImagesPanel coverImagesPanel;
	private final Tools.GUIConfigurator guiConfigurator;
	
	private Book currentBook;

	BookPanel(MyHomeLibrary main)
	{
		super(new GridBagLayout());
		this.main = main;
		currentBook = null;
		
		setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		labID = new JLabel("###");
		
		fldTitle = new JTextField();
		fldCatalogID = new JTextField();
		fldReleaseYear = new JTextField();
		
		fldAuthors = new JTextField();
		fldBookSeriesPos = new JTextField();
		fldBookSeriesPos.setHorizontalAlignment(JTextField.CENTER);
		
		btnAddAuthor   = Tools.createButton("Add", true, null, e->{
			if (currentBook == null) return;
			
			Author author = AddAuthorDialog.showDialog( this.main.mainWindow, this.main, currentBook.authors );
			if (author!=null && !currentBook.authors.contains(author))
			{
				currentBook.authors.add(author);
				fldAuthors.setText(currentBook.concatenateAuthors());
				this.main.notifier.books.fieldChanged(this, currentBook, Field.Authors);
			}
		});
		btnEditAuthors = Tools.createButton("Edit", true, null, e->{
			if (currentBook == null) return;
			
			boolean changed = EditAuthorsDialog.showDialog( this.main, currentBook.authors, ()->fldAuthors.setText(currentBook.concatenateAuthors()) );
			if (changed)
				this.main.notifier.books.fieldChanged(this, currentBook, Field.Authors);
		});
		btnChangeBSPos = Tools.createButton("Pos.", true, null, e->{
			// TODO Auto-generated method stub (btnChangeBSPos)
		});
		
		cmbbxBookSeries = new BookSeriesComboBox();
		cmbbxPublisher = new PublisherComboBox();
		
		guiConfigurator = new Tools.GUIConfigurator( fldTitle.getForeground(), fldTitle.getBackground() );
		guiConfigurator.configureStringField(fldTitle      , str -> !str.isBlank(), str -> Tools.doIfNotNull(currentBook, b -> { b.title     = str; this.main.notifier.books.fieldChanged(this, b, Field.Title      ); }));
		guiConfigurator.configureStringField(fldCatalogID  , null                 , str -> Tools.doIfNotNull(currentBook, b -> { b.catalogID = str; this.main.notifier.books.fieldChanged(this, b, Field.CatalogID  ); }));
		guiConfigurator.configureIntField   (fldReleaseYear, n -> n>0             , n   -> Tools.doIfNotNull(currentBook, b -> { b.releaseYear = n; this.main.notifier.books.fieldChanged(this, b, Field.ReleaseYear); }));
		guiConfigurator.configureOutputField(fldAuthors);
		guiConfigurator.configureOutputField(fldBookSeriesPos);
		
		labTitle       = new JLabel("Title"         +":  ");
		labAuthors     = new JLabel("Author(s)"     +":  ");
		labBookSeries  = new JLabel("Book Series"   +":  ");
		labReleaseYear = new JLabel("Release (Year)"+":  ");
		labPublisher   = new JLabel("Publisher"     +":  ");
		labCatalogID   = new JLabel("Catalog ID"    +":  ");
		
		coverImagesPanel = new CoverImagesPanel();
		
		c.weightx = 0;
		c.weighty = 0;
		c.gridy = -1;
		c.gridx = -1;
		
		c.gridy++; c.gridx = -1;
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(labTitle, c);
		c.gridx++; c.weightx = 1; c.gridwidth = 1; add(fldTitle, c);
		c.gridx++; c.weightx = 0; c.gridwidth = 2; add(labID, c);
		
		c.gridy++; c.gridx = -1;
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(labAuthors, c);
		c.gridx++; c.weightx = 1; c.gridwidth = 1; add(fldAuthors, c);
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(btnAddAuthor, c);
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(btnEditAuthors, c);
		
		c.gridy++; c.gridx = -1;
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(labBookSeries, c);
		c.gridx++; c.weightx = 1; c.gridwidth = 1; add(cmbbxBookSeries, c);
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(fldBookSeriesPos, c);
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(btnChangeBSPos, c);
		
		c.gridy++; c.gridx = -1;
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(labReleaseYear, c);
		c.gridx++; c.weightx = 1; c.gridwidth = 3; add(fldReleaseYear, c);
		
		c.gridy++; c.gridx = -1;
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(labPublisher, c);
		c.gridx++; c.weightx = 1; c.gridwidth = 3; add(cmbbxPublisher, c);
		
		c.gridy++; c.gridx = -1;
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(labCatalogID, c);
		c.gridx++; c.weightx = 1; c.gridwidth = 3; add(fldCatalogID, c);
		
		c.gridy++;
		c.gridx = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 4;
		c.fill = GridBagConstraints.BOTH;
		add(coverImagesPanel, c);
		
		updateFields();
	}
	
	private void assignBookSeriesToCurrentBook(BookSeries bs)
	{
		if (currentBook == null) return;
		main.bookStorage.assign(currentBook, bs);
		updateFldBookSeriesPos();
		main.notifier.books.fieldChanged(this, currentBook, Field.BookSeries);
	}
	
	private void updateFldBookSeriesPos()
	{
		if (currentBook == null) return;
		fldBookSeriesPos.setText(Tools.getIfNotNull(currentBook.bookSeries, "", bs_ -> Tools.toOrdinalString(bs_.books.indexOf(currentBook)+1) ));
	}
	
	void updateAfterBookStorageLoaded()
	{
		cmbbxBookSeries.updateValues();
		cmbbxPublisher .updateValues();
	}

	void setBook(Book book)
	{
		currentBook = book;
		updateFields();
	}

	private void updateFields()
	{
		setIgnoreInputEvents(true);
		
		labID           .setText(FORMAT_LABEL_ID.formatted(Tools.getIfNotNull(currentBook, "", b -> b.id)));
		fldTitle        .setText(Tools.getIfNotNull(currentBook, "", b -> b.title));
		fldAuthors      .setText(Tools.getIfNotNull(currentBook, "", b -> b.concatenateAuthors()));
		fldBookSeriesPos.setText(Tools.getIfNotNull(currentBook, "", b -> Tools.getIfNotNull(b.bookSeries, "", bs -> Tools.toOrdinalString(bs.books.indexOf(b)+1) )));
		fldReleaseYear  .setText(Tools.getIfNotNull(currentBook, "", b -> b.releaseYear<0 ? "" : Integer.toString(b.releaseYear)));
		fldCatalogID    .setText(Tools.getIfNotNull(currentBook, "", b -> b.catalogID));
		cmbbxBookSeries .setSelectedItem(Tools.getIfNotNull(currentBook, null, b -> b.bookSeries));
		cmbbxPublisher  .setSelectedItem(Tools.getIfNotNull(currentBook, null, b -> b.publisher ));
		
		setIgnoreInputEvents(false);
		
		coverImagesPanel.imgvwBack .setFile(Tools.getIfNotNull(currentBook, null, b -> b.backCover ));
		coverImagesPanel.imgvwSpine.setFile(Tools.getIfNotNull(currentBook, null, b -> b.spineCover));
		coverImagesPanel.imgvwFront.setFile(Tools.getIfNotNull(currentBook, null, b -> b.frontCover));
		
		for (Component comp : getComponents())
			comp.setEnabled(currentBook != null && isEnabled());
	}

	private void setIgnoreInputEvents(boolean ignoreInputEvents)
	{
		guiConfigurator.ignoreInputEvents = ignoreInputEvents;
		cmbbxBookSeries.ignoreInputEvents = ignoreInputEvents;
		cmbbxPublisher .ignoreInputEvents = ignoreInputEvents;
	}

	private final class BookSeriesComboBox extends EditableComboBox<BookSeries>
	{
		private static final long serialVersionUID = -9116778313951252597L;
	
		private BookSeriesComboBox()
		{
			super(BookSeries.class);
			setRenderer(new Tables.NonStringRenderer<>( obj -> {
				if (obj == null                 ) return "-----";
				if (obj instanceof BookSeries bs) return bs.name;
				return obj.toString();
			} ));
		}
	
		@Override protected List<BookSeries> getValues()
		{
			return Tools.addNull( main.bookStorage.getListOfBookSeries() );
		}
	
		@Override protected void setValue(BookSeries bs)
		{
			if (currentBook==null) return;
			assignBookSeriesToCurrentBook(bs);
		}
	
		@Override protected BookSeries addNewValueAndSet(String str)
		{
			if (currentBook==null) return null;
			BookSeries bs = main.bookStorage.createBookSeries();
			bs.name = str;
			assignBookSeriesToCurrentBook(bs);
			return bs;
		}
	}

	private final class PublisherComboBox extends EditableComboBox<Publisher>
	{
		private static final long serialVersionUID = 4015497969920622929L;

		private PublisherComboBox()
		{
			super(Publisher.class);
		}

		@Override protected List<Publisher> getValues()
		{
			return main.bookStorage.getListOfKnownPublishers();
		}

		@Override protected void setValue(Publisher p)
		{
			if (currentBook == null) return;
			currentBook.publisher = p;
			main.notifier.books.fieldChanged(this, currentBook, Field.Publisher);
		}

		@Override protected Publisher addNewValueAndSet(String str)
		{
			if (currentBook==null) return null;
			currentBook.publisher = main.bookStorage.getOrCreatePublisher(str);
			main.notifier.books.fieldChanged(this, currentBook, Field.Publisher);
			main.notifier.books.publisherAdded(this, currentBook.publisher);
			return currentBook.publisher;
		}
	}

	private class CoverImagesPanel extends JPanel
	{
		private static final long serialVersionUID = -7178751322272176418L;
		private SmallImageView imgvwBack;
		private SmallImageView imgvwSpine;
		private SmallImageView imgvwFront;

		CoverImagesPanel()
		{
			super(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			imgvwBack  = new SmallImageView(200, 300, "Back Cover" );
			imgvwSpine = new SmallImageView(100, 300, "Book Spine" );
			imgvwFront = new SmallImageView(200, 300, "Front Cover");
			
			c.weighty = 1; 
			c.gridx = 0; c.weightx = 2; add(imgvwBack, c);
			c.gridx = 1; c.weightx = 1; add(imgvwSpine, c);
			c.gridx = 2; c.weightx = 2; add(imgvwFront, c);
		}

		@Override
		public void setEnabled(boolean enabled)
		{
			super.setEnabled(enabled);
			imgvwBack .setEnabled(enabled);
			imgvwSpine.setEnabled(enabled);
			imgvwFront.setEnabled(enabled);
		}
	}
	
	private static class SmallImageView extends ImageView
	{
		private static final long serialVersionUID = -4462443790556744707L;

		SmallImageView(int width, int height, String title)
		{
			super(width, height, null, true);
			setBorder(BorderFactory.createTitledBorder(title));
			activateAxes(null, false, false, false, false);
		}

		void setFile(String filename)
		{
			if (filename==null)
			{
				setImage(null);
				return;
			}
			
			File file;
			try { file = FileIO.getImageFile(filename); }
			catch (FileIOException ex) { ex.printStackTrace(); setImage(null); return; }
			
			if (!file.isFile())
			{
				setImage(null);
				return;
			}
			
			BufferedImage image;
			try { image = ImageIO.read(file); }
			catch (IOException ex) { ex.printStackTrace(); setImage(null); return; }
			
			setImage(image);
		}
	}
	
	private static class AddAuthorDialog extends StandardDialog
	{
		private static final long serialVersionUID = 2020962667474886292L;
		private Author selectedAuthor;
		private String selectedNewName;
		private Author result;
	
		AddAuthorDialog(Window window, MyHomeLibrary main, List<Author> currentList)
		{
			super(window, "Add Author", ModalityType.APPLICATION_MODAL, false);
			selectedNewName = null;
			selectedAuthor = null;
			
			JButton btnOk = Tools.createButton("Ok", false, null, e -> {
				if (selectedAuthor!=null)
					result = selectedAuthor;
				else if (selectedNewName!=null)
				{
					result = main.bookStorage.getOrCreateAuthor(selectedNewName);
					main.notifier.books.authorAdded(this, result);
				}
				closeDialog();
			});
			
			Vector<Author> listOfKnownAuthors = new Vector<>( main.bookStorage.getListOfKnownAuthors() );
			listOfKnownAuthors.removeAll(currentList);
			
			JComboBox<Author> comboBox = new JComboBox<>( listOfKnownAuthors );
			comboBox.setEditable(true);
			comboBox.addActionListener(e -> {
				Object selectedObj = comboBox.getSelectedItem();
				selectedNewName = null;
				selectedAuthor = null;
				if      (selectedObj instanceof Author author) selectedAuthor  = author;
				else if (selectedObj instanceof String str   ) selectedNewName = str   ;
				btnOk.setEnabled(selectedAuthor!=null || selectedNewName!=null);
			});
			
			JPanel panel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = -1;
			
			c.gridy++; panel.add(new JLabel("Enter the name of a new author or select an existing one:"), c);
			c.gridy++; panel.add(comboBox, c);
			c.weighty = 1;
			c.gridy++; panel.add(new JLabel(), c);
			
			createGUI(
					panel,
					btnOk,
					Tools.createButton("Cancel", true, null, e -> closeDialog())
			);
		}
		
		static Author showDialog(Window window, MyHomeLibrary main, List<Author> currentList)
		{
			AddAuthorDialog dlg = new AddAuthorDialog(window, main, currentList);
			dlg.showDialog();
			return dlg.result;
		}
	}

	private static class EditAuthorsDialog extends StandardDialog
	{
		private static final long serialVersionUID = 5038062564157536297L;
		
		private final List<Author> authors;
		private final Runnable updateAuthorsField;
		
		private final JList<Author> list;
		private final DefaultListModel<Author> listModel;
		private final JButton btnAdd;
		private final JButton btnRemove;
		private final JButton btnMoveUp;
		private final JButton btnMoveDown;
		
		private List<Author> selected;
		private int[] selectedIndices;
		private boolean someThingChanged;

		EditAuthorsDialog(MyHomeLibrary main, List<Author> authors, Runnable updateAuthorsField)
		{
			super(main.mainWindow, "Edit Authors");
			this.updateAuthorsField = Objects.requireNonNull( updateAuthorsField );
			this.authors = Objects.requireNonNull( authors );
			selected = List.of();
			selectedIndices = new int[0];
			someThingChanged = false;
			
			listModel = new DefaultListModel<>();
			listModel.addAll(this.authors);
			
			list = new JList<>(listModel);
			list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			list.addListSelectionListener(ev -> {
				selected = list.getSelectedValuesList();
				selectedIndices = list.getSelectedIndices();
				updateButtons();
			});
			JScrollPane scrollPane = new JScrollPane(list);
			
			btnAdd = Tools.createButton("Add", true, GrayCommandIcons.IconGroup.Add, e -> {
				Author author = AddAuthorDialog.showDialog(this, main, this.authors);
				if (author!=null && !this.authors.contains(author))
				{
					this.authors.add(author);
					listModel.addElement(author);
					this.updateAuthorsField.run();
					someThingChanged = true;
				}
			});
			btnRemove = Tools.createButton("Remove", true, GrayCommandIcons.IconGroup.Delete, e -> {
				if (!selected.isEmpty())
				{
					this.authors.removeAll(selected);
					for (Author a : selected)
						listModel.removeElement(a);
					this.updateAuthorsField.run();
					someThingChanged = true;
				}
			});
			btnMoveUp   = Tools.createButton("Move", true, GrayCommandIcons.IconGroup.Up  , e -> swap(-1));
			btnMoveDown = Tools.createButton("Move", true, GrayCommandIcons.IconGroup.Down, e -> swap(+1));
			
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 1;
			c.gridx = -1;
			c.gridy = 0;
			buttonPanel.add(btnAdd     , c);
			buttonPanel.add(btnRemove  , c);
			buttonPanel.add(btnMoveUp  , c);
			buttonPanel.add(btnMoveDown, c);
			
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(scrollPane, BorderLayout.CENTER);
			panel.add(buttonPanel, BorderLayout.SOUTH);
			
			createGUI(
					panel,
					Tools.createButton("Close", true, null, e -> closeDialog())
			);
			
			updateButtons();
		}
	
		private void swap(int inc)
		{
			if (selectedIndices.length==1)
			{
				int index = selectedIndices[0];
				boolean success1 = swap(authors, index, index+inc);
				boolean success2 = swap(listModel, index, index+inc);
				if (success2)
				{
					list.setSelectedIndex(index+inc);
				}
				if (success1)
				{
					updateAuthorsField.run();
					someThingChanged = true;
				}
			}
		}

		private boolean swap(DefaultListModel<Author> list, int index1, int index2)
		{
			if (index1<0 || index1>=list.size()) return false;
			if (index2<0 || index2>=list.size()) return false;
			list.set(index2, list.set(index1, list.get(index2)));
			return true;
		}

		private boolean swap(List<Author> list, int index1, int index2)
		{
			if (index1<0 || index1>=list.size()) return false;
			if (index2<0 || index2>=list.size()) return false;
			list.set(index2, list.set(index1, list.get(index2)));
			return true;
		}

		private void updateButtons()
		{
			btnAdd     .setEnabled(true);
			btnRemove  .setEnabled(!selected.isEmpty());
			btnMoveUp  .setEnabled(selectedIndices.length==1);
			btnMoveDown.setEnabled(selectedIndices.length==1);
		}

		static boolean showDialog(MyHomeLibrary main, List<Author> authors, Runnable updateAuthorsField)
		{
			EditAuthorsDialog dlg = new EditAuthorsDialog(main, authors, updateAuthorsField);
			dlg.showDialog();
			return dlg.someThingChanged;
		}
	
	}
}
