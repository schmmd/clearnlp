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
}
