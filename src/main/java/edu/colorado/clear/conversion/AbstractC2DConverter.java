package edu.colorado.clear.conversion;

import edu.colorado.clear.constituent.CTLib;
import edu.colorado.clear.constituent.CTNode;
import edu.colorado.clear.constituent.CTTree;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.headrule.HeadRule;
import edu.colorado.clear.headrule.HeadRuleMap;

abstract public class AbstractC2DConverter
{
	protected HeadRuleMap m_headrules;
	
	public AbstractC2DConverter(HeadRuleMap headrules)
	{
		m_headrules = headrules;
	}
	
	/**
	 * Sets the head of the specific node and all its sub-nodes.
	 * Calls {@link AbstractC2DConverter#findHeads(CTNode)}.
	 * @param curr the node to process.
	 */
	protected void setHeads(CTNode curr)
	{
		// terminal nodes become the heads of themselves
		if (!curr.isPhrase())
		{
			curr.c2d = new C2DInfo(curr);
			return;
		}
		
		// set the heads of all children
		for (CTNode child : curr.getChildren())
			setHeads(child);
		
		// stop traversing if it is the top node
		if (curr.isPTag(CTLib.PTAG_TOP))
			return;
		
		// only one child
		if (curr.getChildrenSize() == 1)
		{
			curr.c2d = new C2DInfo(curr.getChild(0));
			return;
		}
		
		// find the headrule of the current node
		HeadRule rule = m_headrules.get(curr.pTag);
				
		if (rule == null)
		{
			System.err.println("Error: headrules not found for \""+curr.pTag+"\"");
			rule = m_headrules.get(CTLib.PTAG_X);
		}			
				
		setHeadsAux(rule, curr);
	}
	
	/**
	 * Sets the head of the specific phrase node.
	 * This is a helper method of {@link AbstractC2DConverter#setHeads(CTNode)}.
	 * @param rule the headrule to the specific node.
	 * @param curr the phrase node.
	 */
	abstract protected void setHeadsAux(HeadRule rule, CTNode curr);
	
	/**
	 * Returns the dependency tree converted by the specific constituent tree. 
	 * @param cTree the constituent tree to be converted.
	 * @return the dependency tree converted by the specific constituent tree.
	 */
	abstract public DEPTree toDEPTree(CTTree cTree);
}
