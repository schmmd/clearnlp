package edu.colorado.clear.util.map;

import java.util.HashMap;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

import edu.colorado.clear.util.pair.Pair;
import edu.colorado.clear.util.pair.StringDoublePair;


@SuppressWarnings("serial")
public class Prob2DMap extends HashMap<String,ObjectIntOpenHashMap<String>>
{
	static private final String TOTAL = "_T_";
	private int i_total;
	
	public void add(String key, String value)
	{
		ObjectIntOpenHashMap<String> map;
		
		if (containsKey(key))
		{
			map = get(key);
			
			map.put(value, map.get(value)+1);
			map.put(TOTAL, map.get(TOTAL)+1);
		}
		else
		{
			map = new ObjectIntOpenHashMap<String>();
			put(key, map);

			map.put(value, 1);
			map.put(TOTAL, 1);
		}
		
		i_total++;
	}
	
	public StringDoublePair[] getProb1D(String key)
	{
		Pair<Double,StringDoublePair[]> p = getProb1DAux(key);
		return (p == null) ? null : p.o2;
	}
	
	public StringDoublePair[] getProb2D(String key)
	{
		Pair<Double,StringDoublePair[]> p = getProb1DAux(key);
		if (p == null)	return null;
		
		double prior = p.o1;
		StringDoublePair[] probs = p.o2;
		
		for (StringDoublePair prob : probs)
			prob.d *= prior;
		
		return probs;
	}
	
	private Pair<Double,StringDoublePair[]> getProb1DAux(String key)
	{
		ObjectIntOpenHashMap<String> map = get(key);
		if (map == null)	return null;
		
		StringDoublePair[] probs = new StringDoublePair[map.size()-1];
		int i = 0, total = map.get(TOTAL);
		String value;
		
		for (ObjectCursor<String> cur : map.keys())
		{
			value = cur.value;
			
			if (!value.equals(TOTAL))
				probs[i++] = new StringDoublePair(value, (double)map.get(value)/total);
		}
		
		double prior = (double)total / i_total; 
		return new Pair<Double,StringDoublePair[]>(prior, probs);
	}
}
