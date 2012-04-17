package edu.colorado.clear.util.map;

import java.util.HashSet;
import java.util.Set;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

public class Prob1DMap extends ObjectIntOpenHashMap<String>
{
	private int i_total;
	
	public void add(String key)
	{
		put(key, get(key)+1);
		i_total++;
	}
	
	public double getProb(String key)
	{
		return (double)get(key) / i_total;
	}
	
	public Set<String> toSet(int cutoff)
	{
		Set<String> set = new HashSet<String>();
		String key;
		
		for (ObjectCursor<String> cur : keys())
		{
			key = cur.value;
			
			if (get(key) > cutoff)
				set.add(key);
		}
		
		return set;
	}
}
