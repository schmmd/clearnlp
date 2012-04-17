package edu.colorado.clear.dependency;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.Set;

import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.feature.xml.DEPFtrXml;
import edu.colorado.clear.util.pair.StringIntPair;

public class DEPParser extends AbstractDEPParser
{ 
	protected StringIntPair[] g_heads;
	
	public DEPParser(DEPFtrXml xml)
	{
		super(xml);
	}
	
	/** Constructs a dependency parser for training. */
	public DEPParser(DEPFtrXml xml, Set<String> sPunc, StringTrainSpace space)
	{
		super(xml, sPunc, space);
	}
	
	/** Constructs a dependency parser for predicting. */
	public DEPParser(DEPFtrXml xml, Set<String> sPunc, StringModel model)
	{
		super(xml, sPunc, model);
	}
	
	/** Constructs a dependency parser for boosting. */
	public DEPParser(DEPFtrXml xml, Set<String> sPunc, StringModel model, StringTrainSpace space)
	{
		super(xml, sPunc, model, space);
	}
	
	/** Constructs a dependency parser for predicting. */
	public DEPParser(DEPFtrXml xml, BufferedReader fin)
	{
		super(xml, fin);
	}
	
	/** Constructs a dependency parser for demo. */
	public DEPParser(PrintStream fout)
	{
		super(fout);
	}
	
	protected void initGoldHeads()
	{
		g_heads = d_tree.getHeads();
	}
	
	/** Called by {@link DEPParser#getGoldLabels()}. */
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
	
	/** Called by {@link DEPParser#getGoldLabels()}. */
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
	
	/** Called by {@link DEPParser#getGoldLabels()}. */
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
	
	/** @return {@code true} if {@code node1} is an ancestor of {@code node2}. */
	protected boolean isAncestor(DEPNode node1, DEPNode node2)
	{
		return node2.isDescendentOf(node1);
	}
	
	protected boolean isLabel(DEPNode node, String label)
	{
		return node.isLabel(label);
	}
	
	protected boolean hasHead(DEPNode node)
	{
		return node.hasHead();
	}
	
	protected void setHead(DEPNode node, DEPNode head, String deprel)
	{
		node.setHead (head, deprel);
	}
}
