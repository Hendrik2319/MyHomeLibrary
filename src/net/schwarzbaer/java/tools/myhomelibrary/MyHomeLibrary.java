package net.schwarzbaer.java.tools.myhomelibrary;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.schwarzbaer.java.lib.system.Settings;
import net.schwarzbaer.java.tools.myhomelibrary.data.BookStorage;
import net.schwarzbaer.java.tools.myhomelibrary.data.Notifier;
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
	public final Notifier notifier;
	public final JFileChooser imageImportFileChooser;

	private MyHomeLibrary()
	{
		notifier = new Notifier();
		bookStorage = new BookStorage(this);
		mainWindow = new MainWindow(this);
		
		imageImportFileChooser = new JFileChooser();
		imageImportFileChooser.setMultiSelectionEnabled(false);
		imageImportFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		imageImportFileChooser.setFileFilter(new FileNameExtensionFilter("JPEG-Image (*.jpg, *.jpeg)", "jpg", "jpeg"));
		imageImportFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PNG-Image (*.png)", "png"));
	}

	private void initialize()
	{
		bookStorage.readFromFile();
		notifier.storages.bookStorageLoaded(this);
		//ImageImportDialog.test(mainWindow);
		imageImportFileChooser.setCurrentDirectory(new File(".").getAbsoluteFile());
	}
	
	public static class AppSettings extends Settings.DefaultAppSettings<AppSettings.ValueGroup, AppSettings.ValueKey>
	{
		public enum ValueKey
		{
			SplitPane_BooksTab, ImageImportDialog_CutOutEngine_SubRasterSize,
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
