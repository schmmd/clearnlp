package com.googlecode.clearnlp.propbank.verbnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.googlecode.clearnlp.util.UTXml;


@SuppressWarnings("serial")
public class PVRoleset extends HashMap<String,PVRoles>
{
	static public final String ATTR_ID = "id";
	String s_rolesetId;
	
	public PVRoleset(Element eRoleset, String rolesetId, boolean fromMap)
	{
		s_rolesetId = rolesetId;
		
		if (fromMap)	initRolesFromMap(eRoleset);
		else			initRolesFromFrameset(eRoleset);
	}
	
	private void initRolesFromMap(Element eRoleset)
	{
		NodeList list = eRoleset.getElementsByTagName(PVMap.E_ROLES);
		int i, size = list.getLength();
		Element eRoles;
		String  vncls;
		
		for (i=0; i<size; i++)
		{
			eRoles = (Element)list.item(i);
			vncls  = UTXml.getTrimmedAttribute(eRoles, PVRoles.ATTR_VNCLS);
			
			put(vncls, new PVRoles(eRoles, vncls));
		}
	}
	
	private void initRolesFromFrameset(Element eRoleset)
	{
		String[] vnclses = UTXml.getTrimmedAttribute(eRoleset, PVRoles.ATTR_VNCLS).split(" ");
		NodeList nRoles  = eRoleset.getElementsByTagName(PVMap.E_ROLE);
		PVRoles  pvRoles;
		
		for (String vncls : vnclses)
		{
			if (vncls.isEmpty() || vncls.equals("-"))	continue;
			pvRoles = new PVRoles(nRoles, vncls);
			
			if (pvRoles.isEmpty())
				System.err.println("Mismatch: "+s_rolesetId+" "+vncls);
			else
				put(vncls, pvRoles);
		}
	}
	
	public PVRoles getSubVNRoles(String superVNClass)
	{
		for (String vncls : keySet())
		{
			if (vncls.startsWith(superVNClass))
				return get(vncls);
		}
		
		return null;
	}
	
	public PVRoles getSuperVNRoles(String subVNClass)
	{
		for (String vncls : keySet())
		{
			if (subVNClass.startsWith(vncls))
				return get(vncls);
		}
		
		return null;
	}
	
	public String toString()
	{
		List<String> vnclses = new ArrayList<String>(keySet());
		Collections.sort(vnclses);
		
		StringBuilder build = new StringBuilder();
		
		for (String vncls : vnclses)
		{
			build.append("\n");
			build.append(get(vncls));				
		}
		
		return UTXml.getTemplate(PVMap.E_ROLESET, build.substring(1), "  ", ATTR_ID, s_rolesetId);
	}
}
