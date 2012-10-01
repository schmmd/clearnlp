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

/**
 * Liblinear L2-regularized support vector classification algorithm.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class LiblinearL2SVPREC extends LiblinearL2SV
{
	private double p_bias;
	
	/**
	 * Constructs the liblinear L2-regularized support vector classification algorithm.
	 * @param lossType 1 for L1-loss, 2 for L2-loss.
	 * @param cost the cost.
	 * @param eps the tolerance of termination criterion.
	 * @param bias the bias.
	 * @param pBias the precision bias.
	 */
	public LiblinearL2SVPREC(byte lossType, double cost, double eps, double bias, double pBias)
	{
		super(lossType, cost, eps, bias);
		p_bias = pBias;
	}
	
	protected double getU(double[] upper_bound, byte[] aY, int i)
	{
		double U = upper_bound[GETI(aY, i)];
		return (aY[i] == -1) ? p_bias * U : U;
	}
}
	