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
		@Override public void authorAdded   (Object source, Author    author                       ) { forEach("authorAdded"   , source, l -> l.authorAdded   (source, author       )); }
		@Override public void publisherAdded(Object source, Publisher publisher                    ) { forEach("publisherAdded", source, l -> l.publisherAdded(source, publisher    )); }
		@Override public void bookRemoved   (Object source, Book      book                         ) { forEach("bookRemoved"   , source, l -> l.bookRemoved   (source, book         )); }
	}
	
	public static class BookSeriesChangeController extends Controller<BookSeriesChangeListener> implements BookSeriesChangeListener
	{
		@Override public void fieldChanged (Object source,     BookSeries  bookSeries,     BookSeries.Field  field ) { forEach("fieldChanged" , source, l -> l.fieldChanged (source, bookSeries, field )); }
		@Override public void fieldsChanged(Object source, Set<BookSeries> bookSeries, Set<BookSeries.Field> fields) { forEach("fieldsChanged", source, l -> l.fieldsChanged(source, bookSeries, fields)); }
	}
	
	public static class StorageController extends Controller<StorageListener> implements StorageListener
	{
		@Override public void bookStorageLoaded(Object source) { forEach("bookStorageLoaded", source, l -> l.bookStorageLoaded(source)); }
	}
	
	public interface BookChangeListener
	{
		void fieldChanged (Object source,     Book  book ,     Book.Field  field );
		void fieldsChanged(Object source, Set<Book> books, Set<Book.Field> fields);
		void authorAdded   (Object source, Author    author   );
		void publisherAdded(Object source, Publisher publisher);
		void bookRemoved   (Object source, Book      book     );
		
		public static class Adapter implements BookChangeListener
		{
			@Override public void fieldChanged (Object source,     Book  book ,     Book.Field  field ) {}
			@Override public void fieldsChanged(Object source, Set<Book> books, Set<Book.Field> fields) {}
			@Override public void authorAdded   (Object source, Author    author   ) {}
			@Override public void publisherAdded(Object source, Publisher publisher) {}
			@Override public void bookRemoved   (Object source, Book      book     ) {}
		}
	}
	
	public interface BookSeriesChangeListener
	{
		void fieldChanged (Object source,     BookSeries  bookSeries,     BookSeries.Field  field );
		void fieldsChanged(Object source, Set<BookSeries> bookSeries, Set<BookSeries.Field> fields);
		
		public static class Adapter implements BookSeriesChangeListener
		{
			@Override public void fieldChanged (Object source,     BookSeries  bookSeries,     BookSeries.Field  field ) {}
			@Override public void fieldsChanged(Object source, Set<BookSeries> bookSeries, Set<BookSeries.Field> fields) {}
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
