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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import com.carrotsearch.hppc.IntArrayList;
import com.googlecode.clearnlp.classification.prediction.IntPrediction;
import com.googlecode.clearnlp.classification.train.AbstractTrainSpace;

/**
 * AdaGrad algorithm using hinge loss.
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class AdaGradHinge extends AbstractAdaGrad
{
	/**
	 * @param alpha the learning rate.
	 * @param rho the smoothing denominator.
	 */
	public AdaGradHinge(int iter, double alpha, double rho, Random rand)
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
		
		IntPrediction max;
		int[] indices;
		int i, j;
		
		int      yi;
		int[]    xi;
		double[] vi = null;
		
		for (i=0; i<n_iter; i++)
		{
			indices = getShuffledIndices(N);
			Arrays.fill(gs, 0);
			
			for (j=0; j<N; j++)
			{
				yi = ys.get(indices[j]);
				xi = xs.get(indices[j]);
				if (space.hasWeight())	vi = vs.get(indices[j]);
				
				max = getPrediction(L, yi, xi, vi, weights);
				
				if (max.label != yi)
				{
					updateCounts (L, gs, yi, max.label, xi, vi);
					updateWeights(L, gs, yi, max.label, xi, vi, weights);
				}
			}
		}
	}
	
	protected IntPrediction getPrediction(int L, int y, int[] x, double[] v, double[] weights)
	{
		double[] scores = getScores(L, x, v, weights);
		scores[y] -= 1;
		
		IntPrediction max = new IntPrediction(0, scores[0]);
		int label;
		
		for (label=1; label<L; label++)
		{
			if (max.score < scores[label])
				max.set(label, scores[label]);
		}
	
		return max;
	}
	
	protected void updateCounts(int L, double[] gs, int yp, int yn, int[] x, double[] v)
	{
		int i, len = x.length;
		
		if (v != null)
		{
			double d;
			
			for (i=0; i<len; i++)
			{
				d = v[i] * v[i];
				
				gs[getWeightIndex(L, yp, x[i])] += d;
				gs[getWeightIndex(L, yn, x[i])] += d;
			}
		}
		else
		{
			for (i=0; i<len; i++)
			{
				gs[getWeightIndex(L, yp, x[i])]++;
				gs[getWeightIndex(L, yn, x[i])]++;
			}
		}
	}
	
	protected void updateWeights(int L, double[] gs, int yp, int yn, int[] x, double[] v, double[] weights)
	{
		int i, xi, len = x.length;
		double vi;
		
		if (v != null)
		{
			for (i=0; i<len; i++)
			{
				xi = x[i]; vi = v[i];
				weights[getWeightIndex(L, yp, xi)] += getUpdate(L, gs, yp, xi) * vi;
				weights[getWeightIndex(L, yn, xi)] -= getUpdate(L, gs, yn, xi) * vi;
			}
		}
		else
		{
			for (i=0; i<len; i++)
			{
				xi = x[i];
				weights[getWeightIndex(L, yp, xi)] += getUpdate(L, gs, yp, xi);
				weights[getWeightIndex(L, yn, xi)] -= getUpdate(L, gs, yn, xi);
			}
		}
	}
}
	