package net.schwarzbaer.java.tools.myhomelibrary.data;

public class BookSeries implements UniqueID.IdBased<BookSeries>
{
	public final String id;
	public String name = null;
	
	BookSeries(String id)
	{
		this.id = id;
	}

	@Override
	public String getID()
	{
		return id;
	}
}
