package edu.colorado.clear.util;

public class UTString
{
	/** White space characters ({@code " \t\n\r\f"}). */
	static final public String WHITESPACES = " \t\n\r\f";
	
	static public boolean isAllUpperCase(String str)
	{
		int i, size = str.length();
		
		for (i=0; i<size; i++)
		{
			if (!Character.isUpperCase(str.charAt(i)))
				return false;
		}
		
		return true;
	}
	
	static public boolean isAllLowerCase(String str)
	{
		int i, size = str.length();
		
		for (i=0; i<size; i++)
		{
			if (!Character.isLowerCase(str.charAt(i)))
				return false;
		}
		
		return true;
	}
	
	static public boolean beginsWithUpperCase(String str)
	{
		return Character.isUpperCase(str.charAt(0));
	}
	
	static public int getNumOfCapitalsNotAtBeginning(String str)
	{
		int i, size = str.length(), n = 0;
		
		for (i=1; i<size; i++)
		{
			if (Character.isUpperCase(str.charAt(i)))
				n++;
		}
		
		return n;
	}
	
	static public boolean containsDigit(String str)
	{
		int i, size = str.length();
		
		for (i=0; i<size; i++)
		{
			if (Character.isDigit(str.charAt(i)))
				return true;
		}
		
		return false;
	}
	
	static public String[] getPrefixes(String form, int n)
	{
		int i, length = form.length() - 1;
		if (length < n)	n = length;	
		String[] prefixes = new String[n];
		
		for (i=0; i<n; i++)
			prefixes[i] = form.substring(0, i+1);
		
		return prefixes;
	}
	
	static public String[] getSuffixes(String form, int n)
	{
		int i, length = form.length() - 1;
		if (length < n)	n = length;	
		String[] suffixes = new String[n];
		
		for (i=0; i<n; i++)
			suffixes[i] = form.substring(length-i);
		
		return suffixes;
	}
}
