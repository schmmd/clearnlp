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
package edu.colorado.clear.propbank;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import edu.colorado.clear.constituent.CTNode;
import edu.colorado.clear.constituent.CTReader;
import edu.colorado.clear.constituent.CTTree;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;

/**
 * PropBank argument.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class PBLib
{
	static final public String SRL_REL      = "rel";
	static final public String SRL_DSP      = "DSP";
	static final public String SRL_DIS	 	= "DIS";
	static final public String SRL_ARG0 	= "ARG0";
	static final public String SRL_ARGM 	= "ARGM";
	static final public String SRL_ARGM_MOD = "ARGM-MOD";
	static final public String SRL_LINK     = "LINK";
	static final public String SRL_LINK_SLC = "LINK-SLC";
	static final public String SRL_LINK_PRO = "LINK-PRO";
	static final public String SRL_LINK_PSV = "LINK-PSV";
	
	static final public String SRL_C_V = "C-V";
	
	/** The delimiter between terminal ID and height ({@code ":"}). */
	static final public String DELIM_LOC  = ":";
	/** The delimiter between locations and label ({@code "-"}). */
	static final public String DELIM_LABEL  = "-";
	/** The delimiter between different fields ({@code " "}). */
	static final public String DELIM_INST = " ";
	/** The location operators ({@code "*&,;"}). */
	static final public String LOC_TYPES  = "*&,;";
	
	static final public Pattern WRONG_ROLESET = Pattern.compile(".*\\.(ER|NN|IE|YY)");
	
	/**
	 * Returns the sorted list of PropBank instances from the specific file. 
	 * @param propFile the PropBank file to retrieve instances from.
	 * @return the sorted list of PropBank instances from the specific file.
	 */
	static public List<PBInstance> getPBInstanceList(String propFile)
	{
		List<PBInstance> list = new ArrayList<PBInstance>();
		PBReader   reader = new PBReader(UTInput.createBufferedFileReader(propFile));
		PBInstance instance;
		
		while ((instance = reader.nextInstance()) != null)
			list.add(instance);

		reader.close();
		Collections.sort(list);
		return list;
	}
	
	/**
	 * Returns the sorted list of PropBank instances from the specific file.
	 * Each instance takes the constituent tree associated with it.
	 * @param propFile the PropBank file to retrieve instances from.
	 * @param treeDir the Treebank directory path.
	 * @param norm if {@code true}, normalize indices of constituent trees.
	 * @return the sorted list of PropBank instances from the specific file.
	 */
	static public List<PBInstance> getPBInstanceList(String propFile, String treeDir, boolean norm)
	{
		List<PBInstance> list = PBLib.getPBInstanceList(propFile);
		CTReader reader = new CTReader();
		CTTree   tree   = null;
		String treeFile = "";
		int    treeId   = -1;
		
		for (PBInstance instance : list)
		{
			if (!treeFile.equals(instance.treePath))
			{
				treeFile = instance.treePath;
				treeId   = -1;
				reader.close();
				reader.open(UTInput.createBufferedFileReader(treeDir+File.separator+treeFile));
			}
			
			for (; treeId < instance.treeId; treeId++)
				tree = reader.nextTree();
			
			if (norm)	reader.normalizeIndices(tree);
			tree.setPBLocs();
			instance.setTree(tree);
		}
		
		return list;
	}
	
	/**
	 * Returns a map using "treePath TreeId" as a key and a list of associated instances as a value.  
	 * Each instance takes the constituent tree associated with it.
	 * @param propFile the PropBank file to retrieve instances from.
	 * @param treeDir the Treebank directory path.
	 * @param norm if {@code true}, normalize indices of constituent trees.
	 * @return a map using "treePath TreeId" as a key and a list of associated instances as a value.
	 */
	static public Map<String,List<PBInstance>> getPBInstanceMap(String propFile, String treeDir, boolean norm)
	{
		Map<String,List<PBInstance>> map = new HashMap<String,List<PBInstance>>();
		List<PBInstance> list = null;
		String ckey, pkey = "";
		
		for (PBInstance inst : PBLib.getPBInstanceList(propFile, treeDir, norm))
		{
			ckey = getTreePathId(inst);
			
			if (!ckey.equals(pkey))
			{
				list = new ArrayList<PBInstance>();
				pkey = ckey;
				map.put(ckey, list);
			}
			
			list.add(inst);
		}
		
		return map;
	}
	
	static private String getTreePathId(PBInstance inst)
	{
		StringBuilder build = new StringBuilder();
		
		build.append(inst.treePath);
		build.append(PBLib.DELIM_INST);
		build.append(inst.treeId);
		
		return build.toString();
	}
	
	/**
	 * Prints the list of PropBank instances to the specific file.
	 * @param instances the list of PropBank instances to print.
	 * @param outputFile the name of the file to print.
	 */
	static public void printPBInstances(List<PBInstance> instances, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		
		for (PBInstance instance : instances)
			fout.println(instance.toString());
				
		fout.close();
	}
	
	static public boolean isSame(PBInstance instance, CTTree tree1, CTTree tree2)
	{
		if (tree1.getTerminals().size() != tree2.getTerminals().size())
			return false;
		
		CTNode node1, node2;
		
		for (PBArg arg : instance.getArgs())
		{
			for (PBLoc loc : arg.getLocs())
			{
				node1 = tree1.getNode(loc);
				node2 = tree2.getNode(loc);
				
				if (!node1.toForms(false, " ").equals(node2.toForms(false, " ")))
					return false;
			}
		}
		
		return true;
	}
	
	static public boolean isNumberedArgument(PBArg arg)
	{
		return arg.label.length() == 4 && Character.isDigit(arg.label.charAt(3));
	}
}
