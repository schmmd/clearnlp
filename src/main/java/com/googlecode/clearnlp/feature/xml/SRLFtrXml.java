/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package com.googlecode.clearnlp.feature.xml;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.util.UTXml;

/**
 * @since 1.2.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class SRLFtrXml extends AbstractFtrXml
{
	static private final String  CUTOFF_DOWN = "down";
	static private final String  CUTOFF_UP   = "up";
	
	static public final char S_PREDICAT = 'p';
	static public final char S_ARGUMENT = 'a';
	
	static public final String R_H		= "h";		// head
	static public final String R_LMD	= "lmd";	// leftmost dependent
	static public final String R_RMD	= "rmd";	// rightmost dependent
	static public final String R_LND	= "lnd";	// left-nearest dependent
	static public final String R_RND	= "rnd";	// right-nearest dependent
	static public final String R_LNS	= "lns";	// left-nearest sibling
	static public final String R_RNS	= "rns";	// right-nearest sibling

	static public final String F_FORM		= "f";
	static public final String F_LEMMA		= "m";
	static public final String F_POS		= "p";
	static public final String F_DEPREL		= "d";
	static public final String F_DISTANCE	= "n";
	static public final String F_DEPREL_SET	= "ds";
	static public final String F_GRAND_DEPREL_SET = "gds";
	
	static public final Pattern P_FEAT		= Pattern.compile("^ft=(.+)$");		
	static public final Pattern P_BOOLEAN	= Pattern.compile("^b(\\d+)$");
	static public final Pattern P_SUBCAT 	= Pattern.compile("^sc(["+F_POS+F_DEPREL+"])(\\d+)$");
	static public final Pattern P_PATH	 	= Pattern.compile("^pt(["+F_POS+F_DEPREL+F_DISTANCE+"])(\\d+)$");
	static public final Pattern P_ARGN 	 	= Pattern.compile("^argn(\\d+)$");
	
	static protected final Pattern P_REL	= Pattern.compile(R_H+"|"+R_LMD+"|"+R_RMD+"|"+R_LND+"|"+R_RND+"|"+R_LNS+"|"+R_RNS);
	static protected final Pattern P_FIELD	= Pattern.compile(F_FORM+"|"+F_LEMMA+"|"+F_POS+"|"+F_DEPREL+"|"+F_DISTANCE+"|"+F_DEPREL_SET+"|"+F_GRAND_DEPREL_SET);
	
	protected final String XML_LEXICA_PREDICATE = "predicate";
	private int cutoff_down, cutoff_up;
	private Pattern p_predicates;
	
	public SRLFtrXml(InputStream fin)
	{
		super(fin);
	}
	
	public SRLFtrXml(InputStream fin, boolean skipInvisible)
	{
		super(fin, skipInvisible);
	}
	
	protected void initCutoffMore(NodeList eList)
	{
		int i, size = eList.getLength();
		Element eCutoff;
		
		for (i=0; i<size; i++)
		{
			eCutoff = (Element)eList.item(i);
			
			cutoff_down = eCutoff.hasAttribute(CUTOFF_DOWN) ? Integer.parseInt(eCutoff.getAttribute(CUTOFF_DOWN)) : 0;
			cutoff_up   = eCutoff.hasAttribute(CUTOFF_UP)   ? Integer.parseInt(eCutoff.getAttribute(CUTOFF_UP))   : 0;
		}
	}
	
	public int getDownCutoff()
	{
		return cutoff_down;
	}
	
	public int getUpCutoff()
	{
		return cutoff_up;
	}
	
	public boolean isPredicate(DEPNode node)
	{
		return p_predicates.matcher(node.pos).find();
	}

	protected void initMore(Document doc) throws Exception
	{
		initMoreLexica(doc);
	}
	
	private void initMoreLexica(Document doc)
	{
		NodeList eList = doc.getElementsByTagName(XML_LEXICA);
		int i, size = eList.getLength();
		String type, label;
		Element eLexica;
		
		for (i=0; i<size; i++)
		{
			eLexica = (Element)eList.item(i);
			type    = UTXml.getTrimmedAttribute(eLexica, XML_TYPE);
			label   = UTXml.getTrimmedAttribute(eLexica, XML_LABEL);
			
			if (type.equals(XML_LEXICA_PREDICATE))
				p_predicates = Pattern.compile("^"+label+"$");
		}
	}
	
	@Override
	protected boolean validSource(char source)
	{
		return source == S_PREDICAT || source == S_ARGUMENT;
	}

	@Override
	protected boolean validRelation(String relation)
	{
		return P_REL.matcher(relation).matches();
	}
	
	protected boolean validField(String field)
	{
		return P_FIELD  .matcher(field).matches() ||  
			   P_FEAT   .matcher(field).matches() ||
			   P_BOOLEAN.matcher(field).matches() ||  
			   P_SUBCAT .matcher(field).matches() ||
			   P_PATH   .matcher(field).matches() ||
			   P_ARGN   .matcher(field).matches();
	}
}