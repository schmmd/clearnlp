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

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.Set;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.feature.xml.DEPFtrXml;
import com.googlecode.clearnlp.util.pair.StringIntPair;


public class DAGParser extends AbstractDEPParser
{ 
	private StringIntPair[][] g_heads;
	
	public DAGParser(DEPFtrXml xml)
	{
		super(xml);
	}
	
	/** Constructs a dependency parser for training. */
	public DAGParser(DEPFtrXml xml, Set<String> sPunc, StringTrainSpace space)
	{
		super(xml, sPunc, space);
	}
	
	/** Constructs a dependency parser for predicting. */
	public DAGParser(DEPFtrXml xml, Set<String> sPunc, StringModel model)
	{
		super(xml, sPunc, model);
	}
	
	/** Constructs a dependency parser for boosting. */
	public DAGParser(DEPFtrXml xml, Set<String> sPunc, StringModel model, StringTrainSpace space)
	{
		super(xml, sPunc, model, space);
	}
	
	public DAGParser(DEPFtrXml xml, BufferedReader fin)
	{
		super(xml, fin);
	}
	
	public DAGParser(PrintStream fout)
	{
		super(fout);
	}
	
	protected void initGoldHeads()
	{
		g_heads = d_tree.getXHeads();
	}
	
	protected String[] getGoldLabelArc()
	{
		String[] labels = new String[3];
		
		for (StringIntPair head : g_heads[i_lambda])
		{
			if (head.i == i_beta)
			{
				labels[IDX_ARC]    = LB_LEFT;
				labels[IDX_DEPREL] = head.s;
				return labels;
			}	
		}
		
		for (StringIntPair head : g_heads[i_beta])
		{
			if (head.i == i_lambda)
			{
				labels[IDX_ARC]    = LB_RIGHT;
				labels[IDX_DEPREL] = head.s;
				return labels;
			}	
		}
		
		labels[IDX_ARC]    = LB_NO;
		labels[IDX_DEPREL] = "";
		
		return labels;
	}
	
	protected boolean isGoldShift()
	{
		for (StringIntPair head : g_heads[i_beta])
		{
			if (head.i < i_lambda)
				return false;
		}
		
		int i;
		
		for (i=i_lambda-1; i>0; i--)
		{
			if (s_reduce.contains(i))
				continue;
			
			if (isGoldHead(i_beta, i))
				return false;
		}
		
		return true;
	}

	protected boolean isGoldReduce(boolean hasHead)
	{
		if (!hasHead && !d_tree.get(i_lambda).hasXHead())
			return false;
		
		int i, size = d_tree.size();
		
		for (i=i_beta+1; i<size; i++)
		{
			if (isGoldHead(i_lambda, i) || isGoldHead(i, i_lambda))
				return false;
		}
		
		return true;
	}
	
	private boolean isGoldHead(int id1, int id2)
	{
		for (StringIntPair head : g_heads[id2])
		{
			if (id1 == head.i)
				return true;
		}
		
		return false;
	}

	protected boolean isAncestor(DEPNode node1, DEPNode node2)
	{
		return node2.isXDescendentOf(node1);
	}
	
	protected boolean isLabel(DEPNode node, String label)
	{
		for (DEPArc arc : node.getXHeads())
		{
			if (arc.isLabel(label))
				return true;
		}
		
		return false;
	}

	protected boolean hasHead(DEPNode node)
	{
		return node.hasXHead();
	}

	protected void setHead(DEPNode node, DEPNode head, String deprel)
	{
		node.addXHead(head, deprel);
	}
}
