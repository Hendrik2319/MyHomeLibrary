package net.schwarzbaer.java.tools.myhomelibrary.views;

import javax.swing.JMenuItem;
import javax.swing.JScrollPane;

import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.tools.myhomelibrary.Tools;

class ImagesTableContextMenu extends Tables.TableContextMenu<ImagesTable.ImagesTableModel.ImageData, ImagesTable.ImagesTableModel, ImagesTable.ImagesTableModel.ColumnID>
{
	private static final long serialVersionUID = -3327229329712694897L;
	private final ImagesTable table;
	JMenuItem miReload;

	ImagesTableContextMenu(ImagesTable table, ImagesTable.ImagesTableModel tableModel, JScrollPane tableScrollPane)
	{
		super(table, tableModel);
		this.table = table;
		 
		addTo(tableScrollPane);
	}

	@Override
	protected void insertBeforeStdElements()
	{
		add(miReload = Tools.createMenuItem("Reload", true, GrayCommandIcons.IconGroup.Reload, e -> table.reloadData()));
		
		addSeparator();
	}
}
