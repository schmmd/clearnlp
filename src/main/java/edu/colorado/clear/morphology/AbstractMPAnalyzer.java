package edu.colorado.clear.morphology;

import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.pos.POSNode;

abstract public class AbstractMPAnalyzer
{
	abstract public String getLemma(String form, String pos);
	
	public void lemmatize(POSNode[] nodes)
	{
		for (POSNode node : nodes)
			node.lemma = getLemma(node.form, node.pos);
	}
	
	public void lemmatize(DEPTree tree)
	{
		int i, size = tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			node.lemma = getLemma(node.form, node.pos);
		}
	}
}
