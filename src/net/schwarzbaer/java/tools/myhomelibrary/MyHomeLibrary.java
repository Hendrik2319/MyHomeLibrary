package net.schwarzbaer.java.tools.myhomelibrary;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.java.lib.system.Settings;
import net.schwarzbaer.java.tools.myhomelibrary.data.BookStorage;
import net.schwarzbaer.java.tools.myhomelibrary.views.MainWindow;

public class MyHomeLibrary
{
	public static void main(String[] args)
	{
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new MyHomeLibrary().initialize();
	}
	
	public final static AppSettings appSettings = new AppSettings();
	public final MainWindow mainWindow;
	public final BookStorage bookStorage;

	private MyHomeLibrary()
	{
		bookStorage = new BookStorage(this);
		mainWindow = new MainWindow(this);
	}

	private void initialize()
	{
		bookStorage.readFromFile();
	}
	
	public static class AppSettings extends Settings.DefaultAppSettings<AppSettings.ValueGroup, AppSettings.ValueKey>
	{
		public enum ValueKey
		{
			SplitPane_BooksTab,
		}
		
		private enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		AppSettings()
		{
			super(MyHomeLibrary.class, ValueKey.values());
		}
	}
}
