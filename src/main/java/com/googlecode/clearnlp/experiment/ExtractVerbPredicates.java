package com.googlecode.clearnlp.experiment;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.reader.SRLReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;

public class ExtractVerbPredicates
{
	final Pattern P_VERBS = Pattern.compile("VB.*");
	
	public ExtractVerbPredicates(String inputDir, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		SRLReader reader = new SRLReader(0, 1, 3, 5, 6, 8, 10, 12);
		String[] inputFiles = UTFile.getSortedFileList(inputDir);
		DEPTree tree;
		
		for (String inputFile : inputFiles)
		{
			reader.open(UTInput.createBufferedFileReader(inputFile));
			System.out.println(inputFile);
			
			while ((tree = reader.next()) != null)
			{
				if (strip(tree))
					fout.println(tree.toStringSRL()+"\n");
			}
			
			reader.close();
		}
		
		fout.close();
	}
	
	public boolean strip(DEPTree tree)
	{
		Set<DEPNode> preds = getPredicates(tree);
		if (preds.isEmpty())	return false;
		
		int i, size = tree.size();
		List<DEPArc> remove;
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node   = tree.get(i);
			remove = new ArrayList<DEPArc>();
			
			for (DEPArc arc : node.getSHeads())
			{
				if (!preds.contains(arc.getNode()))
					remove.add(arc);
			}
			
			node.removeSHeads(remove);
		}
		
		return true;
	}
	
	public Set<DEPNode> getPredicates(DEPTree tree)
	{
		Set<DEPNode> preds = new HashSet<DEPNode>();
		int i, size = tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			if (node.getFeat(DEPLib.FEAT_PB) != null && (P_VERBS.matcher(node.pos).find() || node.containsSHead("AM-PRR")))
				preds.add(node);
			else
				node.removeFeat(DEPLib.FEAT_PB);
		}
		
		return preds;
	}

	static public void main(String[] args)
	{
		new ExtractVerbPredicates(args[0], args[1]);
	}
}
