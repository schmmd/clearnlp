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
import java.util.List;
import java.util.Set;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.feature.xml.DEPFtrXml;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.triple.Triple;

public class DEPParser extends AbstractDEPParser
{ 
	protected DEPFtrXml			f_xml;
	protected StringTrainSpace	s_space;
	protected StringModel		s_model;
	
	/** Constructs a dependency parser for training. */
	public DEPParser(DEPFtrXml xml, Set<String> sPunc, StringTrainSpace space)
	{
		super(FLAG_TRAIN);
		f_xml   = xml;
		s_punc  = sPunc;
		s_space = space;
	}
	
	/** Constructs a dependency parser for cross-validation. */
	public DEPParser(DEPFtrXml xml, Set<String> sPunc, StringModel model)
	{
		super(FLAG_PREDICT);
		f_xml   = xml;
		s_punc  = sPunc;
		s_model = model;
	}
	
	/** Constructs a dependency parser for predicting. */
	public DEPParser(DEPFtrXml xml, BufferedReader fin)
	{
		super(FLAG_PREDICT);
		f_xml  = xml;
		loadModel(fin);
	}
	
	/** Constructs a dependency parser for bootstrapping. */
	public DEPParser(DEPFtrXml xml, Set<String> sPunc, StringModel model, StringTrainSpace space)
	{
		super(FLAG_BOOST);
		f_xml    = xml;
		s_punc   = sPunc;
		s_model  = model;
		s_space  = space;
	}
	
	/** Constructs a dependency parser for demonstration. */
	public DEPParser(PrintStream fout)
	{
		super(FLAG_DEMO);
		f_trans = fout;
	}
	
	/** Loads collections and a dependency parsing model from the specific input reader. */
	public void loadModel(BufferedReader fin)
	{
		try
		{
			s_punc = UTInput.getStringSet(fin);
		}
		catch (Exception e) {e.printStackTrace();}
		
		s_model = new StringModel(fin);
	}
	
	/** Saves collections and a dependency parsing model to the specific output stream. */
	public void saveModel(PrintStream fout)
	{
		UTOutput.printSet(fout, s_punc);
		s_model.save(fout);
	}
	
	/** @return the dependency parsing model. */
	public StringModel getModel()
	{
		return s_model;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.clearnlp.dependency.AbstractDEPParser#getLabels()
	 */
	protected String[] getLabels()
	{
		if (i_flag == FLAG_DEMO)	return getGoldLabels();
		
		StringFeatureVector vector = getFeatureVector(f_xml);
		String[] labels = null;
		
		if (i_flag == FLAG_TRAIN)
		{
			String join = UTArray.join(labels = getGoldLabels(), LB_DELIM);
			s_space.addInstance(join, vector);
		}
		else if (i_flag == FLAG_PREDICT)
		{
			labels = getAutoLabels(vector);
		}
		else if (i_flag == FLAG_BOOST)
		{
			String join = UTArray.join(getGoldLabels(), LB_DELIM);
			s_space.addInstance(join, vector);
			
			labels = getAutoLabels(vector);
		}

		return labels;
	}
	
	/** Called by {@link DEPParser#getLabels()}. */
	private String[] getAutoLabels(StringFeatureVector vector)
	{
		StringPrediction p = s_model.predictBest(vector);
		return p.label.split(LB_DELIM);
	}
	
	protected void postProcessAux(DEPNode node, int dir, Triple<DEPNode,String,Double> max)
	{
		StringFeatureVector vector;
		List<StringPrediction> ps;
		int i, size = d_tree.size();
		String deprel;
		DEPNode head;
		
		if (dir < 0)	i_beta   = node.id;
		else			i_lambda = node.id;
		
		for (i=node.id+dir; 0<=i && i<size; i+=dir)
		{
			head = d_tree.get(i);			
			if (head.isDescendentOf(node))	continue;
			
			if (dir < 0)	i_lambda = i;
			else			i_beta   = i;
			
			vector = getFeatureVector(f_xml);
			ps = s_model.predictAll(vector);
			s_model.toProbability(ps);
			
			for (StringPrediction p : ps)
			{
				if (p.score <= max.o3)
					break;
				
				if ((dir < 0 && p.label.startsWith(LB_RIGHT)) || (dir > 0 && p.label.startsWith(LB_LEFT)))
				{
					deprel = p.label.split(LB_DELIM)[IDX_DEPREL];
					max.set(head, deprel, p.score);
					break;
				}
			}
		}
	}
}
