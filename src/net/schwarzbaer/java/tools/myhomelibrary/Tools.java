package net.schwarzbaer.java.tools.myhomelibrary;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import net.schwarzbaer.java.lib.gui.GeneralIcons;
import net.schwarzbaer.java.lib.gui.ProgressDialog;

public class Tools
{
	public static final double IMAGE_REDUCTION__THRESHOLD = 1.3;
	public static final double IMAGE_REDUCTION__MAX_SIZE = 1000.0;
	public static final Comparator<String> COMPARATOR__IGNORING_CASE = Comparator.<String,String>comparing(String::toLowerCase).thenComparing(Comparator.naturalOrder());
	
	public static JButton createButton(String text, boolean isEnabled, GeneralIcons.IconGroup icons, ActionListener al)
	{
		return configureAbstractButton(new JButton(text), isEnabled, null, icons, al);
	}

	public static JToggleButton createToggleButton(String text, boolean isEnabled, boolean checked, ButtonGroup bg, GeneralIcons.IconGroup icons, Consumer<Boolean> setValue)
	{
		return configureCheckBox(new JToggleButton(text, checked), isEnabled, bg, icons, setValue);
	}

	public static JCheckBox createCheckBox(String text, boolean isEnabled, boolean isChecked, ButtonGroup bg, GeneralIcons.IconGroup icons, Consumer<Boolean> setValue)
	{
		return configureCheckBox(new JCheckBox(text, isChecked), isEnabled, bg, icons, setValue);
	}

	public static JMenuItem createMenuItem(String text, boolean isEnabled, GeneralIcons.IconGroup icons, ActionListener al)
	{
		return configureAbstractButton(new JMenuItem(text), isEnabled, null, icons, al);
	}

	public static JCheckBoxMenuItem createCheckBoxMenuItem(String text, boolean isEnabled, boolean isChecked, ButtonGroup bg, GeneralIcons.IconGroup icons, Consumer<Boolean> setValue)
	{
		return configureCheckBox(new JCheckBoxMenuItem(text, isChecked), isEnabled, bg, icons, setValue);
	}
	
	private static <Type extends AbstractButton> Type configureCheckBox(Type comp, boolean isEnabled, ButtonGroup bg, GeneralIcons.IconGroup icons, Consumer<Boolean> setValue)
	{
		return configureAbstractButton(comp, isEnabled, bg, icons, setValue==null ? null : e -> setValue.accept(comp.isSelected()));
	}
	
	private static <Type extends AbstractButton> Type configureAbstractButton(Type comp, boolean isEnabled, ButtonGroup bg, GeneralIcons.IconGroup icons, ActionListener al)
	{
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		if (icons!=null) icons.setIcons(comp);
		if (bg!=null) bg.add(comp);
		return comp;
	}

	public static <V> Comparator<V> createComparatorByName(Function<V, String> getName)
	{
		return Comparator.<V,String>comparing( getName, COMPARATOR__IGNORING_CASE );
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
	
	public static <V> V callChecked(String label, V replacement, Supplier<V> action)
	{
		try
		{
			return action.get();
		}
		catch (Exception ex)
		{
			//ex.printStackTrace();
			System.err.printf("%s while %s: %s%n", ex.getClass().getCanonicalName(), label, ex.getMessage());
			return replacement;
		}
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

	public static void setTaskValue(ProgressDialog pd, int value)
	{
		SwingUtilities.invokeLater(() -> {
			pd.setValue(value);
		});
	}

	public static void setTaskTitle(ProgressDialog pd, String taskTitle, int min, int value, int max)
	{
		SwingUtilities.invokeLater(() -> {
			pd.setTaskTitle(taskTitle);
			pd.setValue(min, value, max);
		});
	}

	public static void setIndeterminateTaskTitle(ProgressDialog pd, String taskTitle)
	{
		SwingUtilities.invokeLater(() -> {
			pd.setTaskTitle(taskTitle);
			pd.setIndeterminate(true);
		});
	}

	public static <V> boolean swap(DefaultListModel<V> list, int index1, int index2)
	{
		if (index1<0 || index1>=list.size()) return false;
		if (index2<0 || index2>=list.size()) return false;
		list.set(index2, list.set(index1, list.get(index2)));
		return true;
	}

	public static <V> boolean swap(List<V> list, int index1, int index2)
	{
		if (index1<0 || index1>=list.size()) return false;
		if (index2<0 || index2>=list.size()) return false;
		list.set(index2, list.set(index1, list.get(index2)));
		return true;
	}
	
	public static final Color TEXTCOLOR_NONSUBMITTED_TEXT = Color.BLUE;
	public static final Color BGCOLOR_WRONG_INPUT = Color.RED;
	
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

		public void configureDoubleField(JTextField field, DoublePredicate isOK, DoubleConsumer setValue)
		{
			configureField(field, Double::parseDouble, n->isOK.test(n), n->setValue.accept(n));
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
					SwingUtilities.invokeLater(()->field.setBackground(BGCOLOR_WRONG_INPUT));
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
					field.setForeground(TEXTCOLOR_NONSUBMITTED_TEXT);
				}
			});
		}
	}
}
