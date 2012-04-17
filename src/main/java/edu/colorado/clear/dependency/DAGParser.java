package edu.colorado.clear.dependency;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.Set;

import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.feature.xml.DEPFtrXml;
import edu.colorado.clear.util.pair.StringIntPair;

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
