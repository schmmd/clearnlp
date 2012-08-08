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
package com.goolgecode.clearnlp.conversion;

import com.goolgecode.clearnlp.constituent.CTLib;
import com.goolgecode.clearnlp.constituent.CTNode;
import com.goolgecode.clearnlp.constituent.CTTree;
import com.goolgecode.clearnlp.dependency.DEPTree;
import com.goolgecode.clearnlp.headrule.HeadRule;
import com.goolgecode.clearnlp.headrule.HeadRuleMap;

/**
 * Abstract constituent to dependency converter.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
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
