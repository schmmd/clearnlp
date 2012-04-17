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
package edu.colorado.clear.reader;

import java.util.List;

import edu.colorado.clear.dependency.DEPLib;
import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;

/**
 * Dependency reader.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class SRLReader extends DEPReader
{
	private int i_sheads;

	/**
	 * Constructs a dependency reader.
	 * @param iId the index of the ID field.
	 * @param iForm the index of the form field.
	 * @param iLemma the index of the lemma field.
	 * @param iPos the index of the POS field.
	 * @param iFeats the index of the feats field.
	 * @param iHeadId the index of the head ID field.
	 * @param iDeprel the index of the dependency label field.
	 */
	public SRLReader(int iId, int iForm, int iLemma, int iPos, int iFeats, int iHeadId, int iDeprel, int iSheads)
	{
		super(iId, iForm, iLemma, iPos, iFeats, iHeadId, iDeprel);
		i_sheads = iSheads;
	}
	
	public void init(int iId, int iForm, int iLemma, int iPos, int iFeats, int iHeadId, int iDeprel, int iSheads)
	{
		super.init(iId, iForm, iLemma, iPos, iFeats, iHeadId, iDeprel);
		i_sheads = iSheads;
	}
	
	/* (non-Javadoc)
	 * @see edu.colorado.clear.reader.AbstractReader#next()
	 */
	public DEPTree next()
	{
		DEPTree tree = null;
		
		try
		{
			List<String[]> lines = readLines();
			if (lines == null)	return null;
			
			tree = getDPTree(lines);
			if (i_sheads >= 0)	setSHeads(lines, tree);
		}
		catch (Exception e) {e.printStackTrace();}
		
		return tree;
	}
	
	private void setSHeads(List<String[]> lines, DEPTree tree)
	{
		int i, headId, size = tree.size();
		String heads, label;
		String[] tmp;
		DEPNode node;

		for (i=1; i<size; i++)
		{
			node  = tree.get(i);
			heads = lines.get(i-1)[i_sheads];
			if (heads.equals(AbstractColumnReader.BLANK_COLUMN))	continue;

			for (String head : heads.split(DEPLib.DELIM_HEADS))
			{
				tmp    = head.split(DEPLib.DELIM_HEADS_KEY);
				headId = Integer.parseInt(tmp[0]);
				label  = tmp[1];
				
				node.addSHead(tree.get(headId), label);
			}
		}
	}
}
