package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;
import net.schwarzbaer.java.tools.myhomelibrary.data.ImageData;

class ImagesTableContextMenu extends Tables.TableContextMenu<ImageData, ImagesTable.ImagesTableModel, ImagesTable.ImagesTableModel.ColumnID>
{
	private static final long serialVersionUID = -3327229329712694897L;
	private final MyHomeLibrary main;
	private final ImagesTable table;
	JMenuItem miReload;
	JMenuItem miResetRowOrder;
	JMenuItem miDeleteRows;

	ImagesTableContextMenu(MyHomeLibrary main, ImagesTable table, ImagesTable.ImagesTableModel tableModel, JScrollPane tableScrollPane)
	{
		super(table, tableModel);
		this.main = main;
		this.table = table;
		addTo(tableScrollPane);
	}

	@Override
	protected void insertBeforeStdElements()
	{
		add(miDeleteRows = Tools.createMenuItem("###"         , true, GrayCommandIcons.IconGroup.Delete, e -> deleteSelectedRows()));
		add(miReload     = Tools.createMenuItem("Reload Table", true, GrayCommandIcons.IconGroup.Reload, e -> table.reloadData()));
		
		addSeparator();
		
		add(miResetRowOrder = Tools.createMenuItem("Reset Row Order", true, GrayCommandIcons.IconGroup.Reload, e -> table.tableRowSorter.resetSortOrder()));
	}

	private void deleteSelectedRows()
	{
		if (table.selectedRows.isEmpty())
			return;
		
		table.checkUsage(table.selectedRows);
		
		boolean atLeastOneIsUsedByBook = table.selectedRows.stream().anyMatch(d -> d.book!=null);
		if (atLeastOneIsUsedByBook)
		{
			String title = "Can't delete";
			String msg = "Sorry, can't delete selected images.\r\nAt least one of them is used by a book.";
			JOptionPane.showMessageDialog(main.mainWindow, msg, title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		String rowsToDelete = table.selectedRows
			.stream()
			.map(d -> {
				String str = "\"%s\"".formatted( d.file.getName() );
				
				List<String> remarks = new ArrayList<String>();
				//if (d.book!=null) remarks.add("is used as cover image");
				if (d.size==null) remarks.add("is no image");
				if (d.nameParts==null) remarks.add("has wrong name scheme");
				else if (!main.bookStorage.hasBook(d.nameParts.bookID())) remarks.add("is an image of a deleted book");
				
				if (!remarks.isEmpty())
					return "%n   %s (%s)".formatted(str, String.join(", ", remarks));
				return "%n   %s".formatted(str);
				
			})
			.collect(Collectors.joining());
		
		String title = "Are you sure?";
		String msg = "Do you really want to delete %s?%s".formatted(
				table.selectedRows.size() == 1
					? "1 image"
					: "%d images".formatted(table.selectedRows.size()),
				rowsToDelete
		);
		if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(main.mainWindow, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE))
			return;
		
		table.selectedRows
			.stream()
			.map(d -> d.file)
			.filter(File::isFile)
			.forEach(file -> {
				try { Files.delete(file.toPath()); }
				catch (NoSuchFileException        ex) { /* ex.printStackTrace(); */ System.err.printf("NoSuchFileException"       +" while deleting file \"%s\": %s%n", file.getAbsolutePath(), ex.getMessage()); }
				catch (DirectoryNotEmptyException ex) { /* ex.printStackTrace(); */ System.err.printf("DirectoryNotEmptyException"+" while deleting file \"%s\": %s%n", file.getAbsolutePath(), ex.getMessage()); }                                                                                                                                    
				catch (IOException                ex) { /* ex.printStackTrace(); */ System.err.printf("IOException"               +" while deleting file \"%s\": %s%n", file.getAbsolutePath(), ex.getMessage()); }
			});
		
		table.tableModel.removeRows(table.selectedRows);
	}

	@Override
	protected void updateElementsBeforeInvokationOfContextMenu()
	{
		miDeleteRows.setEnabled(!table.selectedRows.isEmpty());
		miDeleteRows.setText(
				table.selectedRows.isEmpty()
					? "Delete Rows"
					: table.selectedRows.size() == 1
						? "Delete 1 Row"
						: "Delete %d Rows".formatted(table.selectedRows.size())
		);
	}
	
}
