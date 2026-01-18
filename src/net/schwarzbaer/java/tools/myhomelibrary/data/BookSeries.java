package net.schwarzbaer.java.tools.myhomelibrary.data;

import java.util.Vector;

public class BookSeries implements UniqueID.IdBased<BookSeries>
{
	public final String id;
	public       String name = null;
	public final Vector<Book> books;
	
	BookSeries(String id)
	{
		this.id = id;
		books = new Vector<>();
	}

	@Override
	public String getID()
	{
		return id;
	}
}
