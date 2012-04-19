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
package edu.colorado.clear.dependency;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import com.carrotsearch.hppc.IntOpenHashSet;

import edu.colorado.clear.classification.model.AbstractModel;
import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.classification.vector.StringFeatureVector;
import edu.colorado.clear.engine.AbstractTool;
import edu.colorado.clear.feature.xml.FtrToken;
import edu.colorado.clear.feature.xml.SRLFtrXml;
import edu.colorado.clear.util.pair.IntIntPair;
import edu.colorado.clear.util.pair.StringIntPair;

public class SRLParser extends AbstractTool
{
	static public final int MODEL_SIZE	= 2; 
	static public final int MODEL_LEFT	= 0;
	static public final int MODEL_RIGHT	= 1;
	static public final int MODEL_DOWN	= 2;
	static public final int MODEL_UP	= 3;
	
	static protected final int PATH_ALL		= 0;
	static protected final int PATH_UP		= 1;
	static protected final int PATH_DOWN	= 2;
	static protected final int SUBCAT_ALL	= 0;
	static protected final int SUBCAT_LEFT	= 1;
	static protected final int SUBCAT_RIGHT	= 2;
	
	static protected final String LB_NO_ARG = "N";
	
	protected byte					i_flag;
	protected SRLFtrXml[]			f_xmls;
	protected StringTrainSpace[]	s_spaces;
	protected StringModel[]			s_models;
	protected DEPTree				d_tree;
	protected int					i_pred;
	protected int					i_arg;
	protected IntIntPair			n_trans;
	protected PrintStream			f_trans;
	
	protected StringIntPair[][]		g_heads;
	protected DEPNode[]				lm_deps, rm_deps;
	protected DEPNode[]				ln_sibs, rn_sibs;
	protected DEPNode				d_lca;
	protected IntOpenHashSet    	s_skip;
	protected List<String>			l_argns;
	
	/** Constructs a semantic role labeler for training. */
	public SRLParser(SRLFtrXml[] xmls, StringTrainSpace[] spaces)
	{
		i_flag   = FLAG_TRAIN;
		f_xmls   = xmls;
		s_spaces = spaces;
	}
	
	/** Constructs a semantic role labeler for cross-validation. */
	public SRLParser(SRLFtrXml[] xmls, StringModel[] models)
	{
		i_flag   = FLAG_PREDICT;
		f_xmls   = xmls;
		s_models = models;
	}
	
	/** Constructs a semantic role labeler for predicting. */
	public SRLParser(SRLFtrXml[] xmls)
	{
		i_flag   = FLAG_PREDICT;
		f_xmls   = xmls;
		s_models = new StringModel[xmls.length];
	}
	
	/** Constructs a semantic role labeler for bootstrapping. */
	public SRLParser(SRLFtrXml[] xmls, StringModel[] models, StringTrainSpace[] spaces)
	{
		i_flag   = FLAG_BOOST;
		f_xmls   = xmls;
		s_models = models;
		s_spaces = spaces;
	}
	
	/** Constructs a semantic role labeler for demonstration. */
	public SRLParser(PrintStream fout)
	{
		i_flag  = FLAG_DEMO;
		f_trans = fout;
	}
	
	/** Loads a semantic role labeling model from the specific input-reader. */
	public void loadModel(BufferedReader fin, int idx)
	{
		s_models[idx] = new StringModel(fin);
	}
	
	/** Saves a semantic role labeling model to the specific output-stream. */
	public void saveModel(PrintStream fout, int idx)
	{
		s_models[idx].save(fout);
	}
	
	/** @return the semantic role labeling models. */
	public StringModel[] getModels()
	{
		return s_models;
	}
	
	/** Initializes the semantic role labeler given the specific dependency tree. */
	public void init(DEPTree tree)
	{
		d_tree  = tree;
		i_pred  = getNextPredId(0);
		s_skip  = new IntOpenHashSet();
		l_argns = new ArrayList<String>();
		n_trans = new IntIntPair(0, 0);
		
		if (i_flag != FLAG_PREDICT)
			g_heads = tree.getSHeads();

		initArcs();
		tree.clearSHeads();
	}
	
	/** @return the ID of the next predicate. */
	private int getNextPredId(int prevId)
	{
		DEPNode pred = d_tree.getNextPredicate(prevId);
		return (pred != null) ? pred.id : d_tree.size();
	}
	
	/** Initializes dependency arcs of all nodes. */
	private void initArcs()
	{
		int i, j, len, size = d_tree.size();
		DEPNode curr, prev, next;
		List<DEPArc> deps;
		DEPArc lmd, rmd;
		
		lm_deps = new DEPNode[size];
		rm_deps = new DEPNode[size];
		ln_sibs = new DEPNode[size];
		rn_sibs = new DEPNode[size];
		
		d_tree.setDependents();
		
		for (i=1; i<size; i++)
		{
			deps = d_tree.get(i).getDependents();
			if (deps.isEmpty())	continue;
			
			len = deps.size(); 
			lmd = deps.get(0);
			rmd = deps.get(len-1);
			
			if (lmd.getNode().id < i)	lm_deps[i] = lmd.getNode();
			if (rmd.getNode().id > i)	rm_deps[i] = rmd.getNode();
			
			for (j=1; j<len; j++)
			{
				curr = deps.get(j  ).getNode();
				prev = deps.get(j-1).getNode();

				if (ln_sibs[curr.id] == null || ln_sibs[curr.id].id < prev.id)
					ln_sibs[curr.id] = prev;
			}
			
			for (j=0; j<len-1; j++)
			{
				curr = deps.get(j  ).getNode();
				next = deps.get(j+1).getNode();

				if (rn_sibs[curr.id] == null || rn_sibs[curr.id].id > next.id)
					rn_sibs[curr.id] = next;
			}
		}
	}

	/**
	 * Returns the number of transitions used for labeling the current dependency tree.
	 * @return the number of transitions used for labeling the current dependency tree.
	 */
	public IntIntPair getNumTransitions()
	{
		return n_trans;
	}

	/** Labels the dependency tree. */
	public void label(DEPTree tree)
	{
		init(tree);
		
		int size = tree.size();
		DEPNode pred;
		
		while (i_pred < size)
		{
			pred = tree.get(i_pred);
			n_trans.i1++;
			
			s_skip .clear();
			s_skip .add(i_pred);
			s_skip .add(DEPLib.ROOT_ID);
			l_argns.clear();
			
			d_lca = pred;

			do
			{
				labelAux(d_lca);
				d_lca = d_lca.getHead();				
			}
			while (d_lca != null);
			
			i_pred = getNextPredId(i_pred);
		}
		
		if (i_flag == FLAG_DEMO)
			f_trans.println();
	}
	
	/** Called by {@link SRLParser#label(DEPTree)}. */
	private void labelAux(DEPNode head)
	{
		if (!s_skip.contains(head.id))
		{
			i_arg = head.id;
			addArgument(getLabel(getDirIndex()));	
		}
		
		labelDown(head.getDependents());
	}
	
	/** Called by {@link SRLParser#labelAux(DEPNode, IntOpenHashSet)}. */
	private void labelDown(List<DEPArc> arcs)
	{
		for (DEPArc arc : arcs)
		{
			if (!s_skip.contains(arc.getNode().id))
			{
				i_arg = arc.getNode().id;
				addArgument(getLabel(getDirIndex()));
			
				if (i_pred == d_lca.id)
			//	if (getLabel(MODEL_DOWN).equals(AbstractModel.LABEL_TRUE))
					labelDown(arc.getNode().getDependents());
			}
		}
	}
	
	private int getDirIndex()
	{
		return (i_arg < i_pred) ? MODEL_LEFT : MODEL_RIGHT;
	}
	
	private String getLabel(int idx)
	{
		StringFeatureVector vector = (i_flag != FLAG_DEMO) ? getFeatureVector(f_xmls[idx]) : null;
		String label = null;
		
		if (i_flag == FLAG_TRAIN)
		{
			label = getGoldLabel(idx);
			s_spaces[idx].addInstance(label, vector);
		}
		else if (i_flag == FLAG_PREDICT)
		{
			label = getAutoLabel(idx, vector);
		}
		else if (i_flag == FLAG_BOOST)
		{
			s_spaces[idx].addInstance(getGoldLabel(idx), vector);
			label = getAutoLabel(idx, vector);
		}
		else // if (i_flag == FLAG_TRANSITION)
			label = getGoldLabel(idx);

		return label;
	}
	
	/** Called by {@link SRLParser#getLabel(byte)}. */
	private String getGoldLabel(int idx)
	{
		switch (idx)
		{
		case MODEL_LEFT : return getGoldArgLabel();
		case MODEL_RIGHT: return getGoldArgLabel();
		case MODEL_DOWN : return getGoldDownLabel();
		case MODEL_UP   : return getGoldUpLabel();
		}
		
		return null;
	}
	
	/** Called by {@link SRLParser#getGoldLabel(byte)}. */
	private String getGoldArgLabel()
	{
		for (StringIntPair head : g_heads[i_arg])
		{
			if (head.i == i_pred)
				return head.s;
		}
		
		return LB_NO_ARG;
	}
	
	/** Called by {@link SRLParser#getGoldLabel(byte)}. */
	private String getGoldDownLabel()
	{
		return containsArgument(d_tree.get(i_arg)) ? AbstractModel.LABEL_TRUE : AbstractModel.LABEL_FALSE;
	}
	
	/** Called by {@link SRLParser#getGoldLabel(byte)}. */
	private String getGoldUpLabel()
	{
		return null;
	}

	/** Called by {@link SRLParser#getLabel(byte)}. */
	private String getAutoLabel(int idx, StringFeatureVector vector)
	{
		return s_models[idx].predictBest(vector).label;
	}

	private boolean containsArgument(DEPNode node)
	{
		DEPNode dep;
		
		for (DEPArc arc : node.getDependents())
		{
			dep = arc.getNode();
			
			for (StringIntPair p : g_heads[dep.id])
			{
				if (p.i == i_pred)
					return true;
			}
			
			if (containsArgument(dep))
				return true;
		}
		
		return false;
	}
	
	private void addArgument(String label)
	{
		s_skip.add(i_arg);
		n_trans.i2++;
		
		if (i_flag == FLAG_DEMO)
			printState(label);
		
		if (!label.equals(LB_NO_ARG))
		{
			DEPNode pred = d_tree.get(i_pred);
			DEPNode arg  = d_tree.get(i_arg);
			
			arg.addSHead(pred, label);
			
			if (SRLLib.isNumberedArgument(label))
				l_argns.add(label);
		}
	}
	
	private void printState(String label)
	{
		StringBuilder build = new StringBuilder();
		
		build.append(i_pred);
		build.append(" -");
		build.append(label);
		build.append("-> ");	
		build.append(i_arg);
		
		f_trans.println(build.toString());
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
		else if (token.isField(SRLFtrXml.F_DISTANCE))
		{
			return getDistance(node);
		}
		else if ((m = SRLFtrXml.P_ARGN.matcher(token.field)).find())
		{
			int idx = l_argns.size() - Integer.parseInt(m.group(1)) - 1;
			return (idx >= 0) ? l_argns.get(idx) : null;
		}
		else if ((m = SRLFtrXml.P_PATH.matcher(token.field)).find())
		{
			String type = m.group(1);
			int    dir  = Integer.parseInt(m.group(2));
			
			return getPath(type, dir);
		}
		else if ((m = SRLFtrXml.P_SUBCAT.matcher(token.field)).find())
		{
			String type = m.group(1);
			int    dir  = Integer.parseInt(m.group(2));
			
			return getSubcat(node, type, dir);
		}
		else if ((m = SRLFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		else if ((m = SRLFtrXml.P_BOOLEAN.matcher(token.field)).find())
		{
			DEPNode pred = d_tree.get(i_pred);
			int    field = Integer.parseInt(m.group(1));
			
			switch (field)
			{
			case 0: return (node.isDependentOf(pred)) ? token.field : null;
			case 1: return (pred.isDependentOf(node)) ? token.field : null;
			case 2: return (pred == d_lca) ? token.field : null;
			case 3: return (node == d_lca) ? token.field : null;
			}
		}
		
		return null;
	}
	
	protected String[] getFields(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		
		if (token.isField(SRLFtrXml.F_DEPREL_SET))
		{
			List<DEPArc> deps = node.getDependents();
			if (deps.isEmpty())	return null;
			
			Set<String> set = new HashSet<String>();
			for (DEPArc arc : deps)	set.add(arc.label);
			
			String[] fields = new String[set.size()];
			set.toArray(fields);
			return fields;
		}
		
		return null;
	}
	
	private String getDistance(DEPNode node)
	{
		int dist = Math.abs(i_pred - node.id);
		
		if      (dist <=  5)	return "0";
		else if (dist <= 10)	return "1";
		else if (dist <= 15)	return "2";
		else					return "3";
	}
	
	private String getPath(String type, int dir)
	{
		DEPNode pred = d_tree.get(i_pred);
		DEPNode arg  = d_tree.get(i_arg);
		
		if (dir == PATH_UP)
		{
			if (d_lca != pred)
				return getPathAux(d_lca, pred, type, SRLLib.DELIM_PATH_UP, true);
		}
		else if (dir == PATH_DOWN)
		{
			if (d_lca != arg)
				return getPathAux(d_lca, arg, type, SRLLib.DELIM_PATH_DOWN, true);
		}
		else
		{
			if (pred == d_lca)
				return getPathAux(pred, arg, type, SRLLib.DELIM_PATH_DOWN, true);
			else if (pred.isDescendentOf(arg))
				return getPathAux(arg, pred, type, SRLLib.DELIM_PATH_UP, true);
			else
			{
				String path = getPathAux(d_lca, pred, type, SRLLib.DELIM_PATH_UP, true);
				path += getPathAux(d_lca, arg, type, SRLLib.DELIM_PATH_DOWN, false);
				
				return path;
			}			
		}
		
		return null;
	}
	
	private String getPathAux(DEPNode top, DEPNode bottom, String type, String delim, boolean includeTop)
	{
		StringBuilder build = new StringBuilder();
		DEPNode head = bottom;
		int dist = 0;
		
		do
		{
			if (type.equals(SRLFtrXml.F_POS))
			{
				build.append(delim);
				build.append(head.pos);
			}
			else if (type.equals(SRLFtrXml.F_DEPREL))
			{
				build.append(delim);
				build.append(head.getLabel());
			}
			else if (type.equals(SRLFtrXml.F_DISTANCE))
			{
				dist++;
			}
		
			head = head.getHead();
		}
		while (head != top);
		
		if (type.equals(SRLFtrXml.F_POS))
		{
			if (includeTop)
			{
				build.append(delim);
				build.append(top.pos);	
			}
		}
		else if (type.equals(SRLFtrXml.F_DISTANCE))
		{
			build.append(delim);
			build.append(dist);
		}
		
		return build.length() == 0 ? null : build.toString();
	}
	
	private String getSubcat(DEPNode node, String type, int dir)
	{
		List<DEPArc>  deps  = node.getDependents();
		StringBuilder build = new StringBuilder();
		int i, size = deps.size();
		DEPNode dep;
		
		if (dir == SUBCAT_LEFT)
		{
			for (i=0; i<size; i++)
			{
				dep = deps.get(i).getNode();
				if (dep.id > node.id)	break;
				getSubcatAux(build, dep, type);
			}
		}
		else if (dir == SUBCAT_RIGHT)
		{
			for (i=size-1; i>=0; i--)
			{
				dep = deps.get(i).getNode();
				if (dep.id < node.id)	break;
				getSubcatAux(build, dep, type);
			}
		}
		else
		{
			for (i=0; i<size; i++)
			{
				dep = deps.get(i).getNode();
				getSubcatAux(build, dep, type);
			}
		}
		
		return build.length() == 0 ? null : build.substring(SRLLib.DELIM_SUBCAT.length());
	}
	
	private void getSubcatAux(StringBuilder build, DEPNode node, String type)
	{
		build.append(SRLLib.DELIM_SUBCAT);
		
		if (type.equals(SRLFtrXml.F_POS))
			build.append(node.pos);
		else if (type.equals(SRLFtrXml.F_DEPREL))
			build.append(node.getLabel());
	}
	
	private DEPNode getNode(FtrToken token)
	{
		DEPNode node = null;
		
		switch (token.source)
		{
		case SRLFtrXml.S_PREDICAT: node = d_tree.get(i_pred);	break;
		case SRLFtrXml.S_ARGUMENT: node = d_tree.get(i_arg);	break;
		}
		
		if (token.relation != null)
		{
			     if (token.isRelation(SRLFtrXml.R_H))	node = node.getHead();
			else if (token.isRelation(SRLFtrXml.R_LMD))	node = lm_deps[node.id];
			else if (token.isRelation(SRLFtrXml.R_RMD))	node = rm_deps[node.id];			
			else if (token.isRelation(SRLFtrXml.R_LNS))	node = ln_sibs[node.id];
			else if (token.isRelation(SRLFtrXml.R_RNS))	node = rn_sibs[node.id];
		}
		
		return node;
	}
}
