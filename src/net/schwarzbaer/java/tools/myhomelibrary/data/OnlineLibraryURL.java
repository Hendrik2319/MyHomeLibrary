package net.schwarzbaer.java.tools.myhomelibrary.data;

import java.util.Objects;
import java.util.function.Function;

import net.schwarzbaer.java.tools.myhomelibrary.Tools;

public enum OnlineLibraryURL
{
	DeutscheNationalbibliothek("DNB", "Deutsche Nationalbibliothek",
			b -> Tools.getIfNotNull(
					getISBNNumbers(b),
					null,
					isbn -> "https://portal.dnb.de/opac/simpleSearch?query=num+all+%22"+isbn+"%22&cqlMode=true"
			)
	),
	BritishLibrary("British Library", "British Library",
			b -> Tools.getIfNotNull(
					getISBNNumbers(b),
					null,
					isbn -> "https://catalogue.bl.uk/nde/search?query="+isbn+"&tab=Everything&search_scope=MyInst_and_CI&searchInFulltext=false&vid=44BL_MAIN:BLL01_NDE"
			)
	),
	Amazon("Amazon", "Amazon (de)",
			b -> Tools.getIfNotNull(
					getISBNNumbers(b),
					null,
					isbn -> "https://www.amazon.de/s?search-alias=stripbooks&field-keywords="+isbn
			)
	),
	Thalia("Thalia", "Thalia (de)",
			b -> Tools.getIfNotNull(
					b.isbn,
					null,
					isbn -> "https://www.thalia.de/suche?sq="+isbn+"&keepTerm=true"
			)
	),
	Wikipedia("Wikipedia", "Wikipedia (de) - ISBN-Suche",
			b -> Tools.getIfNotNull(
					getISBNNumbers(b),
					null,
					isbn -> "https://de.wikipedia.org/wiki/Spezial:ISBN-Suche/"+isbn
			)
	),
	;
	public final String shortTitle;
	public final String title;
	public final Function<Book, String> buildURL;

	OnlineLibraryURL(String shortTitle, String title, Function<Book,String> buildURL)
	{
		this.shortTitle = Objects.requireNonNull(shortTitle);
		this.title      = Objects.requireNonNull(title     );
		this.buildURL   = Objects.requireNonNull(buildURL  );
	}

	@Override
	public String toString()
	{
		return title;
	}

	private static String getISBNNumbers(Book b)
	{
		if (b==null || b.isbn==null) return null;
		return Tools.filterStr(b.isbn, Character::isDigit);
	}
}
