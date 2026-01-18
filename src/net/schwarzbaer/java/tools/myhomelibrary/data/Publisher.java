package net.schwarzbaer.java.tools.myhomelibrary.data;

public record Publisher(String name)
{
	@Override
	public String toString()
	{
		return name;
	}
}
