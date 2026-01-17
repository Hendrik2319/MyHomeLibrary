package net.schwarzbaer.java.tools.myhomelibrary.data;

public class Author
{
	String name;
	
	public Author(String name)
	{
		this.name = name;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
}
