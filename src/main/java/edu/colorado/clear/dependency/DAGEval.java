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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.colorado.clear.util.pair.StringIntPair;

/**
 * Compare two dependency-based semantic role labeling outputs.
 * @author Jinho D. Choi
 * <b>Last update:</b> 4/19/2011
 */
public class DAGEval
{
	private final int IDX_CORRECT = 0;
	private final int IDX_AUTO    = 1;
	private final int IDX_GOLD    = 2;
	
	private final String UAS   = "UAS";
	private final String LAS   = "LAS";
	private final String GROUP = "GROUP";
	private final String REST  = "REST";
	
	private HashMap<String,int[]> m_counts;
	private Pattern               p_group;
	
	public DAGEval()
	{
		init(Pattern.compile(".*"));
	}
	
	public DAGEval(Pattern group)
	{
		init(group);
	}
	
	public void init(Pattern group)
	{
		m_counts = new HashMap<String, int[]>();
		p_group  = group;
		
		m_counts.put(UAS  , new int[3]);
		m_counts.put(LAS  , new int[3]);
		m_counts.put(GROUP, new int[3]);
		m_counts.put(REST , new int[3]);
	}

	public void evaluate(List<List<StringIntPair>> gold, List<List<StringIntPair>> auto)
	{
		List<StringIntPair> gHeads, aHeads;
		int i, size = gold.size();
		
		for (i=1; i<size; i++)
		{
			gHeads = gold.get(i);
			aHeads = auto.get(i);
			
			measure(gHeads, aHeads);
		}
	}
	
	private void measure(List<StringIntPair> gHeads, List<StringIntPair> aHeads)
	{
		int[] uas   = m_counts.get(UAS);
		int[] las   = m_counts.get(LAS);
		int[] group = m_counts.get(GROUP);
		int[] rest  = m_counts.get(REST);
		int[] arg;
		
		boolean match;
		
		for (StringIntPair gHead : gHeads)
		{
			match = p_group.matcher(gHead.s).find();
			arg   = getArray(gHead.s);
			arg[IDX_GOLD]++;
			
			if (match)	group[IDX_GOLD]++;
			else		rest [IDX_GOLD]++;
			
			for (StringIntPair aHead : aHeads)
			{
				if (gHead.i == aHead.i)
				{
					uas[IDX_CORRECT]++;
					
					if (gHead.s.equals(aHead.s))
					{
						las[IDX_CORRECT]++;
						arg[IDX_CORRECT]++;
						
						if (match)	group[IDX_CORRECT]++;
						else		rest [IDX_CORRECT]++;
					}
				}
			}
		}
		
		for (StringIntPair aHead : aHeads)
		{
			match = p_group.matcher(aHead.s).find();
			arg   = getArray(aHead.s);
			arg[IDX_AUTO]++;
			
			if (match)	group[IDX_AUTO]++;
			else		rest [IDX_AUTO]++;
		}

		uas[IDX_AUTO] += aHeads.size();
		uas[IDX_GOLD] += gHeads.size();
		
		las[IDX_AUTO] += aHeads.size();
		las[IDX_GOLD] += gHeads.size();
	}
	
	private int[] getArray(String label)
	{
		if (m_counts.containsKey(label))
		{
			return m_counts.get(label);
		}
		else
		{
			int[] counts = new int[3];
			m_counts.put(label, counts);
			
			return counts;
		}
	}
	
	public void print()
	{
		String hline = "------------------------------------------------------------";
		int total = getTotalCount();
		
		System.out.println(hline);
		System.out.printf("%10s%10s%10s%10s%10s%10s\n", "Label", "Count", "Dist.", "P", "R", "F1");
		
		System.out.println(hline);
		printLabel(UAS, total);
		printLabel(LAS, total);
		
		System.out.println(hline);
		printLabel(GROUP, total);
		printLabel(REST , total);
		
		ArrayList<String> labels = new ArrayList<String>(m_counts.keySet());
		Collections.sort(labels);
		
		System.out.println(hline);
		
		for (String label : labels)
		{
			if (!(label.equals(UAS) || label.equals(LAS) || label.equals(GROUP) || label.equals(REST)))
				printLabel(label, total);
		}
		
		System.out.println(hline);
	}
	
	private void printLabel(String label, int total)
	{
		int[] counts = m_counts.get(label);
		int     auto = counts[IDX_AUTO];
		int     gold = counts[IDX_GOLD];
		double  dist = 100d * gold / total;
		
		double precision = (auto == 0) ? 0 : 100d * counts[IDX_CORRECT] / auto;
		double recall    = (gold == 0) ? 0 : 100d * counts[IDX_CORRECT] / gold;
		double f1        = getF1(precision, recall);
		
		System.out.printf("%10s%10d%10.2f%10.2f%10.2f%10.2f\n", label, gold, dist, precision, recall, f1);
	}
		
	public double getF1LAS()
	{
		int[] counts = m_counts.get(UAS);
		
		double precision = 100d * counts[IDX_CORRECT] / counts[IDX_AUTO];
		double recall    = 100d * counts[IDX_CORRECT] / counts[IDX_GOLD];
		
		return getF1(precision, recall);
	}
	
	public int getTotalCount()
	{
		return m_counts.get(UAS)[IDX_GOLD];
	}
	
	static public double getF1(double precision, double recall)
	{
		return (precision + recall == 0) ? 0 : 2 * (precision * recall) / (precision + recall);
	}
}
