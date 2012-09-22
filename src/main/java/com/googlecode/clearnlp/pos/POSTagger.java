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
package com.googlecode.clearnlp.pos;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.engine.AbstractEngine;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.feature.xml.POSFtrXml;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTString;
import com.googlecode.clearnlp.util.map.Prob1DMap;
import com.googlecode.clearnlp.util.map.Prob2DMap;
import com.googlecode.clearnlp.util.pair.StringDoublePair;

/**
 * Part-of-speech tagger.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class POSTagger extends AbstractEngine
{
	private Set<String>			s_lemmas;
	private Prob2DMap			p_ambi;
	private Map<String,String>	m_ambi;
	private Prob1DMap			p_forms;
	private Set<String>	        s_forms;
	private POSFtrXml			f_xml;
	private StringTrainSpace	s_space;
	private StringModel			s_model;
	private POSNode[]			p_nodes;
	private String[]			g_labels;
	private int					i_source;
	private double              d_denominator;
	
	/** Constructs a POS tagger for collecting lexica. */
	public POSTagger(Set<String> sLemma)
	{
		super(FLAG_LEXICA);
		
		s_lemmas = sLemma;
		p_ambi   = new Prob2DMap();
		p_forms  = new Prob1DMap();
	}
	
	/** Constructs a POS tagger for training. */
	public POSTagger(POSFtrXml xml, Set<String> sLemmas, Set<String> sForms, Map<String,String> ambiguityMap, StringTrainSpace trainSpace)
	{
		super(FLAG_TRAIN);
		f_xml    = xml;
		s_lemmas = sLemmas;
		s_forms  = sForms;
		m_ambi   = ambiguityMap;
		s_space  = trainSpace;
	}
	
	/** Constructs a POS tagger for cross-validation. */
	public POSTagger(POSFtrXml xml, Set<String> sLemmas, Set<String> sForms, Map<String,String> ambiguityMap, StringModel model)
	{
		super(FLAG_PREDICT);
		f_xml    = xml;
		s_lemmas = sLemmas;
		s_forms  = sForms;
		m_ambi   = ambiguityMap;
		s_model  = model;
		d_denominator = Math.sqrt(sForms.size());
	}
	
	/** Constructs a POS tagger for predicting. */
	public POSTagger(POSFtrXml xml, BufferedReader fin)
	{
		super(FLAG_PREDICT);
		f_xml  = xml;
		
		try
		{
			s_lemmas = UTInput.getStringSet(fin);
			s_forms  = UTInput.getStringSet(fin);
			m_ambi   = UTInput.getStringMap(fin, " ");	
		}
		catch (Exception e) {e.printStackTrace();}
		
		s_model = new StringModel(fin);
		d_denominator = Math.sqrt(s_forms.size());
	}

	/** Saves collections and a POS tagging model to the specific output-stream. */
	public void saveModel(PrintStream fout)
	{
		UTOutput.printSet(fout, s_lemmas);
		UTOutput.printSet(fout, s_forms);
		UTOutput.printMap(fout, m_ambi, " ");
		s_model.save(fout);
	}
	
	/** Initializes the POS tagger given the specific array of POS nodes. */
	public void init(POSNode[] nodes)
	{
		p_nodes = nodes;
		
		if (i_flag == FLAG_TRAIN)
			g_labels = POSLib.getLabels(nodes);
		
		int i, size = nodes.length;
		
		for (i=0; i<size; i++)
			nodes[i].pos = AbstractReader.DUMMY_TAG;
	}
	
	/** Tags the POS nodes. */
	public void tag(POSNode[] nodes)
	{
		POSLib.normalizeForms(nodes);
		
		if (i_flag == FLAG_LEXICA)
		{
			addLexica(nodes);
			return;
		}
		
		init(nodes);
		
		StringFeatureVector vector;
		int size = nodes.length;
		
		for (i_source=0; i_source<size; i_source++)
		{
			vector = getFeatureVector(f_xml);
			
			switch (i_flag)
			{
			case FLAG_TRAIN  : train  (vector);	break;
			case FLAG_PREDICT: predict(vector);	break;
			}
		}
	}
	
	/** Called by {@link POSTagger#tag(POSNode[])}. */
	private void train(StringFeatureVector vector)
	{
		String label = g_labels[i_source];
		p_nodes[i_source].pos = label;
		
		if (vector.size() > 0)
			s_space.addInstance(label, vector);
	}
	
	/** Called by {@link POSTagger#tag(POSNode[])}. */
	private void predict(StringFeatureVector vector)
	{
		StringPrediction p = s_model.predictBest(vector);
		p_nodes[i_source].pos = p.label;
	}
	
	/** Called by {@link POSTagger#tag(POSNode[])}. */
	private void addLexica(POSNode[] nodes)
	{
		for (POSNode node : nodes)
		{
			if (s_lemmas.contains(node.lemma))
			{
				p_forms.add(node.simplifiedForm);
				p_ambi .add(node.simplifiedForm, node.pos);
			}
		}
	}

	public Map<String, String> getAmbiguityMap(double cutoff)
	{
		Map<String, String> mAmbi = new HashMap<String, String>();
		StringDoublePair[] ps;
		StringBuilder build;
		
		for (String form : p_ambi.keySet())
		{
			ps = p_ambi.getProb1D(form);
			build = new StringBuilder();
			Arrays.sort(ps);
			
			for (StringDoublePair p : ps)
			{
				if (p.d <= cutoff)	break;
				
				build.append(AbstractColumnReader.BLANK_COLUMN);
				build.append(p.s);
			}
			
			if (build.length() > 0)
				mAmbi.put(form, build.substring(1));				
		}
		
		return mAmbi;
	}
	
	public Set<String> getFormSet(int cutoff)
	{
		Set<String> set = new HashSet<String>();
		String key;
		
		for (ObjectCursor<String> cur : p_forms.keys())
		{
			key = cur.value;
					
			if (p_forms.get(key) > cutoff)
				set.add(key);
		}
		
		return set;
	}
	
	public double getCosineSimilarity(POSNode[] nodes)
	{
		Set<String> set = new HashSet<String>();
		
		for (POSNode node : nodes)
			set.add(node.simplifiedForm);
		
		double d = Math.sqrt(set.size()) * d_denominator;
		set.retainAll(s_forms);
		
		return (double)set.size() / d;
	}
	
	protected String getField(FtrToken token)
	{
		POSNode node = getNode(token);
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(POSFtrXml.F_FORM))
		{
			return (s_lemmas.contains(node.lemma)) ? node.simplifiedForm : null;
		}
		else if (token.isField(POSFtrXml.F_LEMMA))
		{
			return (s_lemmas.contains(node.lemma)) ? node.lemma : null;
		}
		else if (token.isField(POSFtrXml.F_POS))
		{
			return node.isPos(AbstractReader.DUMMY_TAG) ? null : node.pos;
		}
		else if (token.isField(POSFtrXml.F_AMBIGUITY))
		{
			return m_ambi.get(node.simplifiedForm);
		}
		else if ((m = POSFtrXml.P_BOOLEAN.matcher(token.field)).find())
		{
			int field = Integer.parseInt(m.group(1));
			
			switch (field)
			{
			case 0: return UTString.isAllUpperCase(node.simplifiedForm) ? token.field : null;
			case 1: return UTString.isAllLowerCase(node.simplifiedForm) ? token.field : null;
			case 2: return UTString.beginsWithUpperCase(node.simplifiedForm) ? token.field : null;
			case 3: return UTString.getNumOfCapitalsNotAtBeginning(node.simplifiedForm) == 1 ? token.field : null;
			case 4: return UTString.getNumOfCapitalsNotAtBeginning(node.simplifiedForm)  > 1 ? token.field : null;
			case 5: return node.simplifiedForm.contains(".") ? token.field : null;
			case 6: return UTString.containsDigit(node.simplifiedForm) ? token.field : null;
			case 7: return node.simplifiedForm.contains("-") ? token.field : null;
			}
		}
		
		return null;
	}
	
	protected String[] getFields(FtrToken token)
	{
		POSNode node = getNode(token);
		if (node == null)	return null;
		Matcher m;
		
		if ((m = POSFtrXml.P_PREFIX.matcher(token.field)).find())
		{
			String[] fields = UTString.getPrefixes(node.lemma, Integer.parseInt(m.group(1)));
			return fields.length == 0 ? null : fields;
		}
		else if ((m = POSFtrXml.P_SUFFIX.matcher(token.field)).find())
		{
			String[] fields = UTString.getSuffixes(node.lemma, Integer.parseInt(m.group(1)));
			return fields.length == 0 ? null : fields;
		}
		
		return null;
	}
	
	private POSNode getNode(FtrToken token)
	{
		int index = i_source;
		index += token.offset;
		
		return (0 <= index && index < p_nodes.length) ? p_nodes[index] : null;
	}
}
