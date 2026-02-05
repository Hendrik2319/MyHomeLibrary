package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedRowSorter;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;

public class BooksTable extends JTable
{
	private static final long serialVersionUID = -4421855766356160275L;
	        final BooksTableModel tableModel;
	@SuppressWarnings("unused")
	private final SimplifiedRowSorter tableRowSorter;
	private final BooksTableCellRenderer tableCellRenderer;
	        final JScrollPane tableScrollPane;

	BooksTable()
	{
		setModel(tableModel = new BooksTableModel());
		
		setRowHeight(Book.FRONTCOVERTHUMB_MAXHEIGHT+6);
		setRowSorter(tableRowSorter = new Tables.SimplifiedRowSorter(tableModel));
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setColumnSelectionAllowed(false);
		tableModel.setTable(this);
		tableModel.setColumnWidths(this);
		
		tableCellRenderer = new BooksTableCellRenderer(this);
		tableModel.setAllDefaultRenderers(cl -> tableCellRenderer);
		
		tableScrollPane = new JScrollPane(this);
		tableScrollPane.setPreferredSize(new Dimension(1000,500));
	}
	
	static class BooksTableModel extends Tables.SimplifiedTableModel<BooksTableModel.ColumnID>
	{
		enum ColumnID implements Tables.SimplifiedColumnIDInterface
		{
			BookCover("Cover"    , BufferedImage.class, Book.FRONTCOVERTHUMB_MAXWIDTH+6),
			BookInfo ("Book Info", Book         .class, 500),
			;
			private final SimplifiedColumnConfig cfg;
			ColumnID(String name, Class<?> columnClass, int width)
			{
				cfg = new SimplifiedColumnConfig(name, columnClass, 20, -1, width, width);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return cfg; }
		}

		private List<Book> data;

		BooksTableModel()
		{
			super(ColumnID.values());
			data = null;
		}
		
		void setData(List<Book> data)
		{
			System.out.printf("BooksTableModel.setData( %s )%n", data==null ? "<null>" : "%d Books".formatted(data.size()));
			this.data = data;
			fireTableUpdate();
		}

		@Override
		public int getRowCount()
		{
			return data==null ? 0 : data.size();
		}
		
		public Book getRow(int rowIndex)
		{
			if (data==null)
				return null;
			
			if (rowIndex<0 || rowIndex>=data.size())
				return null;
			
			return data.get(rowIndex);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID)
		{
			Book row = getRow(rowIndex);
			
			if (row==null || columnID==null)
				return null;
			
			switch (columnID)
			{
			case BookCover:
				 return row.frontCoverThumb;
			case BookInfo:
				 return row;
			}
			
			return null;
		}

		void fireColumnUpdateForFields(Set<Book.Field> fields)
		{
			fields
				.stream()
				.map(BooksTableCellRenderer::getColumnID)
				.filter(id -> id!=null)
				.distinct()
				.forEach(this::fireTableColumnUpdate);
		}

		void fireColumnUpdateForField(Book.Field field)
		{
			ColumnID columnID = BooksTableCellRenderer.getColumnID(field);
			if (columnID!=null)
				fireTableColumnUpdate(columnID);
		}
	}
}
