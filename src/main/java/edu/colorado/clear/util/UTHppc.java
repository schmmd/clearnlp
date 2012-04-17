package edu.colorado.clear.util;

import com.carrotsearch.hppc.IntContainer;
import com.carrotsearch.hppc.IntOpenHashSet;

public class UTHppc
{
	static public int max(IntContainer c)
	{
		int max = Integer.MIN_VALUE;
		
		for (int i : c.toArray())
			max = Math.max(max, i);
		
		return max;
	}
	
	static public int min(IntContainer c)
	{
		int min = Integer.MAX_VALUE;
		
		for (int i : c.toArray())
			min = Math.min(min, i);
		
		return min;
	}
	
	/**
	 * Returns {@code true} if {@code s2} is the subset of {@code s1}.
	 * @return {@code true} if {@code s2} is the subset of {@code s1}.
	 */
	static public boolean isSubset(IntContainer s1, IntContainer s2)
	{
		for (int i : s2.toArray())
		{
			if (!s1.contains(i))
				return false;
		}
		
		return true;
	}
	
	static public IntOpenHashSet intersection(IntContainer c1, IntContainer c2)
	{
		IntOpenHashSet s1 = new IntOpenHashSet(c1);
		IntOpenHashSet s2 = new IntOpenHashSet(c2);
		
		s1.retainAll(s2);
		return s1;
	}
}
