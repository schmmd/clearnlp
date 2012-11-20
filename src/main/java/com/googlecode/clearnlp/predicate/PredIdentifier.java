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
package com.googlecode.clearnlp.predicate;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import com.googlecode.clearnlp.classification.model.AbstractModel;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.feature.xml.SRLFtrXml;

/**
 * @since 1.0.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class PredIdentifier extends AbstractPredIdentifier
{
	private SRLFtrXml			f_xml;
	private StringTrainSpace	s_space;
	private StringModel			s_model;
	private DEPTree				d_tree;
	private int					i_pred;
	
	/** Constructs a predicate identifier for training. */
	public PredIdentifier(SRLFtrXml xml, StringTrainSpace space)
	{
		super(FLAG_TRAIN);
		f_xml   = xml;
		s_space = space;
	}
	
	/** Constructs a predicate identifier for predicting. */
	public PredIdentifier(SRLFtrXml xml, StringModel model)
	{
		super(FLAG_PREDICT);
		f_xml   = xml;
		s_model = model;
	}
	
	/** Constructs a predicate identifier for predicting. */
	public PredIdentifier(SRLFtrXml xml, BufferedReader fin)
	{
		super(FLAG_PREDICT);
		f_xml   = xml;
		s_model = new StringModel(fin);
	}
	
	/** Called by {@link PredIdentifier#parse(DEPTree)}. */
	public void init(DEPTree tree)
	{
		d_tree = tree;
		tree.setDependents();
		
		if (i_flag == FLAG_PREDICT)
			tree.clearPredicates();
	}
	
	public void saveModel(PrintStream fout)
	{
		s_model.save(fout);
	}
	
	public StringModel getModel()
	{
		return s_model;
	}
	
	public void identify(DEPTree tree)
	{
		init(tree);

		int size = d_tree.size();
		DEPNode node;
		
		for (i_pred=1; i_pred<size; i_pred++)
		{
			node = tree.get(i_pred);
			
			if (f_xml.isPredicate(node))
				identifyAux(node);
		}
	}
	
	private void identifyAux(DEPNode node)
	{
		StringFeatureVector vector = getFeatureVector(f_xml);
		String label;

		if (i_flag == FLAG_TRAIN)
		{
			label = (node.getFeat(DEPLib.FEAT_PB) == null) ? AbstractModel.LABEL_FALSE :  AbstractModel.LABEL_TRUE;
			s_space.addInstance(label, vector);
		}
		else if (i_flag == FLAG_PREDICT)
		{
			label = s_model.predictBest(vector).label;
			
			if (label.equals(AbstractModel.LABEL_TRUE))
				node.addFeat(DEPLib.FEAT_PB, node.lemma+".XX");
		}
	}
	
	protected String getField(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(SRLFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(SRLFtrXml.F_LEMMA))
		{
			return node.lemma;
		}
		else if (token.isField(SRLFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(SRLFtrXml.F_DEPREL))
		{
			return node.getLabel();
		}
		else if ((m = SRLFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		
		return null;
	}
	
	protected String[] getFields(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		
		if (token.isField(SRLFtrXml.F_DEPREL_SET))
		{
			return getDeprelSet(node.getDependents());
		}
		
		return null;
	}
	
	private String[] getDeprelSet(List<DEPArc> deps)
	{
		if (deps.isEmpty())	return null;
		
		Set<String> set = new HashSet<String>();
		for (DEPArc arc : deps)	set.add(arc.getLabel());
		
		String[] fields = new String[set.size()];
		set.toArray(fields);
		
		return fields;		
	}
	
	private DEPNode getNode(FtrToken token)
	{
		DEPNode node = getNodeAux(token);
		if (node == null)	return null;
		
		if (token.relation != null)
		{
			     if (token.isRelation(SRLFtrXml.R_H))	node = node.getHead();
			else if (token.isRelation(SRLFtrXml.R_LMD))	node = node.getLeftMostDependent();
			else if (token.isRelation(SRLFtrXml.R_RMD))	node = node.getRightMostDependent();
		}
		
		return node;
	}
	
	/** Called by {@link PredIdentifier#getNode(FtrToken)}. */
	private DEPNode getNodeAux(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_pred);
		
		int cIndex = i_pred + token.offset;
		
		if (0 < cIndex && cIndex < d_tree.size())
			return d_tree.get(cIndex);
		
		return null;
	}
}
