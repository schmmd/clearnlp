package com.googlecode.clearnlp.pos;

import com.googlecode.clearnlp.classification.prediction.StringPrediction;

public class POSState
{
	public int              input;	// index of input
	public double           score;	// sum of all previous scores
	public StringPrediction label;
	
	public POSState(int input, double score, StringPrediction label)
	{
		this.input = input;
		this.score = score;
		this.label = label;
	}
}
