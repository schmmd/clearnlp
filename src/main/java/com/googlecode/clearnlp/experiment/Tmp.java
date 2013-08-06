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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntDeque;
import com.googlecode.clearnlp.constituent.CTNode;
import com.googlecode.clearnlp.constituent.CTReader;
import com.googlecode.clearnlp.constituent.CTTree;
import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPFeat;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLEval;
import com.googlecode.clearnlp.dependency.srl.SRLabeler;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.io.FileExtFilter;
import com.googlecode.clearnlp.morphology.MPLib;
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
	//	printTreesForCKY(args);
	//	wc(args[0]);
	//	projectivize(args[0], args[1]);
	//	evalSubPOS(args[0]);
	//	countLR(args[0]);
	//	measureTime();
	}
	
	void countLR(String inputFile)
	{
	//	DEPReader reader = new DEPReader(0, 1, 2, 3, 5, 6, 7);
		DEPReader reader = new DEPReader(0, 1, 2, 4, 6, 8, 10);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		int i, size, left = 0, right = 0, l, r, prevId, depId;
		DEPTree tree;
		DEPNode node;
		
		while ((tree = reader.next()) != null)
		{
			tree.setDependents();
			size = tree.size();
			
			for (i=1; i<size; i++)
			{
				node = tree.get(i);
				prevId = -1;
				l = r = 0;
				
				for (DEPArc arc : node.getDependents())
				{
					depId = arc.getNode().id;
					
					if (depId - prevId == 1)
					{
						if (depId < node.id)	l++;
						else 					r++;
					}
					
					prevId = depId;
				}
				
				if      (l > 1)	left++;
				else if (r > 1)	right++;
			}
		}
		
		reader.close();
		System.out.printf("Left: %d, Right: %d\n", left, right);
	}
	
	void measureTime()
	{
		int i, j, len = 10, size = 1000000;
		IntArrayList list;
		IntDeque deque;
		long st, et;
		
		st = System.currentTimeMillis();
		
		for (i=0; i<size; i++)
		{
			list = new IntArrayList();
			
			for (j=0; j<len; j++)
				list.add(j);
			
			list.remove(list.size()-1);
		}
		
		et = System.currentTimeMillis();
		System.out.println(et-st);
		
		st = System.currentTimeMillis();
		
		for (i=0; i<size; i++)
		{
			deque = new IntArrayDeque();
			
			for (j=0; j<len; j++)
				deque.addLast(j);
			
			deque.removeLast();
		}
		
		et = System.currentTimeMillis();
		System.out.println(et-st);
	}
	
	void evalSubPOS(String inputFile) throws Exception
	{
		BufferedReader reader = UTInput.createBufferedFileReader(inputFile);
		Pattern delim = Pattern.compile("\t");
		int correct = 0, total = 0;
		DEPFeat p, g;
		String[] ls;
		String line;
		
		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			
			if (!line.isEmpty())
			{
				ls = delim.split(line);
				g = new DEPFeat(ls[6]);
				p = new DEPFeat(ls[7]);
				
				if (g.get("SubPOS").equals(p.get("SubPOS")))
					correct++;
						
				total++;
			}
		}
		
		System.out.printf("%5.2f (%d/%d)\n", 100d*correct/total, correct, total);
	}
	
	void projectivize(String inputFile, String outputFile)
	{
		DEPReader reader = new DEPReader(0, 1, 2, 4, 6, 8, 10);
		DEPTree tree;
		
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fold = UTOutput.createPrintBufferedFileStream(outputFile+".old");
		PrintStream fnew = UTOutput.createPrintBufferedFileStream(outputFile+".new");
		int i;
		
		for (i=0; (tree = reader.next()) != null; i++)
		{
			fold.println(tree.toStringCoNLL()+"\n");
			tree.projectivize();
			fnew.println(tree.toStringCoNLL()+"\n");
			
			if (i%1000 == 0)	System.out.print(".");
		}	System.out.println();
		
		reader.close();
		fold.close();
		fnew.close();
	}
	
	void wc(String inputFile)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(inputFile));
		CTTree tree;
		int sc, wc;
		
		for (sc=0,wc=0; (tree = reader.nextTree()) != null; sc++)
			wc += tree.getTokens().size();
		
		System.out.println(sc+" "+wc);
	}
	
	void stripTrees(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(args[0]+".strip");
		Set<String> set = new HashSet<String>();
		String forms;
		CTTree tree;
		int i;
		
		for (i=0; (tree = reader.nextTree()) != null; i++)
		{
			forms = tree.toForms();
			
			if (!set.contains(forms))
			{
				set.add(forms);
				fout.println(tree+"\n");
			}
		}
		
		fout.close();
		System.out.println(i+" -> "+set.size());
	}
	
	void splitTrees(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		PrintStream[] fout = new PrintStream[4];
		CTTree tree;
		int i, j;
		
		fout[0] = UTOutput.createPrintBufferedFileStream(args[0]+".trn.parse");
		fout[1] = UTOutput.createPrintBufferedFileStream(args[0]+".trn.raw");
		fout[2] = UTOutput.createPrintBufferedFileStream(args[0]+".tst.parse");
		fout[3] = UTOutput.createPrintBufferedFileStream(args[0]+".tst.raw");
		
		for (i=0; (tree = reader.nextTree()) != null; i++)
		{
			j = (i%6 == 0) ? 2 : 0;
			
			fout[j]  .println(tree.toString()+"\n");
			fout[j+1].println(tree.toForms());
		}
		
		for (PrintStream f : fout)	f.close();
	}
	
	void printTreesForCKY(String[] args)
	{
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(args[0]));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(args[1]);
		CTTree tree;
		CTNode root;
		int count;
		
		while ((tree = reader.nextTree()) != null)
		{
			root = tree.getRoot();
			
			if (root.getChildrenSize() == 1)
			{
				count = stripPunct(tree);
				
				if (root.getChildrenSize() > 0 && tree.getTokens().size()-count >= 4 && !containsEmptyCategories(tree) && isCKYTree(root.getChild(0)))
					fout.println(tree+"\n");
			}
		}
		
		reader.close();
		fout.close();
	}
	
	boolean containsEmptyCategories(CTTree tree)
	{
		for (CTNode node : tree.getTerminals())
		{
			if (node.isEmptyCategory())
				return true;
		}
		
		return false;
	}
	
	int stripPunct(CTTree tree)
	{
		int count = 0;
		
		for (CTNode node : tree.getTokens())
		{
			if (MPLib.containsOnlyPunctuation(node.form))
			{
				node.getParent().removeChild(node);
				count++;
			}
		}
		
		return count;
	}
	
	boolean isCKYTree(CTNode node)
	{
		if (!node.isPhrase())
			return true;
		
		int size = node.getChildrenSize();
		
	/*	if (size == 1)
		{
			if (!node.getChild(0).isPhrase())
				return true;
		}*/
		
		if (size != 2)
			return false;
		
		for (CTNode child : node.getChildren())
		{
			if (!isCKYTree(child))
				return false;
		}
		
		return true;
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
			EngineProcess.normalizeForms(nodes);
			
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
			print(outputDir+"/"+NAMES[i], maps[i]);
	}
	
	void print(String outputFile, Prob1DMap map)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		
		for (StringIntPair p : map.toSortedList())
			fout.printf("%s\t%d\n", p.s, p.i);
				
		fout.close();
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
				inputFile  = inputPath + "/" + inputFile;
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
					sHead = sArc.getNode();
				//	sHead = sArc.getNode().getHead();
					
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
