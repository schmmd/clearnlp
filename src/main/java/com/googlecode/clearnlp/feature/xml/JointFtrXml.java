/**
* Copyright 2012 University of Massachusetts Amherst
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
*   
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.googlecode.clearnlp.feature.xml;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.util.UTRegex;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.StringIntPair;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class JointFtrXml extends AbstractFtrXml
{
	static public final char S_INPUT	= 'i';
	static public final char S_STACK	= 's';
	static public final char S_LAMBDA	= 'l';
	static public final char S_BETA		= 'b';
	static public final char S_PRED		= 'p';
	static public final char S_ARG		= 'a';
	
	static public final String R_H		= "h";		// head
	static public final String R_H2		= "h2";		// grand-head
	static public final String R_LMD	= "lmd";	// leftmost dependent
	static public final String R_RMD	= "rmd";	// rightmost dependent
	static public final String R_LMD2	= "lmd2";	// leftmost dependent 2
	static public final String R_RMD2	= "rmd2";	// rightmost dependent 2
	static public final String R_LND	= "lnd";	// left-nearest dependent
	static public final String R_RND	= "rnd";	// right-nearest dependent
	static public final String R_LNS	= "lns";	// left-nearest sibling
	static public final String R_RNS	= "rns";	// right-nearest sibling

	static public final String F_FORM					= "f";
	static public final String F_SIMPLIFIED_FORM		= "sf";
	static public final String F_LOWER_SIMPLIFIED_FORM	= "lsf";
	static public final String F_POS					= "p";
	static public final String F_POS_SET				= "ps";
	static public final String F_AMBIGUITY_CLASS		= "a";
	static public final String F_LEMMA					= "m";
	static public final String F_DEPREL					= "d";
	static public final String F_DIRECTION				= "dir";
	static public final String F_DISTANCE				= "n";
	static public final String F_DEPREL_SET				= "ds";
	static public final String F_GRAND_DEPREL_SET		= "gds";
	static public final String F_LEFT_VALENCY			= "lv";
	static public final String F_RIGHT_VALENCY			= "rv";
	static public final String F_LNPL					= "lnpl";	// left-nearest punctuation of lambda
	static public final String F_RNPL					= "rnpl";	// right-nearest punctuation of lambda
	static public final String F_LNPB					= "lnpb";	// left-nearest punctuation of beta
	static public final String F_RNPB					= "rnpb";	// right-nearest punctuation of beta
	
	static public final Pattern P_BOOLEAN  	= Pattern.compile("^b(\\d+)$");
	static public final Pattern P_PREFIX  	= Pattern.compile("^pf(\\d+)$");
	static public final Pattern P_SUFFIX  	= Pattern.compile("^sf(\\d+)$");
	static public final Pattern P_FEAT		= Pattern.compile("^ft=(.+)$");		
	static public final Pattern P_SUBCAT 	= Pattern.compile("^sc(["+F_POS+F_DEPREL+"])(\\d+)$");
	static public final Pattern P_PATH	 	= Pattern.compile("^pt(["+F_POS+F_DEPREL+F_DISTANCE+"])(\\d+)$");
	static public final Pattern P_ARGN 	 	= Pattern.compile("^argn(\\d+)$");

	static protected final Pattern P_REL	= UTRegex.getORPattern(R_H, R_H2, R_LMD, R_RMD, R_LMD2, R_RMD2, R_LND, R_RND, R_LNS, R_RNS); 
	static protected final Pattern P_FIELD	= UTRegex.getORPattern(F_FORM, F_SIMPLIFIED_FORM, F_LOWER_SIMPLIFIED_FORM, F_LEMMA, F_POS, F_POS_SET, F_AMBIGUITY_CLASS, F_DEPREL, F_DIRECTION, F_DISTANCE, F_DEPREL_SET, F_LEFT_VALENCY, F_RIGHT_VALENCY, F_LNPL, F_RNPL, F_LNPB, F_RNPB);
	
	final String CUTOFF_AMBIGUITY			= "ambiguity";	// part-of-speech tagging
	final String CUTOFF_DOCUMENT_FREQUENCY	= "df";			// part-of-speech tagging
	final String CUTOFF_PATH_DOWN			= "down";		// semantic role labeling
	final String CUTOFF_PATH_UP				= "up";			// semantic role labeling
	
	final String LEXICA_PUNCTUATION 	= "punctuation";	// dependency parsing
	final String LEXICA_PREDICATE		= "predicate";		// predicate identification
	
	double			cutoff_ambiguity;	// part-of-speech tagging
	int				cutoff_df;			// part-of-speech tagging
	int				cutoff_pathDown;	// semantic role labeling
	int				cutoff_pathUp;		// semantic role labeling
	StringIntPair	p_punc;				// dependency parsing
	Pattern			p_predicates;		// predicate identification
	
	public JointFtrXml(InputStream fin)
	{
		super(fin);
	}
	
	/** For part-of-speech tagging. */
	public double getAmbiguityClassThreshold()
	{
		return cutoff_ambiguity;
	}
	
	/** For part-of-speech tagging. */
	public int getDocumentFrequencyCutoff()
	{
		return cutoff_df; 
	}
	
	/** Semantic role labeling. */
	public int getPathDownCutoff()
	{
		return cutoff_pathDown;
	}
	
	/** Semantic role labeling. */
	public int getPathUpCutoff()
	{
		return cutoff_pathUp;
	}
	
	/** For dependency parsing. */
	public int getPunctuationCutoff()
	{
		return p_punc.i;
	}
	
	/** For dependency parsing. */
	public String getPunctuationLabel()
	{
		return p_punc.s;
	}
	
	/** For predicate identification. */
	public boolean isPredicate(DEPNode node)
	{
		return p_predicates.matcher(node.pos).find();
	}
	
	@Override
	protected void initCutoffMore(NodeList eList)
	{
		Element eCutoff = (Element)eList.item(0);
		
		cutoff_ambiguity = eCutoff.hasAttribute(CUTOFF_AMBIGUITY) ? Double.parseDouble(eCutoff.getAttribute(CUTOFF_AMBIGUITY)) : 0d;
		cutoff_df = eCutoff.hasAttribute(CUTOFF_DOCUMENT_FREQUENCY) ? Integer.parseInt(eCutoff.getAttribute(CUTOFF_DOCUMENT_FREQUENCY)) : 0;
		cutoff_pathDown = eCutoff.hasAttribute(CUTOFF_PATH_DOWN) ? Integer.parseInt(eCutoff.getAttribute(CUTOFF_PATH_DOWN)) : 0;
		cutoff_pathUp = eCutoff.hasAttribute(CUTOFF_PATH_UP)   ? Integer.parseInt(eCutoff.getAttribute(CUTOFF_PATH_UP)) : 0;
	}
	
	@Override
	protected void initMore(Document doc) throws Exception
	{
		initMoreLexica(doc);
	}
	
	/** Called by {@link JointFtrXml#initMore(Document)}. */
	private void initMoreLexica(Document doc)
	{
		NodeList eList = doc.getElementsByTagName(XML_LEXICA);
		int i, cutoff, size = eList.getLength();
		String type, label;
		Element eLexica;
		
		p_punc = new StringIntPair("", 0);
		
		for (i=0; i<size; i++)
		{
			eLexica = (Element)eList.item(i);
			type    = UTXml.getTrimmedAttribute(eLexica, XML_TYPE);
			label   = UTXml.getTrimmedAttribute(eLexica, XML_LABEL);
			cutoff  = Integer.parseInt(UTXml.getTrimmedAttribute(eLexica, XML_CUTOFF));
			
			if      (type.equals(LEXICA_PUNCTUATION))
				p_punc.set(label, cutoff);
			else if (type.equals(LEXICA_PREDICATE))
				p_predicates = Pattern.compile("^"+label+"$");
		}
	}
	
	@Override
	protected boolean validSource(char source)
	{
		return source == S_INPUT || source == S_STACK || source == S_LAMBDA || source == S_BETA || source == S_PRED || source == S_ARG;
	}

	@Override
	protected boolean validRelation(String relation)
	{
		return P_REL.matcher(relation).matches();
	}
	
	protected boolean validField(String field)
	{
		return P_FIELD  .matcher(field).matches() ||
			   P_BOOLEAN.matcher(field).matches() ||
			   P_PREFIX .matcher(field).matches() ||
			   P_SUFFIX .matcher(field).matches() ||
			   P_FEAT   .matcher(field).matches() ||
			   P_SUBCAT .matcher(field).matches() ||
			   P_PATH   .matcher(field).matches() ||
			   P_ARGN   .matcher(field).matches();
	}
}
