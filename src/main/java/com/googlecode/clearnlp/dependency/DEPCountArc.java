package com.googlecode.clearnlp.dependency;

public class DEPCountArc implements Comparable<DEPCountArc>
{
	public int order, depId, headId;
	public double count;
	public String deprel;
	
	public DEPCountArc(double count, int order, int depId, int headId, String deprel)
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
		double n = p.count - count;
		
		if      (n > 0)	return  1;
		else if (n < 0)	return -1;
		else			return order - p.order;
	}
}