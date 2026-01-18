package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.schwarzbaer.java.lib.gui.ImageView;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO.FileIOException;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book.Field;
import net.schwarzbaer.java.tools.myhomelibrary.data.BookSeries;
import net.schwarzbaer.java.tools.myhomelibrary.data.BookStorage;
import net.schwarzbaer.java.tools.myhomelibrary.data.Notifier;
import net.schwarzbaer.java.tools.myhomelibrary.data.Publisher;

class BookPanel extends JPanel
{
	private static final long serialVersionUID = 7734255246870700253L;
	private static final String FORMAT_LABEL_ID = "   ID:  %s";
	
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
	private final BookStorage bookStorage;
	private final Notifier notifier;
	
	private Book currentBook;

	BookPanel(BookStorage bookStorage, Notifier notifier)
	{
		super(new GridBagLayout());
		this.bookStorage = Objects.requireNonNull( bookStorage );
		this.notifier    = Objects.requireNonNull( notifier    );
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
			// TODO Auto-generated method stub (btnAddAuthor)
		});
		btnEditAuthors = Tools.createButton("Edit", true, null, e->{
			// TODO Auto-generated method stub (btnEditAuthors)
		});
		btnChangeBSPos = Tools.createButton("Pos.", true, null, e->{
			// TODO Auto-generated method stub (btnChangeBSPos)
		});
		
		cmbbxBookSeries = new BookSeriesComboBox();
		cmbbxPublisher = new PublisherComboBox();
		
		guiConfigurator = new Tools.GUIConfigurator( fldTitle.getForeground(), fldTitle.getBackground() );
		guiConfigurator.configureStringField(fldTitle      , str -> !str.isBlank(), str -> Tools.doIfNotNull(currentBook, b -> { b.title     = str; this.notifier.books.fieldChanged(this, b, Field.Title      ); }));
		guiConfigurator.configureStringField(fldCatalogID  , null                 , str -> Tools.doIfNotNull(currentBook, b -> { b.catalogID = str; this.notifier.books.fieldChanged(this, b, Field.CatalogID  ); }));
		guiConfigurator.configureIntField   (fldReleaseYear, n -> n>0             , n   -> Tools.doIfNotNull(currentBook, b -> { b.releaseYear = n; this.notifier.books.fieldChanged(this, b, Field.ReleaseYear); }));
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
		bookStorage.assign(currentBook, bs);
		updateFldBookSeriesPos();
		notifier.books.fieldChanged(this, currentBook, Field.BookSeries);
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
			return Tools.addNull( bookStorage.getListOfBookSeries() );
		}
	
		@Override protected void setValue(BookSeries bs)
		{
			if (currentBook==null) return;
			assignBookSeriesToCurrentBook(bs);
		}
	
		@Override protected BookSeries addNewValueAndSet(String str)
		{
			if (currentBook==null) return null;
			BookSeries bs = bookStorage.createBookSeries();
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
			return bookStorage.getListOfKnownPublishers();
		}

		@Override protected void setValue(Publisher p)
		{
			if (currentBook == null) return;
			currentBook.publisher = p;
			notifier.books.fieldChanged(this, currentBook, Field.Publisher);
		}

		@Override protected Publisher addNewValueAndSet(String str)
		{
			if (currentBook==null) return null;
			currentBook.publisher = bookStorage.getOrCreatePublisher(str);
			notifier.books.fieldChanged(this, currentBook, Field.Publisher);
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
}
