package edu.colorado.clear.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Option;

import edu.colorado.clear.reader.AbstractColumnReader;
import edu.colorado.clear.util.UTInput;

public class POSEvaluate extends AbstractRun
{
	@Option(name="-g", usage="the gold-standard file (input; required)", required=true, metaVar="<filename>")
	private String s_goldFile;
	@Option(name="-s", usage="the system file (input; required)", required=true, metaVar="<filename>")
	private String s_autoFile;
	@Option(name="-gi", usage="the column index of POS tags in the gold-standard file (input; required)", required=true, metaVar="<integer>")
	private int    i_goldIndex;
	@Option(name="-si", usage="the column index of POS tags in the sytem file (input; required)", required=true, metaVar="<integer>")
	private int    i_autoIndex;
	
	private Map<String,int[]> m_labels;
	
	public POSEvaluate() {}
	
	public POSEvaluate(String[] args)
	{
		initArgs(args);
		run(s_goldFile, s_autoFile, i_goldIndex, i_autoIndex);
	}
	
	public void run(String goldFile, String autoFile, int goldIndex, int autoIndex)
	{
		BufferedReader fGold = UTInput.createBufferedFileReader(goldFile);
		BufferedReader fAuto = UTInput.createBufferedFileReader(autoFile);
		String[] gold, auto;
		int[] counts;
		String line;
		
		m_labels = new HashMap<String,int[]>();
		
		try
		{
			while ((line = fGold.readLine()) != null)
			{
				gold = line.split(AbstractColumnReader.DELIM_COLUMN);
				auto = fAuto.readLine().split(AbstractColumnReader.DELIM_COLUMN);
				
				line = line.trim();
				if (line.isEmpty())	 continue;
				counts = getCounts(gold[goldIndex]);
				
				if (gold[goldIndex].equals(auto[autoIndex]))
					counts[0]++;
				
				counts[1]++;
			}
		}
		catch (IOException e) {e.printStackTrace();}
		
		print();
	}
	
	private void print()
	{
		String hline = "----------------------------------------";
		
		System.out.println(hline);
		System.out.printf("%10s%10s%10s%10s\n", "Label", "Count", "Dist.", "Acc.");
		System.out.println(hline);
		
		int[] counts = getTotalCounts();
		int   total  = counts[1];
		
		printAccuracy("ALL", total, counts);
		System.out.println(hline);
		
		List<String> tags = new ArrayList<String>(m_labels.keySet());
		Collections.sort(tags);
		
		for (String tag : tags)
			printAccuracy(tag, total, m_labels.get(tag));
		System.out.println(hline);
	}
	
	private void printAccuracy(String label, int total, int[] counts)
	{
		System.out.printf("%10s%10d%10.2f%10.2f\n", label, counts[1], 100d*counts[1]/total, 100d*counts[0]/counts[1]);
	}
	
	private int[] getCounts(String tag)
	{
		int[] counts;
		
		if (m_labels.containsKey(tag))
			counts = m_labels.get(tag);
		else
		{
			counts = new int[2];
			m_labels.put(tag, counts);
		}
		
		return counts;
	}
	
	private int[] getTotalCounts()
	{
		int[] gCounts = null, lCounts;
		int i;
		
		for (String tag : m_labels.keySet())
		{
			lCounts = m_labels.get(tag);
			
			if (gCounts == null)
				gCounts = Arrays.copyOf(lCounts, lCounts.length);
			else
			{
				for (i=0; i<gCounts.length; i++)
					gCounts[i] += lCounts[i];
			}
		}
		
		return gCounts;
	}

	static public void main(String[] args)
	{
		new POSEvaluate(args);
	}
}
