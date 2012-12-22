package com.googlecode.clearnlp.coreference;

public class Mention
{
	public String id;
	public String type;
	public int    beginIndex;
	public int    endIndex;

	public Mention(String id, int beginIndex, int endIndex)
	{
		init(id, null, beginIndex, endIndex);
	}
	
	public Mention(String id, String type, int beginIndex, int endIndex)
	{
		init(id, type, beginIndex, endIndex);
	}
	
	public void init(String id, String type, int beginIndex, int endIndex)
	{
		this.id         = id;
		this.type       = type;
		this.beginIndex = beginIndex;
		this.endIndex   = endIndex;
	}
}
