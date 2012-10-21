/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
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
package com.googlecode.clearnlp.experiment;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.googlecode.clearnlp.constituent.CTNode;
import com.googlecode.clearnlp.constituent.CTReader;
import com.googlecode.clearnlp.constituent.CTTree;
import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLEval;
import com.googlecode.clearnlp.dependency.srl.SRLabeler;
import com.googlecode.clearnlp.io.FileExtFilter;
import com.googlecode.clearnlp.morphology.MPLib;
import com.googlecode.clearnlp.pos.POSLib;
import com.googlecode.clearnlp.pos.POSNode;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.reader.POSReader;
import com.googlecode.clearnlp.reader.SRLReader;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.map.Prob1DMap;
import com.googlecode.clearnlp.util.map.Prob2DMap;
import com.googlecode.clearnlp.util.pair.IntIntPair;
import com.googlecode.clearnlp.util.pair.StringDoublePair;
import com.googlecode.clearnlp.util.pair.StringIntPair;


public class Tmp
{
	public Tmp(String[] args) throws Exception
	{
	//	mapPropBankToDependency(args[0], args[1]);
	//	countSRL(args);
	//	traverse(args[0]);
	//	getTokens(args[0], args[1]);
	//	converNonASC(args);
	}
	
	void countSRL(String[] args)
	{
	//	SRLReader reader = new SRLReader(0, 1, 2, 4, 6, 7, 9, 12);
		SRLReader reader = new SRLReader(0, 1, 3, 5, 6, 8, 10, 12);
		reader.open(UTInput.createBufferedFileReader(args[0]));
				
		PrintStream fout  = UTOutput.createPrintBufferedFileStream(args[1]);
		SRLabeler parser = new SRLabeler(fout);
		DEPTree tree;
		StringIntPair[][] gHeads;
		SRLEval eval = new SRLEval();
		IntIntPair p = new IntIntPair(0, 0);
				
		while ((tree = reader.next()) != null)
		{
			gHeads = tree.getSHeads();
			parser.label(tree);
			eval.evaluate(gHeads, tree.getSHeads());
			p.i1 += parser.getNumTransitions().i1;
			p.i2 += parser.getNumTransitions().i2;
		}
				
		fout.close();
		eval.print();
				
		System.out.println(p.i1+" "+p.i2);
	}
	
	void traverse(String inputFile)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(inputFile));
		CTTree tree;
		
		while ((tree = reader.nextTree()) != null)
			traverseAux(tree.getRoot());
	}
	
	void traverseAux(CTNode node)
	{
		if (node.isPTag("SBAR") && node.containsTags("+IN|TO") && node.containsTags("DT"))
			System.out.println(node);
		
		for (CTNode child : node.getChildren())
			traverseAux(child);
	}
	
	
	
	
	
	
	
	void getTokens(String inputFile, String outputDir)
	{
		POSReader reader = new POSReader(1, 3);
		POSNode[] nodes;
		
		final String[] NAMES = {"containsOnlyPunctuation.txt", "endsWithPeriod.txt", "containsPeriod.txt", "startsWithPrime.txt", "containsPrime.txt", "containsHyphen.txt", "containsAnyPunctuation.txt"};
		int i, size = NAMES.length;
		
		Prob1DMap[] maps = new Prob1DMap[size];
		String lemma;
		
		for (i=0; i<size; i++)
			maps[i] = new Prob1DMap();
		
		reader.open(UTInput.createBufferedFileReader(inputFile));
		
		while ((nodes = reader.next()) != null)
		{
			POSLib.normalizeForms(nodes);
			
			for (POSNode node : nodes)
			{
				lemma = MPLib.revertBracket(node.form);
				lemma = MPLib.normalizeDigits(lemma);
				lemma = lemma.toLowerCase();
				
				if (MPLib.containsOnlyPunctuation(lemma))
					maps[0].add(lemma);
				else if (lemma.endsWith("."))
				{
					if (lemma.length() > 2)
						maps[1].add(lemma);
				}
				else if (MPLib.containsAnySpecificPunctuation(lemma, '.'))
					maps[2].add(lemma);
				else if (lemma.startsWith("'") || lemma.startsWith("`"))
					maps[3].add(lemma);
				else if (MPLib.containsAnySpecificPunctuation(lemma, '\'', '`'))
					maps[4].add(lemma);
				else if (MPLib.containsAnySpecificPunctuation(lemma, '-'))
					maps[5].add(lemma);
				else if (MPLib.containsAnyPunctuation(lemma))
					maps[6].add(lemma);
			}
		}
		
		for (i=0; i<size; i++)
			print(outputDir+File.separator+NAMES[i], maps[i]);
	}
	
	void print(String outputFile, Prob1DMap map)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		
		for (StringIntPair p : map.toSortedList())
			fout.printf("%s\t%d\n", p.s, p.i);
				
		fout.close();
	}
	
	void countNonProjective(String filename)
	{
		DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);
		reader.open(UTInput.createBufferedFileReader(filename));
		int total = 0, count = 0;
		DEPTree tree;
		
		while ((tree = reader.next()) != null)
		{
			total++;
			if (!tree.getNonProjectiveSet().isEmpty()) count++;
		}
		
		System.out.printf("%f (%d/%d)", 100d*count/total, count, total);
	}
	
	void mapPropBankToDependency(String inputFile, String outputFile)
	{
		final String NONE = "NONE"; 

		SRLReader reader = new SRLReader(0, 1, 2, 3, 4, 5, 6, 8);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		Prob2DMap map = new Prob2DMap();
		DEPNode node, head;
		String deprel, label, ftags;
		DEPTree tree;
		int i, size;
		
		while ((tree = reader.next()) != null)
		{
			size = tree.size();
			
			for (i=1; i<size; i++)
			{
				node   = tree.get(i);
				head   = node.getHead();
				deprel = node.getLabel();
				if ((ftags = node.getFeat(DEPLib.FEAT_SEM)) != null)
					deprel = ftags;
				
				for (DEPArc arc : node.getSHeads())
				{
					label = arc.getLabel();
				//	if (label.startsWith("R-AM"))
				//		label = label.substring(2);
					
					if (arc.getNode() == head)
						map.add(label, deprel);
					else
						map.add(label, NONE);
				}
			}
		}
		
		List<String> keys = new ArrayList<String>(map.keySet());
		DecimalFormat format = new DecimalFormat("##.##"); 
		Collections.sort(keys);
		StringDoublePair[] ps;
		StringBuilder build;
		double none;
		String tmp;
		
		for (String key : keys)
		{
			build = new StringBuilder();
			ps = map.getProb1D(key);
			Arrays.sort(ps);
			none = 0;
			
			for (StringDoublePair p : ps)
			{
				if (p.s.equals(NONE))
					none = p.d;
				else if (p.d >= 0.2)
				{
					build.append("\\d"+p.s.toUpperCase());
					build.append(":");
					build.append(format.format(100d*p.d));
					build.append(", ");
				}
			}
			
			tmp = build.length() == 0 ? "" : build.substring(0, build.length()-2);
			fout.printf("%s\t%s\t%f\t%d\t%d\n", key, tmp, 100d*none, map.get(key).get(NONE), map.getTotal1D(key));
		}
		
		fout.close();
	}
	
	public List<String[]> getFilenames(String inputPath, String inputExt, String outputExt)
	{
		List<String[]> filenames = new ArrayList<String[]>();
		File f = new File(inputPath);
		String outputFile;
		
		if (f.isDirectory())
		{
			for (String inputFile : f.list(new FileExtFilter(inputExt)))
			{
				inputFile  = inputPath + File.separator + inputFile;
				outputFile = inputFile + "." + outputExt;
				filenames.add(new String[]{inputFile, outputFile});
			}
		}
		else
			filenames.add(new String[]{inputPath, inputPath+"."+outputExt});
		
		return filenames;
	}
	
	public void converNonASC(String[] args) throws Exception
	{
		Pattern asc1 = Pattern.compile("[^\\p{ASCII}]");
		Pattern asc2 = Pattern.compile("\\p{ASCII}");
		Pattern tab  = Pattern.compile("\t");
		BufferedReader fin;
		PrintStream fout;
		String[] tmp;
		String line, str;
		int i;
		
		for (String[] io : getFilenames(args[0], args[1], args[2]))
		{
			System.out.println(io[1]);
			fin  = UTInput.createBufferedFileReader(io[0]);
			fout = UTOutput.createPrintBufferedFileStream(io[1]);
			
			while ((line = fin.readLine()) != null)
			{
				line = line.trim();
				
				if (line.isEmpty())
				{
					fout.println();
					continue;
				}
				
				tmp = tab.split(line);
				
				for (i=0; i<tmp.length; i++)
				{
					str = tmp[i];
					
					if (asc2.matcher(str).find())
						tmp[i] = asc1.matcher(str).replaceAll("");
					else
						tmp[i] = "^ASCII";
				}
				
				fout.println(UTArray.join(tmp, "\t"));
			}
			
			fout.close();
		}
	}
	
	public void countSemanticDependents(String[] args)
	{
		SRLReader reader = new SRLReader(0, 1, 2, 3, 4, 5, 6, 8);
		reader.open(UTInput.createBufferedFileReader(args[0]));
		DEPTree tree;
		DEPNode node, dHead, sHead;
		int i;
		
		while ((tree = reader.next()) != null)
		{
			tree.setDependents();
			
			for (i=1; i<tree.size(); i++)
			{
				node  = tree.get(i);
				dHead = node.getHead();
				
				for (DEPArc sArc : node.getSHeads())
				{
				//	sHead = sArc.getNode();
					sHead = sArc.getNode().getHead();
					
					if (sHead != dHead && sHead != dHead.getHead() && node.isDescendentOf(sHead))
					{
						System.out.println(node.id+" "+sArc.getNode().id+" "+tree.toStringSRL());
						try {System.in.read();} catch (IOException e) {e.printStackTrace();}
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		try
		{
			new Tmp(args);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
