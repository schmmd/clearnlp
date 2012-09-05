package com.googlecode.clearnlp.propbank.verbnet;

import org.w3c.dom.Element;

import com.googlecode.clearnlp.util.UTXml;


public class PVRole implements Comparable<PVRole>
{
	static public final String ATTR_N		= "n";
	static public final String ATTR_F		= "f";
	static public final String ATTR_VNTHETA	= "vntheta";

	String n;
	String f;
	String vntheta;
	
	public PVRole(Element eRole)
	{
		n       = UTXml.getTrimmedAttribute(eRole, ATTR_N);
		f       = UTXml.getTrimmedAttribute(eRole, ATTR_F);
		vntheta = UTXml.getTrimmedAttribute(eRole, ATTR_VNTHETA);
	}
	
	public PVRole(String n, String f, String vntheta)
	{
		this.n       = n;
		this.f       = f;
		this.vntheta = vntheta;
	}
	
	/** @param argn the numbered argument (e.g., ARG0). */
	public boolean isArgN(String argn)
	{
		return argn.length() > 3 && n.equals(argn.substring(3,4));
	}
	
	@Override
	public int compareTo(PVRole role)
	{
		int diff = n.compareTo(role.n);
		
		if      (diff > 0)	return  1;
		else if (diff < 0)	return -1;
		else				return f.compareTo(role.f);
	}
	
	public String toString()
	{
		return UTXml.startsElement(true, PVMap.E_ROLE, ATTR_N, n, ATTR_F, f, ATTR_VNTHETA, vntheta);		
	}
}
