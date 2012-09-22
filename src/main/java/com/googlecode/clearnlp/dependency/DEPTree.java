/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package com.googlecode.clearnlp.dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.pair.IntIntPair;
import com.googlecode.clearnlp.util.pair.StringIntPair;


/**
 * Dependency tree.
 * See <a target="_blank" href="http://code.google.com/p/clearnlp/source/browse/trunk/src/edu/colorado/clear/test/dependency/DPTreeTest.java">DPTreeTest</a> for the use of this class.
 * @see DEPNode
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
@SuppressWarnings("serial")
public class DEPTree extends ArrayList<DEPNode>
{
	/**
	 * Constructs a dependency tree.
	 * An artificial root node gets inserted automatically.
	 */
	public DEPTree()
	{
		DEPNode root = new DEPNode();
		
		root.initRoot();
		add(root);
	}
	
	/**
	 * Returns the dependency node with the specific ID.
	 * If there is no such node, returns {@code null}.
	 * @return the dependency node with the specific ID.
	 */
	public DEPNode get(int id)
	{
		try
		{
			return super.get(id);
		}
		catch (IndexOutOfBoundsException e)
		{
			return null;
		}
	}

	/**
	 * Returns an array of (headId, deprel) pair in each node.
	 * @return an array of (headId, deprel) pair in each node.
	 */
	public StringIntPair[] getHeads()
	{
		int i, size = size();
		DEPArc head;
		
		StringIntPair[] heads = new StringIntPair[size];
		heads[0] = new StringIntPair(DEPLib.ROOT_TAG, DEPLib.NULL_ID);
		
		for (i=1; i<size; i++)
		{
			head = get(i).d_head;
			heads[i] = new StringIntPair(head.label, head.getNode().id);
		}
		
		return heads;
	}
	
	/**
	 * Returns a list of (xHeadId, xLabel) pair list in each node.
	 * @return a list of (xHeadId, xLabel) pair list in each node.
	 */
	public StringIntPair[][] getXHeads()
	{
		return getHeadsAux(true);
	}
	
	/**
	 * Returns a list of (sHeadId, sLabel) pair list in each node.
	 * @return a list of (sHeadId, sLabel) pair list in each node.
	 */
	public StringIntPair[][] getSHeads()
	{
		return getHeadsAux(false);
	}
	
	/** Called by {@link DEPTree#getSHeads()}. */
	private StringIntPair[][] getHeadsAux(boolean isXhead)
	{
		int i, j, len, size = size();
		StringIntPair[] heads;
		List<DEPArc> arcs;
		DEPArc arc;
		
		StringIntPair[][] xHeads = new StringIntPair[size][];
		xHeads[0] = new StringIntPair[0];
		
		for (i=1; i<size; i++)
		{
			arcs  = isXhead ? get(i).getXHeads() : get(i).getSHeads();
			len   = arcs.size();
			heads = new StringIntPair[len];
			
			for (j=0; j<len; j++)
			{
				arc = arcs.get(j);
				heads[j] = new StringIntPair(arc.label, arc.getNode().id);
			}
			
			xHeads[i] = heads;
		}
		
		return xHeads;
	}
	
	/** Clears dependency head information (excluding secondary dependencies) of all nodes in this tree. */
	public void clearHeads()
	{
		for (DEPNode node : this)
			node.d_head.clear();
	}
	
	public void clearXHeads()
	{
		for (DEPNode node : this)
			node.x_heads.clear();
	}
	
	/** Clears semantic head information of all nodes in this tree. */
	public void clearSHeads()
	{
		for (DEPNode node : this)
			node.s_heads.clear();
	}
	
	public void setDependents()
	{
		int i, size = size();
		DEPNode node, head;
		
		for (i=0; i<size; i++)
			get(i).l_dependents = new ArrayList<DEPArc>();
		
		for (i=1; i<size; i++)
		{
			node = get(i);
			head = node.getHead();
			head.addDependent(node, node.getLabel());
		}
	}
	
	public void initXHeads()
	{
		int i, size = size();
		
		for (i=0; i<size; i++)
			get(i).x_heads = new ArrayList<DEPArc>();
	}
	
	public void initSHeads()
	{
		int i, size = size();
		
		for (i=0; i<size; i++)
			get(i).s_heads = new ArrayList<DEPArc>();
	}
	
	public List<List<DEPArc>> getArgumentList()
	{
		int i, size = size();
		List<DEPArc> args;
		DEPNode node;
		
		List<List<DEPArc>> list = new ArrayList<List<DEPArc>>();
		for (i=0; i<size; i++)	list.add(new ArrayList<DEPArc>());
		
		for (i=1; i<size; i++)
		{
			node = get(i);
			
			for (DEPArc arc : node.getSHeads())
			{
				args = list.get(arc.getNode().id);
				args.add(new DEPArc(node, arc.getLabel()));
			}
		}
		
		return list;
	}
	
	/**
	 * Returns {@code true} if this tree contains a cycle.
	 * @return {@code true} if this tree contains a cycle.
	 */
	public boolean containsCycle()
	{
		int i, size = size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = get(i);
			
			if (node.getHead().isDescendentOf(node))
				return true;
		}
		
		return false;
	}
	
	public IntOpenHashSet getNonProjectiveSet()
	{
		IntObjectOpenHashMap<IntOpenHashSet> map = new IntObjectOpenHashMap<IntOpenHashSet>();
		int i, j, bIdx, eIdx, size = size();
		DEPNode curr, head, dep;
		
		for (i=1; i<size; i++)
		{
			curr = get(i);
			head = curr.getHead();
			
			if (curr.id < head.id)
			{
				bIdx = curr.id;
				eIdx = head.id;
			}
			else
			{
				bIdx = head.id;
				eIdx = curr.id;
			}
			
			for (j=bIdx+1; j<eIdx; j++)
			{
				curr = get(j);
				head = curr.getHead();
				
				if (head.id < bIdx || head.id > eIdx)
				{
					addNonProjectiveMap(map, i, j);
					addNonProjectiveMap(map, j, i);
				}

				for (DEPArc arc : curr.getDependents())
				{
					dep = arc.getNode();
					
					if (dep.id < bIdx || dep.id > eIdx)
					{
						addNonProjectiveMap(map, i, dep.id);
						addNonProjectiveMap(map, dep.id, i);						
					}
				}
			}
		}
		
		return getNonProjectiveMapAux(map);
	}
	
	private void addNonProjectiveMap(IntObjectOpenHashMap<IntOpenHashSet> map, int cIdx, int nIdx)
	{
		IntOpenHashSet set;
		
		if (map.containsKey(cIdx))
			set = map.get(cIdx);
		else
		{
			set = new IntOpenHashSet();
			map.put(cIdx, set);
		}
		
		set.add(nIdx);
	}
	
	private IntOpenHashSet getNonProjectiveMapAux(IntObjectOpenHashMap<IntOpenHashSet> map)
	{
		IntIntPair max = new IntIntPair(-1, -1);
		IntOpenHashSet set, remove;
		boolean removed;
		int[] keys;
		
		do
		{
			max.set(-1, -1);
			keys = map.keys().toArray();
			Arrays.sort(keys);
			
			for (int key : keys)
			{
				set = map.get(key);
				
				if (set.size() > max.i2)
					max.set(key, set.size());
			}
			
			removed = false;
			
			if (max.i2 > 0)
			{
				remove = new IntOpenHashSet();
				
				for (IntCursor cur : map.get(max.i1))
				{
					if (map.containsKey(cur.value))
					{
						set = map.get(cur.value);
						
						if (set.contains(max.i1))
						{
							removed = true;
							set.remove(max.i1);
							if (set.isEmpty())	remove.add(cur.value);
						}
					}
				}
				
				for (IntCursor cur : remove)
					map.remove(cur.value);
			}
		}
		while (removed);
						
		return new IntOpenHashSet(map.keys());
	}
	
	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#toString()
	 */
	public String toString()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i));
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringDEP()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringDEP());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringDAG()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringDAG());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringCoNLL()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringCoNLL());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	public String toStringSRL()
	{
		StringBuilder build = new StringBuilder();
		int i, size = size();
		
		for (i=1; i<size; i++)
		{
			build.append(DEPReader.DELIM_SENTENCE);
			build.append(get(i).toStringSRL());
		}

		return build.substring(DEPReader.DELIM_SENTENCE.length());
	}
	
	/** Adds counts of [N, LAS, UAS, LS]. */
	public void addScoreCounts(StringIntPair[] gHeads, int[] counts)
	{
		int i, size = size();
		StringIntPair head;
		DEPNode node;
		
		counts[0] += size - 1;
		
		for (i=1; i<size; i++)
		{
			node = get(i);
			if (!node.hasHead())	continue;
			
			head = gHeads[i];
			
			if (head.i == node.getHead().id)
				counts[2]++;
			
			if (gHeads[i].s.equals(node.getLabel()))
			{
				if (head.i == node.getHead().id)
					counts[1]++;
				
				counts[3]++;
			}
		}
	}
	
	public DEPNode getNextPredicate(int prevId)
	{
		int i, size = size();
		DEPNode pred;
		
		for (i=prevId+1; i<size; i++)
		{
			pred = get(i);
			
			if (pred.getFeat(DEPLib.FEAT_PB) != null)
				return pred;
		}
		
		return null;
	}
}