package net.schwarzbaer.java.tools.myhomelibrary.views;

import javax.swing.JTabbedPane;

import net.schwarzbaer.java.lib.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.system.Settings.DefaultAppSettings.SplitPaneDividersDefinition;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary.AppSettings.ValueKey;

public class MainWindow extends StandardMainWindow
{
	private static final long serialVersionUID = -2399166493832761711L;
	private final JTabbedPane tabbedPane;
	private final BooksTab booksTab;

	public MainWindow()
	{
		super("My Home Library");
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Books", booksTab = new BooksTab());
		
		startGUI(tabbedPane);
		
		MyHomeLibrary.appSettings.registerAppWindow(this);
		MyHomeLibrary.appSettings.registerSplitPaneDividers(
				new SplitPaneDividersDefinition<>(this, ValueKey.class)
				.add(booksTab, ValueKey.SplitPane_BooksTab)
		);
	}
}
