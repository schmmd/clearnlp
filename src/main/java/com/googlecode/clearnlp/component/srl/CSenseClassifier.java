/**
* Copyright 2012-2013 University of Massachusetts Amherst
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
package com.googlecode.clearnlp.component.srl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class CSenseClassifier extends AbstractStatisticalComponent
{
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private String ENTRY_CONFIGURATION;
	private String ENTRY_FEATURE;
	private String ENTRY_LEXICA;
	private String ENTRY_MODEL;
	
	protected final int LEXICA_SENSES = 0;
	protected final int LEXICA_LEMMAS = 1;
	
	protected Map<String,Set<String>>		m_collect;	// for collecting lexica
	protected Map<String,String>			m_senses;
	protected ObjectIntOpenHashMap<String>	m_lemmas;
	protected String[]						g_senses;
	protected int 							i_pred;
	protected String						s_key;
	
//	====================================== CONSTRUCTORS ======================================

	/** Constructs a roleset classifier for collecting lexica. */
	public CSenseClassifier(JointFtrXml[] xmls, String key)
	{
		super(xmls);
		initKey(key);
		m_collect = new HashMap<String,Set<String>>();
	}
		
	/** Constructs a roleset classifier for training. */
	public CSenseClassifier(JointFtrXml[] xmls, StringTrainSpace[] spaces, Object[] lexica, String key)
	{
		super(xmls, spaces, lexica);
		initKey(key);
	}
	
	/** Constructs a roleset classifier for developing. */
	public CSenseClassifier(JointFtrXml[] xmls, StringModel[] models, Object[] lexica, String key)
	{
		super(xmls, models, lexica);
		initKey(key);
	}
	
	/** Constructs a roleset classifier for decoding. */
	public CSenseClassifier(ZipInputStream in, String key)
	{
		i_flag = FLAG_DECODE;
		
		initKey(key);
		loadModels(in);
	}
	
	@Override @SuppressWarnings("unchecked")
	protected void initLexia(Object[] lexica)
	{
		m_senses = (Map<String,String>)lexica[LEXICA_SENSES];
		m_lemmas = (ObjectIntOpenHashMap<String>)lexica[LEXICA_LEMMAS];
	}
	
	private void initKey(String key)
	{
		ENTRY_CONFIGURATION = NLPLib.MODE_SENSE + "_" + key + NLPLib.ENTRY_CONFIGURATION;
		ENTRY_FEATURE	    = NLPLib.MODE_SENSE + "_" + key + NLPLib.ENTRY_FEATURE;
		ENTRY_LEXICA	    = NLPLib.MODE_SENSE + "_" + key + NLPLib.ENTRY_LEXICA;
		ENTRY_MODEL		    = NLPLib.MODE_SENSE + "_" + key + NLPLib.ENTRY_MODEL;
		
		s_key = key;
	}
	
//	====================================== LOAD/SAVE MODELS ======================================
	
	@Override
	public void loadModels(ZipInputStream zin)
	{
		int fLen = ENTRY_FEATURE.length(), mLen = ENTRY_MODEL.length();
		f_xmls   = new JointFtrXml[1];
		s_models = null;
		ZipEntry zEntry;
		String   entry;
				
		try
		{
			while ((zEntry = zin.getNextEntry()) != null)
			{
				entry = zEntry.getName();
				
				if      (entry.equals(ENTRY_CONFIGURATION))
					loadDefaultConfiguration(zin);
				else if (entry.startsWith(ENTRY_FEATURE))
					loadFeatureTemplates(zin, Integer.parseInt(entry.substring(fLen)));
				else if (entry.startsWith(ENTRY_MODEL))
					loadStatisticalModels(zin, Integer.parseInt(entry.substring(mLen)));
				else if (entry.equals(ENTRY_LEXICA))
					loadLexica(zin);
			}		
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void loadLexica(ZipInputStream zin) throws Exception
	{
		BufferedReader fin = new BufferedReader(new InputStreamReader(zin));
		LOG.info("Loading lexica.\n");
		
		m_senses = UTInput.getStringMap(fin, " ");
		m_lemmas = UTInput.getStringIntOpenHashMap(fin, " ");
	}

	@Override
	public void saveModels(ZipOutputStream zout)
	{
		try
		{
			saveDefaultConfiguration(zout, ENTRY_CONFIGURATION);
			saveFeatureTemplates    (zout, ENTRY_FEATURE);
			saveLexica              (zout);
			saveStatisticalModels   (zout, ENTRY_MODEL);
			zout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void saveLexica(ZipOutputStream zout) throws Exception
	{
		zout.putNextEntry(new ZipEntry(ENTRY_LEXICA));
		PrintStream fout = UTOutput.createPrintBufferedStream(zout);
		
		UTOutput.printMap(fout, m_senses, " ");		fout.flush();
		UTOutput.printMap(fout, m_lemmas, " ");		fout.flush();
		
		zout.closeEntry();
	}
	
//	====================================== GETTERS AND SETTERS ======================================

	@Override
	public Object[] getLexica()
	{
		Object[] lexica = new Object[2];
		Map<String,String> mSenses = getSenseMap();
		
		lexica[LEXICA_SENSES] = mSenses;
		lexica[LEXICA_LEMMAS] = getLemmas(m_collect.keySet(), mSenses);
		
		return lexica;
	}
	
	private Map<String,String> getSenseMap()
	{
		Map<String,String> map = new HashMap<String,String>();
		Set<String> set;
		
		for (String lemma : m_collect.keySet())
		{
			set = m_collect.get(lemma);
			
			if (set.size() == 1)
				map.put(lemma, new ArrayList<String>(set).get(0));
		}
		
		return map;
	}
	
	private ObjectIntOpenHashMap<String> getLemmas(Set<String> sLemmas, Map<String,String> mSenses)
	{
		ObjectIntOpenHashMap<String> map = new ObjectIntOpenHashMap<String>();
		int idx = 0;
		
		for (String lemma : sLemmas)
		{
			if (!mSenses.containsKey(lemma))
				map.put(lemma, idx++);
		}
		
		return map;
	}
	
	@Override
	public void countAccuracy(int[] counts)
	{
		int i, correct = 0, total = 0;
		String gRoleset;
		DEPNode node;
		
		for (i=1; i<t_size; i++)
		{
			node = d_tree.get(i);
			gRoleset = g_senses[i];
			
			if (gRoleset != null)
			{
				total++;
				
				if (gRoleset.equals(node.getFeat(s_key)))
					correct++;
			}
		}
		
		counts[0] += total;
		counts[1] += correct;
	}
	
//	====================================== PROCESS ======================================
	
	@Override
	public void process(DEPTree tree)
	{
		init(tree);
		processAux();
	}
	
	/** Called by {@link CSenseClassifier#process(DEPTree)}. */
	protected void init(DEPTree tree)
	{
	 	d_tree = tree;
	 	t_size = tree.size();

	 	if (i_flag != FLAG_DECODE)
	 		g_senses = d_tree.getSenses(s_key);
	 	
	 	tree.setDependents();
	}
	
	/** Called by {@link CSenseClassifier#process(DEPTree)}. */
	protected void processAux()
	{
		if (i_flag == FLAG_LEXICA)	addLexica();
		else						classify();
	}
	
	protected void addLexica()
	{
		String sense, lemma;
		Set<String> set;
		
		for (i_pred=1; i_pred<t_size; i_pred++)
		{
			sense = g_senses[i_pred];
			
			if (sense != null)
			{
				lemma = d_tree.get(i_pred).lemma;
				set   = m_collect.get(lemma);
				
				if (set == null)
				{
					set = new HashSet<String>();
					m_collect.put(lemma, set);
				}
				
				set.add(sense);
			}
		}
	}
	
	/** Called by {@link CSenseClassifier#processAux()}. */
	protected void classify()
	{
		String cue = (i_flag == FLAG_DECODE && s_key.equals(DEPLib.FEAT_VN)) ? DEPLib.FEAT_PB : s_key;
		DEPNode pred;
		String  sense;
		
		for (i_pred=1; i_pred<t_size; i_pred++)
		{
			pred = d_tree.get(i_pred);
			
			if (pred.getFeat(cue) != null)
			{
				if ((sense = m_senses.get(pred.lemma)) == null)
				{
					if (m_lemmas.containsKey(pred.lemma))
						sense = getLabel(m_lemmas.get(pred.lemma));
					else if (s_key.equals(DEPLib.FEAT_PB))
						sense = pred.lemma+".01";
					else if (s_key.equals(DEPLib.FEAT_VN))
						sense = "unknown";
				}
				
				pred.addFeat(s_key, sense);				
			}
		}
	}
	
	/** Called by {@link CSenseClassifier#classify()}. */
	protected String getLabel(int modelId)
 	 {
		StringFeatureVector vector = getFeatureVector(f_xmls[0]);
		String label = null;
		
		if (i_flag == FLAG_TRAIN)
		{
			label = getGoldLabel();
			s_spaces[modelId].addInstance(label, vector);
		}
		else if (i_flag == FLAG_DECODE || i_flag == FLAG_DEVELOP)
		{
			label = getAutoLabel(vector, modelId);
		}
		
		return label;
	}
	
	/** Called by {@link CSenseClassifier#getLabel()}. */
	private String getGoldLabel()
	{
		return g_senses[i_pred];
	}
	
	/** Called by {@link CSenseClassifier#getLabel()}. */
	private String getAutoLabel(StringFeatureVector vector, int modelId)
	{
		StringPrediction p = s_models[modelId].predictBest(vector);
		return p.label;
	}

//	====================================== FEATURE EXTRACTION ======================================

	@Override
	protected String getField(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(JointFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(JointFtrXml.F_LEMMA))
		{
			return node.lemma;
		}
		else if (token.isField(JointFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(JointFtrXml.F_DEPREL))
		{
			return node.getLabel();
		}
		else if (token.isField(JointFtrXml.F_LEFT_DEP_POS))
		{
			return getDependents(node.getLeftDependents(), "p");
		}
		else if (token.isField(JointFtrXml.F_RIGHT_DEP_POS))
		{
			return getDependents(node.getRightDependents(), "p");
		}
		else if (token.isField(JointFtrXml.F_LEFT_DEP_DEPREL))
		{
			return getDependents(node.getLeftDependents(), "d");
		}
		else if (token.isField(JointFtrXml.F_RIGHT_DEP_DEPREL))
		{
			return getDependents(node.getRightDependents(), "d");
		}
		else if ((m = JointFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		
		return null;
	}
	
	private String getDependents(List<DEPNode> nodes, String type)
	{
		boolean isPos = type.equals("p");
		
		if (!nodes.isEmpty())
		{
			StringBuilder build = new StringBuilder();
			
			for (DEPNode node : nodes)
			{
				build.append("_");
				
				if (isPos)	build.append(node.pos);
				else		build.append(node.getLabel());
			}
			
			return build.substring(1);
		}
		else
			return null;
	}
	
	@Override
	protected String[] getFields(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		
		if (token.isField(JointFtrXml.F_DEPREL_SET))
		{
			return getDeprelSet(node.getDependents());
		}
		
		return null;
	}
	
	private String[] getDeprelSet(List<DEPArc> deps)
	{
		if (deps.isEmpty())	return null;
		
		Set<String> set = new HashSet<String>();
		for (DEPArc arc : deps)	set.add(arc.getLabel());
		
		String[] fields = new String[set.size()];
		set.toArray(fields);
		
		return fields;		
	}
	
//	====================================== NODE GETTER ======================================
	
	/** @return a node specified by the feature token. */
	private DEPNode getNode(FtrToken token)
	{
		DEPNode node = getNodeAux(token);
		if (node == null)	return null;
		
		if (token.relation != null)
		{
			     if (token.isRelation(JointFtrXml.R_H))		node = node.getHead();
			else if (token.isRelation(JointFtrXml.R_LMD))	node = node.getLeftMostDependent();
			else if (token.isRelation(JointFtrXml.R_RMD))	node = node.getRightMostDependent();
			else if (token.isRelation(JointFtrXml.R_LND))	node = node.getLeftNearestDependent();
			else if (token.isRelation(JointFtrXml.R_RND))	node = node.getRightNearestDependent();
		}
		
		return node;
	}
	
	/** Called by {@link PredIdentifier#getNode(FtrToken)}. */
	private DEPNode getNodeAux(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_pred);
		
		int cIndex = i_pred + token.offset;
		
		if (0 < cIndex && cIndex < d_tree.size())
			return d_tree.get(cIndex);
		
		return null;
	}
}