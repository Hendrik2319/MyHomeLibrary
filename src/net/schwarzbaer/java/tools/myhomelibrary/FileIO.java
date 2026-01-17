package net.schwarzbaer.java.tools.myhomelibrary;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

public class FileIO
{
	private static final String dataFolderName = "data";
	
	public enum DataFile
	{
		BookStorage("BookStorage.dat"),
		;
		private final String filename;
		DataFile(String filename)
		{
			this.filename = filename;
		}
		
		public File getFile() throws FileIOException
		{
			return new File( getDataFolder(), filename );
		}
	}

	private static File getDataFolder() throws FileIOException
	{
		File dataFolder = new File(dataFolderName);
		if (!dataFolder.isDirectory())
		{
			if (dataFolder.exists())
				throw new FileIOException(
						"Can't create folder \"%s\". There exist".formatted(dataFolderName),
						"something with same name, that's not a folder."
				);
			
			try { Files.createDirectory(dataFolder.toPath()); }
			catch (IOException ex) {
				//ex.printStackTrace();
				throw new FileIOException(
						"IOException while creating folder \"%s\":".formatted( dataFolderName ),
						ex.getMessage()
				);
			}
		}
		return dataFolder;
	}
	
	public static class FileIOException extends Exception
	{
		private static final long serialVersionUID = -3610019531484156215L;
		private final String[] lines;

		private FileIOException(String... lines)
		{
			super( String.join(" ", lines) );
			this.lines = lines;
		}
		
		public String[] getMessageLines()
		{
			return lines;
		}

		public void showMessageDialog(Component window, String title, String firstLine)
		{
			List<String> msg = new ArrayList<>();
			msg.add(firstLine);
			msg.addAll(
					Arrays
						.stream(lines)
						.map(str -> "   "+str)
						.toList()
			);
			JOptionPane.showMessageDialog(window, msg.toArray(), title, JOptionPane.ERROR_MESSAGE);
		}
	}
}
