package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.ImageView;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.system.ClipboardTools;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO.FileIOException;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Author;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book.Field;
import net.schwarzbaer.java.tools.myhomelibrary.data.BookSeries;
import net.schwarzbaer.java.tools.myhomelibrary.data.Notifier;
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
	private final JCheckBox chkbxRead;
	private final JCheckBox chkbxOwned;
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
			
			EditAuthorsDialog.showDialog( this.main, currentBook.authors, () -> {
				fldAuthors.setText(currentBook.concatenateAuthors());
				this.main.notifier.books.fieldChanged(this, currentBook, Field.Authors);
			} );
		});
		btnChangeBSPos = Tools.createButton("Pos.", true, null, e->{
			if (currentBook==null || currentBook.bookSeries==null || currentBook.bookSeries.books.size() <= 1)
				return;
			
			ChangeBSPosDialog.showDialog( this.main, currentBook.bookSeries, () -> {
				updateFldBookSeriesPos();
				this.main.notifier.books.fieldChanged(this, currentBook, Field.BookSeries);
			} );
		});
		
		cmbbxBookSeries = new BookSeriesComboBox();
		cmbbxPublisher = new PublisherComboBox();
		
		guiConfigurator = new Tools.GUIConfigurator( fldTitle.getForeground(), fldTitle.getBackground() );
		guiConfigurator.configureStringField(fldTitle      , str -> !str.isBlank(), str -> Tools.doIfNotNull(currentBook, b -> { b.title     = str; this.main.notifier.books.fieldChanged(this, b, Field.Title    ); }));
		guiConfigurator.configureStringField(fldCatalogID  , null                 , str -> Tools.doIfNotNull(currentBook, b -> { b.catalogID = str; this.main.notifier.books.fieldChanged(this, b, Field.CatalogID); }));
		guiConfigurator.configureStringField(fldReleaseYear, null                 , str -> Tools.doIfNotNull(currentBook, b -> { b.release   = str; this.main.notifier.books.fieldChanged(this, b, Field.Release  ); }));
		guiConfigurator.configureOutputField(fldAuthors);
		guiConfigurator.configureOutputField(fldBookSeriesPos);
		
		chkbxRead  = Tools.createCheckBox("Read" , true, false, null, null, b -> {
			if (currentBook == null || guiConfigurator.ignoreInputEvents) return;
			currentBook.read = b;
			this.main.notifier.books.fieldChanged(this, currentBook, Field.Read);
		});
		chkbxOwned = Tools.createCheckBox("Owned", true, false, null, null, b -> {
			if (currentBook == null || guiConfigurator.ignoreInputEvents) return;
			currentBook.owned = b;
			this.main.notifier.books.fieldChanged(this, currentBook, Field.Owned);
		});
		
		labTitle       = new JLabel("Title"      +":  ");
		labAuthors     = new JLabel("Author(s)"  +":  ");
		labBookSeries  = new JLabel("Book Series"+":  ");
		labReleaseYear = new JLabel("Release"    +":  ");
		labPublisher   = new JLabel("Publisher"  +":  ");
		labCatalogID   = new JLabel("Catalog ID" +":  ");
		
		coverImagesPanel = new CoverImagesPanel(this.main);
		
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
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(labPublisher, c);
		c.gridx++; c.weightx = 1; c.gridwidth = 3; add(cmbbxPublisher, c);
		
		c.gridy++; c.gridx = -1;
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(labCatalogID, c);
		c.gridx++; c.weightx = 1; c.gridwidth = 3; add(fldCatalogID, c);
		
		c.gridy++; c.gridx = -1;
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(labReleaseYear, c);
		c.gridx++; c.weightx = 1; c.gridwidth = 1; add(fldReleaseYear, c);
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(chkbxRead , c);
		c.gridx++; c.weightx = 0; c.gridwidth = 1; add(chkbxOwned, c);
		
		c.gridy++;
		c.gridx = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 4;
		c.fill = GridBagConstraints.BOTH;
		add(coverImagesPanel, c);
		
		updateFields();
		
		this.main.notifier.bookSeries.addListener(new Notifier.BookSeriesChangeListener() {
			@Override public void fieldChanged (Object source,      BookSeries  bookSeries,     BookSeries.Field  field ) {}
			@Override public void fieldsChanged(Object source,  Set<BookSeries> bookSeries, Set<BookSeries.Field> fields) {}
			@Override public void added        (Object source,      BookSeries  bookSeries) { updateCmbbxBookSeries(source); }
			@Override public void deleted      (Object source, List<BookSeries> bookSeries) { updateCmbbxBookSeries(source); }
			
			private void updateCmbbxBookSeries(Object source)
			{
				if (source == BookPanel.this) return;
				cmbbxBookSeries.updateValues();
			}
		});
		
		this.main.notifier.publishers.addListener(new Notifier.PublisherChangeListener()
		{
			@Override public void added  (Object source,      Publisher  publisher ) { updateCmbbxPublisher(source); }
			@Override public void deleted(Object source, List<Publisher> publishers) { updateCmbbxPublisher(source); }
			
			private void updateCmbbxPublisher(Object source)
			{
				if (source == BookPanel.this) return;
				cmbbxPublisher.updateValues();
			}
		});
	}
	
	private void assignBookSeriesToCurrentBook(BookSeries bs)
	{
		if (currentBook == null) return;
		main.bookStorage.assign(currentBook, bs);
		updateFldBookSeriesPos();
		updateBtnChangeBSPos();
		main.notifier.books.fieldChanged(this, currentBook, Field.BookSeries);
	}
	
	private void updateFldBookSeriesPos()
	{
		if (currentBook == null) return;
		fldBookSeriesPos.setText(Tools.getIfNotNull(currentBook.bookSeries, "", bs -> Tools.toOrdinalString(bs.books.indexOf(currentBook)+1) ));
	}
	
	private void updateBtnChangeBSPos()
	{
		btnChangeBSPos.setEnabled(
				currentBook!=null &&
				currentBook.bookSeries!=null &&
				currentBook.bookSeries.books.size()>1
		);
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
		fldReleaseYear  .setText(Tools.getIfNotNull(currentBook, "", b -> b.release  ));
		fldCatalogID    .setText(Tools.getIfNotNull(currentBook, "", b -> b.catalogID));
		cmbbxBookSeries .setSelectedItem(Tools.getIfNotNull(currentBook, null, b -> b.bookSeries));
		cmbbxPublisher  .setSelectedItem(Tools.getIfNotNull(currentBook, null, b -> b.publisher ));
		chkbxRead       .setSelected(Tools.getIfNotNull(currentBook, false, b -> b.read ));
		chkbxOwned      .setSelected(Tools.getIfNotNull(currentBook, false, b -> b.owned));
		
		setIgnoreInputEvents(false);
		
		coverImagesPanel.setBook(currentBook);
		
		for (Component comp : getComponents())
			comp.setEnabled(currentBook != null && isEnabled());
		
		updateBtnChangeBSPos();
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
	
		@Override protected BookSeries getExistingValue(String str)
		{
			return main.bookStorage.getBookSeries(str);
		}

		@Override protected BookSeries addNewValueAndSet(String str)
		{
			if (currentBook==null) return null;
			BookSeries bs = main.bookStorage.createBookSeries();
			bs.name = str;
			assignBookSeriesToCurrentBook(bs);
			main.notifier.bookSeries.added(BookPanel.this, bs);
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
			main.notifier.books.fieldChanged(BookPanel.this, currentBook, Field.Publisher);
		}

		@Override protected Publisher getExistingValue(String str)
		{
			return main.bookStorage.getPublisher(str);
		}

		@Override protected Publisher addNewValueAndSet(String str)
		{
			if (currentBook==null) return null;
			currentBook.publisher = main.bookStorage.getOrCreatePublisher(str);
			main.notifier.books.fieldChanged(BookPanel.this, currentBook, Field.Publisher);
			main.notifier.publishers.added(BookPanel.this, currentBook.publisher);
			return currentBook.publisher;
		}
	}

	private static class CoverImagesPanel extends JPanel
	{
		private static final long serialVersionUID = -7178751322272176418L;
		
		private final MyHomeLibrary main;
		private final SmallImageView imgvwBack;
		private final SmallImageView imgvwSpine;
		private final SmallImageView imgvwFront;
		private Book currentBook;

		CoverImagesPanel(MyHomeLibrary main)
		{
			super(new GridBagLayout());
			this.main = main;
			currentBook = null;
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			imgvwBack  = new SmallImageView(this.main, 200, 300, "Back Cover" , Book.CoverPart.back , createFileInBookSetter((b,filename) -> b.backCover  = filename, Field.BackCover , null));
			imgvwSpine = new SmallImageView(this.main, 100, 300, "Book Spine" , Book.CoverPart.spine, createFileInBookSetter((b,filename) -> b.spineCover = filename, Field.SpineCover, null));
			imgvwFront = new SmallImageView(this.main, 200, 300, "Front Cover", Book.CoverPart.front, createFileInBookSetter((b,filename) -> b.frontCover = filename, Field.FrontCover, this::updateFrontCoverThumb));
			
			c.weighty = 1; 
			c.gridx = 0; c.weightx = 2; add(imgvwBack, c);
			c.gridx = 1; c.weightx = 1; add(imgvwSpine, c);
			c.gridx = 2; c.weightx = 2; add(imgvwFront, c);
		}
		
		private void updateFrontCoverThumb(Book book, BufferedImage image)
		{
			if (book==null) return;
			book.updateFrontCoverThumb(image);
			
			main.notifier.books.fieldChanged(this, book, Field.FrontCoverThumb);
		}

		private SmallImageView.FileInBookSetter createFileInBookSetter(BiConsumer<Book,String> setValue, Field field, BiConsumer<Book,BufferedImage> processImage)
		{
			return (filename, image) -> Tools.doIfNotNull(
					currentBook,
					b -> {
						setValue.accept(b, filename);
						if (processImage!=null)
							processImage.accept(b,image);
						main.notifier.books.fieldChanged(this, b, field);
					}
			);
		}

		void setBook(Book currentBook)
		{
			this.currentBook = currentBook;
			String bookId =            Tools.getIfNotNull(this.currentBook, null, b -> b.id);
			imgvwBack .setData(bookId, Tools.getIfNotNull(this.currentBook, null, b -> b.backCover ));
			imgvwSpine.setData(bookId, Tools.getIfNotNull(this.currentBook, null, b -> b.spineCover));
			imgvwFront.setData(bookId, Tools.getIfNotNull(this.currentBook, null, b -> b.frontCover));
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
		private final ModifiedImageViewContextMenu contextMenu;
		private final FileInBookSetter setFileInBook;
		
		interface FileInBookSetter
		{
			void setFileInBook(String filename, BufferedImage image);
		}
		
		SmallImageView(MyHomeLibrary main, int width, int height, String title, Book.CoverPart coverPart, FileInBookSetter setFileInBook)
		{
			super(null, width, height, null, true, (iv, wPIL, isG) -> new ModifiedImageViewContextMenu(main, iv, wPIL, isG, coverPart));
			this.setFileInBook = Objects.requireNonNull( setFileInBook );
			
			setBorder(BorderFactory.createTitledBorder(title));
			activateAxes(null, false, false, false, false);
			
			if (getContextMenu() instanceof ModifiedImageViewContextMenu modMenu)
			{
				contextMenu = modMenu;
				contextMenu.setImageFileSetter(this::setFile);
			}
			else
				contextMenu = null;
		}

		void setData(String bookId, String filename)
		{
			if (contextMenu!=null)
				contextMenu.setBookId(bookId, filename);
			
			setImage( FileIO.loadImagefile( filename ) );
		}
		
		private void setFile(String filename, BufferedImage image, boolean loadImage)
		{
			if (image==null && loadImage)
				image = FileIO.loadImagefile(filename);
			
			setImage(image);
			setFileInBook.setFileInBook(filename, image);
		}
		
		private static class ModifiedImageViewContextMenu extends ImageViewContextMenu
		{
			private static final long serialVersionUID = 1369192488640827455L;
			private final MyHomeLibrary main;
			private final Book.CoverPart coverPart;
			private ImageFileSetter setImgFile;
			private JMenu menuPrevImages;
			private String bookId;

			interface ImageFileSetter
			{
				void setImgFile(String filename, BufferedImage image, boolean loadImage);
			}
			
			protected ModifiedImageViewContextMenu(MyHomeLibrary main, ImageView imageView, boolean withPredefinedInterpolationLevel, boolean isGrouped, Book.CoverPart coverPart)
			{
				super(imageView, withPredefinedInterpolationLevel, isGrouped);
				this.main = main;
				this.coverPart = Objects.requireNonNull( coverPart );
				this.setImgFile = null;
				bookId = null;
			}
			
			void setImageFileSetter(ImageFileSetter setImgFile)
			{
				this.setImgFile = setImgFile;
			}

			void setBookId(String bookId, String currentFilename)
			{
				this.bookId = bookId;
				updatePrevImageMenu(currentFilename);
			}

			@Override
			protected void addElementsAtFirst()
			{
				add(Tools.createMenuItem("Paste new image from clipboard", true, null, e -> {
					pasteImage();
				}));
				add(Tools.createMenuItem("Load new image from file ...", true, null, e -> {
					loadImage();
				}));
				
				add(menuPrevImages = new JMenu("Select one of prev. images"));
			}

			private void loadImage()
			{
				if (JFileChooser.APPROVE_OPTION != main.imageImportFileChooser.showOpenDialog(main.mainWindow))
					return;
				
				File file = main.imageImportFileChooser.getSelectedFile();
				BufferedImage image;
				try { image = ImageIO.read(file); }
				catch (IOException ex)
				{
					//ex.printStackTrace();
					System.err.printf("IOException while reading image \"%s\": %s%n", file.getAbsolutePath(), ex.getMessage());
					String title = "IOException while reading image";
					String msg = "IOException while reading image:%n"
							+ "   Image file:\"%s\"%n"
							+ "   Error: %s".formatted(
									file.getAbsolutePath(),
									ex.getMessage()
							);
					JOptionPane.showMessageDialog(main.mainWindow, msg, title, JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (image==null)
				{
					String title = "File isn't an image";
					String msg = "Given file \"%s\"%n isn't an image with a known format.".formatted(file.getAbsolutePath());
					JOptionPane.showMessageDialog(main.mainWindow, msg, title, JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				image = ImageImportDialog.showDialog(main.mainWindow, image);
				if (image==null)
					return;
				
				writeImage(image);
			}

			private void pasteImage()
			{
				BufferedImage image = ClipboardTools.getImageFromClipBoard();
				if (image==null)
				{
					String title = "No image in clipboard";
					String msg = "Sorry, no image data found in clipboard.";
					JOptionPane.showMessageDialog(main.mainWindow, msg, title, JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				image = ImageImportDialog.showDialog(main.mainWindow, image);
				if (image==null)
					return;
				
				writeImage(image);
			}

			private void writeImage(BufferedImage image)
			{
				int imageWidth  = image.getWidth();
				int imageHeight = image.getHeight();
				double f = Math.max(imageWidth, imageHeight) / Tools.IMAGE_REDUCTION__MAX_SIZE;
				if (f>Tools.IMAGE_REDUCTION__THRESHOLD)
				{
					int reducedWidth  = (int) Math.round( imageWidth  / f );
					int reducedHeight = (int) Math.round( imageHeight / f );
					
					String title = "Big Image";
					String[] msg = {
							"Given immage is very big (%d x %d => height > 1000).".formatted(imageWidth, imageHeight),
							"That's bigger, than needed. Image file will need too much space in image storage.",
							"Should I reduce image size to %d x %d?".formatted(reducedWidth, reducedHeight)
					};
					int result = JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
					if (result == JOptionPane.YES_OPTION)
						image = ImageView.computeScaledImageByAreaSampling(image, reducedWidth, reducedHeight, true);
					
					else if (result != JOptionPane.NO_OPTION)
						return;
				}
				
				String filename;
				try
				{
					filename = FileIO.saveImageFile(bookId,coverPart,image);
				}
				catch (FileIOException ex)
				{
					//ex.printStackTrace();
					ex.showMessageDialog(
							this,
							"Can't write image file",
							"Can't write image file ( bookID: \"%s\", cover part: %s ).".formatted(bookId,coverPart)
					);
					return;
				}
				
				if (filename!=null)
				{
					if (setImgFile!=null)
						setImgFile.setImgFile(filename, image, false);
					updatePrevImageMenu(filename);
				}
			}
			
			private void updatePrevImageMenu(String currentFilename)
			{
				String[] filenames;
				try { filenames = FileIO.getAllImageFiles(bookId,coverPart); }
				catch (FileIOException ex)
				{
					// ex.printStackTrace();
					ex.showInErrorConsole("Can't get list of existing images (%s_%s*.jpg):".formatted(bookId,coverPart));
					filenames = new String[0];
				}
				menuPrevImages.removeAll();
				menuPrevImages.setEnabled(filenames.length>0);
				Arrays.sort(filenames);
				ButtonGroup bg = new ButtonGroup();
				for (String filename : filenames)
					if (filename!=null)
						menuPrevImages.add(Tools.createCheckBoxMenuItem(filename, true, filename.equals(currentFilename), bg, null, b -> {
							if (setImgFile!=null)
								setImgFile.setImgFile(filename, null, true);
						}));
			}
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
					main.notifier.authors.added(this, result);
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
		private final Runnable updateAfterChange;
		
		private final JList<Author> list;
		private final DefaultListModel<Author> listModel;
		private final JButton btnAdd;
		private final JButton btnRemove;
		private final JButton btnMoveUp;
		private final JButton btnMoveDown;
		
		private List<Author> selected;
		private int[] selectedIndices;

		private EditAuthorsDialog(MyHomeLibrary main, List<Author> authors, Runnable updateAfterChange)
		{
			super(main.mainWindow, "Edit Authors");
			this.updateAfterChange = Objects.requireNonNull( updateAfterChange );
			this.authors = Objects.requireNonNull( authors );
			selected = List.of();
			selectedIndices = new int[0];
			
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
					this.updateAfterChange.run();
				}
			});
			btnRemove = Tools.createButton("Remove", true, GrayCommandIcons.IconGroup.Delete, e -> {
				if (!selected.isEmpty())
				{
					this.authors.removeAll(selected);
					for (Author a : selected)
						listModel.removeElement(a);
					this.updateAfterChange.run();
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
			c.gridx++; buttonPanel.add(btnAdd     , c);
			c.gridx++; buttonPanel.add(btnRemove  , c);
			c.gridx++; buttonPanel.add(btnMoveUp  , c);
			c.gridx++; buttonPanel.add(btnMoveDown, c);
			
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
				boolean success1 = Tools.swap(authors  , index, index+inc);
				boolean success2 = Tools.swap(listModel, index, index+inc);
				if (success2)
					list.setSelectedIndex(index+inc);
				if (success1)
					updateAfterChange.run();
			}
		}

		private void updateButtons()
		{
			btnAdd     .setEnabled(true);
			btnRemove  .setEnabled(!selected.isEmpty());
			btnMoveUp  .setEnabled(selectedIndices.length==1 && selectedIndices[0]>0);
			btnMoveDown.setEnabled(selectedIndices.length==1 && selectedIndices[0]>=0 && selectedIndices[0]+1<authors.size());
		}

		static void showDialog(MyHomeLibrary main, List<Author> authors, Runnable updateAuthorsField)
		{
			new EditAuthorsDialog(main, authors, updateAuthorsField).showDialog();
		}
	}

	private static class ChangeBSPosDialog extends StandardDialog
	{
		private static final long serialVersionUID = 4898594147398259360L;
		
		private final BookSeries bookSeries;
		private final Runnable updateAfterChange;
		
		private final JList<Book> list;
		private final JButton btnMoveUp;
		private final JButton btnMoveDown;
		
		private int selectedIndex;

		private ChangeBSPosDialog(MyHomeLibrary main, BookSeries bookSeries, Runnable updateAfterChange)
		{
			super(main.mainWindow, "Change order of books in series");
			this.updateAfterChange = Objects.requireNonNull( updateAfterChange );
			this.bookSeries = Objects.requireNonNull( bookSeries );
			selectedIndex = -1;
			
			JTextField fldBSName = new JTextField(this.bookSeries.name, 30);
			fldBSName.setEditable(false);
			fldBSName.setCaretPosition(0);
			
			list = new JList<>(this.bookSeries.books);
			list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			list.addListSelectionListener(ev -> {
				selectedIndex = list.getSelectedIndex();
				updateButtons();
			});
			JScrollPane scrollPane = new JScrollPane(list);
			
			btnMoveUp   = Tools.createButton("Move", true, GrayCommandIcons.IconGroup.Up  , e -> swap(-1));
			btnMoveDown = Tools.createButton("Move", true, GrayCommandIcons.IconGroup.Down, e -> swap(+1));
			
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 1;
			c.gridx = -1;
			c.gridy = 0;
			c.gridx++; buttonPanel.add(btnMoveUp  , c);
			c.gridx++; buttonPanel.add(btnMoveDown, c);
			
			JPanel panelTitle = new JPanel(new BorderLayout());
			panelTitle.add(new JLabel("Book Series: "), BorderLayout.WEST);
			panelTitle.add(fldBSName, BorderLayout.CENTER);
			
			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.add(panelTitle, BorderLayout.NORTH);
			contentPane.add(scrollPane, BorderLayout.CENTER);
			contentPane.add(buttonPanel, BorderLayout.SOUTH);
			
			createGUI(contentPane, Tools.createButton("Close", true, null, e -> closeDialog()));
			
			updateButtons();
		}
		
		private void swap(int inc)
		{
			if (selectedIndex < 0) return;
			
			int index = selectedIndex;
			boolean success = Tools.swap(this.bookSeries.books, index, index+inc);
			if (success)
			{
				list.setSelectedIndex(index+inc);
				updateAfterChange.run();
			}
		}

		private void updateButtons()
		{
			btnMoveUp  .setEnabled(selectedIndex>0);
			btnMoveDown.setEnabled(selectedIndex>=0 && selectedIndex+1<bookSeries.books.size());
		}

		static void showDialog(MyHomeLibrary main, BookSeries bookSeries, Runnable updateFldBookSeriesPos)
		{
			new ChangeBSPosDialog(main, bookSeries, updateFldBookSeriesPos).showDialog();
		}
	}
}
