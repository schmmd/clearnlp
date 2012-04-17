package edu.colorado.clear.conversion;

import edu.colorado.clear.constituent.CTNode;
import edu.colorado.clear.dependency.DEPFeat;

public class C2DInfo
{
	CTNode d_head;
	CTNode p_head;
	String s_label;
	DEPFeat d_feats;
	
	private boolean b_head;
	
	/** Initializes the dependency head of a constituent. */
	public C2DInfo(CTNode head)
	{
		s_label = null;
		b_head  = false;
		
		if (head.c2d == null)	// for terminals: head = itself
		{
			d_head  = head;
			p_head  = null;
			d_feats = new DEPFeat();
		}
		else					// for phrases: head = child
		{
			d_head = head.c2d.getDependencyHead();
			p_head = head;
		}
	}
	
	/** Sets heads for siblings */
	public void setHead(CTNode head, String label)
	{
		d_head.c2d.d_head = head.c2d.getDependencyHead();
		setLabel(label);
		b_head = true;
	}
	
	public void setHeadTerminal(CTNode head, String label)
	{
		d_head.c2d.d_head = head;
		setLabel(label);
		b_head = true;
	}
	
	public boolean hasHead()
	{
		return b_head;
	}
	
	public void setLabel(String label)
	{
		if (p_head == null)
			s_label = label;
		else
			d_head.c2d.s_label = label;
	}
	
	public String getLabel()
	{
		return s_label;
	}
	
	public String putFeat(String key, String value)
	{
		return d_head.c2d.d_feats.put(key, value);
	}
	
	public String getFeat(String key)
	{
		return d_feats.get(key);
	}
	
	public CTNode getDependencyHead()
	{
		return d_head;
	}
	
	public CTNode getPhraseHead()
	{
		return p_head;
	}
}
