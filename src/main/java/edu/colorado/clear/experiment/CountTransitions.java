package edu.colorado.clear.experiment;

import java.util.List;
import java.util.Set;

import edu.colorado.clear.dependency.DEPArc;
import edu.colorado.clear.dependency.DEPLibEn;
import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.reader.SRLReader;
import edu.colorado.clear.util.UTInput;

public class CountTransitions
{
	final int N = 10;
	
	public CountTransitions(String inputFile, boolean isGold)
	{
		SRLReader reader;
		if (isGold)	reader = new SRLReader(0, 1, 2, 4, 6, 7,  9, 12);
		else		reader = new SRLReader(0, 1, 3, 5, 6, 8, 10, 12);

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
		new CountTransitions(args[0], Boolean.parseBoolean(args[1]));
	}
}
