package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedRowSorter;

public class BooksTable extends JTable
{
	private static final long serialVersionUID = -4421855766356160275L;
	private final BooksTableModel tableModel;
	private final SimplifiedRowSorter tableRowSorter;
	        final JScrollPane tableScrollPane;

	BooksTable()
	{
		setModel(tableModel = new BooksTableModel());
		
		setRowSorter(tableRowSorter = new Tables.SimplifiedRowSorter(tableModel));
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setColumnSelectionAllowed(false);
		tableModel.setTable(this);
		tableModel.setColumnWidths(this);
		
//		tableCellRenderer = new TransactionTableCellRenderer(this.main);
//		tableModel.setAllDefaultRenderers(cl -> tableCellRenderer);
		
//		categoryCellEditor       = new Tables.ComboboxCellEditor<>(() -> Tools.addNull(this.main.categoryStorage.getAllCategoriesAsVector()) );
//		timeRangeCellEditor      = new Tables.ComboboxCellEditor<>(Tools.addNull( TimeRange.values(), TimeRange[]::new ));
//		timeRangeValueCellEditor = new Tables.ComboboxCellEditor<>(this::getListOfTimeRangeValues);
//		categoryCellEditor      .setRenderer(new  CategoryCellEditorListRenderer  (tableModel, t -> t.storedVars.categoryByRule));
//		timeRangeCellEditor     .setRenderer(new RuleValueCellEditorListRenderer<>(tableModel, t -> Tools.getIfNotNull(t.storedVars.timeRangeByRule, null, trv->trv.timeRange)));
//		timeRangeValueCellEditor.setRenderer(new RuleValueCellEditorListRenderer<>(tableModel, t -> t.storedVars.timeRangeByRule));
		
//		setDefaultEditor(Category      .class, categoryCellEditor);
//		setDefaultEditor(TimeRange     .class, timeRangeCellEditor);
//		setDefaultEditor(TimeRangeValue.class, timeRangeValueCellEditor);
		
		tableScrollPane = new JScrollPane(this);
		tableScrollPane.setPreferredSize(new Dimension(1000,500));
	}
}
