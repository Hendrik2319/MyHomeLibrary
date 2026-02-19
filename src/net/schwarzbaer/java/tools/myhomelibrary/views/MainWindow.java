package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.io.File;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;

import net.schwarzbaer.java.lib.globalsettings.GlobalSettings;
import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.ProgressDialog;
import net.schwarzbaer.java.lib.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.system.Settings.DefaultAppSettings.SplitPaneDividersDefinition;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary.AppSettings.ValueKey;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.Author;
import net.schwarzbaer.java.tools.myhomelibrary.data.BookSeries;
import net.schwarzbaer.java.tools.myhomelibrary.data.OnlineLibraryURL;
import net.schwarzbaer.java.tools.myhomelibrary.data.Publisher;

public class MainWindow extends StandardMainWindow
{
	private static final long serialVersionUID = -2399166493832761711L;
	private final MyHomeLibrary main;
	private final JTabbedPane tabbedPane;
	private final BooksTab booksTab;
	private final ImagesTab imagesTab;

	public MainWindow(MyHomeLibrary main)
	{
		super("My Home Library");
		this.main = main;
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Books" ,  booksTab = new BooksTab(this.main));
		tabbedPane.addTab("Images", imagesTab = new ImagesTab(this.main));
		
		startGUI(tabbedPane, new MenuBar());
		
		MyHomeLibrary.appSettings.registerAppWindow(this);
		MyHomeLibrary.appSettings.registerSplitPaneDividers(
				new SplitPaneDividersDefinition<>(this, ValueKey.class)
				.add( booksTab, ValueKey.SplitPane_BooksTab)
				.add(imagesTab, ValueKey.SplitPane_ImagesTab)
		);
	}

	public void initialize(ProgressDialog pd)
	{
		Tools.setIndeterminateTaskTitle(pd, "Collect image data");
		imagesTab.table.reloadData();
	}

	private class MenuBar extends JMenuBar
	{
		private static final long serialVersionUID = 6298143685728714248L;
		private final JMenu mnSettings;
		private final JMenu mnCleanUp;
		private final JMenu mnOnlineLibrary;
		
		MenuBar()
		{
			mnSettings = add(new JMenu("Settings"));
			
			mnSettings.add(Tools.createMenuItem("Set Path to Browser", true, null, e -> {
				File file = GlobalSettings.getInstance().askUserForExecutable(MainWindow.this, "Select Browser", GlobalSettings.Key.Browser);
				if (file!=null) System.out.printf("Path to Browser was set to \"%s\"%n", file.getAbsolutePath());
			}));
			
			mnSettings.add(mnOnlineLibrary = new JMenu("Online Library"));
			
			ButtonGroup bg = new ButtonGroup();
			OnlineLibraryURL onlineLibrary = Tools.getOnlineLibrary();
			for (OnlineLibraryURL ol : OnlineLibraryURL.values())
				mnOnlineLibrary.add(Tools.createCheckBoxMenuItem(ol.title, true, ol==onlineLibrary, bg, null, b -> {
					if (b) Tools.setOnlineLibrary(ol);
				}));
			
			
			mnCleanUp = add(new JMenu("Clean Up"));
			
			mnCleanUp.add(Tools.createMenuItem("Delete Empty Book Series", true, GrayCommandIcons.IconGroup.Delete, e -> {
				List<BookSeries> deleted = main.bookStorage.deleteEmptyBookSeries();
				main.notifier.bookSeries.deleted(MainWindow.this, deleted);
			}));
			
			mnCleanUp.add(Tools.createMenuItem("Delete Unused Authors", true, GrayCommandIcons.IconGroup.Delete, e -> {
				List<Author> deleted = main.bookStorage.deleteUnusedAuthors();
				main.notifier.authors.deleted(MainWindow.this, deleted);
			}));
			
			mnCleanUp.add(Tools.createMenuItem("Delete Unused Publishers", true, GrayCommandIcons.IconGroup.Delete, e -> {
				List<Publisher> deleted = main.bookStorage.deleteUnusedPublishers();
				main.notifier.publishers.deleted(MainWindow.this, deleted);
			}));
		}
	}
}
