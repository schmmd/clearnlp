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
package edu.colorado.clear.experiment;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.colorado.clear.dependency.DEPArc;
import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.reader.DEPReader;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;

public class ExtractFables
{
	public ExtractFables(String inputFile, String outputFile)
	{
		DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		DEPTree tree;
		DEPNode node, tmp;
		List<DEPArc> deps;
		int i, j, len, size;
		DEPArc arc;
		Set<String> set = getTempSet();
		String lemma, lower;
		
		while ((tree = reader.next()) != null)
		{
			tree.setDependents();
			size = tree.size();
			
			for (i=1; i<size; i++)
			{
				node = tree.get(i);
				if (node.getFeat("t") == null)	continue;
				
				deps = new ArrayList<DEPArc>(node.getDependents());
				addFeats(node, "hd", node.getHead(), node.getLabel());
				
				if (!deps.isEmpty())
				{
					len = deps.size();
					
					for (j=len-1; j>=0; j--)
					{
						arc = deps.get(j);
						tmp = arc.getNode();
						
						if (tmp.id < node.id)
						{
							addFeats(node, "ln", tmp, arc.getLabel());
							break;
						}
					}
					
					for (j=0; j<len; j++)
					{
						arc = deps.get(j);
						tmp = arc.getNode();
						
						if (tmp.id > node.id)
						{
							addFeats(node, "rn", tmp, arc.getLabel());
							break;
						}
					}
					
					for (DEPArc c : deps)
					{
						tmp   = c.getNode();
						lemma = tmp.lemma;
						
						if (c.getLabel().startsWith("aux"))
						{
							lower = tmp.form.toLowerCase();
							
							if (lemma.equals("have"))
								node.addFeat("aux-have", lower);
							else if (lemma.equals("be") || lemma.equals("'s"))
								node.addFeat("aux-be", lower);
							else if (tmp.isPos("MD"))
								node.addFeat("aux-md", lower);
						}
						
						if (set.contains(lemma))
							node.addFeat("sig", lemma);
					}
				}
			}
			
			fout.println(tree+"\n");
		}
		
		fout.close();
	}
	
	void addFeats(DEPNode node, String key, DEPNode aNode, String deprel)
	{
		node.addFeat(key+"f", aNode.form);
		node.addFeat(key+"m", aNode.lemma);
		node.addFeat(key+"p", aNode.pos);
		node.addFeat(key+"d", deprel);
	}
	
	Set<String> getTempSet()
	{
		Set<String> set = new HashSet<String>();
		
		set.add("after");
		set.add("when");
		set.add("until");
		set.add("before");
		set.add("since");
		set.add("already");
		set.add("previously");
		set.add("while");
		set.add("meanwhile");
		set.add("followed");
		set.add("former");
		
		return set;
	}
	
	static public void main(String[] args)
	{
		new ExtractFables(args[0], args[1]);
	}

}
