package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;
import net.schwarzbaer.java.tools.myhomelibrary.views.BooksTable.BooksTableModel.ColumnID;

class BooksTableCellRenderer implements TableCellRenderer
{
	private final BooksTable table;
	private final Tables.LabelRendererComponent labRendComp;
	private final InfoBlock infoBlock;

	BooksTableCellRenderer(BooksTable table)
	{
		this.table = table;
		labRendComp = new Tables.LabelRendererComponent();
		infoBlock = new InfoBlock();
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
						labRendComp.setHorizontalAlignment( SwingConstants.CENTER );
					}
					else if (value==null)
					{
						rendComp = labRendComp;
						labRendComp.configureAsTableCellRendererComponent(table, null, "No Image", isSelected, hasFocus);
						labRendComp.setHorizontalAlignment( SwingConstants.CENTER );
					}
					
					break;
					
				case BookInfo:
					if (value instanceof Book book)
					{
						rendComp = infoBlock;
						infoBlock.setData(book);
						infoBlock.configureAsTableCellRendererComponent(table, isSelected, hasFocus);
					}
					break;
				}
		}
		
		if (rendComp==null)
		{
			rendComp = labRendComp;
			labRendComp.configureAsTableCellRendererComponent(table, null, defaultValueStr, isSelected, hasFocus);
			labRendComp.setHorizontalAlignment( SwingConstants.LEFT );
		}
		
		return rendComp;
	}
	
	private static Font scaleSize(Font font, float value)
	{
		return font.deriveFont(font.getSize2D() * value);
	}

	static ColumnID getColumnID(Book.Field field)
	{
		if (InfoBlock.showsField(field))
			return ColumnID.BookInfo;
		if (field==Book.Field.FrontCoverThumb)
			return ColumnID.BookCover;
		return null;
	}

	private static class InfoBlock extends Tables.CustomRendererComponent
	{
		private static final long serialVersionUID = -3584565062346788798L;
		private static final int BORDER_SPACING = 1;
		private static final int LEFT_SPACING = 3;
		private final TextBox fldTitle;
		private final TextBox fldAuthors;
		private final TextBox fldBookSeries;
		private final TextBox fldPublisher;
		private final TextBox fldCatalogID;
		private final TextBox fldRelease;
		private final TextBox fldISBN;
		private final TextBox fldPrice;
		private final TextBox fldPageCount;
		private final TextBox fldCover;
		private final TextBox fldNotRead;
		private final TextBox fldNotOwned;
		private Integer titleHeight = null;
		private Integer thirdColumWidth = null;
		
		InfoBlock()
		{
			// first 3 rows
			fldTitle      = new TextBox();
			fldAuthors    = new TextBox();
			fldBookSeries = new TextBox();
			
			// 1st column
			fldPublisher = new TextBox();
			fldISBN      = new TextBox();
			fldRelease   = new TextBox();
			
			// 2nd column
			fldPrice     = new TextBox();
			fldCatalogID = new TextBox();
			fldPageCount = new TextBox();
			
			// 3rd column
			fldNotRead   = new TextBox();
			fldNotOwned  = new TextBox();
			fldCover     = new TextBox();
			
			fldTitle.setFont(
					scaleSize(fldTitle.getFont(), 1.25f).deriveFont(Font.BOLD)
			);
		}

		@Override
		protected void paintContent(Graphics g)
		{
			int width  = this.getWidth ();
			int height = this.getHeight();
			
			if (g instanceof Graphics2D g2)
			{
				g2.setColor(getBackground());
				g2.fillRect(0, 0, width, height);
				
				g2.setColor(getForeground());
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
				
				width  -= 2*BORDER_SPACING + LEFT_SPACING;
				height -= 2*BORDER_SPACING;
				
				if (titleHeight==null)
					titleHeight = getTitleHeight(g2);
				
				if (thirdColumWidth==null)
					thirdColumWidth = getMax3rdColumnWidth(g2);
				
				
				int stdRowHeight = (height - titleHeight) / 5;
				int remainingWidth = Math.max(0, width - thirdColumWidth);
				int columWidth1 = (remainingWidth*6)/10;
				int columWidth2 = remainingWidth - columWidth1;
				int columWidth3 = Math.min(width, thirdColumWidth);
				
				int x = BORDER_SPACING + LEFT_SPACING;
				int y = BORDER_SPACING;
				fldTitle     .paint(g2, x, y, width, titleHeight ); y += titleHeight;
				fldAuthors   .paint(g2, x, y, width, stdRowHeight); y += stdRowHeight;
				fldBookSeries.paint(g2, x, y, width, stdRowHeight); y += stdRowHeight;
				
				x = BORDER_SPACING + LEFT_SPACING;
				fldPublisher .paint(g2, x, y, columWidth1, stdRowHeight); x += columWidth1;
				fldPrice     .paint(g2, x, y, columWidth2, stdRowHeight); x += columWidth2;
				fldNotRead   .paint(g2, x, y, columWidth3, stdRowHeight);
				y += stdRowHeight;
				
				x = BORDER_SPACING + LEFT_SPACING;
				fldISBN      .paint(g2, x, y, columWidth1, stdRowHeight); x += columWidth1;
				fldCatalogID .paint(g2, x, y, columWidth2, stdRowHeight); x += columWidth2;
				fldNotOwned  .paint(g2, x, y, columWidth3, stdRowHeight);
				y += stdRowHeight;
				
				x = BORDER_SPACING + LEFT_SPACING;
				fldRelease   .paint(g2, x, y, columWidth1, stdRowHeight); x += columWidth1;
				fldPageCount .paint(g2, x, y, columWidth2, stdRowHeight); x += columWidth2;
				fldCover     .paint(g2, x, y, columWidth3, stdRowHeight);
				y += stdRowHeight;
			}
		}

		static boolean showsField(Book.Field field)
		{
			return
					field==Book.Field.Title      ||
					field==Book.Field.Authors    ||
					field==Book.Field.BookSeries ||
					field==Book.Field.Publisher  ||
					field==Book.Field.CatalogID  ||
					field==Book.Field.Release    ||
					field==Book.Field.ISBN       ||
					field==Book.Field.Price      ||
					field==Book.Field.PageCount  ||
					field==Book.Field.FrontCover ||
					field==Book.Field.SpineCover ||
					field==Book.Field.BackCover  ||
					field==Book.Field.Read       ||
					field==Book.Field.Owned;
		}

		public void setData(Book book)
		{
			fldTitle     .setText(Tools.getIfNotNull(book, "<null>", b -> Tools.getIfNotNull(b.title, "<unnamed book>")));
			fldBookSeries.setText(book==null || book.bookSeries==null ? "----" : "%s book of series \"%s\"".formatted(
					Tools.toOrdinalString( book.bookSeries.books.indexOf(book)+1 ),
					book.bookSeries.name != null && !book.bookSeries.name.isBlank()
						? book.bookSeries.name
						: "<unnamed series>"
			));
			fldAuthors    .setText("Author%s: %s"  .formatted(
					book==null || book.authors.size()<2 ? "" : "s",
					book==null || book.authors.isEmpty() ? "---" : book.concatenateAuthors()
			));
			fldPublisher  .setText("Publisher: %s" .formatted(book==null || book.publisher==null || book.publisher.name().isBlank() ? "---" : book.publisher.name()    ));
			fldCatalogID  .setText("Catalog ID: %s".formatted(book==null || book.catalogID==null || book.catalogID.isBlank()        ? "---" : book.catalogID           ));
			fldRelease    .setText("Release: %s"   .formatted(book==null || book.release  ==null || book.release  .isBlank()        ? "---" : book.release             ));
			fldISBN       .setText("ISBN: %s"      .formatted(book==null || book.isbn     ==null || book.isbn     .isBlank()        ? "---" : book.isbn                ));
			fldPrice      .setText("Price: %s"     .formatted(book==null || book.price==0                                           ? "---" : "%1.2f €".formatted(book.price)));
			fldPageCount  .setText("Pages: %s"     .formatted(book==null || book.pagecount==0                                       ? "---" : book.pagecount           ));
			fldCover      .setText("Cover: %s"     .formatted(
					book==null
						? "---"
						: "%s%s%s".formatted(
								book.backCover ==null ? "- " : "B",
								book.spineCover==null ? "- " : "S",
								book.frontCover==null ? "- " : "F"
						)
			));
			fldNotRead .setText(book==null || book.read  ? "" : "Not Read" );
			fldNotOwned.setText(book==null || book.owned ? "" : "Not Owned");
		}
		
		private int getTitleHeight(Graphics2D g2)
		{
			return getPreferredSize(g2, fldTitle, "Test").height;
		}

		private int getMax3rdColumnWidth(Graphics2D g2)
		{
			Integer maxWidth = null;
			maxWidth = getMaxWidth(g2, maxWidth, fldCover, "Cover: %s".formatted("---"));
			maxWidth = getMaxWidth(g2, maxWidth, fldCover, "Cover: %s".formatted("- - - "));
			maxWidth = getMaxWidth(g2, maxWidth, fldCover, "Cover: %s".formatted("BSF"));
			maxWidth = getMaxWidth(g2, maxWidth, fldNotRead, "Not Read" );
			maxWidth = getMaxWidth(g2, maxWidth, fldNotOwned, "Not Owned");
			return maxWidth;
		}

		private int getMaxWidth(Graphics2D g2, Integer maxWidth, TextBox field, String str)
		{
			int width = getPreferredSize(g2, field, str).width;
			return Math.max(width, maxWidth==null ? -1 : maxWidth) ;
		}

		private Dimension getPreferredSize(Graphics2D g2, TextBox field, String str)
		{
			String prevStr = field.getText();
			field.setText(str);
			Dimension preferredSize = field.getPreferredSize(g2);
			field.setText(prevStr);
			return preferredSize;
		}
	}

	private static class TextBox
	{
		private String text;
		private Font font;

		TextBox()
		{
			text = "";
			font = new JLabel().getFont();
		}
		
		void paint(Graphics2D g2, int x, int y, int width, int height)
		{
			AdjustedText adjustedText = AdjustedText.compute(g2, font, text, width);
			int textX = (int) Math.round( adjustedText.bounds.getX() );
			int textY = (int) Math.round( adjustedText.bounds.getY() );
			
			g2.setFont(font);
			g2.drawString(adjustedText.text, x-textX, y-textY);
		}
		
		record AdjustedText(String text, Rectangle2D bounds)
		{
			static AdjustedText compute(Graphics2D g2, Font font, String text, int maxWidth)
			{
				Rectangle2D bounds = getStringBounds(g2, font, text);
				if (bounds.getWidth() <= maxWidth)
					return new AdjustedText(text, bounds);
				
				int reducedLength = (int) Math.floor( text.length() * maxWidth / bounds.getWidth() );
				String reducedText = text.substring(0, reducedLength)+"...";
				bounds = getStringBounds(g2, font, reducedText);
				
				while (bounds.getWidth() <= maxWidth)
					bounds = getStringBounds(g2, font, reducedText = text.substring(0, ++reducedLength)+"...");
				
				while (bounds.getWidth() > maxWidth && reducedLength > 0)
					bounds = getStringBounds(g2, font, reducedText = text.substring(0, --reducedLength)+"...");
				
				return new AdjustedText(reducedText, bounds);
			}

			private static Rectangle2D getStringBounds(Graphics2D g2, Font font, String str)
			{
				return font.getStringBounds(str, g2.getFontRenderContext());
			}
		}
		
		Dimension getPreferredSize(Graphics2D g2)
		{
			Rectangle2D bounds = font.getStringBounds(text, g2.getFontRenderContext());
			int width  = (int) Math.ceil( bounds.getWidth() );
			int height = (int) Math.ceil( bounds.getHeight() );
			return new Dimension(width, height);
		}

		Font   getFont() { return font; }
		String getText() { return text; }
		void setFont(Font   font) { this.font = font; }
		void setText(String text) { this.text = text; }
	}
}
