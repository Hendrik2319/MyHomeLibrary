package net.schwarzbaer.java.tools.myhomelibrary.data;

interface IdBased<ValueType extends IdBased<ValueType>> extends Comparable<ValueType>
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
