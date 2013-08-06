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
package com.googlecode.clearnlp.morphology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.googlecode.clearnlp.constituent.CTLibEn;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.util.pair.Pair;


/**
 * English morphological analyzer.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class EnglishMPAnalyzer extends AbstractMPAnalyzer
{
	final public String FIELD_DELIM = "_";
	
	final String LANG_DIR  = AbstractReader.LANG_EN + "/";
	final String NOUN_EXC  = LANG_DIR+"noun.exc";
	final String VERB_EXC  = LANG_DIR+"verb.exc";
	final String ADJ_EXC   = LANG_DIR+"adj.exc";
	final String ADV_EXC   = LANG_DIR+"adv.exc";
	final String NOUN_BASE = LANG_DIR+"noun.txt";
	final String VERB_BASE = LANG_DIR+"verb.txt";
	final String ADJ_BASE  = LANG_DIR+"adj.txt";
	final String ADV_BASE  = LANG_DIR+"adv.txt";
	final String ORD_BASE  = LANG_DIR+"ordinal.txt";
	final String CRD_BASE  = LANG_DIR+"cardinal.txt";
	final String NOUN_RULE = LANG_DIR+"noun.rule";
	final String VERB_RULE = LANG_DIR+"verb.rule";
	final String ADJ_RULE  = LANG_DIR+"adj.rule";
	final String ABBR_RULE = LANG_DIR+"abbr.rule";
	
	final String POS_NOUN = "N";
	final String POS_VERB = "V";
	final String POS_ADJ  = "J";
	final String POS_ADV  = "R";
	
	/** Noun exceptions */
	Map<String,String> m_noun_exc;
	/** Verb exceptions */
	Map<String,String> m_verb_exc;
	/** Adjective exceptions */
	Map<String,String> m_adj_exc;
	/** Adverb exceptions */
	Map<String,String> m_adv_exc;
	
	/** Noun base-forms */
	Set<String> s_noun_base;
	/** Verb base-forms */
	Set<String> s_verb_base;
	/** Adjective base-forms */
	Set<String> s_adj_base;
	/** Adverb base-forms */
	Set<String> s_adv_base;
	/** Ordinal forms */
	Set<String> s_ord_base;
	/** Cardinal forms */
	Set<String> s_crd_base;
	
	/** Noun detachment rules */
	List<Pair<String,String>> a_noun_rule;
	/** Verb detachment rules */
	List<Pair<String,String>> a_verb_rule;
	/** Adjective detachment rules */
	List<Pair<String,String>> a_adj_rule;
	/** Abbreviation replacement rules */
	Map<String,String>         m_abbr_rule;
	
	/**
	 * Constructs an English morphological analyzer from the specific dictionary file.
	 * @param dictFile the name of a dictionary file.
	 */
	public EnglishMPAnalyzer(String dictFile)
	{
		try
		{
			init(new ZipInputStream(new FileInputStream(dictFile)));
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public EnglishMPAnalyzer(InputStream stream)
	{
		try
		{
			init(new ZipInputStream(stream));
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Constructs an English morphological analyzer from the specific input stream. 
	 * @param inputStream the input stream containing dictionary files.
	 * @throws IOException
	 */
	public EnglishMPAnalyzer(ZipInputStream inputStream) throws IOException
	{
		try
		{
			init(inputStream);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Initializes this morphological analyzer.
	 * @param inputStream the input stream containing dictionary files.
	 */
	public void init(ZipInputStream zin) throws IOException
	{
		ZipEntry zEntry;
		String filename;
		
		while ((zEntry = zin.getNextEntry()) != null)
		{
			filename = zEntry.getName();
			
			if      (filename.equals(NOUN_EXC))		m_noun_exc  = getExcecptionMap(zin);
			else if (filename.equals(VERB_EXC))		m_verb_exc  = getExcecptionMap(zin);
			else if (filename.equals( ADJ_EXC))		m_adj_exc   = getExcecptionMap(zin);
			else if (filename.equals( ADV_EXC))		m_adv_exc   = getExcecptionMap(zin);
			else if (filename.equals(NOUN_BASE))	s_noun_base = getBaseSet(zin);
			else if (filename.equals(VERB_BASE))	s_verb_base = getBaseSet(zin);
			else if (filename.equals( ADJ_BASE))	s_adj_base  = getBaseSet(zin);
			else if (filename.equals( ADV_BASE))	s_adv_base  = getBaseSet(zin);
			else if (filename.equals( ORD_BASE))	s_ord_base  = getBaseSet(zin);
			else if (filename.equals( CRD_BASE))	s_crd_base  = getBaseSet(zin);
			else if (filename.equals(NOUN_RULE))	a_noun_rule = getRuleList(zin);
			else if (filename.equals(VERB_RULE))	a_verb_rule = getRuleList(zin);
			else if (filename.equals( ADJ_RULE))	a_adj_rule  = getRuleList(zin);
			else if (filename.equals(ABBR_RULE))	m_abbr_rule = getAbbreviationMap(zin);
		}
		
		zin.close();
	}
	
	/**
	 * Called by {@link EnglishMPAnalyzer#init(ZipInputStream)}. 
	 * @return HashMap taking exceptions as keys and their base-forms as values.
	 */
	private HashMap<String,String> getExcecptionMap(ZipInputStream zin) throws IOException
	{
		HashMap<String, String> map = new HashMap<String, String>();
		BufferedReader          fin = new BufferedReader(new InputStreamReader(zin));
		
		StringTokenizer tok;
		String line, exc, base;
		
		while ((line = fin.readLine()) != null)
		{
			tok  = new StringTokenizer(line);
			exc  = (tok.hasMoreTokens()) ? tok.nextToken() : null;
			base = (tok.hasMoreTokens()) ? tok.nextToken() : null;
			
			if (exc != null && base != null)
			{
				map.put(exc, base);
				while (tok.hasMoreTokens())	map.put(tok.nextToken(), base);
			}
		}
		
		return map;
	}
	
	/**
	 * Called by {@link EnglishMPAnalyzer#init(ZipInputStream)}.
	 * @return HashSet containing base-forms.
	 */
	private HashSet<String> getBaseSet(ZipInputStream zin) throws IOException
	{
		HashSet<String> set = new HashSet<String>();
		BufferedReader  fin = new BufferedReader(new InputStreamReader(zin));
		String line;
		
		while ((line = fin.readLine()) != null)
			set.add(line.trim());
		
		return set;
	}
	
	/**
	 * Called by {@link EnglishMPAnalyzer#init(ZipInputStream)}.
	 * @return List containing rules.
	 */
	private ArrayList<Pair<String,String>> getRuleList(ZipInputStream zin) throws IOException
	{
		ArrayList<Pair<String,String>> list = new ArrayList<Pair<String,String>>();
		BufferedReader fin = new BufferedReader(new InputStreamReader(zin));
		
		StringTokenizer tok;
		String line, str0, str1;
		
		while ((line = fin.readLine()) != null)
		{
			tok  = new StringTokenizer(line);
			str0 = tok.nextToken();
			str1 = (tok.hasMoreTokens()) ? tok.nextToken() : "";
			
			list.add(new Pair<String,String>(str0, str1));
		}
		
		return list;
	}
	
	/**
	 * Called by {@link EnglishMPAnalyzer#init(ZipInputStream)}.
	 * @return HashMap taking (abbreviation and pos-tag) as the key and its base-form as the value.
	 */
	private HashMap<String,String> getAbbreviationMap(ZipInputStream zin) throws IOException
	{
		HashMap<String, String> map = new HashMap<String, String>();
		BufferedReader          fin = new BufferedReader(new InputStreamReader(zin));
		
		StringTokenizer tok;
		String line, abbr, pos, key, base;
		
		while ((line = fin.readLine()) != null)
		{
			tok  = new StringTokenizer(line);
			abbr = tok.nextToken();
			pos  = tok.nextToken();
			key  = abbr + FIELD_DELIM + pos;
			base = tok.nextToken();
			
			map.put(key, base);
		}
			
		return map;
	}
	
	@Override
	public String getLemma(String form, String pos)
	{
		form = MPLib.normalizeBasic(form);
		return getLemmaAux(form.toLowerCase(), pos);
	}
	
	@Override
	public Set<String> getPOSTags(String form)
	{
		Set<String> set = new HashSet<String>();
		form = form.toLowerCase();
		
		if (m_noun_exc.containsKey(form) || getBaseAux(form, s_noun_base, a_noun_rule) != null)
			set.add(POS_NOUN);
		
		if (m_verb_exc.containsKey(form) || getBaseAux(form, s_verb_base, a_verb_rule) != null)
			set.add(POS_VERB);
		
		if (m_adj_exc.containsKey(form) || getBaseAux(form, s_adj_base , a_adj_rule) != null)
			set.add(POS_ADJ);
		
		if (m_adv_exc .containsKey(form))
			set.add(POS_ADV);

		return set;
	}
	
	/** Called by {@link EnglishMPAnalyzer#getLemma(String, String)}. */
	private String getLemmaAux(String form, String pos)
	{
		// numbers
		String morphem = getNumber(form, pos);
		if (morphem != null)	return morphem;
		
		// exceptions
		morphem = getException(form, pos);
		if (morphem != null)	return morphem;
				
		// base-forms
		morphem = getBase(form, pos);
		if (morphem != null)	return morphem;
				
		// abbreviations
		morphem = getAbbreviation(form, pos);
		if (morphem != null)	return morphem;

		return form;
	}
	
	/** Called by {@link EnglishMPAnalyzer#getLemma(String, String)}. */
	private String getNumber(String form, String pos)
	{
		if (pos.equals(CTLibEn.POS_CD))
		{
			if (s_crd_base.contains(form))
				return "#crd#";
			
			if (form.equals("0st") || form.equals("0nd") || form.equals("0rd") || form.equals("0th") || s_ord_base.contains(form))
				return "#ord#";
		}
		
		return null;
	}
	
	/** Called by {@link EnglishMPAnalyzer#getLemma(String, String)}. */
	private String getException(String form, String pos)
	{
		if (MPLibEn.isNoun     (pos))	return m_noun_exc.get(form);
		if (MPLibEn.isVerb     (pos))	return m_verb_exc.get(form);
		if (MPLibEn.isAdjective(pos))	return m_adj_exc .get(form);
		if (MPLibEn.isAdverb   (pos))	return m_adv_exc .get(form);
		
		return null;
	}
	
	/** Called by {@link EnglishMPAnalyzer#getLemma(String, String)}. */
	private String getBase(String form, String pos)
	{
		if (MPLibEn.isNoun(pos))		return getBaseAux(form, s_noun_base, a_noun_rule);
		if (MPLibEn.isVerb(pos))		return getBaseAux(form, s_verb_base, a_verb_rule);
		if (MPLibEn.isAdjective(pos))	return getBaseAux(form, s_adj_base , a_adj_rule);
		
		return null;
	}
	
	/** Called by {@link EnglishMPAnalyzer#getBase(String, String)}. */
	private String getBaseAux(String form, Set<String> set, List<Pair<String,String>> rule)
	{
		int offset;	String base;
		
		for (Pair<String,String> tup : rule)
		{
			if (form.endsWith(tup.o1))
			{
				offset = form.length() - tup.o1.length();
				base   = form.substring(0, offset) + tup.o2;
				
				if (set.contains(base))	return base;
			}
		}
		
		return null;
	}
	
	/** Called by {@link EnglishMPAnalyzer#getLemma(String, String)}. */
	private String getAbbreviation(String form, String pos)
	{
		String key = form + FIELD_DELIM + pos;

		return m_abbr_rule.get(key);
	}
}
