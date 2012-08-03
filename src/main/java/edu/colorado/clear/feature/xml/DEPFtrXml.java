/**
* Copyright (c) 2011, Regents of the University of Colorado
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
package edu.colorado.clear.feature.xml;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.colorado.clear.util.UTXml;
import edu.colorado.clear.util.pair.StringIntPair;

/**
 * Dependency feature template.
 * @author Jinho D. Choi
 * <b>Last update:</b> 4/12/2011
 */
public class DEPFtrXml extends AbstractFtrXml
{
	static protected final String XML_LEXICA_PUNCTUATION = "punctuation";
	
	static public final char   S_LAMBDA	= 'l';
	static public final char   S_STACK	= 's';
	static public final char   S_BETA	= 'b';
	
	static public final String R_H		= "h";		// head
	static public final String R_LMD	= "lmd";	// leftmost dependent
	static public final String R_RMD	= "rmd";	// rightmost dependent
	
	static public final String F_FORM	= "f";
	static public final String F_LEMMA	= "m";
	static public final String F_POS	= "p";
	static public final String F_DEPREL	= "d";
	static public final String F_LNPL	= "lnpl";	// left-nearest punctuation of lambda
	static public final String F_RNPL	= "rnpl";	// right-nearest punctuation of lambda
	static public final String F_LNPB	= "lnpb";	// left-nearest punctuation of beta
	static public final String F_RNPB	= "rnpb";	// right-nearest punctuation of beta
	
	static public final Pattern P_FEAT	  = Pattern.compile("^ft=(.+)$");		
	static public final Pattern P_BOOLEAN = Pattern.compile("^b(\\d+)$");
	
	static protected final Pattern P_REL	= Pattern.compile(R_H+"|"+R_LMD+"|"+R_RMD);
	static protected final Pattern P_FIELD	= Pattern.compile(F_FORM+"|"+F_LEMMA+"|"+F_POS+"|"+F_DEPREL+"|"+F_LNPL+"|"+F_RNPL+"|"+F_LNPB+"|"+F_RNPB);
	
	private StringIntPair p_punc;
	
	public DEPFtrXml(InputStream fin)
	{
		super(fin);
	}
	
	public DEPFtrXml(InputStream fin, boolean skipInvisible)
	{
		super(fin, skipInvisible);
	}
	
	public String getPunctuationLabel()
	{
		return p_punc.s;
	}
	
	public int getPunctuationCutoff()
	{
		return p_punc.i;
	}
	
	protected void initCutoffMore(NodeList eList) {}
	
	protected void initMore(Document doc) throws Exception
	{
		initMoreLexica(doc);
	}
	
	private void initMoreLexica(Document doc)
	{
		NodeList eList = doc.getElementsByTagName(XML_LEXICA);
		int i, cutoff, size = eList.getLength();
		String type, label;
		Element eLexica;
		
		for (i=0; i<size; i++)
		{
			eLexica = (Element)eList.item(i);
			type    = UTXml.getTrimmedAttribute(eLexica, XML_TYPE);
			label   = UTXml.getTrimmedAttribute(eLexica, XML_LABEL);
			cutoff  = Integer.parseInt(UTXml.getTrimmedAttribute(eLexica, XML_CUTOFF));
			
			if (type.equals(XML_LEXICA_PUNCTUATION))
				p_punc = new StringIntPair(label, cutoff);
		}
		
		if (p_punc == null)
			p_punc = new StringIntPair("", 0);
	}
	
	protected boolean validSource(char source)
	{
		return source == S_STACK || source == S_LAMBDA || source == S_BETA;
	}
	
	protected boolean validRelation(String relation)
	{
		return P_REL.matcher(relation).matches();
	}
	
	protected boolean validField(String field)
	{
		return P_FIELD  .matcher(field).matches() ||  
			   P_FEAT   .matcher(field).matches() ||
			   P_BOOLEAN.matcher(field).matches();
	}

	@Override
	protected String toStringCutoffs()
	{
		return null;
	}
}
