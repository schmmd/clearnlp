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
package com.googlecode.clearnlp.classification.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import com.carrotsearch.hppc.IntArrayList;
import com.googlecode.clearnlp.classification.train.AbstractTrainSpace;

/**
 * AdaGrad algorithm using logistic regression.
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class AdaGradLR extends AbstractAdaGrad
{
	/**
	 * @param alpha the learning rate.
	 * @param rho the smoothing denominator.
	 */
	public AdaGradLR(int iter, double alpha, double rho, Random rand)
	{
		super(iter, alpha, rho, rand);
	}
	
	@Override
	public double[] getWeight(AbstractTrainSpace space, int numThreads)
	{
		double[] weights = new double[space.getFeatureSize() * space.getLabelSize()];
		
		updateWeight(space, weights);
		return weights;
	}
	
	public void updateWeight(AbstractTrainSpace space)
	{
		updateWeight(space, space.getModel().getWeights());
	}
	
	public void updateWeight(AbstractTrainSpace space, double[] weights)
	{	
		final int D = space.getFeatureSize();
		final int L = space.getLabelSize();
		final int N = space.getInstanceSize();
		double[] gs = new double[D*L];
		
		IntArrayList        ys = space.getYs();
		ArrayList<int[]>    xs = space.getXs();
		ArrayList<double[]> vs = space.getVs();
		
		int[] indices;
		int i, j;
		
		int      yi;
		int[]    xi;
		double[] vi = null, grad;
		
		for (i=0; i<n_iter; i++)
		{
			indices = getShuffledIndices(N);
			Arrays.fill(gs, 0);
			
			for (j=0; j<N; j++)
			{
				yi = ys.get(indices[j]);
				xi = xs.get(indices[j]);
				if (space.hasWeight())	vi = vs.get(indices[j]);
				
				grad = getGradients(L, yi, xi, vi, weights);
				updateCounts(L, gs, grad, xi, vi);
				updateWeights(L, gs, grad, xi, vi, weights);
			}
		}
	}
	
	protected double[] getGradients(int L, int y, int[] x, double[] v, double[] weights)
	{
		double[] scores = getScores(L, x, v, weights);
		normalize(scores);

		int i; for (i=0; i<L; i++) scores[i] *= -1;
		scores[y] += 1;
		
		return scores;
	}
	
	protected void updateCounts(int L, double[] gs, double[] grad, int[] x, double[] v)
	{
		int i, label, len = x.length;
		double[] g = new double[L];
		double d;

		for (label=0; label<L; label++)
			g[label] = grad[label] * grad[label];
		
		if (v != null)
		{
			for (i=0; i<len; i++)
			{
				d = v[i] * v[i];
				
				for (label=0; label<L; label++)
					gs[getWeightIndex(L, label, x[i])] += d * g[label];
			}
		}
		else
		{
			for (i=0; i<len; i++)
				for (label=0; label<L; label++)
					gs[getWeightIndex(L, label, x[i])] += g[label];
		}
	}
	
	protected void updateWeights(int L, double[] gs, double[] grad, int[] x, double[] v, double[] weights)
	{
		int i, label, len = x.length;
		
		if (v != null)
		{
			for (i=0; i<len; i++)
				for (label=0; label<L; label++)
					weights[getWeightIndex(L, label, x[i])] += getUpdate(L, gs, label, x[i]) * grad[label] * v[i];
		}
		else
		{
			for (i=0; i<len; i++)
				for (label=0; label<L; label++)
					weights[getWeightIndex(L, label, x[i])] += getUpdate(L, gs, label, x[i]) * grad[label];
		}
	}
}
	