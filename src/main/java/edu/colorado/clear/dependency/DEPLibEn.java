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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.carrotsearch.hppc.IntOpenHashSet;


public class DEPLibEn extends DEPLib
{
	static public final String DEP_GAP	= "gap";
	static public final String DEP_REF	= "ref";
	static public final String DEP_RNR	= "rnr";
	
	static public final String DEP_PASS		= "pass";
	static public final String DEP_AUX		= "aux";
	static public final String DEP_AUXPASS	= DEP_AUX+DEP_PASS;
	
	static public final String DEP_SUBJ			= "subj";
	static public final String DEP_CSUBJ		= "c"+DEP_SUBJ;
	static public final String DEP_NSUBJ		= "n"+DEP_SUBJ;
	static public final String DEP_XSUBJ		= "x"+DEP_SUBJ;
	static public final String DEP_CSUBJPASS	= DEP_CSUBJ+DEP_PASS;
	static public final String DEP_NSUBJPASS	= DEP_NSUBJ+DEP_PASS;
	
	static public final String DEP_ACOMP		= "acomp";			//
	static public final String DEP_ADVCL		= "advcl";			//
	static public final String DEP_ADVMOD		= "advmod";			//
	static public final String DEP_AGENT		= "agent";			//
	static public final String DEP_AMOD			= "amod";			//
	static public final String DEP_APPOS		= "appos";			//
	static public final String DEP_ATTR			= "attr";			//
	static public final String DEP_CC			= "cc";				//
	static public final String DEP_CCOMP		= "ccomp";			//
	static public final String DEP_COMPLM		= "complm";			
	static public final String DEP_CONJ			= "conj";			
	static public final String DEP_DET			= "det";			//
	static public final String DEP_DOBJ 		= "dobj";			//
	static public final String DEP_EXPL 		= "expl";			//
	static public final String DEP_IOBJ 		= "iobj";			//
	static public final String DEP_INTJ			= "intj";			//
	static public final String DEP_MARK			= "mark";			
	static public final String DEP_META			= "meta";			//
	static public final String DEP_NEG			= "neg";			//
	static public final String DEP_INFMOD		= "infmod";			//
	static public final String DEP_NMOD 		= "nmod";			//
	static public final String DEP_NN			= "nn";				//
	static public final String DEP_NPADVMOD		= "npadvmod";		//
	static public final String DEP_NUM			= "num";			//
	static public final String DEP_NUMBER		= "number";			//
	static public final String DEP_OPRD			= "oprd";
	static public final String DEP_PARATAXIS 	= "parataxis";		//
	static public final String DEP_PARTMOD		= "partmod";		//
	static public final String DEP_PCOMP 		= "pcomp";			//
	static public final String DEP_POBJ 		= "pobj";			//
	static public final String DEP_POSS			= "poss";			//
	static public final String DEP_POSSESSIVE 	= "possessive";	//
	static public final String DEP_PRECONJ		= "preconj";		//
	static public final String DEP_PREDET		= "predet";			//
	static public final String DEP_PREP			= "prep";			//
	static public final String DEP_PRT 			= "prt";			//
	static public final String DEP_PUNCT		= "punct";			//
	static public final String DEP_QUANTMOD		= "quantmod";		//
	static public final String DEP_RCMOD		= "rcmod";			//
	static public final String DEP_XCOMP		= "xcomp";			//
	
	static public void postProcess(DEPTree tree)
	{
		tree.setDependents();
		devideAdverbialModifiers(tree);
		appendPassiveLabels(tree);
		appendObjectLabels(tree);
	}
	
	static private void devideAdverbialModifiers(DEPTree tree)
	{
		int i, size = tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			if (node.isLabel(DEP_ADVMOD))
			{
				String form = node.form.toLowerCase();
				
				if (form.equals("never") || form.equals("not") || form.equals("n't") || form.equals("'nt") || form.equals("no"))
					node.setLabel(DEP_NEG);
			}
		}
	}
	
	static private void appendPassiveLabels(DEPTree tree)
	{
		Set<String> sbjs = new HashSet<String>();
		sbjs.add(DEP_CSUBJ);	sbjs.add(DEP_NSUBJ);
		
		int i, size = tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			if (node.containsDependent(DEPLibEn.DEP_AUXPASS))
			{
				for (DEPNode child : node.getDependentsByLabels(sbjs))
					child.setLabel(child.getLabel()+DEPLibEn.DEP_PASS);
			}
		}
	}
	
	static private void appendObjectLabels(DEPTree tree)
	{
		int i, size = tree.size();
		List<DEPNode> list;
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			if ((list = node.getDependentsByLabel(DEPLibEn.DEP_DOBJ)).size() > 1)
				list.get(0).setLabel(DEPLibEn.DEP_IOBJ);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	static public void mergeSheads(DEPTree tree)
	{
		Pattern argn = Pattern.compile("(A\\d|C-A\\d|R-A\\d).*");
		int i, size = tree.size(), xMiss = 0;
		List<DEPArc> sHeads;
		DEPNode node, head;
		String label;
		DEPArc xArc;
		
		for (i=1; i<size; i++)
			tree.get(i).x_heads.clear();
		
		for (i=1; i<size; i++)
		{
			node   = tree.get(i);
			sHeads = node.getSHeads();
			
			for (DEPArc sArc : sHeads)
			{
				head  = sArc.getNode();
				label = sArc.getLabel();
				
				if (!argn.matcher(label).find())
					continue;
				
				if (head.isDescendentOf(node))
				{
					xArc = head.getXHead(node);
					
					if (xArc == null)
						head.addXHead(node, "I-"+label);
					else
						xMiss++;
				}
				else
				{
					xArc = node.getXHead(head);
					
					if (xArc == null)
						node.addXHead(head, label);
					else if (xArc.getLabel().startsWith("I-"))
						xArc.setLabel(label);
					else
						xMiss++;
				}
				
			/*	if (head.isDescendentOf(node))
				{
					xArc = head.getXHead(node);
					
					if (xArc == null)
					{
						if (label.endsWith(PBLib.SRL_DSP))
							head.addXHead(node, "AM-DIS");
						else
							head.addXHead(node, "I-"+label);
					}
					else if (label.endsWith(PBLib.SRL_DIS) || label.endsWith(PBLib.SRL_DSP))
					{
						xArc.setLabel("AM-DIS");
						xCount++;
					}
					else if (xArc.getLabel().startsWith("AM") || node.isRoot())
					{
						xCount++;
					}
					else
						System.err.println("C: "+node.id+" "+head.id+" "+sArc.getLabel()+"\n"+tree+"\n");
				}
				else
				{
					xArc = node.getXHead(head);
					
					if (xArc == null)
					{
						node.addXHead(head, label);
					}
					else if (xArc.isLabel("AM-DIS"))
					{
						xCount++;
					}
					else if (xArc.getLabel().startsWith("I"))
					{
						xArc.setLabel(label);
						xCount++;
					}
					else
						System.err.println("R: "+node.id+" "+head.id+" "+sArc.getLabel()+"\n"+tree+"\n");
				}*/
			}
		}
		
		if (xMiss > 0)	System.err.println(tree+"\n");
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			head = node.getHead();		
					
			if (node.getXHeads().isEmpty())
				node.addXHead(head, node.getLabel());
		}
	}
	
	static public void splitSheads(DEPTree tree)
	{
		IntOpenHashSet set = new IntOpenHashSet();
		int i, size = tree.size();
		String label, roleset;
		DEPNode node, head;
		
		for (i=1; i<size; i++)
			tree.get(i).s_heads.clear();
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			for (DEPArc arc : node.x_heads)
			{
				label = arc.getLabel();
								
				if (Character.isUpperCase(label.charAt(0)))
				{		
					head = arc.getNode();
					
					if (label.startsWith("I-"))
					{
						head.addSHead(node, label.substring(2));
						set.add(node.id);
					}
					else
					{
						node.addSHead(head, label);
						set.add(head.id);
					}
				}
			}
		}
		
		for (i=1; i<size; i++)
		{
			node    = tree.get(i);
			roleset = node.getFeat(DEPLib.FEAT_PB);
			
			if (roleset != null)
			{
				if (!set.contains(node.id) && !roleset.endsWith("MS"))
					node.removeFeat(DEPLib.FEAT_PB);
			}
			else if (set.contains(node.id))
				node.addFeat(DEPLib.FEAT_PB, node.lemma+".XX");
		}
	}
}
