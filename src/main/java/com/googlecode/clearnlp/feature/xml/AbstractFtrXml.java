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
package com.googlecode.clearnlp.feature.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Abstract feature XML templates.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
abstract public class AbstractFtrXml
{
	static protected final String XML_TEMPLATE	= "feature_template";
	static protected final String XML_CUTOFF	= "cutoff";
	static protected final String XML_LABEL		= "label";
	static protected final String XML_TYPE		= "type";
	static protected final String XML_FEATURE	= "feature";
	static protected final String XML_LEXICA	= "lexica";
	static protected final String XML_REGEX		= "regex";
	
	/** The type of feature. */
	static protected final String XML_FEATURE_T	= "t";
	/** The number of tokens used for each feature. */
	static protected final String XML_FEATURE_N	= "n";
	/** The field to extract features from (e.g., form, pos). */
	static protected final String XML_FEATURE_F	= "f";
	/** If {@code false}, this feature is not visible. */
	static protected final String XML_FEATURE_VISIBLE = "visible"; 
	/** If {@code false}, this feature is not visible. */
	static protected final String XML_FEATURE_NOTE    = "note";
	/** Field delimiter ({@code ":"}). */
	static protected final String DELIM_F = ":";
	/** Relation delimiter ({@code "_"}). */
	static protected final String DELIM_R = "_";
	
	protected FtrTemplate[] f_templates;
	protected boolean       b_skipInvisible;
	protected int[]         cutoff_label;
	protected int[]         cutoff_feature;
	
	/**
	 * @param fin the input stream of the XML file.
	 * Skips invisible features.
	 * @param fin the input stream of the XML file.
	 */
	public AbstractFtrXml(InputStream fin)
	{
		init(fin, true);
	}
	
	/**
	 * @param fin the input stream of the XML file.
	 * @param fin the input stream of the XML file.
	 * @param skipInvisible if {@code true}, skips invisible features.
	 */
	public AbstractFtrXml(InputStream fin, boolean skipInvisible)
	{
		init(fin, skipInvisible);
	}
	
	/**
	 * Called by constructors.
	 * Initializes cutoffs and feature templates.
	 */
	protected void init(InputStream fin, boolean skipInvisible)
	{
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		b_skipInvisible = skipInvisible;
		
		try
		{
			DocumentBuilder builder = dFactory.newDocumentBuilder();
			Document        doc     = builder.parse(fin);
			
			initCutoffs (doc);
			initFeatures(doc);
			initMore(doc);
		}
		catch (Exception e) {e.printStackTrace();System.exit(1);}
	}
	
	/** Called by {@link AbstractFtrXml#init(InputStream, boolean)}. */
	protected void initCutoffs(Document doc) throws Exception
	{
		NodeList eList = doc.getElementsByTagName(XML_CUTOFF);
		int i, size = eList.getLength();
		Element eCutoff;
		
		cutoff_label   = new int[size];
		cutoff_feature = new int[size];
		
		for (i=0; i<size; i++)
		{
			eCutoff = (Element)eList.item(i);
			
			cutoff_label  [i] = eCutoff.hasAttribute(XML_LABEL  ) ? Integer.parseInt(eCutoff.getAttribute(XML_LABEL  )) : 0;
			cutoff_feature[i] = eCutoff.hasAttribute(XML_FEATURE) ? Integer.parseInt(eCutoff.getAttribute(XML_FEATURE)) : 0;
		}
		
		initCutoffMore(eList);
	}
	
	abstract protected void initMore(Document doc) throws Exception;
	
	/** Called by {@link AbstractFtrXml#init(InputStream, boolean)}. */
	protected void initFeatures(Document doc) throws Exception
	{
		NodeList eList = doc.getElementsByTagName(XML_FEATURE);
		int i, j, n = eList.getLength();
		FtrTemplate template;
		Element eFeature;

		List<FtrTemplate> list = new ArrayList<FtrTemplate>();
		
		for (i=0,j=0; i<n; i++)
		{
			eFeature = (Element)eList.item(i);
			template = getFtrTemplate(eFeature);
			
			if (template != null)
			{
				list.add(template);
				
				if (!template.isBooleanFeature())
					template.type += j++;
			}
		}
		
		f_templates = new FtrTemplate[list.size()];
		list.toArray(f_templates);
	}
	
	/** Called by {@link AbstractFtrXml#initFeatures(Document)}. */
	protected FtrTemplate getFtrTemplate(Element eFeature)
	{
		String tmp  = eFeature.getAttribute(XML_FEATURE_VISIBLE).trim();
		boolean visible = tmp.isEmpty() ? true : Boolean.parseBoolean(tmp);
		if (b_skipInvisible && !visible)	return null;
		
		String type = eFeature.getAttribute(XML_FEATURE_T).trim();
		int    n    = Integer.parseInt(eFeature.getAttribute(XML_FEATURE_N)), i;
		String note = eFeature.getAttribute(XML_FEATURE_NOTE).trim();
		
		FtrTemplate ftr = new FtrTemplate(type, n, visible, note);

		for (i=0; i<n; i++)
			ftr.setFtrToken(i, getFtrToken(eFeature.getAttribute(XML_FEATURE_F + i)));
		
		return ftr;
	}
 
	/**
	 * Called by {@link AbstractFtrXml#getFtrTemplate(Element)}.
	 * @param ftr (e.g., "l.f", "l+1.m", "l-1.p", "l0_hd.d")
	 */
	protected FtrToken getFtrToken(String ftr)
	{
		String[] aField    = ftr      .split(DELIM_F);	// {"l-1_hd", "p"}
		String[] aRelation = aField[0].split(DELIM_R);	// {"l-1", "hd"} 
		
		char source = aRelation[0].charAt(0);
		if (!validSource(source))	xmlError(ftr);
		
		int offset = 0;
		if (aRelation[0].length() >= 2)
		{
			if (aRelation[0].charAt(1) == '+')	offset = Integer.parseInt(aRelation[0].substring(2)); 
			else								offset = Integer.parseInt(aRelation[0].substring(1));
		}
		
		String relation = null;
		if (aRelation.length > 1)
		{
			relation = aRelation[1];
			if (!validRelation(relation))	xmlError(ftr);
		}
		
		String field = aField[1];
		if (!validField(field))	xmlError(ftr);

		return new FtrToken(source, offset, relation, field);
	}
	
	/** Prints system error and exits. */
	protected void xmlError(String error)
	{
		System.err.println("Invalid feature: "+error);
		System.exit(1);
	}
	
	/**
	 * Returns the array of feature templates.
	 * @return the array of feature templates.
	 */
	public FtrTemplate[] getFtrTemplates()
	{
		return f_templates;
	}
	
	/**
	 * Returns the index'th label cutoff.
	 * If the label cutoff is not specified, returns 0.
	 * @param index the index of the label cutoff to be returned.
	 * @return the index'th label cutoff.
	 */
	public int getLabelCutoff(int index)
	{
		return (index < cutoff_label.length) ? cutoff_label[index] : 0; 
	}
	
	/**
	 * Returns the index'th feature cutoff.
	 * If the feature cutoff is not specified, returns 0.
	 * @param index the index of the feature cutoff to be returned.
	 * @return the index'th feature cutoff.
	 */
	public int getFeatureCutoff(int index)
	{
		return (index < cutoff_feature.length) ? cutoff_feature[index] : 0;
	}
	
	public String toString()
	{
		Formatter format = new Formatter();
		
		format.format("<%s>\n", XML_TEMPLATE);
		format.format("%s", toStringCutoffs());
		
		for (FtrTemplate ft : f_templates)
			format.format("  <%s %s/>\n", XML_FEATURE, ft.toString());
				
		format.format("</%s>\n", XML_TEMPLATE);
		
		return format.toString();
	}


	/**
	 * Initializes sub-class specific cutoffs.
	 * @param eList the list of cutoff elements.
	 */
	abstract protected void initCutoffMore(NodeList eList);
	/**
	 * Returns {@code true} if the specific source is valid.
	 * @param source the source to be compared.
	 * @return {@code true} if the specific source is valid.
	 */
	abstract protected boolean validSource(char source);
	/**
	 * Returns {@code true} if the specific relation is valid.
	 * @param relation the relation to be compared.
	 * @return {@code true} if the specific relation is valid.
	 */
	abstract protected boolean validRelation(String relation);
	/**
	 * Returns {@code true} if the specific field is valid.
	 * @param filed the field to be compared.
	 * @return {@code true} if the specific field is valid.
	 */
	abstract protected boolean validField(String filed);
	/**
	 * Returns the string representation of cutoffs.
	 * @return the string representation of cutoffs.
	 */
	abstract protected String toStringCutoffs();
}