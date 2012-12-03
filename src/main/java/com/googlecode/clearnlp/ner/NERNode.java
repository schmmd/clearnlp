package com.googlecode.clearnlp.ner;

import com.googlecode.clearnlp.pos.POSNode;

public class NERNode extends POSNode
{
	/** The named entity tag of this node (default: null). */
	public String nament;
	
	public boolean isNamex(String namex)
	{
		return this.nament.equals(namex);
	}
}
