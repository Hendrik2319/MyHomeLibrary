package net.schwarzbaer.java.tools.myhomelibrary.data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Book implements UniqueID.IdBased<Book>
{
	public enum Field {
		Title, Authors, Publisher, CatalogID, BookSeries, ReleaseYear, FrontCover, BackCover, SpineCover
	}
	
	public final String id;
	public String title = null;
	public final List<Author> authors;
	public Publisher  publisher = null;
	public String     catalogID = null;
	public BookSeries bookSeries = null;
	public int        releaseYear = -1;
	public String     frontCover = null;
	public String      backCover = null;
	public String     spineCover = null;
	public BufferedImage frontCoverThumb = null;

	Book(String id)
	{
		this.id = Objects.requireNonNull( id );
		authors = new ArrayList<>();
	}

	@Override
	public String getID()
	{
		return id;
	}

	@Override
	public String toString()
	{
		return title==null || title.isBlank() ? "<nameless book>" : title;
	}

	public String concatenateAuthors()
	{
		return authors.stream().map(Author::name).collect(Collectors.joining(", "));
	}
}
