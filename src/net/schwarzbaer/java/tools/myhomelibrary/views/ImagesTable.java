package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO.FileIOException;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;
import net.schwarzbaer.java.tools.myhomelibrary.data.BookStorage;

class ImagesTable extends JTable
{
	private static final long serialVersionUID = 3303408662583526060L;
	        final ImagesTableModel tableModel;
	@SuppressWarnings("unused")
	private final Tables.SimplifiedRowSorter tableRowSorter;
	        final JScrollPane tableScrollPane;
	private ImagesTableContextMenu tableContextMenu;

	ImagesTable()
	{
		setModel(tableModel = new ImagesTableModel());
		
		setRowSorter(tableRowSorter = new Tables.SimplifiedRowSorter(tableModel));
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setColumnSelectionAllowed(false);
		tableModel.setTable(this);
		tableModel.setColumnWidths(this);
		
//		tableCellRenderer = new BooksTableCellRenderer(this);
//		tableModel.setAllDefaultRenderers(cl -> tableCellRenderer);
		
		tableScrollPane = new JScrollPane(this);
		tableScrollPane.setPreferredSize(new Dimension(1000,500));
		
		tableContextMenu = new ImagesTableContextMenu(this, tableModel, tableScrollPane);
	}

	void reloadData()
	{
		if (tableContextMenu.miReload!=null)
			tableContextMenu.miReload.setEnabled(false);
		
		File[] files;
		try { files = FileIO.getAllFilesInImageFolder(); }
		catch (FileIOException ex)
		{
			//ex.printStackTrace();
			ex.showMessageDialog(
					this,
					"Can't read files",
					"Error while reading files in image folder:"
			);
			return;
		}
		
		List<ImagesTableModel.ImageData> list = Arrays
			.stream(files)
			.map(ImagesTableModel.ImageData::new)
			.toList();
		tableModel.setData(new Vector<>(list));
		
		new Thread(() -> {
			completeImageData(list);
			SwingUtilities.invokeLater(()-> {
				if (tableContextMenu.miReload!=null)
					tableContextMenu.miReload.setEnabled(true);
			});
		}).start();
	}

	private void completeImageData(List<ImagesTableModel.ImageData> list)
	{
		for (ImagesTableModel.ImageData imgData : list)
		{
			BufferedImage image;
			try { image = ImageIO.read(imgData.file); }
			catch (IOException ex)
			{
				//ex.printStackTrace();
				System.err.printf("IOException while reading file \"%s\": %s%n", imgData.file.getAbsolutePath(), ex.getMessage());
				continue;
			}
			if (image==null)
				continue;
			
			imgData.size = new Dimension(image.getWidth(), image.getHeight());
			tableModel.fireTableColumnUpdate(ImagesTableModel.ColumnID.Image_Width);
			tableModel.fireTableColumnUpdate(ImagesTableModel.ColumnID.Image_Height);
		}
	}

	class ImagesTableModel extends Tables.SimpleGetValueTableModel2<ImagesTableModel, ImagesTableModel.ImageData, ImagesTableModel.ColumnID>
	{
		enum ColumnID implements Tables.SimpleGetValueTableModel2.ColumnIDTypeInt2b<ImagesTableModel,ImageData>, SwingConstants
		{
			// Column Widths: [150, 60, 45, 45, 65, 70, 45, 35] in ModelOrder
			File_Name          ( config("FileName"  , String        .class, 150, null  ).setValFunc(d -> Tools.getIfNotNull(d.file, null, File::getName)) ),
			File_Size          ( config("FileSize"  , Long          .class,  60, null  ).setValFunc(d -> Tools.getIfNotNull(d.file, null, File::length )) ),
			Image_Width        ( config("Width"     , Integer       .class,  45, null  ).setValFunc(d -> Tools.getIfNotNull(d.size, null, s -> s.width )) ),
			Image_Height       ( config("Height"    , Integer       .class,  45, null  ).setValFunc(d -> Tools.getIfNotNull(d.size, null, s -> s.height)) ),
			NamePart_bookID    ( config("Book ID"   , String        .class,  65, CENTER).setValFunc(d -> Tools.getIfNotNull(d.nameParts, null, NameParts::bookID   )) ),
			NamePart_coverPart ( config("Cover Part", Book.CoverPart.class,  70, CENTER).setValFunc(d -> Tools.getIfNotNull(d.nameParts, null, NameParts::coverPart)) ),
			NamePart_extra     ( config("Extra"     , String        .class,  45, CENTER).setValFunc(d -> Tools.getIfNotNull(d.nameParts, null, NameParts::extra    )) ),
			NamePart_extension ( config("Ext."      , String        .class,  35, CENTER).setValFunc(d -> Tools.getIfNotNull(d.nameParts, null, NameParts::extension)) ),
			;
			final Tables.SimplifiedColumnConfig2<ImagesTableModel, ImageData, ?> cfg;
			ColumnID(Tables.SimplifiedColumnConfig2<ImagesTableModel, ImageData, ?> cfg) { this.cfg = cfg; }
			@Override public Tables.SimplifiedColumnConfig2<ImagesTableModel, ImageData, ?> getColumnConfig() { return this.cfg; }
			@Override public Function<ImageData, ?> getGetValue() { return cfg.getValue; }
			@Override public BiFunction<ImagesTableModel, ImageData, ?> getGetValueM() { return cfg.getValueM; }
			
			private static <T> Tables.SimplifiedColumnConfig2<ImagesTableModel, ImageData, T> config(String name, Class<T> columnClass, int prefWidth, Integer horizontalAlignment)
			{
				return new Tables.SimplifiedColumnConfig2<>(name, columnClass, 20, -1, prefWidth, prefWidth, horizontalAlignment);
			}
		}
		
		ImagesTableModel()
		{
			super(ColumnID.values());
		}

		@Override protected ImagesTableModel getThis() { return this; }

		static class ImageData
		{
			final File file;
			final NameParts nameParts;
			Dimension size = null;
			
			ImageData(File file)
			{
				this.file = Objects.requireNonNull( file );
				nameParts = NameParts.parse( this.file.getName() );
			}
		}
		
		record NameParts(String bookID, Book.CoverPart coverPart, String extra, String extension)
		{
			static NameParts parse(String filename)
			{
				// VZQIQQH_front_008.jpg
				// VZQIQQH_back.jpg
				
				if (filename.length() < BookStorage.LENGTH_BOOK_ID+1) return null;
				if (filename.charAt(BookStorage.LENGTH_BOOK_ID)!='_') return null;
				String bookID = filename.substring(0, BookStorage.LENGTH_BOOK_ID);
				
				String str = filename.substring(BookStorage.LENGTH_BOOK_ID+1);
				
				Book.CoverPart coverPart = null;
				for (Book.CoverPart cp : Book.CoverPart.values())
					if (str.startsWith(cp.name())) { coverPart = cp; break; }
				if (coverPart == null) return null;
				
				str = str.substring(coverPart.name().length());
				
				int pos = str.lastIndexOf('.');
				if (pos<0) return null;
				
				String ext   = str.substring(pos+1);
				String extra = str.substring(0,pos);
				
				return new NameParts(bookID, coverPart, extra, ext);
			}
		}
	}
}
