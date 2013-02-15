package com.googlecode.clearnlp.dependency;

public class DEPCountArc implements Comparable<DEPCountArc>
{
	public int count, order, depId, headId;
	public String deprel;
	
	public DEPCountArc(int count, int order, int depId, int headId, String deprel)
	{
		this.count  = count;
		this.order  = order;
		this.depId  = depId;
		this.headId = headId;
		this.deprel = deprel;
	}
	
	@Override
	public int compareTo(DEPCountArc p)
	{
		int n = p.count - count;
		return (n == 0) ? order - p.order : n;
	}
}