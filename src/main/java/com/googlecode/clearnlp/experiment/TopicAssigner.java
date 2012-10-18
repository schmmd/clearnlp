package com.googlecode.clearnlp.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.io.FileExtFilter;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;

public class TopicAssigner
{
	final Pattern P_SPACE = Pattern.compile(" ");
	final Pattern P_COLON = Pattern.compile(":");
	final Pattern P_UNDER = Pattern.compile("_");
	
	public TopicAssigner(String depDir, String tpcDir, int threshold) throws IOException
	{
		String depFile, tpcFile, outFile;
		
		for (String basename : new File(depDir).list(new FileExtFilter("dep.2")))
		{
			System.out.println(basename);
			depFile = depDir + File.separator + basename;
			tpcFile = tpcDir + File.separator + basename;
			outFile = depDir + File.separator + basename + ".tpc";
			
			assign(depFile, tpcFile, outFile, threshold);
		}
	}
	
	public void assign(String depFile, String tpcFile, String outFile, int threshold) throws IOException
	{
		List<List<String[]>> topics = getTopics(tpcFile, threshold);
		DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);
		reader.open(UTInput.createBufferedFileReader(depFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outFile);
		IntOpenHashSet[] sets;
		StringBuilder build;
		IntOpenHashSet set;
		int[] indices;
		DEPTree tree;
		int i, size;
		
		while ((tree = reader.next()) != null)
		{
			sets  = assignTopics(topics, tree);
			size  = sets.length;
			build = new StringBuilder();
			
			for (i=1; i<size; i++)
			{
				set = sets[i];
				
				if (set.isEmpty())
				{
					build.append("_");
				}
				else
				{
					indices = set.toArray();
					Arrays.sort(indices);
					build.append("tpc=");
					build.append(UTArray.join(indices, ","));
				}
				
				build.append("\n");
			}
			
			fout.println(build.toString());
		}
		
		fout.close();
	}
	
	private IntOpenHashSet[] assignTopics(List<List<String[]>> topics, DEPTree tree)
	{
		int nodeId, topicId, i, nSize = tree.size(), tSize = topics.size(), iSize;
		IntOpenHashSet[] sets = new IntOpenHashSet[nSize];
		boolean match;
		
		for (nodeId=1; nodeId<nSize; nodeId++)
			sets[nodeId] = new IntOpenHashSet();
		
		for (nodeId=1; nodeId<nSize; nodeId++)
		{
			for (topicId=0; topicId<tSize; topicId++)
			{
				for (String[] topic : topics.get(topicId))
				{
					iSize = topic.length;
					match = true;
					
					for (i=0; i<iSize; i++)
					{
						if (nodeId+i >= nSize || !tree.get(nodeId+i).form.toLowerCase().equals(topic[i]))
						{
							match = false;
							break;
						}
					}
					
					if (match)
					{
						for (i=0; i<iSize; i++)
							sets[nodeId+i].add(topicId);
					}
				}
			}
		}
		
		return sets;
	}
	
	public List<List<String[]>> getTopics(String tpcFile, int threshold) throws IOException
	{
		BufferedReader fin = UTInput.createBufferedFileReader(tpcFile);
		List<List<String[]>> topics = new ArrayList<List<String[]>>();
		List<String[]> topic;
		String[] tmp;
		String line;
		
		while ((line = fin.readLine()) != null)
		{
			topic = new ArrayList<String[]>();
			
			for (String item : P_SPACE.split(line))
			{
				tmp = P_COLON.split(item);
				if (tmp.length < 2)	continue;
				
				if (Integer.parseInt(tmp[1]) > threshold)
					topic.add(P_UNDER.split(tmp[0]));
			}
			
			if (!topic.isEmpty())
				topics.add(topic);
		}
		
		fin.close();
		return topics;
	}
	
	static public void main(String[] args)
	{
		try
		{
			new TopicAssigner(args[0], args[1], Integer.parseInt(args[2]));
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
