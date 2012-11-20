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
package com.googlecode.clearnlp.dependency;

import com.googlecode.clearnlp.util.pair.StringIntPair;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class DEPTransitionState
{
	public StringIntPair[]	p_heads;
	public boolean[]		b_skips;
	public DEPNode[]		lm_deps, rm_deps;
	public DEPNode[]		ln_sibs, rn_sibs;
	public int 				i_lambda, i_beta, t_size;
	
	/** @param size the size of a dependency tree. */
	public DEPTransitionState(int size)
	{
		init(size);
	}

	/** @param size the size of a dependency tree. */
	public void init(int size)
	{
	 	i_lambda = 0;
	 	i_beta   = 1;
	 	t_size   = size;
	 	p_heads  = new StringIntPair[size];
	 	b_skips  = new boolean[size];
		lm_deps  = new DEPNode[size];
	 	rm_deps  = new DEPNode[size];
	 	ln_sibs  = new DEPNode[size];
	 	rn_sibs  = new DEPNode[size];
	 	
	 	int i; for (i=0; i<size; i++)
	 		p_heads[i] = new StringIntPair(null, -1);
	}
	
	public boolean isTerminate()
	{
		return i_beta >= t_size;
	}
	
	public boolean isDeterministicShift()
	{
		return i_lambda < 0;
	}
	
	public boolean isLambdaRoot()
	{
		return i_lambda == DEPLib.ROOT_ID;
	}
	
	public boolean isLambdaDescendentOfBeta()
	{
		return isDescendentOf(i_lambda, i_beta);
	}
	
	public boolean isBetaDescendentOfLambda()
	{
		return isDescendentOf(i_beta, i_lambda);
	}
	
	/** @return {@code true} if the {@code i}'th node is a descendent of the {@code j}'th node. */
	public boolean isDescendentOf(int i, int j)
	{
		int headId = p_heads[i].i;
		
		while (headId >= 0)
		{
			if (headId == j)
				return true;
			
			headId = p_heads[headId].i;
		}
		
		return false;
	}
	
	public boolean hasLambdaHead()
	{
		return hasHead(i_lambda);
	}
	
	public boolean hasBetaHead()
	{
		return hasHead(i_beta);
	}

	/** @return {@code true} if the {@code i}'th node has a head. */
	public boolean hasHead(int i)
	{
		return p_heads[i].i >= 0;
	}
}
