package net.schwarzbaer.java.tools.myhomelibrary;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.schwarzbaer.java.lib.gui.GeneralIcons;

public class Tools
{
	public static JButton createButton(String text, boolean isEnabled, GeneralIcons.IconGroup icons, ActionListener al)
	{
		JButton comp = new JButton(text);
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		if (icons!=null) icons.setIcons(comp);
		return comp;
	}
	
	public static String toOrdinalString(int n)
	{
		if (n<1) return "--";
		
		int n1 = n%10;
		int n2 = n%100;
		
		if (n1==1 && n2!=11) return "%dst".formatted(n);
		if (n1==2 && n2!=12) return "%dnd".formatted(n);
		if (n1==3 && n2!=13) return "%drd".formatted(n);
		
		return "%dth".formatted(n);
	}

	public static <V> List<V> addNull(List<V> list)
	{
		List<V> resultList = new ArrayList<>();
		resultList.add(null);
		resultList.addAll(list);
		return resultList;
	}

	public static <V> V getIfNotNull(V value, V replacement)
	{
		return getIfNotNull(value, replacement, v->v);
	}
	public static <V1,V2> V2 getIfNotNull(V1 value, V2 replacement, Function<V1,V2> getValue)
	{
		return value==null ? replacement : getValue.apply(value);
	}
	public static <V> void doIfNotNull(V value, Consumer<V> action)
	{
		if (value!=null)
			action.accept(value);
	}
	
	public static void callChecked(String label, Runnable action)
	{
		try
		{
			action.run();
		}
		catch (Exception ex)
		{
			//ex.printStackTrace();
			System.err.printf("%s while %s: %s%n", ex.getClass().getCanonicalName(), label, ex.getMessage());
		}
	}
	
	public static class GUIConfigurator
	{
		private final Color defaultTextFieldForeground;
		private final Color defaultTextFieldBackground;
		public boolean ignoreInputEvents;

		public GUIConfigurator(Color defaultTextFieldForeground, Color defaultTextFieldBackground)
		{
			ignoreInputEvents = false;
			this.defaultTextFieldForeground = defaultTextFieldForeground;
			this.defaultTextFieldBackground = defaultTextFieldBackground;
		}

		public void configureOutputField(JTextField field)
		{
			field.setEditable(false);
		}

		public void configureIntField(JTextField field, IntPredicate isOK, IntConsumer setValue)
		{
			configureField(field, Integer::parseInt, n->isOK.test(n), n->setValue.accept(n));
		}

		public void configureStringField(JTextField field, Predicate<String> isOK, Consumer<String> setValue)
		{
			configureField(field, str->str, isOK, setValue);
		}

		public <V> void configureField(JTextField field, Function<String,V> parser, Predicate<V> isOK, Consumer<V> setValue)
		{
			field.addActionListener(e -> {
				if (ignoreInputEvents) return;
				
				SwingUtilities.invokeLater(()->field.setForeground(defaultTextFieldForeground));
				String text = field.getText();
				
				V value;
				try { value = parser.apply(text); }
				catch (Exception ex)
				{
					//ex.printStackTrace();
					//System.err.printf("%s while editing text field: %s%n", ex.getClass().getCanonicalName(), ex.getMessage());
					value = null;
				}
				
				if (value==null || (isOK!=null && !isOK.test(value)))
				{
					SwingUtilities.invokeLater(()->field.setBackground(Color.RED));
					return;
				}
				
				field.setBackground(defaultTextFieldBackground);
				setValue.accept(value);
			});
			
			field.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e)
				{
					field.setBackground(defaultTextFieldBackground);
					field.setForeground(Color.BLUE);
				}
			});
		}
	}
}
