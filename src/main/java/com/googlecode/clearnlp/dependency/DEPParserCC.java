/**
 * Copyright 2012 University of Massachusetts Amherst
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.googlecode.clearnlp.util.pair.Pair;
import com.googlecode.clearnlp.util.triple.Triple;

public class DEPParserCC extends AbstractDEPParser
{ 
	protected DEPFtrXml[]			f_xmls;
	protected StringTrainSpace[]	s_spaces;
	protected StringModel[]			s_models;
	protected int					n_models;
	protected double                d_lower;
	
	public int n_total = 0, n_1st = 0;
	
	/** Constructs a dependency parser for training. */
	public DEPParserCC(DEPFtrXml[] xmls, Set<String> sPunc, StringTrainSpace[] spaces)
	{
		super(FLAG_TRAIN);
		f_xmls   = xmls;
		s_punc   = sPunc;
		s_spaces = spaces;
		n_models = f_xmls.length;
	}
	
	/** Constructs a dependency parser for cross-validation. */
	public DEPParserCC(DEPFtrXml[] xmls, Set<String> sPunc, StringModel[] models, double lowerBound)
	{
		super(FLAG_PREDICT);
		f_xmls   = xmls;
		s_punc   = sPunc;
		s_models = models;
		n_models = f_xmls.length;
		d_lower  = lowerBound;
	}
	
	/** Constructs a dependency parser for predicting. */
	public DEPParserCC(DEPFtrXml[] xmls, BufferedReader fin, double lowerBound)
	{
		super(FLAG_PREDICT);
		f_xmls   = xmls;
		n_models = f_xmls.length;
		d_lower  = lowerBound;
		loadModel(fin);
	}
	
	/** Constructs a dependency parser for bootstrapping. */
	public DEPParserCC(DEPFtrXml[] xmls, Set<String> sPunc, StringModel[] models, StringTrainSpace[] spaces, double lowerBound)
	{
		super(FLAG_BOOST);
		f_xmls   = xmls;
		s_punc   = sPunc;
		s_models = models;
		s_spaces = spaces;
		n_models = f_xmls.length;
		d_lower  = lowerBound;
	}
	
	/** Constructs a dependency parser for demonstration. */
	public DEPParserCC(PrintStream fout)
	{
		super(FLAG_DEMO);
		f_trans = fout;
	}
	
	/** Loads collections and a dependency parsing model from the specific input reader. */
	public void loadModel(BufferedReader fin)
	{
		try
		{
			s_punc = getStringSet(fin);
		}
		catch (Exception e) {e.printStackTrace();}
		
		s_models = new StringModel[n_models];
		int i;
		
		for (i=0; i<n_models; i++)
			s_models[i] = new StringModel(fin);
	}
	
	/** Saves collections and a dependency parsing model to the specific output-stream. */
	public void saveModel(PrintStream fout)
	{
		printSet(fout, s_punc);
		
		for (StringModel model : s_models)
			model.save(fout);
	}
	
	/** @return the dependency parsing model. */
	public StringModel[] getModels()
	{
		return s_models;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.clearnlp.dependency.AbstractDEPParser#getLabels()
	 */
	protected String[] getLabels()
	{
		if (i_flag == FLAG_DEMO)	return getGoldLabels();
		
		StringFeatureVector[] vectors = new StringFeatureVector[n_models];
		String[] labels = null;	int i;
		
		for (i=0; i<n_models; i++)
			vectors[i] = getFeatureVector(f_xmls[i]);
		
		if (i_flag == FLAG_TRAIN)
		{
			String join = UTArray.join(labels = getGoldLabels(), LB_DELIM);
			
			for (i=0; i<n_models; i++)
				s_spaces[i].addInstance(join, vectors[i]);			
		}
		else if (i_flag == FLAG_PREDICT)
		{
			labels = getAutoLabels(vectors).o1;
		}
		else if (i_flag == FLAG_BOOST)
		{
			String[] gLabels = getGoldLabels();
			String join = UTArray.join(gLabels, LB_DELIM);
			
			for (i=0; i<n_models; i++)
				s_spaces[i].addInstance(join, vectors[i]);
			
			labels = getAutoLabels(vectors).o1;
		}

		return labels;
	}
	
	/** Called by {@link DEPParserCC#getLabels()}. */	
	private Pair<String[],Integer> getAutoLabels(StringFeatureVector[] vectors)
	{
		List<StringPrediction> ps;
		StringPrediction p = null;
		int i;
		
		for (i=0; i<n_models; i++)
		{
			ps = s_models[i].predictAll(vectors[i]);
			
			if ((p = ps.get(0)).score > 0 && ps.get(1).score <= 0)
				break;
		}
		
		n_total++;
		
		if (i == 0)
		{
			n_1st++;
		}
		
		if (i == n_models)	i--;
		return new Pair<String[],Integer>(p.label.split(LB_DELIM), i);
	}
	
	protected void postProcessAux(DEPNode node, int dir, Triple<DEPNode,String,Double> max)
	{
		int i, size = d_tree.size(), idx = n_models - 1;
		StringFeatureVector vector;
		List<StringPrediction> ps;
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
			
			vector = getFeatureVector(f_xmls[idx]);
			ps = s_models[idx].predictAll(vector);
			s_models[idx].toProbability(ps);
			
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
