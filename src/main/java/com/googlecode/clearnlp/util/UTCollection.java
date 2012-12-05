package com.googlecode.clearnlp.util;

import java.util.Collection;

public class UTCollection
{
	static public String[] toArray(Collection<String> col)
	{
		String[] array = new String[col.size()];
		col.toArray(array);
		return array;
	}
	
	static public String toString(Collection<String> col, String delim)
	{
		StringBuilder build = new StringBuilder();
		
		for (String item : col)
		{
			build.append(delim);
			build.append(item);
		}
		
		return build.substring(delim.length());
	}
}
