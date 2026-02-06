package net.schwarzbaer.java.tools.myhomelibrary.views;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.java.lib.gui.ImageView;
import net.schwarzbaer.java.tools.myhomelibrary.MyHomeLibrary;
import net.schwarzbaer.java.tools.myhomelibrary.data.ImageData;

class ImagesTab extends JSplitPane
{
	private static final long serialVersionUID = 8474850684546394110L;
	        final ImagesTable table;
	private final ImageView imageView;
	private List<ImageData> selectedRows;

	ImagesTab(MyHomeLibrary main)
	{
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		
		table = new ImagesTable(main);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		imageView = new ImageView(500, 600);
		
		setLeftComponent(table.tableScrollPane);
		setRightComponent(imageView);
		
		table.getSelectionModel().addListSelectionListener(e -> {
			selectedRows = table.getSelectedRows_();
			
			BufferedImage image = null;
			if (selectedRows.size()==1)
			{
				File file = selectedRows.get(0).file;
				try { image = ImageIO.read(file); }
				catch (IOException ex) {
					System.err.printf("IOException while reading file \"%s\": %s%n", file.getAbsolutePath(), ex.getMessage());
					image = null;
				}
			}
			imageView.setImage(image);
		});
	}
}
