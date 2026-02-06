package net.schwarzbaer.java.tools.myhomelibrary.data;

import java.awt.Dimension;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.schwarzbaer.java.tools.myhomelibrary.FileIO;

public class ImageData
{
	public final File file;
	public final FileIO.NameParts nameParts;
	public Dimension size = null;
	public Book book = null;
	
	private ImageData(File file)
	{
		this.file = Objects.requireNonNull( file );
		nameParts = FileIO.NameParts.parse( this.file.getName() );
	}

	public static List<ImageData> toList(File[] files)
	{
		return Arrays
				.stream(files)
				.map(ImageData::new)
				.toList();
	}
}