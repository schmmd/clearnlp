package com.googlecode.clearnlp.dependency;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.googlecode.clearnlp.util.pair.StringIntPair;

public class DEPState
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
}
