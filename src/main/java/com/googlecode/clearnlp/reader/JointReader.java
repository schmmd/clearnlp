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
package com.googlecode.clearnlp.reader;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPFeat;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;


/**
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class JointReader extends AbstractColumnReader<DEPTree>
{
	protected int i_id;
	protected int i_form;
	protected int i_lemma;
	protected int i_pos;
	protected int i_feats;
	protected int i_headId;
	protected int i_deprel;
	protected int i_sheads;
	
	/**
	 * Constructs a dependency reader.
	 * @param iId the column index of the node ID field.
	 * @param iForm the column index of the word-form field.
	 * @param iLemma the column index of the lemma field.
	 * @param iPos the column index of the POS field.
	 * @param iFeats the column index of the feats field.
	 * @param iHeadId the column index of the head ID field.
	 * @param iDeprel the column index of the dependency label field.
	 * @param iSHeads the column index of the semantic head field.
	 */
	public JointReader(int iId, int iForm, int iLemma, int iPos, int iFeats, int iHeadId, int iDeprel, int iSHeads)
	{
		init(iId, iForm, iLemma, iPos, iFeats, iHeadId, iDeprel, iSHeads);
	}
	
	public void init(int iId, int iForm, int iLemma, int iPos, int iFeats, int iHeadId, int iDeprel, int iSHeads)
	{
		i_id     = iId;
		i_form   = iForm;
		i_lemma  = iLemma;
		i_pos    = iPos;
		i_feats  = iFeats;
		i_headId = iHeadId;
		i_deprel = iDeprel;
		i_sheads = iSHeads;
	}
	
	@Override
	public DEPTree next()
	{
		DEPTree tree = null;
		
		try
		{
			List<String[]> lines = readLines();
			if (lines == null)	return null;
			
			tree = getDEPTree(lines);
		}
		catch (Exception e) {e.printStackTrace();}
		
		return tree;
	}

	protected DEPTree getDEPTree(List<String[]> lines)
	{
		String form, lemma, pos, feats;
		int id, i, size = lines.size();
		DEPTree tree = new DEPTree();
		DEPNode node;
		String[] tmp;
		
		// initialize place holders
		for (i=0; i<size; i++)
			tree.add(new DEPNode());
		
		for (i=0; i<size; i++)
		{
			tmp    = lines.get(i);
			form   = tmp[i_form];
			id     = (i_id    < 0) ? i+1  : Integer.parseInt(tmp[i_id]);
			lemma  = (i_lemma < 0) ? null : tmp[i_lemma];
			pos    = (i_pos   < 0) ? null : tmp[i_pos];
			feats  = (i_feats < 0) ? AbstractColumnReader.BLANK_COLUMN : tmp[i_feats];

			node = tree.get(id);
			node.init(id, form, lemma, pos, new DEPFeat(feats));
			
			if (i_headId >= 0 && !tmp[i_headId].equals(AbstractColumnReader.BLANK_COLUMN))
				node.setHead(tree.get(Integer.parseInt(tmp[i_headId])), tmp[i_deprel]);
			
			if (i_sheads >= 0 && !tmp[i_sheads].equals(AbstractColumnReader.BLANK_COLUMN))
				node.setSHeads(getSHeads(tree, tmp[i_sheads]));
		}
		
		return tree;
	}
	
	private List<DEPArc> getSHeads(DEPTree tree, String heads)
	{
		List<DEPArc> sHeads = new ArrayList<DEPArc>();
		int headId, idx;
		String label;
		
		for (String head : heads.split(DEPLib.DELIM_HEADS))
		{
			idx    = head.indexOf(DEPLib.DELIM_HEADS_KEY);
			headId = Integer.parseInt(head.substring(0, idx));
			label  = head.substring(idx+1);
			
			sHeads.add(new DEPArc(tree.get(headId), label));
		}
		
		return sHeads;
	}
}
