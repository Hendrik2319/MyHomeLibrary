package net.schwarzbaer.java.tools.myhomelibrary.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.schwarzbaer.java.tools.myhomelibrary.FileIO;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO.FileIOException;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.UniqueID.UniqueIDException;

public class BookStorage
{
	private static final String HEADER_BOOK = "[Book]";
	private static final String HEADER_BOOK_SERIES = "[BookSeries]";
	private static final UniqueID bookIDs = new UniqueID(7);
	private static final UniqueID bookSeriesIDs = new UniqueID(4);
	
	private final MyHomeLibrary main;
	private final Map<String,Book> books;
	private final Map<String,BookSeries> bookSeries;
	private final Map<String,Author> authors;
	private final Map<String,Publisher> publishers;
	
	public BookStorage(MyHomeLibrary main)
	{
		this.main = main;
		books      = new HashMap<>();
		bookSeries = new HashMap<>();
		authors    = new HashMap<>();
		publishers = new HashMap<>();
	}
	
	public List<Book> getListOfBooks()
	{
		return books
				.values()
				.stream()
				.sorted(
						Comparator.<Book,String>comparing(
								b -> Tools.getIfNotNull(b.title, "<unnamed>"),
								Comparator.<String,String>comparing(String::toLowerCase).thenComparing(Comparator.naturalOrder())
						)
				)
				.toList();
	}

	public List<BookSeries> getListOfBookSeries()
	{
		return bookSeries
				.values()
				.stream()
				.sorted(
						Comparator.<BookSeries,String>comparing(
								bs -> Tools.getIfNotNull(bs.name, "<unnamed>"),
								Comparator.<String,String>comparing(String::toLowerCase).thenComparing(Comparator.naturalOrder())
						)
				)
				.toList();
	}

	public List<Publisher> getListOfKnownPublishers()
	{
		return publishers
				.values()
				.stream()
				.sorted(
						Comparator.<Publisher,String>comparing(
								p -> Tools.getIfNotNull(p.name(), "<unnamed>"),
								Comparator.<String,String>comparing(String::toLowerCase).thenComparing(Comparator.naturalOrder())
						)
				)
				.toList();
	}

	public void assign(Book book, BookSeries bookSeries)
	{
		if (book.bookSeries!=null)
			book.bookSeries.books.remove(book);
		
		book.bookSeries = bookSeries;
		
		if (!book.bookSeries.books.contains(book))
			book.bookSeries.books.add(book);
	}

	public Book createBook()
	{
		return createBook(bookIDs.createNew());
	}

	private Book createBook(String id)
	{
		Book book = new Book(id);
		books.put(id, book);
		return book;
	}
	
	public BookSeries createBookSeries()
	{
		return createBookSeries(bookSeriesIDs.createNew());
	}

	private BookSeries createBookSeries(String id)
	{
		BookSeries bookSeries = new BookSeries(id);
		this.bookSeries.put(id, bookSeries);
		return bookSeries;
	}
	
	public Author getOrCreateAuthor(String name)
	{
		return authors.computeIfAbsent(name, Author::new);
	}

	public Publisher getOrCreatePublisher(String name)
	{
		return publishers.computeIfAbsent(name, Publisher::new);
	}

	private enum Field
	{
		ID, name, book, title, bookSeries, author, releaseYear, publisher, catalogID, frontCover, spineCover, backCover
	}
	
	private static String getLineValue(String line, Field field)
	{
		String prefix = field.toString() + " = ";
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}

	private int parseInt(String str) throws Exception
	{
		try
		{
			return Integer.parseInt(str);
		}
		catch (NumberFormatException ex)
		{
			//ex.printStackTrace();
			throw new Exception("Can't parse \"%s\" to integer: ".formatted(str), ex);
		}
	}

	public void readFromFile()
	{
		File file;
		try { file = FileIO.DataFile.BookStorage.getFile(); }
		catch (FileIOException ex)
		{
			// ex.printStackTrace();
			ex.showMessageDialog(
					main.mainWindow,
					"Can't read BookStorage",
					"Sorry, can't read BookStorage from file."
			);
			return;
		}
		
		books.clear();
		bookSeries.clear();
		authors.clear();
		publishers.clear();
		
		if (file.isFile())
			System.out.printf("Read BookStorage from file \"%s\" ...%n", file.getAbsolutePath());
		
		int ln = 0;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
		{
			Book currentBook = null;
			BookSeries currentBookSeries = null;
			List<String> currentBookList = null;
			Map<String,List<String>> allBookLists = new HashMap<>();
			boolean expectingBook = false;
			boolean expectingBookSeries = false;
			String valueStr;
			
			List<String> lines = in.lines().toList();
			
			for (String line : lines)
			{
				ln++;
				
				if (line.isBlank())
					continue;
				
				if (line.equals(HEADER_BOOK_SERIES))
				{
					currentBook = null;
					currentBookSeries = null;
					currentBookList = null;
					expectingBookSeries = true;
				}
				
				if (line.equals(HEADER_BOOK))
				{
					currentBook = null;
					currentBookSeries = null;
					currentBookList = null;
					expectingBook = true;
				}
				
				if ((valueStr = getLineValue(line, Field.ID))!=null)
				{
					if (expectingBookSeries)
					{
						bookSeriesIDs.addKnownID(valueStr);
						currentBook = null;
						currentBookSeries = createBookSeries(valueStr);
						allBookLists.put(valueStr, currentBookList = new ArrayList<>());
						expectingBookSeries = false;
					}
					else if (expectingBook)
					{
						bookIDs.addKnownID(valueStr);
						currentBook = createBook(valueStr);
						currentBookSeries = null;
						currentBookList = null;
						expectingBook = false;
					}
				}
				
				if (currentBookSeries!=null && currentBookList!=null)
				{
					if ((valueStr = getLineValue(line, Field.name))!=null) currentBookSeries.name = valueStr;
					if ((valueStr = getLineValue(line, Field.book))!=null) currentBookList.add(valueStr);
				}
				
				if (currentBook!=null)
				{
					if ((valueStr = getLineValue(line, Field.title      ))!=null) currentBook.title       = valueStr;
					if ((valueStr = getLineValue(line, Field.bookSeries ))!=null) currentBook.bookSeries  = bookSeries.get(valueStr);
					if ((valueStr = getLineValue(line, Field.author     ))!=null) currentBook.authors     .add(getOrCreateAuthor(valueStr));
					if ((valueStr = getLineValue(line, Field.releaseYear))!=null) currentBook.releaseYear = parseInt(valueStr);
					if ((valueStr = getLineValue(line, Field.publisher  ))!=null) currentBook.publisher   = getOrCreatePublisher(valueStr);
					if ((valueStr = getLineValue(line, Field.catalogID  ))!=null) currentBook.catalogID   = valueStr;
					if ((valueStr = getLineValue(line, Field.frontCover ))!=null) currentBook.frontCover  = valueStr;
					if ((valueStr = getLineValue(line, Field.spineCover ))!=null) currentBook.spineCover  = valueStr;
					if ((valueStr = getLineValue(line, Field.backCover  ))!=null) currentBook.backCover   = valueStr;
				}
			}
			
			allBookLists.forEach((seriesID, bookIDList) -> {
				BookSeries bookSeries = this.bookSeries.get(seriesID);
				if (bookSeries==null)
					throw new IllegalStateException();
				if (!seriesID.equals(bookSeries.id))
					throw new IllegalStateException();
				for (String bookID : bookIDList)
				{
					Book book = books.get(bookID);
					if (book==null)
						System.err.printf("BookSeries[ID:%s] references to unknown Book[ID:%s].%n", seriesID, bookID);
					else
					{
						if (!bookID.equals(book.id))
							throw new IllegalStateException();
						bookSeries.books.add(book);
						if (book.bookSeries == null)
							System.err.printf("BookSeries[ID:%s] references to Book[ID:%s]. But this Book have no reference to any BookSeries.%n", seriesID, bookID);
						else if (book.bookSeries != bookSeries)
							System.err.printf("BookSeries[ID:%s] references to Book[ID:%s]. But this Book references to another BookSeries[ID:%s].%n", seriesID, bookID, book.bookSeries.id);
					}
				}
			});
			
			books.forEach((bookID, book) -> {
				if (book.bookSeries==null) return;
				if (!book.bookSeries.books.contains(book))
					System.err.printf("Book[ID:%s] references to BookSeries[ID:%s]. But this BookSeries have no reference to this Book.%n", book.id, book.bookSeries.id);
			});
			
			System.out.printf("... done%n");
		}
		catch (FileNotFoundException ex)
		{}
		catch (IOException | UncheckedIOException ex)
		{
			//ex.printStackTrace();
			System.err.printf("%s while reading BookStorage in line %d: %s%n", ex.getClass().getCanonicalName(), ln, ex.getMessage());
		}
		catch (UniqueIDException ex)
		{
			//ex.printStackTrace();
			System.err.printf("UniqueIDException while reading BookStorage in line %d: %s%n", ln, ex.getMessage());
		}
		catch (Exception ex)
		{
			//ex.printStackTrace();
			System.err.printf("%s while reading BookStorage in line %d: %s%n", ex.getClass().getCanonicalName(), ln, ex.getMessage());
			for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause())
				System.err.printf("   caused by %s: %s%n", cause.getClass().getCanonicalName(), cause.getMessage());
		}
	}

	public void writeToFile()
	{
		if (books.isEmpty() && bookSeries.isEmpty())
			return;
		
		File file;
		try { file = FileIO.DataFile.BookStorage.getFile(); }
		catch (FileIOException ex)
		{
			// ex.printStackTrace();
			ex.showMessageDialog(
					main.mainWindow,
					"Can't write BookStorage",
					"Sorry, can't write BookStorage to file."
			);
			return;
		}
		
		System.out.printf("Write BookStorage to file \"%s\" ...%n", file.getAbsolutePath());
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8))
		{
			bookSeries.values().stream().sorted().forEach(bs -> {
				System.out.println(HEADER_BOOK_SERIES);
				System.out.printf("%s = %s%n", Field.ID, bs.id);
				if (bs.name!=null)    System.out.printf("%s = %s%n", Field.name, bs.name);
				bs.books.forEach(b -> System.out.printf("%s = %s%n", Field.book, b.id   ));
				System.out.println();
			});
			
			books.values().stream().sorted().forEach(b -> {
				System.out.println(HEADER_BOOK);
				System.out.printf("%s = %s%n", Field.ID, b.id);
				if (b.title      !=null) System.out.printf("%s = %s%n", Field.title      , b.title        );
				if (b.bookSeries !=null) System.out.printf("%s = %s%n", Field.bookSeries , b.bookSeries.id);
				b.authors.forEach(a ->   System.out.printf("%s = %s%n", Field.author     , a.name()       ));
				if (b.releaseYear>0    ) System.out.printf("%s = %d%n", Field.releaseYear, b.releaseYear  );
				if (b.publisher  !=null) System.out.printf("%s = %s%n", Field.publisher  , b.publisher    );
				if (b.catalogID  !=null) System.out.printf("%s = %s%n", Field.catalogID  , b.catalogID    );
				if (b.frontCover !=null) System.out.printf("%s = %s%n", Field.frontCover , b.frontCover   );
				if (b.spineCover !=null) System.out.printf("%s = %s%n", Field.spineCover , b.spineCover   );
				if (b.backCover  !=null) System.out.printf("%s = %s%n", Field.backCover  , b.backCover    );
				System.out.println();
			});
			
			System.out.printf("... done%n");
		}
		catch (IOException ex)
		{
			//ex.printStackTrace();
			System.err.printf("IOException while writing BookStorage: %s%n", ex.getMessage());
		}
	}
}
