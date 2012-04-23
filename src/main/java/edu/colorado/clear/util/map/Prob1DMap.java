package edu.colorado.clear.util.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

import edu.colorado.clear.util.pair.StringIntPair;

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
	
	public List<StringIntPair> toSortedList()
	{
		List<StringIntPair> list = new ArrayList<StringIntPair>();
		String key;
		
		for (ObjectCursor<String> cur : keys())
		{
			key = cur.value;
			list.add(new StringIntPair(key, get(key)));
		}
		
		Collections.sort(list);
		return list;
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
