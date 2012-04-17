package edu.colorado.clear.util.list;

import com.carrotsearch.hppc.IntArrayList;

public class SortedIntArrayList extends IntArrayList
{
	/* (non-Javadoc)
	 * @see com.carrotsearch.hppc.IntArrayList#add(int)
	 */
	public void add(int e1)
	{
		int i, size = size();
		
		for (i=0; i<size; i++)
		{
			if (get(i) > e1)
				break;
		}
		
		insert(i, e1);
	}
}
