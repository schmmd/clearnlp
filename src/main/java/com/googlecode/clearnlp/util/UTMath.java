package com.googlecode.clearnlp.util;


public class UTMath
{
	static public int[][] getCombinations(int n)
	{
		int i, j, k, v = 1, t = (int)Math.pow(2, n);
		int[][] b = new int[t][n];
		
		for (i=0; i<n; i++)
		{
			k = (int)Math.pow(2, i);
			
			for (j=0; j<t; j++)
			{
				if (j%k == 0)
					v = (v + 1) % 2;
				
				b[j][i] = v;
			}
		}
		
		return b;
	}
}
