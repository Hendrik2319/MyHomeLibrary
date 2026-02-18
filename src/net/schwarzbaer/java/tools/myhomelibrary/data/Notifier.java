package net.schwarzbaer.java.tools.myhomelibrary.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class Notifier
{
	private static final boolean DEBUG___SHOW_MESSAGES = false; // T-O-D-O: disable debug messages in Notifier
	
	public final BookChangeController       books      = new BookChangeController();
	public final BookSeriesChangeController bookSeries = new BookSeriesChangeController();
	public final AuthorChangeController     authors    = new AuthorChangeController();
	public final PublisherChangeController  publishers = new PublisherChangeController();
	public final StorageController          storages   = new StorageController();
	
	public static class Controller<Listener>
	{
		private final List<Listener> listeners = new ArrayList<>();
		private final String selfLabel;
		
		protected Controller()
		{
			this.selfLabel = getClass().getSimpleName();
		}
		
		public void    addListener(Listener listener) { listeners.   add(listener); }
		public void removeListener(Listener listener) { listeners.remove(listener); }
		
		protected void forEach(String callLabel, Object source, Consumer<Listener> action)
		{
			if (DEBUG___SHOW_MESSAGES)
				System.err.printf("%-35s.%-20s[%-25s]%n", selfLabel, callLabel, source==null ? "<null>" : source.getClass().getSimpleName());
			listeners.forEach(action);
		}
	}
	
	public static class BookChangeController extends Controller<BookChangeListener> implements BookChangeListener
	{
		@Override public void fieldChanged  (Object source,     Book  book ,     Book.Field  field ) { forEach("fieldChanged"  , source, l -> l.fieldChanged  (source, book , field )); }
		@Override public void fieldsChanged (Object source, Set<Book> books, Set<Book.Field> fields) { forEach("fieldsChanged" , source, l -> l.fieldsChanged (source, books, fields)); }
		@Override public void added         (Object source,     Book  book                         ) { forEach("added"         , source, l -> l.added         (source, book         )); }
		@Override public void deleted       (Object source,     Book  book                         ) { forEach("deleted"       , source, l -> l.deleted       (source, book         )); }
	}
	
	public static class BookSeriesChangeController extends Controller<BookSeriesChangeListener> implements BookSeriesChangeListener
	{
		@Override public void fieldChanged (Object source,      BookSeries  bookSeries,     BookSeries.Field  field ) { forEach("fieldChanged" , source, l -> l.fieldChanged (source, bookSeries, field )); }
		@Override public void fieldsChanged(Object source,  Set<BookSeries> bookSeries, Set<BookSeries.Field> fields) { forEach("fieldsChanged", source, l -> l.fieldsChanged(source, bookSeries, fields)); }
		@Override public void added        (Object source,      BookSeries  bookSeries                              ) { forEach("added"        , source, l -> l.added        (source, bookSeries        )); }
		@Override public void deleted      (Object source, List<BookSeries> bookSeries                              ) { forEach("deleted"      , source, l -> l.deleted      (source, bookSeries        )); }
	}
	
	public static class AuthorChangeController extends Controller<AuthorChangeListener> implements AuthorChangeListener
	{
		@Override public void added  (Object source,      Author  author ) { forEach("added"  , source, l -> l.added  (source, author )); }
		@Override public void deleted(Object source, List<Author> authors) { forEach("deleted", source, l -> l.deleted(source, authors)); }
	}
	
	public static class PublisherChangeController extends Controller<PublisherChangeListener> implements PublisherChangeListener
	{
		@Override public void added  (Object source,      Publisher  publisher ) { forEach("added"  , source, l -> l.added  (source, publisher )); }
		@Override public void deleted(Object source, List<Publisher> publishers) { forEach("deleted", source, l -> l.deleted(source, publishers)); }
	}
	
	public static class StorageController extends Controller<StorageListener> implements StorageListener
	{
		@Override public void bookStorageLoaded(Object source) { forEach("bookStorageLoaded", source, l -> l.bookStorageLoaded(source)); }
	}
	
	public interface BookChangeListener
	{
		void fieldChanged (Object source,     Book  book ,     Book.Field  field );
		void fieldsChanged(Object source, Set<Book> books, Set<Book.Field> fields);
		void added        (Object source,     Book  book );
		void deleted      (Object source,     Book  book );
		
		public static class Adapter implements BookChangeListener
		{
			@Override public void fieldChanged (Object source,     Book  book ,     Book.Field  field ) {}
			@Override public void fieldsChanged(Object source, Set<Book> books, Set<Book.Field> fields) {}
			@Override public void added        (Object source,     Book  book ) {}
			@Override public void deleted      (Object source,     Book  book ) {}
		}
	}
	
	public interface BookSeriesChangeListener
	{
		void fieldChanged (Object source,      BookSeries  bookSeries,     BookSeries.Field  field );
		void fieldsChanged(Object source,  Set<BookSeries> bookSeries, Set<BookSeries.Field> fields);
		void added        (Object source,      BookSeries  bookSeries);
		void deleted      (Object source, List<BookSeries> bookSeries);
		
		public static class Adapter implements BookSeriesChangeListener
		{
			@Override public void fieldChanged (Object source,      BookSeries  bookSeries,     BookSeries.Field  field ) {}
			@Override public void fieldsChanged(Object source,  Set<BookSeries> bookSeries, Set<BookSeries.Field> fields) {}
			@Override public void added        (Object source,      BookSeries  bookSeries) {}
			@Override public void deleted      (Object source, List<BookSeries> bookSeries) {}
		}
	}
	
	public interface AuthorChangeListener
	{
		void added  (Object source,      Author  author );
		void deleted(Object source, List<Author> authors);
		
		public static class Adapter implements AuthorChangeListener
		{
			@Override public void added  (Object source,      Author  author ) {}
			@Override public void deleted(Object source, List<Author> authors) {}
		}
	}
	
	public interface PublisherChangeListener
	{
		void added  (Object source,      Publisher  publisher );
		void deleted(Object source, List<Publisher> publishers);
		
		public static class Adapter implements PublisherChangeListener
		{
			@Override public void added  (Object source,      Publisher  publisher ) {}
			@Override public void deleted(Object source, List<Publisher> publishers) {}
		}
	}
	
	public interface StorageListener
	{
		void bookStorageLoaded(Object source);
		
		public static class Adapter implements StorageListener
		{
			@Override public void bookStorageLoaded(Object source) {}
		}
	}
}
