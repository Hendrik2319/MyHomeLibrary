package net.schwarzbaer.java.tools.myhomelibrary;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JOptionPane;

public class FileIO
{
	private static final String dataFolderName = "data";
	private static final String imageFolderName = "images";
	
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
	
	public static BufferedImage loadImagefile(String filename)
	{
		if (filename==null)
			return null;
		
		File file;
		try { file = getImageFile(filename); }
		catch (FileIOException ex) {
			//ex.printStackTrace();
			ex.showInErrorConsole("Can't get image file \"%s\"".formatted(filename));
			return null;
		}
		
		if (!file.isFile())
			return null;
		
		try { return ImageIO.read(file); }
		catch (IOException ex) {
			//ex.printStackTrace();
			System.out.printf("IOException while reading image file \"%s\": %s", filename, ex.getMessage());
			return null;
		}
	}

	public static File getImageFile(String filename) throws FileIOException
	{
		return new File( getImageFolder(), filename );
	}

	public static String saveImageFile(String bookId, String coverPartStr, BufferedImage image) throws FileIOException
	{
		String basefilename = "%s_%s".formatted(bookId,coverPartStr);
		String extension = ".jpg";
		File imageFolder = getImageFolder();
		
		int index = 0;
		File file = new File(imageFolder, basefilename+extension);
		while (file.exists())
			file = new File(imageFolder, "%s_%03d%s".formatted(basefilename, ++index, extension));
		
		saveImageFile(file, image);
		
		return file.getName();
	}

	private static void saveImageFile(File file, BufferedImage image) throws FileIOException
	{
		ImageWriter imageWriter = getImageWriterByFormatName("jpg");
		if (imageWriter==null)
			throw new FileIOException("Can't find an ImageWriter for JPEG");
		
		System.out.printf("Write image to file: %s%n", file.getAbsolutePath());
		
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(file))
		{
			imageWriter.setOutput(ios);
			ImageWriteParam param = imageWriter.getDefaultWriteParam();
			
			if (param.canWriteCompressed())
			{
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			    param.setCompressionQuality(0.8f);
				
				if (param instanceof JPEGImageWriteParam jpegParam)
					jpegParam.setOptimizeHuffmanTables(true);
			}
			
			if (param.canWriteProgressive())
			{
				param.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
			}
			
			imageWriter.write(null, new IIOImage(image, null, null), param);
		}
		catch (FileNotFoundException ex)
		{
			//ex.printStackTrace();
			throw new FileIOException(ex, "FileNotFoundException while writing image file \"%s\": %s", ex.getMessage());
		}
		catch (IOException ex)
		{
			//ex.printStackTrace();
			throw new FileIOException(ex, "IOException while writing image file \"%s\": %s", ex.getMessage());
		}
		finally
		{
			imageWriter.dispose();
		}
	}

	private static ImageWriter getImageWriterByFormatName(String formatName) {
		Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName(formatName);
		if (imageWriters.hasNext())
			return imageWriters.next();
		return null;
	}

	public static String[] getAllImageFiles(String bookId, String coverPartStr) throws FileIOException
	{
		String basefilename = "%s_%s".formatted(bookId,coverPartStr);
		String extension = ".jpg";
		
		File imageFolder = getImageFolder();
		File[] files = imageFolder.listFiles(file -> {
			if (file==null || !file.isFile()) return false;
			String filename = file.getName();
			return filename.startsWith(basefilename) && filename.endsWith(extension);
		});
		
		return Arrays
			.stream(files)
			.map(File::getName)
			.toArray(String[]::new);
	}

	private static File getImageFolder() throws FileIOException
	{
		File imageFolder = new File(getDataFolder(), imageFolderName);
		if (!imageFolder.isDirectory())
		{
			if (imageFolder.exists())
				throw new FileIOException(
						"Can't create folder \"%s\" in data folder. There exist".formatted( imageFolderName ),
						"something with same name, that's not a folder."
				);
			
			try { Files.createDirectory(imageFolder.toPath()); }
			catch (IOException ex) {
				//ex.printStackTrace();
				throw new FileIOException(
						"IOException while creating folder \"%s\" in data folder:".formatted( imageFolderName ),
						ex.getMessage()
				);
			}
		}
		return imageFolder;
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

		private FileIOException(Throwable cause, String... lines)
		{
			super( String.join(" ", lines), cause );
			this.lines = lines;
		}
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
			
			Throwable cause = getCause();
			if (cause!=null) 
				msg.add("   caused by %s: %s".formatted(cause.getClass().getCanonicalName(), cause.getMessage()));
			
			JOptionPane.showMessageDialog(window, msg.toArray(), title, JOptionPane.ERROR_MESSAGE);
		}
		
		public void showInErrorConsole(String firstLine)
		{
			System.err.println(firstLine);
			for (String line : lines)
				System.err.printf("   %s%n", line);
			
			Throwable cause = getCause();
			if (cause!=null) 
				System.err.printf("   caused by %s: %s%n", cause.getClass().getCanonicalName(), cause.getMessage());
		}
	}
}
