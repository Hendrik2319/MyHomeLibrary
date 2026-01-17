package net.schwarzbaer.java.tools.myhomelibrary.data;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class UniqueID
{
	private final int length;
	private final Set<String> knownIDs;
	private final Random rnd;
	private final String[] charArr; 
	
	UniqueID(int length)
	{
		this.length = length;
		knownIDs = new HashSet<>();
		rnd = new Random();
		charArr = new String[this.length];
	}
	
	String createNew()
	{
		String newID = null;
		while (newID==null || knownIDs.contains(newID))
		{
			for (int i=0; i<charArr.length; i++)
				charArr[i] =  Character.toString( nextChar() );
			newID = String.join("", charArr);
		}
		knownIDs.add(newID);
		return newID;
	}

	private char nextChar()
	{
		return (char)rnd.nextInt('A', ((int)'Z')+1);
	}

	void addKnownID(String id) throws UniqueIDException
	{
		if (knownIDs.contains(id))
			throw new UniqueIDException("ID \"%s\" is already in use.", id);
		knownIDs.add(id);
	}
	
	class UniqueIDException extends Exception
	{
		private static final long serialVersionUID = 5645187579709944735L;

		public UniqueIDException(String format, Object... values)
		{
			super(format.formatted(values));
		}
	}
	
	public interface IdBased<ValueType extends IdBased<ValueType>> extends Comparable<ValueType>
	{
		String getID();
		
		@Override
		public default int compareTo(ValueType other)
		{
			if (other==null) return -1;
			String thisID  = this.getID();
			String otherID = other.getID();
			if (thisID ==null) throw new IllegalStateException();
			if (otherID==null) throw new IllegalStateException();
			return thisID.compareTo(otherID);
		}
	}
}
