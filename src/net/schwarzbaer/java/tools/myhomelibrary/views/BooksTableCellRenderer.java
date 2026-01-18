package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.RendererConfigurator;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;
import net.schwarzbaer.java.tools.myhomelibrary.views.BooksTable.BooksTableModel.ColumnID;

class BooksTableCellRenderer implements TableCellRenderer
{
	private final BooksTable table;
	private final Tables.LabelRendererComponent labRendComp;
	private final InfoBlock1 infoBlock1;
	private final InfoBlock2 infoBlock2;

	BooksTableCellRenderer(BooksTable table)
	{
		this.table = table;
		labRendComp = new Tables.LabelRendererComponent();
		infoBlock1 = new InfoBlock1();
		infoBlock2 = new InfoBlock2();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV)
	{
		Component rendComp = null;
		final String defaultValueStr = value==null ? null : value.toString();
		
		if (table == this.table)
		{
			//int rowM = rowV<0 ? -1 : this.table.convertRowIndexToModel(rowV);
			//Book row = this.table.tableModel.getRow(rowM);
			
			int columnM = columnV<0 ? -1 : this.table.convertColumnIndexToModel(columnV);
			ColumnID columnID = this.table.tableModel.getColumnID(columnM);
			
			if (columnID != null)
				switch (columnID)
				{
				case BookCover:
					if (value instanceof BufferedImage image)
					{
						rendComp = labRendComp;
						labRendComp.configureAsTableCellRendererComponent(table, new ImageIcon(image), null, isSelected, hasFocus);
					}
					else if (value==null)
					{
						rendComp = labRendComp;
						labRendComp.configureAsTableCellRendererComponent(table, null, "No Image", isSelected, hasFocus);
					}
					
					break;
					
				case BookInfo1:
					if (value instanceof Book book)
					{
						rendComp = infoBlock1;
						infoBlock1.setData(book);
						infoBlock1.rendConf.configureAsTableCRC(table, isSelected, hasFocus);
					}
					break;
					
				case BookInfo2:
					if (value instanceof Book book)
					{
						rendComp = infoBlock2;
						infoBlock2.setData(book);
						infoBlock2.rendConf.configureAsTableCRC(table, isSelected, hasFocus);
					}
					break;
				}
		}
		
		if (rendComp==null)
		{
			rendComp = labRendComp;
			labRendComp.configureAsTableCellRendererComponent(table, null, defaultValueStr, isSelected, hasFocus);
		}
		
		return rendComp;
	}
	
	private static Font scaleSize(Font font, float value)
	{
		return font.deriveFont(font.getSize2D() * value);
	}

	static ColumnID getColumnID(Book.Field field)
	{
		if (InfoBlock1.showsField(field))
			return ColumnID.BookInfo1;
		if (InfoBlock2.showsField(field))
			return ColumnID.BookInfo2;
		if (field==Book.Field.FrontCover)
			return ColumnID.BookCover;
		return null;
	}

	private static class InfoBlock1 extends JPanel
	{
		private static final long serialVersionUID = -3584565062346788798L;
		private final JLabel fldTitle;
		private final JLabel fldAuthors;
		private final JLabel fldBookSeries;
		private final Tables.RendererConfigurator rendConf;
		
		InfoBlock1()
		{
			super(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = -1;
			c.gridy++; add(fldTitle      = new JLabel(), c);
			c.gridy++; add(fldAuthors    = new JLabel(), c);
			c.gridy++; add(fldBookSeries = new JLabel(), c);
			c.weighty = 1;
			c.gridy++; add(new JLabel(), c);
			
			fldTitle.setFont(
					scaleSize(fldTitle.getFont(), 1.2f).deriveFont(Font.BOLD)
			);
			
			rendConf = Tables.RendererConfigurator.create(
					font -> {},
					this::setBorder,
					this::setOpaque,
					fgColor -> {
						fldTitle     .setForeground(fgColor);
						fldAuthors   .setForeground(fgColor);
						fldBookSeries.setForeground(fgColor);
					},
					this::setBackground
			);
		}

		static boolean showsField(Book.Field field)
		{
			return field==Book.Field.Title || field==Book.Field.Authors || field==Book.Field.BookSeries;
		}

		public void setData(Book book)
		{
			fldTitle     .setText(Tools.getIfNotNull(book, "<null>", b -> Tools.getIfNotNull(b.title, "<unnamed book>")));
			fldAuthors   .setText("Author(s): %s".formatted(book==null || book.authors.isEmpty() ? "---" : book.concatenateAuthors()));
			fldBookSeries.setText(book==null || book.bookSeries==null ? "----" : "%s book of series \"%s\"".formatted(
					Tools.toOrdinalString( book.bookSeries.books.indexOf(book)+1 ),
					book.bookSeries.name != null && !book.bookSeries.name.isBlank()
						? book.bookSeries.name
						: "<unnamed series>"
			));
		}
	}
	
	private static class InfoBlock2 extends JPanel
	{
		private static final long serialVersionUID = 4359321142522911340L;
		private final JLabel fldPublisher;
		private final JLabel fldCatalogID;
		private final JLabel fldReleaseYear;
		private final RendererConfigurator rendConf;
		
		InfoBlock2()
		{
			super(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = -1;
			c.gridy++; add(fldPublisher   = new JLabel(), c);
			c.gridy++; add(fldCatalogID   = new JLabel(), c);
			c.gridy++; add(fldReleaseYear = new JLabel(), c);
			c.weighty = 1;
			c.gridy++; add(new JLabel(), c);
			
			rendConf = Tables.RendererConfigurator.create(
					font -> {},
					this::setBorder,
					this::setOpaque,
					fgColor -> {
						fldPublisher  .setForeground(fgColor);
						fldCatalogID  .setForeground(fgColor);
						fldReleaseYear.setForeground(fgColor);
					},
					this::setBackground
			);
		}

		public static boolean showsField(Book.Field field)
		{
			return field==Book.Field.Publisher || field==Book.Field.CatalogID || field==Book.Field.ReleaseYear;
		}

		public void setData(Book book)
		{
			fldPublisher  .setText("Publisher: %s" .formatted(book==null || book.publisher==null || book.publisher.name().isBlank() ? "---" : book.publisher.name()));
			fldCatalogID  .setText("Catalog ID: %s".formatted(book==null || book.catalogID==null || book.catalogID.isBlank()        ? "---" : book.catalogID       ));
			fldReleaseYear.setText("Release: %s"   .formatted(book==null || book.releaseYear<0                                      ? "---" : book.releaseYear     ));
		}
	}
}
