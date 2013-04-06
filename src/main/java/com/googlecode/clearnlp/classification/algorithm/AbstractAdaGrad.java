/**
* Copyright 2012 University of Massachusetts Amherst
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
package com.googlecode.clearnlp.classification.algorithm;

import java.util.Random;

import com.googlecode.clearnlp.util.UTArray;

/**
 * Abstract algorithm.
 * @since 1.3.2
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
abstract public class AbstractAdaGrad extends AbstractAlgorithm
{
	protected int    n_iter;
	protected Random r_rand;
	protected double d_alpha;
	protected double d_rho;
	
	public AbstractAdaGrad(int iter, double alpha, double rho, Random rand)
	{
		n_iter  = iter;
		r_rand  = rand;
		d_alpha = alpha;
		d_rho   = rho;
	}
	
	protected int[] getShuffledIndices(int N)
	{
		int[] indices = new int[N];
		int i, j;
		
		for (i=0; i<N; i++)
			indices[i] = i;
		
		for (i=0; i<N; i++)
		{
			j = i + r_rand.nextInt(N - i);
			UTArray.swap(indices, i, j);
		}
		
		return indices;
	}
	
	protected double[] getScores(int L, int[] x, double[] v, double[] weights)
	{
		double[] scores = new double[L];
		int i, label, len = x.length;
		
		if (v != null)
		{
			for (i=0; i<len; i++)
				for (label=0; label<L; label++)
					scores[label] += weights[getWeightIndex(L, label, x[i])] * v[i];
		}
		else
		{
			for (i=0; i<len; i++)
				for (label=0; label<L; label++)
					scores[label] += weights[getWeightIndex(L, label, x[i])];
		}
		
		return scores;
	}
	
	protected double getUpdate(int L, double[] gs, int y, int x)
	{
		return d_alpha / (d_rho + Math.sqrt(gs[getWeightIndex(L, y, x)]));
	}
}
