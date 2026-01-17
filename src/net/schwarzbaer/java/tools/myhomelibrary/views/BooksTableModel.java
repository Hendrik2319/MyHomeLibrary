package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.util.function.BiFunction;
import java.util.function.Function;

import javax.swing.SwingConstants;

import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.tools.myhomelibrary.data.Book;

public class BooksTableModel extends Tables.SimpleGetValueTableModel2<BooksTableModel, Book, BooksTableModel.ColumnID>
{
	enum ColumnID implements Tables.SimplifiedColumnIDInterface, Tables.SimpleGetValueTableModel2.ColumnIDTypeInt2b<BooksTableModel, Book>, SwingConstants
	{
		;
		final Tables.SimplifiedColumnConfig2<BooksTableModel, Book, ?> cfg;
		ColumnID(Tables.SimplifiedColumnConfig2<BooksTableModel, Book, ?> cfg) { this.cfg = cfg; }
		@Override public Tables.SimplifiedColumnConfig2<BooksTableModel, Book, ?> getColumnConfig() { return this.cfg; }
		@Override public Function<Book, ?> getGetValue() { return cfg.getValue; }
		@Override public BiFunction<BooksTableModel, Book, ?> getGetValueM() { return cfg.getValueM; }
		
		private static <T> Tables.SimplifiedColumnConfig2<BooksTableModel, Book, T> config(String name, Class<T> columnClass, int prefWidth, Integer horizontalAlignment)
		{
			return new Tables.SimplifiedColumnConfig2<>(name, columnClass, 20, -1, prefWidth, prefWidth, horizontalAlignment);
		}
	}
	
	BooksTableModel()
	{
		super(ColumnID.values());
	}

	@Override protected BooksTableModel getThis() { return this; }
}
