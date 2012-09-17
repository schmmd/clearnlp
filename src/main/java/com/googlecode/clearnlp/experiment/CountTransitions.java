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

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPLibEn;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPParser;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.reader.SRLReader;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;


public class CountTransitions
{
	final int N = 10;
	
	public void countTransitions(String inputFile, boolean isSkip)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream("tmp.txt");
		int[] counts = new int[10];
		int[] totals = new int[10];
		
		DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);
		DEPParser parser = new DEPParser(fout);
		DEPTree   tree;
		int i, n=0, count, index;
		
		reader.open(UTInput.createBufferedFileReader(inputFile));
		
		while ((tree = reader.next()) != null)
		{
			if (isSkip && tree.getNonProjectiveSet().isEmpty())
				continue;
			
			parser.parse(tree);
			n++;

			count = parser.getNumTransitions();
			index = (tree.size() > 101) ? 9 : (tree.size()-2) / 10;
			
			counts[index] += count;
			totals[index] += 1;
		}

		fout.close();
		reader.close();
		
		System.out.println("# of trees: "+n);
		
		for (i=0; i<9; i++)
			System.out.printf("<= %2d: %4.2f (%d/%d)\n", (i+1)*10, (double)counts[i]/totals[i], counts[i], totals[i]);
		
		System.out.printf(" > %2d: %4.2f (%d/%d)\n", i*10, (double)counts[i]/totals[i], counts[i], totals[i]);
	}
	
	public void countArguments(String inputFile, boolean isGold)
	{
		SRLReader reader;
		if (isGold)	reader = new SRLReader(0, 1, 2, 4, 6, 7,  8, 12);
		else		reader = new SRLReader(0, 1, 3, 5, 6, 9, 10, 12);

		DEPTree tree;
		int[]   counts = new int[2];
		int[][] spaces = new int[3][N];
		
		reader.open(UTInput.createBufferedFileReader(inputFile));
		
		while ((tree = reader.next()) != null)
		{
			tree.setDependents();
			checkArguments(tree, counts, spaces);
		}
		
		System.out.printf("Argument coverage: %5.2f ( %d / %d )\n", 100d*counts[0]/counts[1], counts[0], counts[1]);
		
		int tPred = 0, t, i, j;
		double[] f = new double[spaces.length-1];
		
		for (i=0; i<N; i++)
		{
			t = spaces[f.length][i];
			
			for (j=0; j<f.length; j++)
				f[j] = (double)spaces[j][i] / t;
			
			System.out.printf("%3d: %5.2f ( %5d / %5d ) | %5.2f ( %5d / %5d )\n", i, f[0], spaces[0][i], t, f[1], spaces[1][i], t);
			tPred += t;
		}
		
		System.out.printf("# of predicates: %d\n", tPred);
	}
	
	private void checkArguments(DEPTree tree, int[] counts, int[][] spaces)
	{
		List<List<DEPArc>> list = tree.getArgumentList();
		int i, size = tree.size();
		Set<DEPNode> cans;
		List<DEPArc> args;
		DEPNode pred;
		
		int n = (size - 2) / N;
		if (n > N - 1)	n = N - 1;
		
		for (i=1; i<size; i++)
		{
			pred = tree.get(i);
			args = list.get(i);
			cans = pred.getArgumentCandidateSet(1, false);
			
			if (pred.getFeat(DEPLibEn.FEAT_PB) != null)
			{
				spaces[0][n] += size - 2;
				spaces[1][n] += cans.size();
				spaces[2][n] += 1;
			}
			
			if (args.isEmpty())	continue;
			
			for (DEPArc arc : args)
			{
				if (cans.contains(arc.getNode()))
					counts[0]++;
			}
			
			counts[1] += args.size();
		}
	}
	
	static public void main(String[] args)
	{
		CountTransitions c = new CountTransitions();
		
		c.countArguments(args[0], Boolean.parseBoolean(args[1]));
	//	c.countTransitions(args[0], Boolean.parseBoolean(args[1]));
	}
}
