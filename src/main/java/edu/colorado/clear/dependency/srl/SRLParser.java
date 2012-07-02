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
package edu.colorado.clear.dependency.srl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;

import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.classification.vector.StringFeatureVector;
import edu.colorado.clear.dependency.DEPArc;
import edu.colorado.clear.dependency.DEPLib;
import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.engine.AbstractEngine;
import edu.colorado.clear.feature.xml.FtrToken;
import edu.colorado.clear.feature.xml.SRLFtrXml;
import edu.colorado.clear.util.UTOutput;
import edu.colorado.clear.util.map.Prob1DMap;
import edu.colorado.clear.util.pair.IntIntPair;
import edu.colorado.clear.util.pair.StringIntPair;

public class SRLParser extends AbstractEngine
{
	static public final int MODEL_SIZE	= 2;
	static public final int MODEL_LEFT	= 0;
	static public final int MODEL_RIGHT	= 1;
	
	static protected final int PATH_ALL		= 0;
	static protected final int PATH_UP		= 1;
	static protected final int PATH_DOWN	= 2;
	static protected final int SUBCAT_ALL	= 0;
	static protected final int SUBCAT_LEFT	= 1;
	static protected final int SUBCAT_RIGHT	= 2;
	
	static protected final String LB_NO_ARG = "N";
	
	protected byte					i_flag;
	protected SRLFtrXml				f_xml;
	protected StringTrainSpace[]	s_spaces;
	protected StringModel[]			s_models;
	protected DEPTree				d_tree;
	protected int					i_pred;
	protected int					i_arg;
	protected int					n_preds;
	protected IntIntPair			n_trans;
	protected PrintStream			f_trans;
	
	protected StringIntPair[][]		g_heads;
	protected DEPNode[]				lm_deps, rm_deps;
	protected DEPNode[]				ln_sibs, rn_sibs;
	protected DEPNode				d_lca;
	protected IntOpenHashSet    	s_skip;
	protected List<String>			l_argns;
	
	protected Prob1DMap		m_down, m_up;
	protected Set<String>	s_down, s_up;
	
	protected IntObjectOpenHashMap<IntOpenHashSet> m_coverage;
	
	/** Constructs a semantic role labeler for collecting. */
	public SRLParser()
	{
		i_flag = FLAG_LEXICA;
		m_down = new Prob1DMap();
		m_up   = new Prob1DMap();
	}
	
	/** Constructs a semantic role labeler for training. */
	public SRLParser(SRLFtrXml xml, StringTrainSpace[] spaces, Set<String> sDown, Set<String> sUp)
	{
		i_flag   = FLAG_TRAIN;
		f_xml    = xml;
		s_spaces = spaces;
		s_down   = sDown;
		s_up     = sUp;
	}
	
	/** Constructs a semantic role labeler for predicting. */
	public SRLParser(SRLFtrXml xml, StringModel[] models, Set<String> sDown, Set<String> sUp)
	{
		i_flag   = FLAG_PREDICT;
		f_xml    = xml;
		s_models = models;
		s_down   = sDown;
		s_up     = sUp;
	}
	
	/** Constructs a semantic role labeler for bootstrapping. */
	public SRLParser(SRLFtrXml xml, StringModel[] models, StringTrainSpace[] spaces, Set<String> sDown, Set<String> sUp)
	{
		i_flag   = FLAG_BOOST;
		f_xml    = xml;
		s_models = models;
		s_spaces = spaces;
		s_down   = sDown;
		s_up     = sUp;
	}
	
	/** Constructs a semantic role labeler for demonstration. */
	public SRLParser(PrintStream fout)
	{
		i_flag  = FLAG_DEMO;
		f_trans = fout;
	}
	
	/** Saves a semantic role labeling model to the specific output-stream. */
	public void saveModel(PrintStream fout, int idx)
	{
		s_models[idx].save(fout);
	}
	
	public void saveDownSet(PrintStream fout)
	{
		UTOutput.printSet(fout, s_down);
	}
	
	public void saveUpSet(PrintStream fout)
	{
		UTOutput.printSet(fout, s_up);
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
		n_preds = 0;
		
		if (i_flag != FLAG_PREDICT)
			g_heads = tree.getSHeads();

		initArcs();
		tree.clearSHeads();
		
	//	m_coverage = new IntObjectOpenHashMap<IntOpenHashSet>();
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
	
	private void collect(DEPTree tree)
	{
		DEPNode pred = tree.getNextPredicate(0);
		DEPNode head;
		
		tree.setDependents();
		
		while (pred != null)
		{
			for (DEPArc arc : pred.getGrandDependents())
				collectDown(pred, arc.getNode());
		
			head = pred.getHead();
			if (head != null)	collectUp(pred, head.getHead());
			pred = tree.getNextPredicate(pred.id);
		}
	}
	
	private void collectDown(DEPNode pred, DEPNode arg)
	{
		if (arg.isArgumentOf(pred))
		{
			for (String path : getDUPathList(pred, arg.getHead()))
				m_down.add(path);
		}
		
		for (DEPArc arc : arg.getDependents())
			collectDown(pred, arc.getNode());
	}
	
	private void collectUp(DEPNode pred, DEPNode head)
	{
		if (head == null)	return;
		
		for (DEPArc arc : head.getDependents())
		{
			if (arc.getNode().isArgumentOf(pred))
			{
				for (String path : getDUPathList(head, pred))
					m_up.add(path);
				
				break;
			}
		}	
		
		collectUp(pred, head.getHead());
	}
	
	private String getDUPath(DEPNode top, DEPNode bottom)
	{
		return getPathAux(top, bottom, SRLFtrXml.F_DEPREL, SRLLib.DELIM_PATH_DOWN, true);
	}
	
	private List<String> getDUPathList(DEPNode top, DEPNode bottom)
	{
		List<String> paths = new ArrayList<String>();
		
		while (bottom != top)
		{
			paths.add(getDUPath(top, bottom));
			bottom = bottom.getHead();
		}
		
		return paths;
	}
	
	public Set<String> getDownSet(int cutoff)
	{
		return m_down.toSet(cutoff);
	}
	
	public Set<String> getUpSet(int cutoff)
	{
		return m_up.toSet(cutoff);
	}

	/**
	 * Returns the number of transitions used for labeling the current dependency tree.
	 * @return the number of transitions used for labeling the current dependency tree.
	 */
	public IntIntPair getNumTransitions()
	{
		return n_trans;
	}
	
	public int getNumPredicates()
	{
		return n_preds;
	}
	
	public IntIntPair getArgCoverage(StringIntPair[][] gHeads)
	{
		IntIntPair p = new IntIntPair(0, 0);
		int argId, size = gHeads.length;
		StringIntPair[] preds;
		
		for (argId=1; argId<size; argId++)
		{
			preds = gHeads[argId];
			
			for (StringIntPair pred : preds)
			{
				if (m_coverage.get(pred.i).contains(argId))
					p.i1++;
			}
			
			p.i2 += preds.length;
		}
		
		return p;
	}

	/** Labels the dependency tree. */
	public void label(DEPTree tree)
	{
		if (i_flag == FLAG_LEXICA)
		{
			collect(tree);
			return;
		}
		
		init(tree);
		labelAux();
		
		if (i_flag == FLAG_DEMO)
			f_trans.println();
	}
	
	private void labelAux()
	{
		int size = d_tree.size();
		DEPNode pred;
		
		while (i_pred < size)
		{
			pred = d_tree.get(i_pred);
			n_trans.i1++;
			
			s_skip .clear();
			s_skip .add(i_pred);
			s_skip .add(DEPLib.ROOT_ID);
			l_argns.clear();
			
			d_lca = pred;

			do
			{
				labelAux(pred, d_lca);
				d_lca = d_lca.getHead();
				
			/*	if (d_lca == null)
					break;
				else
				{
					if (!s_skip.contains(d_lca.id))
					{
						i_arg = d_lca.id;
						addArgument(getLabel(getDirIndex()));	
					}
					
					if (pred.isDependentOf(d_lca))
						continue;
					else if (!s_up.contains(getDUPath(d_lca, pred)))
						break;
				}*/
			}
			while (d_lca != null);// && (pred.isDependentOf(d_lca) || s_up.contains(getDUPath(d_lca, pred))));
			
			n_preds++;
			i_pred = getNextPredId(i_pred);
		//	m_coverage.put(i_pred, new IntOpenHashSet(s_skip));
		}
	}
	
	/** Called by {@link SRLParser#label(DEPTree)}. */
	private void labelAux(DEPNode pred, DEPNode head)
	{
		if (!s_skip.contains(head.id))
		{
			i_arg = head.id;
			addArgument(getLabel(getDirIndex()));	
		}
		
		labelDown(pred, head.getDependents());
	}
	
	/** Called by {@link SRLParser#labelAux(DEPNode, IntOpenHashSet)}. */
	private void labelDown(DEPNode pred, List<DEPArc> arcs)
	{
		DEPNode arg;
		
		for (DEPArc arc : arcs)
		{
			arg = arc.getNode();
			
			if (!s_skip.contains(arg.id))
			{
				i_arg = arg.id;
				addArgument(getLabel(getDirIndex()));
				
				if (i_pred == d_lca.id && s_down.contains(getDUPath(pred, arg)))
					labelDown(pred, arg.getDependents());
			}
		}
	}
	
	private int getDirIndex()
	{
		return (i_arg < i_pred) ? MODEL_LEFT : MODEL_RIGHT;
	}
	
	private String getLabel(int idx)
	{
		StringFeatureVector vector = (i_flag != FLAG_DEMO) ? getFeatureVector(f_xml) : null;
		String label = null;
		
		if (i_flag == FLAG_TRAIN)
		{
			label = getGoldArgLabel();
			s_spaces[idx].addInstance(label, vector);
		}
		else if (i_flag == FLAG_PREDICT)
		{
			label = getAutoLabel(idx, vector);
		}
		else if (i_flag == FLAG_BOOST)
		{
			s_spaces[idx].addInstance(getGoldArgLabel(), vector);
			label = getAutoLabel(idx, vector);
		}
		else // if (i_flag == FLAG_TRANSITION)
			label = getGoldArgLabel();

		return label;
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

	/** Called by {@link SRLParser#getLabel(byte)}. */
	private String getAutoLabel(int idx, StringFeatureVector vector)
	{
		return s_models[idx].predictBest(vector).label;
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
			case 0: return (node.isDependentOf(pred))  ? token.field : null;
			case 1: return (pred.isDependentOf(node))  ? token.field : null;
			case 2: return (pred.isDependentOf(d_lca)) ? token.field : null;
			case 3: return (pred == d_lca) ? token.field : null;
			case 4: return (node == d_lca) ? token.field : null;
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
			return getDeprelSet(node.getDependents());
		}
		else if (token.isField(SRLFtrXml.F_GRAND_DEPREL_SET))
		{
			return getDeprelSet(node.getGrandDependents());
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
	
/*	private boolean containsArgument(DEPNode node)
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
	}*/
}
