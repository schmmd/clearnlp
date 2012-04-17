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

import edu.colorado.clear.dependency.DEPFeat;
import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;

/**
 * Dependency reader.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPReader extends AbstractColumnReader<DEPTree>
{
	private int i_id;
	private int i_form;
	private int i_lemma;
	private int i_pos;
	private int i_feats;
	private int i_headId;
	private int i_deprel;
	
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
	public DEPReader(int iId, int iForm, int iLemma, int iPos, int iFeats, int iHeadId, int iDeprel)
	{
		init(iId, iForm, iLemma, iPos, iFeats, iHeadId, iDeprel);
	}
	
	public void init(int iId, int iForm, int iLemma, int iPos, int iFeats, int iHeadId, int iDeprel)
	{
		i_id     = iId;
		i_form   = iForm;
		i_lemma  = iLemma;
		i_pos    = iPos;
		i_feats  = iFeats;
		i_headId = iHeadId;
		i_deprel = iDeprel;
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
		}
		catch (Exception e) {e.printStackTrace();}
		
		return tree;
	}
	
	protected DEPTree getDPTree(List<String[]> lines)
	{
		int id, headId, i, size = lines.size();
		String form, lemma, pos, deprel;
		DEPFeat feats;
		String[] tmp;
		DEPNode node;
		
		DEPTree tree = new DEPTree();
		for (i=0; i<size; i++)	tree.add(new DEPNode());
		
		for (i=0; i<size; i++)
		{
			tmp   = lines.get(i);
			id    = Integer.parseInt(tmp[i_id]);
			form  = tmp[i_form];
			lemma = tmp[i_lemma];
			pos   = tmp[i_pos];
			feats = new DEPFeat(tmp[i_feats]);
			
			node = tree.get(id);
			node.init(id, form, lemma, pos, feats);
					
			if (i_headId >= 0 && !tmp[i_headId].equals(AbstractColumnReader.BLANK_COLUMN))
			{
				headId = Integer.parseInt(tmp[i_headId]);
				deprel = tmp[i_deprel];
				
				node.setHead(tree.get(headId), deprel);
			}
		}
		
		return tree;
	}
}
