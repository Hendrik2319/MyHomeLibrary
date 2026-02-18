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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import net.schwarzbaer.java.lib.gui.ProgressDialog;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO;
import net.schwarzbaer.java.tools.myhomelibrary.FileIO.FileIOException;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.UniqueID.UniqueIDException;

public class BookStorage
{
	public  static final int LENGTH_BOOK_ID       = 7;
	public  static final int LENGTH_BOOKSERIES_ID = 4;
	private static final String HEADER_BOOK = "[Book]";
	private static final String HEADER_BOOK_SERIES = "[BookSeries]";
	private static final UniqueID bookIDs       = new UniqueID(LENGTH_BOOK_ID      );
	private static final UniqueID bookSeriesIDs = new UniqueID(LENGTH_BOOKSERIES_ID);
	
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

	public void checkUsage(List<ImageData> list, Runnable updateWhenChanged)
	{
		for (ImageData imgData : list)
		{
			Book oldValue = imgData.book;
			imgData.book = null;
			if (imgData.nameParts!=null)
			{
				String filename = imgData.file.getName();
				Book book = books.get(imgData.nameParts.bookID());
				if (book!=null)
					switch (imgData.nameParts.coverPart())
					{
					case back : if (filename.equals( book.backCover  )) imgData.book = book; break;
					case front: if (filename.equals( book.frontCover )) imgData.book = book; break;
					case spine: if (filename.equals( book.spineCover )) imgData.book = book; break;
					}
			}
			if (imgData.book!=oldValue && updateWhenChanged!=null)
				updateWhenChanged.run();
		}
	}
	
	public boolean hasBook(String bookID)
	{
		return books.containsKey(bookID);
	}

	public List<Book> getListOfBooks()
	{
		return getListOfBooks(null);
	}

	public List<Book> getListOfBooks(Predicate<Book> filter)
	{
		return getList(books, b -> Tools.getIfNotNull(b.title, "<unnamed>"), filter);
	}

	public List<BookSeries> getListOfBookSeries()
	{
		return getList(bookSeries, bs -> Tools.getIfNotNull(bs.name, "<unnamed>"));
	}

	public List<Author> getListOfKnownAuthors()
	{
		return getList(authors, a -> Tools.getIfNotNull(a.name(), "<unnamed>"));
	}

	public List<Publisher> getListOfKnownPublishers()
	{
		return getList(publishers, p -> Tools.getIfNotNull(p.name(), "<unnamed>"));
	}
	
	private <V> List<V> getList(Map<String,V> map, Function<V,String> getName)
	{
		return getList(map, getName, null);
	}
	
	private <V> List<V> getList(Map<String,V> map, Function<V,String> getName, Predicate<V> filter)
	{
		return map
				.values()
				.stream()
				.filter(filter==null ? v->true : filter)
				.sorted( Tools.createComparatorByName(getName) )
				.toList();
	}

	public void assign(Book book, BookSeries bookSeries)
	{
		if (book.bookSeries!=null)
			book.bookSeries.books.remove(book);
		
		book.bookSeries = bookSeries;
		
		if (book.bookSeries!=null && !book.bookSeries.books.contains(book))
			book.bookSeries.books.add(book);
	}

	public void removeBook(Book book)
	{
		books.remove(book.id);
		
		if (book.bookSeries!=null)
			book.bookSeries.books.remove(book);
	}

	public List<BookSeries> deleteEmptyBookSeries()
	{
		List<BookSeries> deleted = new ArrayList<>();
		
		List<String> bookSeriesIDs = new ArrayList<>( bookSeries.keySet() );
		for (String bookSeriesID : bookSeriesIDs)
		{
			BookSeries bs = bookSeries.get(bookSeriesID);
			if (bs.books.isEmpty())
			{
				bookSeries.remove(bookSeriesID);
				deleted.add(bs);
			}
		}
		
		return deleted;
	}

	public List<Author> deleteUnusedAuthors()
	{
		Set<Author> inUse = new HashSet<>();
		
		for (Book b : books.values())
			for (Author a : b.authors)
				inUse.add(a);
		
		Set<String> idsToDelete = new HashSet<>();
		authors.forEach((id,a)->{
			if (!inUse.contains(a))
				idsToDelete.add(id);
		});
		
		ArrayList<Author> deleted = new ArrayList<>();
		idsToDelete.forEach(id -> {
			Author a = authors.remove(id);
			if (a!=null)
				deleted.add(a);
		});
		
		return deleted;
	}

	public List<Publisher> deleteUnusedPublishers()
	{
		Set<Publisher> inUse = new HashSet<>();
		
		for (Book b : books.values())
			if (b.publisher!=null)
				inUse.add(b.publisher);
		
		Set<String> idsToDelete = new HashSet<>();
		publishers.forEach((id,p)->{
			if (!inUse.contains(p))
				idsToDelete.add(id);
		});
		
		ArrayList<Publisher> deleted = new ArrayList<>();
		idsToDelete.forEach(id -> {
			Publisher p = publishers.remove(id);
			if (p!=null)
				deleted.add(p);
		});
		
		return deleted;
	}

	public Book createBook()
	{
		return createIdBased(books, id -> new Book(id,true), bookIDs);
	}
	
	public BookSeries createBookSeries()
	{
		return createIdBased(bookSeries, BookSeries::new, bookSeriesIDs);
	}
	
	private <V extends UniqueID.IdBased<V>> V createIdBased(Map<String,V> map, Function<String,V> constructor, UniqueID idSource)
	{
		return createIdBased(map, constructor, idSource.createNew());
	}
	
	private <V extends UniqueID.IdBased<V>> V createIdBased(Map<String,V> map, Function<String,V> constructor, String id)
	{
		V value = constructor.apply(id);
		map.put(id, value);
		return value;
	}
	
	public BookSeries getBookSeries(String str)
	{
		if (str!=null)
			for (BookSeries bs : bookSeries.values())
				if (str.equals(bs.name))
					return bs;
		return null;
	}

	public Author getOrCreateAuthor(String name)
	{
		return authors.computeIfAbsent(name, Author::new);
	}

	public Publisher getPublisher(String name)
	{
		return publishers.get(name);
	}

	public Publisher getOrCreatePublisher(String name)
	{
		return publishers.computeIfAbsent(name, Publisher::new);
	}

	private enum Field
	{
		ID, name, book, title, bookSeries, author, release, publisher, catalogID, frontCover, spineCover, backCover, not_read, not_owned
	}
	
	private static String getLineValue(String line, Field field)
	{
		String prefix = field.toString() + " = ";
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}

	@SuppressWarnings("unused")
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

	public void readFromFile(ProgressDialog pd)
	{
		Tools.setIndeterminateTaskTitle(pd, "Read BookStorage from file");
		
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
						currentBookSeries = createIdBased(bookSeries, BookSeries::new, valueStr);
						allBookLists.put(valueStr, currentBookList = new ArrayList<>());
						expectingBookSeries = false;
					}
					else if (expectingBook)
					{
						bookIDs.addKnownID(valueStr);
						currentBook = createIdBased(books, id -> new Book(id,false), valueStr);
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
					if ((valueStr = getLineValue(line, Field.release    ))!=null) currentBook.release     = valueStr;
					if ((valueStr = getLineValue(line, Field.publisher  ))!=null) currentBook.publisher   = getOrCreatePublisher(valueStr);
					if ((valueStr = getLineValue(line, Field.catalogID  ))!=null) currentBook.catalogID   = valueStr;
					if ((valueStr = getLineValue(line, Field.frontCover ))!=null) currentBook.frontCover  = valueStr;
					if ((valueStr = getLineValue(line, Field.spineCover ))!=null) currentBook.spineCover  = valueStr;
					if ((valueStr = getLineValue(line, Field.backCover  ))!=null) currentBook.backCover   = valueStr;
					if (line.equals( Field.not_read .name() )) currentBook.read  = false;
					if (line.equals( Field.not_owned.name() )) currentBook.owned = false;
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
		
		Tools.setTaskTitle(pd, "Read FrontCover Thumbs", 0, 0, books.size());
		int i=0;
		for (Book book : books.values())
		{
			book.updateFrontCoverThumb();
			Tools.setTaskValue(pd, ++i);
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
				out.println(HEADER_BOOK_SERIES);
				out.printf("%s = %s%n", Field.ID, bs.id);
				if (bs.name!=null)    out.printf("%s = %s%n", Field.name, bs.name);
				bs.books.forEach(b -> out.printf("%s = %s%n", Field.book, b.id   ));
				out.println();
			});
			
			books.values().stream().sorted().forEach(b -> {
				out.println(HEADER_BOOK);
				out.printf("%s = %s%n", Field.ID, b.id);
				if (b.title      !=null) out.printf("%s = %s%n", Field.title      , b.title        );
				if (b.bookSeries !=null) out.printf("%s = %s%n", Field.bookSeries , b.bookSeries.id);
				b.authors.forEach(a ->   out.printf("%s = %s%n", Field.author     , a.name()       ));
				if (b.release    !=null) out.printf("%s = %s%n", Field.release    , b.release      );
				if (b.publisher  !=null) out.printf("%s = %s%n", Field.publisher  , b.publisher    );
				if (b.catalogID  !=null) out.printf("%s = %s%n", Field.catalogID  , b.catalogID    );
				if (b.frontCover !=null) out.printf("%s = %s%n", Field.frontCover , b.frontCover   );
				if (b.spineCover !=null) out.printf("%s = %s%n", Field.spineCover , b.spineCover   );
				if (b.backCover  !=null) out.printf("%s = %s%n", Field.backCover  , b.backCover    );
				if (!b.read            ) out.printf("%s%n"     , Field.not_read                    );
				if (!b.owned           ) out.printf("%s%n"     , Field.not_owned                   );
				out.println();
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
