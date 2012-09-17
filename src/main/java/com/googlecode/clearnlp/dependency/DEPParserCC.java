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
import java.util.regex.Matcher;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.engine.AbstractEngine;
import com.googlecode.clearnlp.feature.xml.DEPFtrXml;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.map.Prob1DMap;
import com.googlecode.clearnlp.util.pair.StringIntPair;
import com.googlecode.clearnlp.util.triple.Triple;


public class DEPParserCC extends AbstractEngine
{ 
	static final protected String LB_LEFT		= "L";
	static final protected String LB_RIGHT		= "R";
	static final protected String LB_NO			= "N";
	static final protected String LB_SHIFT		= "S";
	static final protected String LB_REDUCE		= "R";
	static final protected String LB_PASS		= "P";
	static final protected String LB_DELIM		= "_";
	static final protected int    IDX_ARC		= 0;
	static final protected int    IDX_LIST		= 1;
	static final protected int    IDX_DEPREL	= 2;
	
	protected byte					i_flag;
	protected DEPFtrXml[]			f_xmls;
	protected StringTrainSpace[]	s_spaces;
	protected StringModel[]			s_models;
	protected DEPTree				d_tree;
	protected int					i_lambda;
	protected int					i_beta;
	protected IntOpenHashSet	    s_reduce;
	protected int					n_trans;
	protected PrintStream			f_trans;
	protected Set<String>			s_punc;
	protected Prob1DMap				m_punc;
	protected int					n_models;
	
	protected DEPNode[]       lm_deps, rm_deps;
	protected StringIntPair[] g_heads;
	protected double          d_lower;
	public int[] n_scores = new int[4];
		
	/** Constructs a dependency parser for collecting lexica. */
	public DEPParserCC(DEPFtrXml[] xmls)
	{
		i_flag = FLAG_LEXICA;
		f_xmls = xmls;
		m_punc = new Prob1DMap();
	}
	
	/** Constructs a dependency parser for training. */
	public DEPParserCC(DEPFtrXml[] xmls, Set<String> sPunc, StringTrainSpace[] spaces)
	{
		i_flag   = FLAG_TRAIN;
		f_xmls   = xmls;
		s_punc   = sPunc;
		s_spaces = spaces;
		n_models = f_xmls.length;
	}
	
	/** Constructs a dependency parser for cross-validation. */
	public DEPParserCC(DEPFtrXml[] xmls, Set<String> sPunc, StringModel[] models)
	{
		i_flag   = FLAG_PREDICT;
		f_xmls   = xmls;
		s_punc   = sPunc;
		s_models = models;
		n_models = f_xmls.length;
	}
	
	/** Constructs a dependency parser for predicting. */
	public DEPParserCC(DEPFtrXml[] xmls, BufferedReader fin)
	{
		i_flag   = FLAG_PREDICT;
		f_xmls   = xmls;
		n_models = f_xmls.length;
		loadModel(fin);
	}
	
	/** Constructs a dependency parser for bootstrapping. */
	public DEPParserCC(DEPFtrXml[] xmls, Set<String> sPunc, StringModel[] models, StringTrainSpace[] spaces)
	{
		i_flag   = FLAG_BOOST;
		f_xmls   = xmls;
		s_punc   = sPunc;
		s_models = models;
		s_spaces = spaces;
		n_models = f_xmls.length;
	}
	
	/** Constructs a dependency parser for demonstration. */
	public DEPParserCC(PrintStream fout)
	{
		i_flag  = FLAG_DEMO;
		f_trans = fout;
	}
	
	/** Saves collections and a dependency parsing model to the specific output-stream. */
	public void saveModel(PrintStream fout)
	{
		UTOutput.printSet(fout, s_punc);
		
		for (StringModel model : s_models)
			model.save(fout);
	}
	
	/** Loads collections and a dependency parsing model from the specific input reader. */
	public void loadModel(BufferedReader fin)
	{
		try
		{
			s_punc = UTInput.getStringSet(fin);
		}
		catch (Exception e) {e.printStackTrace();}
		
		s_models = new StringModel[n_models];
		int i;
		
		for (i=0; i<n_models; i++)
			s_models[i] = new StringModel(fin);
	}
	
	/** @return the dependency parsing model. */
	public StringModel[] getModels()
	{
		return s_models;
	}
	
	/**
	 * Collects lexica from the specific dependency tree.
	 * @param tree the dependency tree to collect lexica from.
	 */
	public void collectLexica(DEPTree tree)
	{
		int i, size = tree.size();
		DEPNode node;
		
		String lPunct = f_xmls[0].getPunctuationLabel();
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			if (node.isLabel(lPunct))
				m_punc.add(node.form);
		}
	}
	
	/**
	 * Returns a set containing punctuation.
	 * @return a set containing punctuation.
	 */
	public Set<String> getPunctuationSet()
	{
		return m_punc.toSet(f_xmls[0].getPunctuationCutoff());
	}
	
	/**
	 * Returns the number of transitions used for parsing the current dependency tree.
	 * @return the number of transitions used for parsing the current dependency tree.
	 */
	public int getNumTransitions()
	{
		return n_trans;
	}
	
	/** Parses the dependency tree. */
	public void parse(DEPTree tree)
	{
		init(tree);
		parseAux();

		if (i_flag == FLAG_BOOST)
			tree.addScoreCounts(g_heads, n_scores);
		else if (i_flag == FLAG_PREDICT)
			postProcess();
		else if (i_flag == FLAG_DEMO)
			f_trans.println();
	}
	
	/** Initializes the dependency parser given the specific dependency tree. */
	public void init(DEPTree tree)
	{
		d_tree   = tree;
		n_trans  = 0;
		i_lambda = 0;
		i_beta   = 1;
		s_reduce = new IntOpenHashSet();
		
		if (i_flag != FLAG_PREDICT)
			g_heads = d_tree.getHeads();

		initArcs();
		tree.clearHeads();
	}
	
	/** Initializes dependency arcs of all nodes. */
	private void initArcs()
	{
		int size = d_tree.size();
		
		lm_deps = new DEPNode[size];
		rm_deps = new DEPNode[size];
	}
	
	/** Called by {@link DEPParserCC#parse(DEPTree)}. */
	private void parseAux()
	{
		int size = d_tree.size();
		DEPNode  lambda, beta;
		String[] labels;
		
		while (i_beta < size)
		{
			if (i_lambda < 0)
			{
				noShift();
				continue;
			}
			
			lambda = d_tree.get(i_lambda);
			beta   = d_tree.get(i_beta); 
			labels = getLabels();
			n_trans++;
			
			if (labels[IDX_ARC].equals(LB_LEFT))
			{
				if (i_lambda == DEPLib.ROOT_ID)
					noShift();
				else if (beta.isDescendentOf(lambda))
					noPass();
				else if (labels[IDX_LIST].equals(LB_REDUCE))
					leftReduce(lambda, beta, labels[IDX_DEPREL]);
				else
					leftPass(lambda, beta, labels[IDX_DEPREL]);
			}
			else if (labels[IDX_ARC].equals(LB_RIGHT))
			{
				if (lambda.isDescendentOf(beta))
					noPass();
				else if (labels[IDX_LIST].equals(LB_SHIFT))
					rightShift(lambda, beta, labels[IDX_DEPREL]);
				else
					rightPass(lambda, beta, labels[IDX_DEPREL]);
			}
			else
			{
				if (labels[IDX_LIST].equals(LB_SHIFT))
					noShift();
				else if (labels[IDX_LIST].equals(LB_REDUCE) && lambda.hasHead())
					noReduce();
				else
					noPass();
			}
		}
	}
	
	/**
	 * Returns an array of {arc-label, list-label, dependency-label}.
	 * @return an array of {arc-label, list-label, dependency-label}.
	 */
	private String[] getLabels()
	{
		if (i_flag == FLAG_DEMO)	return getGoldLabels();
		
		StringFeatureVector[] vectors = new StringFeatureVector[n_models];
		String[] labels = null;	int i;
		
		for (i=0; i<n_models; i++)
			vectors[i] = getFeatureVector(f_xmls[i]);
		
		if (i_flag == FLAG_TRAIN)
		{
			labels = getGoldLabels();
			String lb = UTArray.join(labels, LB_DELIM);
			
			for (i=0; i<n_models; i++)
				s_spaces[i].addInstance(lb, vectors[i]);			
		}
		else if (i_flag == FLAG_PREDICT)
		{
			labels = getAutoLabels(vectors);
		}
		else if (i_flag == FLAG_BOOST)
		{
			String lb = UTArray.join(getGoldLabels(), LB_DELIM);
			
			for (i=0; i<n_models; i++)
				s_spaces[i].addInstance(lb, vectors[i]);
			
			labels = getAutoLabels(vectors);
		}
		else // if (i_flag == FLAG_TRANSITION)
			labels = getGoldLabels();

		return labels;
	}
	
	/** Called by {@link DEPParserCC#getLabels()}. */
	private String[] getGoldLabels()
	{
		String[] labels = getGoldLabelArc();
		
		if (labels[IDX_ARC].equals(LB_LEFT))
		{
			labels[IDX_LIST] = isGoldReduce(true) ? LB_REDUCE : LB_PASS;
		}
		else if (labels[IDX_ARC].equals(LB_RIGHT))
		{
			labels[IDX_LIST] = isGoldShift() ? LB_SHIFT : LB_PASS;
		}
		else
		{
			if (isGoldShift())
				labels[IDX_LIST] = LB_SHIFT;
			else if (isGoldReduce(false))
				labels[IDX_LIST] = LB_REDUCE;
			else
				labels[IDX_LIST] = LB_PASS;
		}
		
		return labels;
	}
	
	/** Called by {@link DEPParserCC#getGoldLabels()}. */
	protected String[] getGoldLabelArc()
	{
		StringIntPair head = g_heads[i_lambda];
		String[] labels = new String[3];
		
		if (head.i == i_beta)
		{
			labels[IDX_ARC]    = LB_LEFT;
			labels[IDX_DEPREL] = head.s;
			return labels;
		}
		
		head = g_heads[i_beta];
		
		if (head.i == i_lambda)
		{
			labels[IDX_ARC]    = LB_RIGHT;
			labels[IDX_DEPREL] = head.s;
			return labels;
		}
		
		labels[IDX_ARC]    = LB_NO;
		labels[IDX_DEPREL] = "";
		
		return labels;
	}
	
	/** Called by {@link DEPParserCC#getGoldLabels()}. */
	protected boolean isGoldShift()
	{
		if (g_heads[i_beta].i < i_lambda)
			return false;
		
		int i;
		
		for (i=i_lambda-1; i>0; i--)
		{
			if (s_reduce.contains(i))
				continue;
			
			if (g_heads[i].i == i_beta)
				return false;
		}
		
		return true;
	}
	
	/** Called by {@link DEPParserCC#getGoldLabels()}. */
	protected boolean isGoldReduce(boolean hasHead)
	{
		if (!hasHead && !d_tree.get(i_lambda).hasHead())
			return false;
		
		int i, size = d_tree.size();
		
		for (i=i_beta+1; i<size; i++)
		{
			if (g_heads[i].i == i_lambda)
				return false;
		}
		
		return true;
	}
	
	/** Called by {@link DEPParserCC#getLabels()}. */
	private String[] getAutoLabels(StringFeatureVector[] vector)
	{
		StringPrediction p = null;	int i;
		
		for (i=0; i<n_models; i++)
		{
			p = s_models[i].predictBest(vector[i]);
			if (p.score > d_lower)	break;
		}
		
		return p.label.split(LB_DELIM);
	}
	
	private void leftReduce(DEPNode lambda, DEPNode beta, String deprel)
	{
		if (i_flag == FLAG_DEMO)
			printState("Left-Reduce", i_lambda+" <-"+deprel+"- "+i_beta);
		
		leftArc(lambda, beta, deprel);
		reduce();
	}
	
	private void leftPass(DEPNode lambda, DEPNode beta, String deprel)
	{
		if (i_flag == FLAG_DEMO)
			printState("Left-Pass", i_lambda+" <-"+deprel+"- "+i_beta);
		
		leftArc(lambda, beta, deprel);
		pass();
	}
	
	private void rightShift(DEPNode lambda, DEPNode beta, String deprel)
	{
		if (i_flag == FLAG_DEMO)
			printState("Right-Shift", i_lambda+" -"+deprel+"-> "+i_beta);
		
		rightArc(lambda, beta, deprel);
		shift();
	}
	
	private void rightPass(DEPNode lambda, DEPNode beta, String deprel)
	{
		if (i_flag == FLAG_DEMO)
			printState("Right-Pass", i_lambda+" -"+deprel+"-> "+i_beta);
		
		rightArc(lambda, beta, deprel);
		pass();
	}
	
	private void noShift()
	{
		if (i_flag == FLAG_DEMO)
			printState("No-Shift", null);
		
		shift();
	}
	
	private void noReduce()
	{
		if (i_flag == FLAG_DEMO)
			printState("No-Reduce", null);
		
		reduce();
	}
	
	private void noPass()
	{
		if (i_flag == FLAG_DEMO)
			printState("No-Pass", null);
		
		pass();
	}
	
	protected void leftArc(DEPNode lambda, DEPNode beta, String deprel)
	{
		lambda.setHead(beta, deprel);
		lm_deps[i_beta] = lambda;
	}
	
	protected void rightArc(DEPNode lambda, DEPNode beta, String deprel)
	{
		beta.setHead(lambda, deprel);
		rm_deps[i_lambda] = beta;
	}
	
	private void shift()
	{
		i_lambda = i_beta++;
	}
	
	private void reduce()
	{
		s_reduce.add(i_lambda);
		passAux();
	}
	
	private void pass()
	{
		passAux();
	}
	
	private void passAux()
	{
		int i;
		
		for (i=i_lambda-1; i>=0; i--)
		{
			if (!s_reduce.contains(i))
			{
				i_lambda = i;
				return;
			}
		}
		
		i_lambda = i;
	}
	
	protected void postProcess()
	{
		Triple<DEPNode,String,Double> max = new Triple<DEPNode,String,Double>(null, null, -1d);
		DEPNode root = d_tree.get(DEPLib.ROOT_ID);
		int i, lIdx = n_models-1, size = d_tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = d_tree.get(i);
			
			if (!node.hasHead())
			{
				max.set(root, DEPLibEn.DEP_ROOT, -1d);
				
				postProcessAux(node, -1, max, lIdx);
				postProcessAux(node, +1, max, lIdx);
				
				node.setHead(max.o1, max.o2);
			}
		}
	}
	
	private void postProcessAux(DEPNode node, int dir, Triple<DEPNode,String,Double> max, int lIdx)
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
			
			vector = getFeatureVector(f_xmls[lIdx]);
			ps = s_models[lIdx].predictAll(vector);
			s_models[lIdx].toProbability(ps);
			
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
	
	private void printState(String trans, String deprel)
	{
		StringBuilder build = new StringBuilder();

		// arc-transition
		build.append(trans);
		build.append("\t");
		
		// lambda_1
		build.append("[");
		
		if (i_lambda >= 0)
		{
			if (i_lambda > 0)
				build.append("L1|");
			
			build.append(i_lambda);
		}
		
		build.append("]");
		build.append("\t");
		
		// lambda_2
		build.append("[");
		int lambda2 = getFirstLambda2();
		
		if (i_beta - lambda2 > 0)
		{
			build.append(lambda2);
			
			if (i_beta - lambda2 > 1)
				build.append("|L2");
		}
		
		build.append("]");
		build.append("\t");
		
		// beta
		build.append("[");
		if (i_beta < d_tree.size())
		{
			build.append(i_beta);
			
			if (i_beta+1 < d_tree.size())
				build.append("|B");
		}
		
		build.append("]");
		build.append("\t");
		
		// relation
		if (deprel != null)
			build.append(deprel);
		
		f_trans.println(build.toString());
	}
	
	/** Called by {@link DEPParserCC#printState(String, String)}. */
	private int getFirstLambda2()
	{
		int i;
		
		for (i=i_lambda+1; i<i_beta; i++)
		{
			if (!s_reduce.contains(i))
				return i;
		}
		
		return d_tree.size();
	}
	
	protected String getField(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(DEPFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(DEPFtrXml.F_LEMMA))
		{
			return node.lemma;
		}
		else if (token.isField(DEPFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(DEPFtrXml.F_DEPREL))
		{
			return node.getLabel();
		}
		else if (token.isField(DEPFtrXml.F_LNPL))
		{
			return getLeftNearestPunctuation (0, i_lambda);
		}
		else if (token.isField(DEPFtrXml.F_RNPL))
		{
			return getRightNearestPunctuation(i_lambda, i_beta);
		}
		else if (token.isField(DEPFtrXml.F_LNPB))
		{
			return getLeftNearestPunctuation (i_lambda, i_beta);
		}
		else if (token.isField(DEPFtrXml.F_RNPB))
		{
			return getRightNearestPunctuation(i_beta, d_tree.size());
		}
		else if ((m = DEPFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		else if ((m = DEPFtrXml.P_BOOLEAN.matcher(token.field)).find())
		{
			int field = Integer.parseInt(m.group(1));
			int size  = d_tree.size();
			
			switch (field)
			{
			case 0: return (i_lambda == 1) ? token.field : null;
			case 1: return (i_beta == size-1) ? token.field : null;
			case 2: return (i_lambda+1 == i_beta) ? token.field : null;
			case 3: return d_tree.get(i_lambda).hasHead() ? token.field : null;
			case 4: return d_tree.get(i_beta).hasHead() ? token.field : null;
			}
		}
		
		return null;
	}
	
	protected String[] getFields(FtrToken token)
	{
		return null;
	}
	
	private DEPNode getNode(FtrToken token)
	{
		DEPNode node = null;
		
		switch (token.source)
		{
		case DEPFtrXml.S_STACK : node = getNodeStack(token);	break;
		case DEPFtrXml.S_LAMBDA: node = getNodeLambda(token);	break;
		case DEPFtrXml.S_BETA  : node = getNodeBeta(token);		break;
		}
		
		if (node == null)	return null;
		
		if (token.relation != null)
		{
			     if (token.isRelation(DEPFtrXml.R_H))	node = node.getHead();
			else if (token.isRelation(DEPFtrXml.R_LMD))	node = lm_deps[node.id];
			else if (token.isRelation(DEPFtrXml.R_RMD))	node = rm_deps[node.id];			
		}
		
		return node;
	}
	
	/** Called by {@link DEPParserCC#getNode(FtrToken)}. */
	private DEPNode getNodeStack(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_lambda);
		
		int offset = Math.abs(token.offset), i;
		int dir    = (token.offset < 0) ? -1 : 1;
					
		for (i=i_lambda+dir; 0<i && i<i_beta; i+=dir)
		{
			if (!s_reduce.contains(i) && --offset == 0)
				return d_tree.get(i);
		}
		
		return null;
	}

	/** Called by {@link DEPParserCC#getNode(FtrToken)}. */
	private DEPNode getNodeLambda(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_lambda);
		
		int cIndex = i_lambda + token.offset;
		
		if (0 < cIndex && cIndex < i_beta)
			return d_tree.get(cIndex);
		
		return null;
	}
	
	/** Called by {@link DEPParserCC#getNode(FtrToken)}. */
	private DEPNode getNodeBeta(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_beta);
		
		int cIndex = i_beta + token.offset;
		
		if (i_lambda < cIndex && cIndex < d_tree.size())
			return d_tree.get(cIndex);
		
		return null;
	}
	
	private String getLeftNearestPunctuation(int lIdx, int rIdx)
	{
		String form;
		int i;
		
		for (i=rIdx-1; i>lIdx; i--)
		{
			form = d_tree.get(i).form;
			
			if (s_punc.contains(form))
				return form;
		}
		
		return null;
	}
	
	private String getRightNearestPunctuation(int lIdx, int rIdx)
	{
		String form;
		int i;
		
		for (i=lIdx+1; i<rIdx; i++)
		{
			form = d_tree.get(i).form;
			
			if (s_punc.contains(form))
				return form;
		}
		
		return null;
	}
}
