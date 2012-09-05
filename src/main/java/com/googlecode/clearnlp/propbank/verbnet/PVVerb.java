package com.googlecode.clearnlp.propbank.verbnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.googlecode.clearnlp.util.UTXml;


@SuppressWarnings("serial")
public class PVVerb extends HashMap<String,PVRoleset>
{
	static public final String ATTR_LEMMA = "lemma";
	String s_lemma;
	
	public PVVerb(Element eVerb, String lemma, boolean fromMap)
	{
		NodeList list = eVerb.getElementsByTagName(PVMap.E_ROLESET);
		int i, size = list.getLength();
		Element   eRoleset;
		String    rolesetId;
		PVRoleset pvRoleset;
		
		s_lemma = lemma;
		
		for (i=0; i<size; i++)
		{
			eRoleset  = (Element)list.item(i);
			rolesetId = UTXml.getTrimmedAttribute(eRoleset, PVRoleset.ATTR_ID);
			pvRoleset = new PVRoleset(eRoleset, rolesetId, fromMap);
			
			if (!pvRoleset.isEmpty())	put(rolesetId, pvRoleset);
		}
	}
	
	public String toString()
	{
		List<String> rolesetIds = new ArrayList<String>(keySet());
		Collections.sort(rolesetIds);
		
		StringBuilder build = new StringBuilder();
		
		for (String rolesetId : rolesetIds)
		{
			build.append("\n");
			build.append(get(rolesetId));
		}
		
		return UTXml.getTemplate(PVMap.E_VERB, build.substring(1), "", ATTR_LEMMA, s_lemma);
	}
}
