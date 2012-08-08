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
package com.goolgecode.clearnlp.classification.algorithm;

import com.goolgecode.clearnlp.classification.train.AbstractTrainSpace;

/**
 * Abstract algorithm.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
abstract public class AbstractAlgorithm
{
	/** The flag to indicate L2-regularized L1-loss support vector classification (dual). */
	static public final byte SOLVER_LIBLINEAR_LR2_L1_SV = 0;
	/** The flag to indicate L2-regularized L2-loss support vector classification (dual). */
	static public final byte SOLVER_LIBLINEAR_LR2_L2_SV = 1;
	/** The flag to indicate L2-regularized logistic regression (dual). */
	static public final byte SOLVER_LIBLINEAR_LR2_LR    = 2;
	
	/**
	 * Returns the weight vector for the specific label given the training space.
	 * @param space the training space.
	 * @param currLabel the label to get the weight vector for.
	 * @return the weight vector for the specific label given the training space.
	 */
	abstract public double[] getWeight(AbstractTrainSpace space, int currLabel);
	
	/** Used for liblinear algorithms. */
	protected int GETI(byte[] y, int i)
	{
		return y[i] + 1;
	}
}
