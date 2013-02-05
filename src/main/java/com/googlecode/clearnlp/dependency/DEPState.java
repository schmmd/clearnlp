package com.googlecode.clearnlp.dependency;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.googlecode.clearnlp.util.pair.StringIntPair;

public class DEPState
{
	public int             lambda;
	public int             beta;
	public DEPLabel        label;
	public StringIntPair[] heads;
	public IntOpenHashSet  reduce;
	
	public DEPState(int lambda, int beta, DEPLabel label, StringIntPair[] heads, IntOpenHashSet reduces)
	{
		this.lambda = lambda;
		this.beta   = beta;
		this.label  = label;
		this.heads  = heads;
		this.reduce = reduces;
	}
}
