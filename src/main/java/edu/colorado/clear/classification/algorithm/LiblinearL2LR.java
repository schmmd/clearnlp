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
 * Liblinear L2-regularized logistic regression algorithm.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class LiblinearL2LR extends AbstractAlgorithm
{
	private double d_cost;
	private double d_eps;
	private double d_bias;
	
	/**
	 * Constructs the liblinear L2-regularized logistic regression algorithm.
	 * @param cost the cost.
	 * @param eps the tolerance of termination criterion.
	 * @param bias the bias.
	 */
	public LiblinearL2LR(double cost, double eps, double bias)
	{
		d_cost = cost;
		d_eps  = eps;
		d_bias = bias;
	}
	
	/* (non-Javadoc)
	 * @see edu.colorado.clear.classification.algorithm.IAlgorithm#getWeight(edu.colorado.clear.classification.train.AbstractTrainSpace, int)
	 */
	public double[] getWeight(AbstractTrainSpace space, int currLabel)
	{
		Random rand = new Random(1);
		
		final int MAX_ITER = 1000;
		final int MAX_ITER_NEWTON = 100;
		final int N = space.getInstanceSize();
		final int D = space.getFeatureSize();
		
		final double eta          = 0.1;
		final double innereps_min = Math.min(1e-8, d_eps);
				
		IntArrayList        ys = space.getYs();
		ArrayList<int[]>    xs = space.getXs();
		ArrayList<double[]> vs = space.getVs();
		
		double[] xTx    = new double[N];
		double[] alpha  = new double[2*N];
		double[] weight = new double[D];
		double C, ywTx, xisq, alpha_old, a, b, d, z, gp, gpp, tmpz;
		
		int [] index = new int [N];
		byte[] aY    = new byte[N];
		
		int i, j, s, iter, iter_newton, iter_inner, ind1, ind2, sign;
		byte     yi;
		int[]    xi;
		double[] vi = null;
		
		double   Gmax;
		double   innereps = 1e-2;
		double[] upper_bound = {d_cost, 0, d_cost};
		
		for (i=0; i<N; i++)
		{
			index[i] = i;
			aY   [i] = (ys.get(i) == currLabel) ? (byte)1 : (byte)-1;
			
			alpha[2*i  ] = Math.min(0.001 * upper_bound[GETI(aY, i)], 1e-8);
			alpha[2*i+1] = upper_bound[GETI(aY, i)] - alpha[2*i];

			d  = aY[i] * alpha[2*i];
			xi = xs.get(i);
			if (space.hasWeight())	vi = vs.get(i);
			
			if (d_bias > 0)
			{
				xTx[i]    += d_bias * d_bias;
				weight[0] += d      * d_bias;
			}
			
			for (j=0; j<xi.length; j++)
			{
				if (space.hasWeight())
				{
					xTx[i]        += vi[j] * vi[j];
					weight[xi[j]] += d     * vi[j];
				}
				else
				{
					xTx[i]        += 1;
					weight[xi[j]] += d;
				}
			}
		}
		
		for (iter=0; iter<MAX_ITER; iter++)
		{
			for (i=0; i<N; i++)
			{
				j = i + rand.nextInt(N - i);
				UTArray.swap(index, i, j);
			}
			
			iter_newton = 0;
			Gmax = 0;
			
			for (s=0; s<N; s++)
			{
				i    = index[s];
				yi   = aY[i];
				xi   = xs.get(i);
				xisq = xTx[i];
				C    = upper_bound[GETI(aY, i)];
				ywTx = (d_bias > 0) ? weight[0] * d_bias : 0;
								
				if (space.hasWeight())
				{
					vi = vs.get(i);
					
					for (j=0; j<xi.length; j++)
						ywTx += weight[xi[j]] * vi[j];
				}
				else
				{
					for (j=0; j<xi.length; j++)
						ywTx += weight[xi[j]];
				}
				
 				ywTx *= yi;
 				a = xisq;
 				b = ywTx;
 				
 				ind1 = 2*i;
 				ind2 = 2*i + 1;
 				sign = 1;
 				
 				// decide to minimize g_1(z) or g_2(z)
 				if (0.5 * a * (alpha[ind2] - alpha[ind1]) + b < 0) 
 				{
 					ind1 = 2*i + 1;
 					ind2 = 2*i;
 					sign = -1;
 				}
 				
 				// g_t(z) = z*log(z) + (C-z)*log(C-z) + 0.5a(z-alpha_old)^2 + sign*b(z-alpha_old)
 				alpha_old = alpha[ind1];
 				z = alpha_old;
 				if (C-z < 0.5*C)	z *= 0.1; 
 					
 				gp = a * (z-alpha_old) + sign * b + Math.log(z/(C-z));
 				Gmax = Math.max(Gmax, Math.abs(gp));
 				
 				// Newton method on the sub-problem
 				for (iter_inner=0; iter_inner<=MAX_ITER_NEWTON; iter_inner++) 
 				{
 					if (Math.abs(gp) < innereps)
 						break;
 					
 					gpp  = a + C/(C-z)/z;
 					tmpz = z - gp/gpp;
 					
 					if (tmpz <= 0)	z *= eta;
 					else 			z = tmpz;
 					
 					gp = a * (z-alpha_old) + sign * b + Math.log(z/(C-z));
 					iter_newton++;
 				}

 				if (iter_inner > 0)
 				{
 					alpha[ind1] = z;
 					alpha[ind2] = C-z;
 					d = sign * (z-alpha_old) * yi;

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
			
			if (Gmax < d_eps)
				break;
			
			if (iter_newton <= N/10) 
				innereps = Math.max(innereps_min, 0.1*innereps);
		}
		
		double v = 0;
		
		for (i=0; i<D; i++)
			v += weight[i] * weight[i];
		
		v *= 0.5;
		
		for (i=0; i<N; i++)
			v += alpha[2*i] * Math.log(alpha[2*i]) + alpha[2*i+1] * Math.log(alpha[2*i+1]) - upper_bound[GETI(aY,i)] * Math.log(upper_bound[GETI(aY,i)]);
		
		StringBuilder build = new StringBuilder();
		
		build.append("- label = ");
		build.append(currLabel);
		build.append(": iter = ");
		build.append(iter);
		build.append(", obj-value = ");
		build.append(v);

		System.out.println(build.toString());
		
		return weight;
	}
}
	