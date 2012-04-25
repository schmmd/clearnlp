/**
* Copyright (c) 2011, Regents of the University of Colorado
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
package edu.colorado.clear.constituent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;

/**
 * Constituent library.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class CTLib
{
	/** The phrase tag of the top node. */
	static final public String PTAG_TOP = "TOP";
	/** The phrase tag of unknown phrases. */
	static final public String PTAG_X   = "X";
	/** The pos tag of empty categories. */
	static final public String POS_NONE = "-NONE-";
	/** The pos tag of unknown tokens. */
	static final public String POS_XX	= "XX";
	
	/**
	 * Normalizes co-indices and gap-indices of the specific tree.
	 * @param tree the tree to be normalized.
	 */
	static public void normalizeIndices(CTTree tree)
	{
		// retrieve all co-indexes
		IntObjectOpenHashMap<List<CTNode>> mOrg = new IntObjectOpenHashMap<List<CTNode>>();
		getCoIndexMap(tree.getRoot(), mOrg);
		if (mOrg.isEmpty())	return;
		
		int[] keys = mOrg.keys().toArray();
		Arrays.sort(keys);
		
		IntIntOpenHashMap mNew = new IntIntOpenHashMap();		
		int coIndex = 1, last, i;
		List<CTNode> list;
		CTNode curr, ec;
		boolean isAnteFound;
		
		for (int key : keys)
		{
			list = mOrg.get(key);
			last = list.size() - 1;
			isAnteFound = false;
			
			for (i=last; i>=0; i--)
			{
				curr = list.get(i);
				
				if (curr.isEmptyCategoryRec())
				{
					ec = curr.getSubTerminals().get(0);
					
					if (i == last || isAnteFound || CTLibEn.RE_ICH_PPA_RNR.matcher(ec.form).find() || CTLibEn.containsCoordination(curr.getLowestCommonAncestor(list.get(i+1))))
						curr.coIndex = -1;
					else
						curr.coIndex = coIndex++;

					if (isAnteFound || i > 0)
						ec.form += "-"+coIndex;
				}
				else if (isAnteFound)
				{
					curr.coIndex = -1;
				}
				else
				{
					curr.coIndex = coIndex;
					mNew.put(key, coIndex);
					isAnteFound  = true;
				}
			}
			
			coIndex++;
		}
		
		int[] lastIndex = {coIndex};
		remapGapIndices(mNew, lastIndex, tree.getRoot());
	}
	
	/** Called by {@link CTReader#normalizeIndices(CTTree)}. */
	static private void getCoIndexMap(CTNode curr, IntObjectOpenHashMap<List<CTNode>> map)
	{
		if (curr.isPhrase())
		{
			if (curr.coIndex != -1)
			{
				int key = curr.coIndex;
				List<CTNode> list;
				
				if (map.containsKey(key))
					list = map.get(key);
				else
				{
					list = new ArrayList<CTNode>();
					map.put(key, list);
				}
				
				list.add(curr);
			}
			
			for (CTNode child : curr.ls_children)
				getCoIndexMap(child, map);
		}
		else if (curr.isEmptyCategory())
		{
			if (curr.form.equals("*0*"))
				curr.form = "0";
		}
	}
	
	/** Called by {@link CTReader#normalizeIndices(CTTree)}. */
	static private void remapGapIndices(IntIntOpenHashMap map, int[] lastIndex, CTNode curr)
	{
		int gapIndex = curr.gapIndex;
		
		if (map.containsKey(gapIndex))
		{
			curr.gapIndex = map.get(gapIndex);
		}
		else if (gapIndex != -1)
		{
			curr.gapIndex = lastIndex[0];
			map.put(gapIndex, lastIndex[0]++);
		}
		
		for (CTNode child : curr.ls_children)
			remapGapIndices(map, lastIndex, child);
	}
}
