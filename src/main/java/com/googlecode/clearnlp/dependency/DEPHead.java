package com.googlecode.clearnlp.dependency;

public class DEPHead
{
	public int    headId;
	public String deprel;
	public double score;
	
	public DEPHead(int headId, String deprel, double score)
	{
		set(headId, deprel, score);
	}
	
	public void set(int headId, String deprel, double score)
	{
		this.headId = headId;
		this.deprel = deprel;
		this.score  = score;
	}
}
