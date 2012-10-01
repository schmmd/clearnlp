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
import java.util.Formatter;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Part-of-speech tagging feature template.
 * @author Jinho D. Choi
 * <b>Last update:</b> 4/12/2011
 */
public class POSFtrXml extends AbstractFtrXml
{
	static public final char SOURCE_I	= 'i';
	
	static public final String  F_FORM		= "f";
	static public final String  F_LEMMA		= "m";
	static public final String  F_POS		= "p";
	static public final String  F_AMBIGUITY	= "a";
	static public final Pattern P_BOOLEAN  	= Pattern.compile("^b(\\d+)$");
	static public final Pattern P_PREFIX  	= Pattern.compile("^pf(\\d+)$");
	static public final Pattern P_SUFFIX  	= Pattern.compile("^sf(\\d+)$");
	
	static protected final Pattern P_FIELD = Pattern.compile(F_FORM+"|"+F_LEMMA+"|"+F_POS+"|"+F_AMBIGUITY);
	static protected final String CUTOFF_AMBIGUITY = "ambiguity";
	static protected final String CUTOFF_DOCUMENT_FREQUENCY = "df";
	
	private double[] cutoff_ambiguity;
	private int[]    cutoff_df;
	
	public POSFtrXml(InputStream fin)
	{
		super(fin);
	}
	
	protected void initCutoffMore(NodeList eList)
	{
		int i, size = eList.getLength();
		Element eCutoff;
		
		cutoff_ambiguity = new double[size];
		cutoff_df = new int[size];
		
		for (i=0; i<size; i++)
		{
			eCutoff = (Element)eList.item(i);
			
			cutoff_ambiguity[i] = eCutoff.hasAttribute(CUTOFF_AMBIGUITY) ? Double.parseDouble(eCutoff.getAttribute(CUTOFF_AMBIGUITY)) : 0d;
			cutoff_df[i] = eCutoff.hasAttribute(CUTOFF_DOCUMENT_FREQUENCY) ? Integer.parseInt(eCutoff.getAttribute(CUTOFF_DOCUMENT_FREQUENCY)) : 0;
		}
	}
	
	protected void initMore(Document doc) {}
	
	protected boolean validSource(char source)
	{
		return source == SOURCE_I;
	}
	
	protected boolean validRelation(String relation)
	{
		return false;
	}
	
	protected boolean validField(String field)
	{
		return P_FIELD.matcher(field).matches() ||
			   P_BOOLEAN.matcher(field).matches() ||
			   P_PREFIX.matcher(field).matches() ||
			   P_SUFFIX.matcher(field).matches();
	}
	
	public double getAmbiguityThreshold(int index)
	{
		return (index < cutoff_ambiguity.length) ? cutoff_ambiguity[index] : 0d;
	}
	
	public int getDocumentFrequency(int index)
	{
		return (index < cutoff_df.length) ? cutoff_df[index] : 0; 
	}
	
	public int getNumberOfConfigurations()
	{
		return cutoff_ambiguity.length;
	}

	protected String toStringCutoffs()
	{
		Formatter format = new Formatter();

		for (int i=0; i<cutoff_label.length; i++)
			format.format("  <%s %s=\"%d\" %s=\"%d\" %s=\"%d\" %s=\"%f\"/>\n", XML_CUTOFF, XML_LABEL, cutoff_label[i], XML_FEATURE, cutoff_feature[i], CUTOFF_DOCUMENT_FREQUENCY, cutoff_df[i], CUTOFF_AMBIGUITY, cutoff_ambiguity[i]);

		format.close();
		return format.toString();
	}
}
