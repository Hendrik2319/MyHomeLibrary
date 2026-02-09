package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

abstract class EditableComboBox<V> extends JComboBox<V>
{
	private static final long serialVersionUID = 1904858000564470454L;
	
	static void setIgnoreInputEvents(boolean b, EditableComboBox<?>... cmbbxs)
	{
		for (EditableComboBox<?> cmbbx : cmbbxs)
			cmbbx.ignoreInputEvents = b;
	}

	private final Class<V> classV;
	private final DefaultComboBoxModel<V> model;
	boolean ignoreInputEvents;

	EditableComboBox(Class<V> classV)
	{
		this.classV = classV;
		ignoreInputEvents = false;
		
		model = new DefaultComboBoxModel<>();
		model.addAll(getValues());
		setModel(model);
		
		setEditable(true);
		addActionListener(this::performAction);
	}

	private void performAction(ActionEvent ev)
	{
		if (ignoreInputEvents) return;
		
		Object selectedItem = getSelectedItem();
		
		if (selectedItem==null)
			setValue( null );
		
		else if (classV.isInstance(selectedItem))
			setValue( classV.cast(selectedItem) );
		
		else if (selectedItem instanceof String selectedStr && !selectedStr.isBlank())
		{
			V value = getExistingValue(selectedStr);
			if (value==null) value = addNewValueAndSet(selectedStr);
			updateValues( value );
		}
	}

	void updateValues()
	{
		updateValues( getSelectedItem() );
	}

	private void updateValues(Object selectedItem)
	{
		ignoreInputEvents = true;
		model.removeAllElements();
		model.addAll(getValues());
		setSelectedItem(selectedItem);
		ignoreInputEvents = false;
	}
	
	protected abstract List<V> getValues();
	protected abstract void setValue(V value);
	protected abstract V getExistingValue(String str);
	protected abstract V addNewValueAndSet(String str);
}
