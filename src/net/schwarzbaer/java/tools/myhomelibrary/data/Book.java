package net.schwarzbaer.java.tools.myhomelibrary.data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.schwarzbaer.java.lib.gui.ImageView;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO;

public class Book implements UniqueID.IdBased<Book>
{
	public static final int FRONTCOVERTHUMB_MAXWIDTH  = 60;
	public static final int FRONTCOVERTHUMB_MAXHEIGHT = 90;
	
	public enum Field {
		Title, Authors, Publisher, CatalogID, BookSeries, Release, FrontCover, BackCover, SpineCover, FrontCoverThumb
	}
	
	public final String id;
	public String title = null;
	public final List<Author> authors;
	public Publisher  publisher = null;
	public String     catalogID = null;
	public BookSeries bookSeries = null;
	public String     release    = null;
	public String     frontCover = null;
	public String      backCover = null;
	public String     spineCover = null;
	public BufferedImage frontCoverThumb = null;

	Book(String id)
	{
		this.id = Objects.requireNonNull( id );
		authors = new ArrayList<>();
	}

	public String getTitle()
	{
		return title!=null && !title.isBlank()
				? "\"%s\"".formatted(title)
				: "[ID:%s]".formatted(id);
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
	
	public void updateFrontCoverThumb()
	{
		updateFrontCoverThumb( FileIO.loadImagefile( frontCover ) );		
	}
	
	public void updateFrontCoverThumb(BufferedImage image)
	{
		frontCoverThumb = scaleFrontCoverThumb(image);
	}

	private static BufferedImage scaleFrontCoverThumb(BufferedImage image)
	{
		if (image==null)
			return null;
		
		int imageWidth  = image.getWidth ();
		int imageHeight = image.getHeight();
		double f = Math.max(
				imageWidth /(double)Book.FRONTCOVERTHUMB_MAXWIDTH ,
				imageHeight/(double)Book.FRONTCOVERTHUMB_MAXHEIGHT
		);
		if (f < 1) // imageWidth && imageHeight are smaller than FRONTCOVERTHUMB values
			return image;
		
		int newWidth  = Math.min( (int) Math.round( imageWidth  / f ), Book.FRONTCOVERTHUMB_MAXWIDTH  );
		int newHeight = Math.min( (int) Math.round( imageHeight / f ), Book.FRONTCOVERTHUMB_MAXHEIGHT );
		return ImageView.computeScaledImageByAreaSampling(image, newWidth, newHeight, true);
	}
}
