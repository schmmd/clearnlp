package edu.colorado.clear.experiment;

import java.io.PrintStream;
import java.util.Arrays;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;

import edu.colorado.clear.dependency.DEPArc;
import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.reader.AbstractColumnReader;
import edu.colorado.clear.reader.SRLReader;
import edu.colorado.clear.util.UTArray;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;

public class SRLExpand
{
	public SRLExpand(String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		SRLReader reader = new SRLReader(0, 1, 2, 3, 4, 5, 6, 7);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		DEPTree tree;
	
		while ((tree = reader.next()) != null)
		{
			tree.setDependents();
			printSpans(fout, tree);
		}
		
		reader.close();
		fout.close();
	}
	
	private void printSpans(PrintStream fout, DEPTree tree)
	{
		String[][] spans = expandSRL(tree);
		
		if (spans == null)
		{
			fout.println(tree+AbstractColumnReader.DELIM_SENTENCE);
			return;
		}
		
		StringBuilder build = new StringBuilder();
		int i, size = tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			build.append(node.toStringDEP());
			build.append(AbstractColumnReader.DELIM_COLUMN);
			build.append(UTArray.join(spans[i], AbstractColumnReader.DELIM_COLUMN));
			build.append(AbstractColumnReader.DELIM_SENTENCE);
		}
		
		fout.println(build.toString());
	}
	
	public String[][] expandSRL(DEPTree tree)
	{
		ObjectIntOpenHashMap<DEPNode> map = new ObjectIntOpenHashMap<DEPNode>();
		int i = 0, predId = 0, size = tree.size();
		DEPNode pred, arg;
		String label;
		
		while ((pred = tree.getNextPredicate(predId)) != null)
		{
			map.put(pred, i++);
			predId = pred.id;
		}
		
		if (map.isEmpty())	return null;

		String[][] spans = new String[size][];
		int len = map.size();
		
		for (i=1; i<size; i++)
		{
			spans[i] = new String[len];
			Arrays.fill(spans[i], AbstractColumnReader.BLANK_COLUMN);
		}
		
		for (i=1; i<size; i++)
		{
			arg = tree.get(i);
			
			for (DEPArc arc : arg.getSHeads())
			{
				pred = arc.getNode();
				if (!map.containsKey(pred))	continue;
				
				predId = map.get(pred);
				label  = arc.getLabel();
				
				for (int spanId : getSpan(pred, arg))
					spans[spanId][predId] = label;
			}
		}
		
		return spans;
	}
	
	private int[] getSpan(DEPNode pred, DEPNode arg)
	{
		IntOpenHashSet sArg = arg .getSubtreeIdSet();
		
		if (pred.isDescendentOf(arg))
			sArg.removeAll(pred.getSubtreeIdSet());			
		
		int[] span = sArg.toArray();
		return span;
	}
	
	static public void main(String[] args)
	{
		new SRLExpand(args[0], args[1]);
	}
}
