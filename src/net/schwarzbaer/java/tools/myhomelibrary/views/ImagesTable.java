package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.DataSource;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO.FileIOException;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;
import net.schwarzbaer.java.tools.myhomelibrary.data.ImageData;

class ImagesTable extends JTable
{
	private static final Color BGCOLOR_NO_IMAGE          = new Color(0xFFCFFF);
	private static final Color BGCOLOR_WRONG_NAME_SCHEME = new Color(0xCFCFFF);
	private static final Color BGCOLOR_UNKNOWN_BOOK      = new Color(0xFFCFCF);
	private static final long serialVersionUID = 3303408662583526060L;
	
	private final MyHomeLibrary main;
	        final ImagesTableModel tableModel;
	private final Tables.GeneralizedTableCellRenderer2<ImageData, ImagesTableModel.ColumnID, ImagesTableModel> tableCellRenderer;
	        final Tables.SimplifiedRowSorter tableRowSorter;
	        final JScrollPane tableScrollPane;
	private ImagesTableContextMenu tableContextMenu;
	        List<ImageData> selectedRows;

	ImagesTable(MyHomeLibrary main)
	{
		this.main = main;
		setModel(tableModel = new ImagesTableModel());
		selectedRows = null;
		
		setRowSorter(tableRowSorter = new Tables.SimplifiedRowSorter(tableModel));
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setColumnSelectionAllowed(false);
		tableModel.setTable(this);
		tableModel.setColumnWidths(this);
		
		tableCellRenderer = new Tables.GeneralizedTableCellRenderer2<>(ImagesTableModel.class);
		tableCellRenderer.setBackgroundColorizer((value, rowM, columnM, columnID, row) -> {
			if (row!=null && row.size     ==null) return BGCOLOR_NO_IMAGE;
			if (row!=null && row.nameParts==null) return BGCOLOR_WRONG_NAME_SCHEME;
			if (columnID==ImagesTableModel.ColumnID.NamePart_bookID && value instanceof String bookID && !this.main.bookStorage.hasBook(bookID))
				return BGCOLOR_UNKNOWN_BOOK;
			return null;
		});
		tableModel.setAllDefaultRenderers(cl -> tableCellRenderer);
		
		tableScrollPane = new JScrollPane(this);
		tableScrollPane.setPreferredSize(new Dimension(1000,500));
		
		tableContextMenu = new ImagesTableContextMenu(this.main, this, tableModel, tableScrollPane);
	}

	List<ImageData> getSelectedRows_()
	{
		return selectedRows = Arrays
			.stream(getSelectedRows())
			.filter(rowV -> rowV>=0)
			.map(this::convertRowIndexToModel)
			.filter(rowM -> rowM>=0)
			.mapToObj(tableModel::getRow)
			.filter(row -> row!=null)
			.toList();
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
		
		List<ImageData> list = ImageData.toList(files);
		tableModel.setData(new Vector<>(list));
		
		new Thread(() -> {
			completeImageData(list);
			checkUsage(list);
			SwingUtilities.invokeLater(()-> {
				if (tableContextMenu.miReload!=null)
					tableContextMenu.miReload.setEnabled(true);
			});
		}).start();
	}

	void checkUsage(List<ImageData> list)
	{
		main.bookStorage.checkUsage(list, () -> tableModel.fireTableColumnUpdate(ImagesTableModel.ColumnID.Usage));
	}

	private void completeImageData(List<ImageData> list)
	{
		for (ImageData imgData : list)
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

	class ImagesTableModel extends Tables.SimpleGetValueTableModel2<ImagesTableModel, ImageData, ImagesTableModel.ColumnID>
	{
		enum ColumnID implements Tables.SimpleGetValueTableModel2.ColumnIDTypeInt2b<ImagesTableModel,ImageData>, SwingConstants
		{
			// Column Widths: [150, 60, 45, 45, 65, 70, 45, 35] in ModelOrder
			File_Name          ( config("FileName"  , String        .class, 150, null  ).setValFunc(d -> Tools.getIfNotNull(d.file, null, File::getName)) ),
			File_Size          ( config("FileSize"  , Long          .class,  60, null  ).setValFunc(d -> Tools.getIfNotNull(d.file, null, File::length )) ),
			Image_Width        ( config("Width"     , Integer       .class,  45, null  ).setValFunc(d -> Tools.getIfNotNull(d.size, null, s -> s.width )) ),
			Image_Height       ( config("Height"    , Integer       .class,  45, null  ).setValFunc(d -> Tools.getIfNotNull(d.size, null, s -> s.height)) ),
			Usage              ( config("In Use"    , Boolean       .class,  45, null  ).setValFunc(d -> d.book!=null) ),
			NamePart_bookID    ( config("Book ID"   , String        .class,  65, CENTER).setValFunc(d -> Tools.getIfNotNull(d.nameParts, null, FileIO.NameParts::bookID   )) ),
			NamePart_coverPart ( config("Cover Part", Book.CoverPart.class,  70, CENTER).setValFunc(d -> Tools.getIfNotNull(d.nameParts, null, FileIO.NameParts::coverPart)) ),
			NamePart_extra     ( config("Extra"     , String        .class,  45, CENTER).setValFunc(d -> Tools.getIfNotNull(d.nameParts, null, FileIO.NameParts::extra    )) ),
			NamePart_extension ( config("Ext."      , String        .class,  35, CENTER).setValFunc(d -> Tools.getIfNotNull(d.nameParts, null, FileIO.NameParts::extension)) ),
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
		
		private List<ImageData> data;

		ImagesTableModel()
		{
			super(ColumnID.values());
		}

		@Override protected ImagesTableModel getThis() { return this; }

		@Override public void setData(ImageData[]           data) { throw new UnsupportedOperationException(); }
		@Override public void setData(DataSource<ImageData> data) { throw new UnsupportedOperationException(); }

		@Override
		public void setData(List<ImageData> data)
		{
			this.data = data;
			super.setData(DataSource.createFrom(data));
		}

		void removeRows(List<ImageData> list)
		{
			data.removeAll(list);
			setData(data);
		}
	}
}
