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
import java.util.Random;

import com.carrotsearch.hppc.IntArrayList;
import com.googlecode.clearnlp.classification.train.AbstractTrainSpace;
import com.googlecode.clearnlp.util.UTArray;


/**
 * Liblinear L2-regularized support vector classification algorithm.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class LiblinearL2SVPREC extends AbstractAlgorithm
{
	private byte   i_lossType;
	private double d_cost;
	private double d_eps;
	private double d_bias;
    private double p_bias;
	
	/**
	 * Constructs the liblinear L2-regularized support vector classification algorithm.
	 * @param lossType 1 for L1-loss, 2 for L2-loss.
	 * @param cost the cost.
	 * @param eps the tolerance of termination criterion.
	 * @param bias the bias.
	 */
	public LiblinearL2SVPREC(byte lossType, double cost, double eps, double bias,double prec_bias)
	{
		i_lossType = lossType;
		d_cost     = cost;
		d_eps      = eps;
		d_bias     = bias;
        p_bias = prec_bias;
	}
	
	/* (non-Javadoc)
	 * @see edu.colorado.clear.classification.algorithm.IAlgorithm#getWeight(edu.colorado.clear.classification.train.AbstractTrainSpace, int)
	 */
	public double[] getWeight(AbstractTrainSpace space, int currLabel)
	{
		Random rand = new Random(5);
		
		final int MAX_ITER = 1000;
		final int N = space.getInstanceSize();
		final int D = space.getFeatureSize();
		
		IntArrayList        ys = space.getYs();
		ArrayList<int[]>    xs = space.getXs();
		ArrayList<double[]> vs = space.getVs();
		
		double[] QD     = new double[N];
		double[] alpha  = new double[N];
		double[] weight = new double[D];
		double U, G, d, alpha_old;
		int [] index = new int [N];
		byte[] aY    = new byte[N];
		
		int active_size = N;
		int i, j, s, iter;
		byte     yi;
		int[]    xi;
		double[] vi = null;
		
		// PG: projected gradient, for shrinking and stopping
		double PG;
		double PGmax_old = Double.POSITIVE_INFINITY;
		double PGmin_old = Double.NEGATIVE_INFINITY;
		double PGmax_new, PGmin_new;
		
		// for loss function
		double[] diag        = {0, 0, 0};
		double[] upper_bound = {d_cost, 0, d_cost};
		
		if (i_lossType == 2)
		{
			diag[0] = 0.5 / d_cost;
			diag[2] = 0.5 / d_cost;
			upper_bound[0] = Double.POSITIVE_INFINITY;
			upper_bound[2] = Double.POSITIVE_INFINITY;
		}
		
		for (i=0; i<N; i++)
		{
			index[i] = i;
			aY   [i] = (ys.get(i) == currLabel) ? (byte)1 : (byte)-1;
			QD   [i] = diag[GETI(aY, i)];

			if (d_bias > 0)	QD[i] += d_bias * d_bias;
			
			if (space.hasWeight())
			{
				for (double value : vs.get(i))
					QD[i] += value * value;
			}
			else
			{
				QD[i] += xs.get(i).length;				
			}
		}
		
		for (iter=0; iter<MAX_ITER; iter++)
		{
			PGmax_new = Double.NEGATIVE_INFINITY;
			PGmin_new = Double.POSITIVE_INFINITY;
			
			for (i=0; i<active_size; i++)
			{
				j = i + rand.nextInt(active_size - i);
				UTArray.swap(index, i, j);
			}
			
			for (s=0; s<active_size; s++)
			{
				i  = index[s];
				yi = aY[i];
				xi = xs.get(i);
                if(yi == 1)
				    U  = p_bias * upper_bound[GETI(aY, i)];
                else
                   U  = upper_bound[GETI(aY, i)];

				G  = (d_bias > 0) ? weight[0] * d_bias : 0;
								
				if (space.hasWeight())
				{
					vi = vs.get(i);
					
					for (j=0; j<xi.length; j++)
						G += weight[xi[j]] * vi[j];
				}
				else
				{
					for (j=0; j<xi.length; j++)
						G += weight[xi[j]];
				}
				
 				G = G * yi - 1;
				G += alpha[i] * diag[GETI(aY, i)];
				
				if (alpha[i] == 0)
				{
					if (G > PGmax_old)
					{
						active_size--;
						UTArray.swap(index, s, active_size);
						s--;
						continue;
					}
					
					PG = Math.min(G, 0);
                }
				else if (alpha[i] == U)
				{
					if (G < PGmin_old)
					{
						active_size--;
						UTArray.swap(index, s, active_size);
						s--;
						continue;
					}
					
					PG = Math.max(G, 0);
				}
				else
				{
					PG = G;
				}
				
				PGmax_new = Math.max(PGmax_new, PG);
				PGmin_new = Math.min(PGmin_new, PG);
				
				if (Math.abs(PG) > 1.0e-12)
				{
					alpha_old = alpha[i];
					alpha[i] = Math.min(Math.max(alpha[i] - G / QD[i], 0.0), U);
					d = (alpha[i] - alpha_old) * yi;
					
					if (d_bias > 0)	weight[0] += d * d_bias;
					
					for (j=0; j<xi.length; j++)
					{
						if (space.hasWeight())
							weight[xi[j]] += d * vi[j];
						else
							weight[xi[j]] += d;
					}
				}
			}
			
			if (PGmax_new - PGmin_new <= d_eps)
			{
				if (active_size == N)
					break;
				else
				{
					active_size = N;
					PGmax_old = Double.POSITIVE_INFINITY;
					PGmin_old = Double.NEGATIVE_INFINITY;
					continue;
				}
			}
			
			PGmax_old = PGmax_new;
			PGmin_old = PGmin_new;
			if (PGmax_old <= 0) PGmax_old = Double.POSITIVE_INFINITY;
			if (PGmin_old >= 0) PGmin_old = Double.NEGATIVE_INFINITY;
		}
		
		int nSV = 0;
		
		for (i = 0; i < N; i++)
			if (alpha[i] > 0) ++nSV;
		
		StringBuilder build = new StringBuilder();
		
		build.append("- label = ");
		build.append(currLabel);
		build.append(": iter = ");
		build.append(iter);
		build.append(", nSV = ");
		build.append(nSV);
		System.out.println(build.toString());
		
		return weight;
	}
}
	