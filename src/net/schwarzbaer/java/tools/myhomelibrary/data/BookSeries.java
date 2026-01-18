package net.schwarzbaer.java.tools.myhomelibrary.data;

import java.util.Objects;
import java.util.Vector;

public class BookSeries implements UniqueID.IdBased<BookSeries>
{
	public final String id;
	public       String name = null;
	public final Vector<Book> books;
	
	BookSeries(String id)
	{
		this.id = Objects.requireNonNull( id );
		books = new Vector<>();
	}

	@Override
	public String getID()
	{
		return id;
	}

	@Override
	public String toString()
	{
		return name==null || name.isBlank() ? "<nameless book series>" : name;
	}
}
