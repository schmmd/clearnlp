/**
* Copyright 2012-2013 University of Massachusetts Amherst
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
*   
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
