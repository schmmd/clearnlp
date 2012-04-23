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
package edu.colorado.clear.classification.algorithm;

import java.util.ArrayList;
import java.util.Random;

import com.carrotsearch.hppc.IntArrayList;

import edu.colorado.clear.classification.train.AbstractTrainSpace;
import edu.colorado.clear.util.UTArray;

/**
 * Liblinear L2-regularized support vector classification algorithm.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class LiblinearL2SV extends AbstractAlgorithm
{
	private byte   i_lossType;
	private double d_cost;
	private double d_eps;
	private double d_bias;
	
	/**
	 * Constructs the liblinear L2-regularized support vector classification algorithm.
	 * @param lossType 1 for L1-loss, 2 for L2-loss.
	 * @param cost the cost.
	 * @param eps the tolerance of termination criterion.
	 * @param bias the bias.
	 */
	public LiblinearL2SV(byte lossType, double cost, double eps, double bias)
	{
		i_lossType = lossType;
		d_cost     = cost;
		d_eps      = eps;
		d_bias     = bias;
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
	