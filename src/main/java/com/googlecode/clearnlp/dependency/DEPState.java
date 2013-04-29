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
package com.googlecode.clearnlp.dependency;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.googlecode.clearnlp.util.pair.StringIntPair;

public class DEPState implements Comparable<DEPState>
{
	public int             lambda;	// index of lambfad
	public int             beta;	// index of beta
	public int             trans;	// number of transitions
	public double          score;	// sum of all previous scores
	public DEPLabel        label;
	public StringIntPair[] heads;
	public IntOpenHashSet  reduce;
	
	public DEPState(int lambda, int beta, int trans, double score, DEPLabel label, StringIntPair[] heads, IntOpenHashSet reduces)
	{
		this.lambda = lambda;
		this.beta   = beta;
		this.trans  = trans;
		this.score  = score;
		this.label  = label;
		this.heads  = heads;
		this.reduce = reduces;
	}
	
	@Override
	public int compareTo(DEPState p)
	{
		double diff = label.score - p.label.score;
		
		if      (diff > 0)	return -1;
		else if (diff < 0)	return  1;
		else				return  0;
	}
}
