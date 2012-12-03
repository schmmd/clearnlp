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
package com.googlecode.clearnlp.component;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;

import com.googlecode.clearnlp.bin.COMLib;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTString;
import com.googlecode.clearnlp.util.map.Prob2DMap;
import com.googlecode.clearnlp.util.pair.StringDoublePair;

/**
 * Part-of-speech tagger using dynamic model selection.
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class CPOSTagger extends AbstractComponent
{
	protected final String ENTRY_FEATURE = COMLib.MODE_POS+"_FEATURE";
	protected final String ENTRY_MODEL   = COMLib.MODE_POS+"_MODEL";
	protected final String ENTRY_LEXICA  = COMLib.MODE_POS+"_LEXICA";
	
	protected final int LEXICA_LOWER_SIMPLIFIED_FORMS = 0;
	protected final int LEXICA_AMBIGUITY_CLASSES      = 1;
	
	protected Set<String>			s_lsfs;		// lower simplified forms
	protected Prob2DMap				p_ambi;		// ambiguity classes (only for collecting)
	protected Map<String,String>	m_ambi;		// ambiguity classes
	protected String[]          	g_tags;		// gold-standard part-of-speech tags
	protected int 					i_input;
	
//	====================================== CONSTRUCTORS ======================================

	/** Constructs a part-of-speech tagger for collecting lexica. */
	public CPOSTagger(JointFtrXml[] xmls, Set<String> sLsfs)
	{
		super(xmls);

		s_lsfs = sLsfs;
		p_ambi = new Prob2DMap();
	}
	
	/** Constructs a part-of-speech tagger for training. */
	public CPOSTagger(JointFtrXml[] xmls, StringTrainSpace[] spaces, Object[] lexica)
	{
		super(xmls, spaces, lexica);
	}
	
	/** Constructs a part-of-speech tagger for developing. */
	public CPOSTagger(JointFtrXml[] xmls, StringModel[] models, Object[] lexica)
	{
		super(xmls, models, lexica);
	}
	
	/** Constructs a part-of-speech tagger for decoding. */
	public CPOSTagger(ZipInputStream in)
	{
		super(in);
	}
	
	@Override @SuppressWarnings("unchecked")
	protected void initLexia(Object[] lexica)
	{
		s_lsfs = (Set<String>)lexica[LEXICA_LOWER_SIMPLIFIED_FORMS];
		m_ambi = (Map<String,String>)lexica[LEXICA_AMBIGUITY_CLASSES];
	}
	
//	====================================== LOAD/SAVE MODELS ======================================
	
	@Override
	public void loadModels(ZipInputStream zin)
	{
		int fLen = ENTRY_FEATURE.length() + 1, mLen = ENTRY_MODEL.length() + 1;
		s_models = new StringModel[1];
		f_xmls   = new JointFtrXml[1];
		ZipEntry zEntry;
		String   entry;
				
		try
		{
			while ((zEntry = zin.getNextEntry()) != null)
			{
				entry = zEntry.getName();
				
				if (entry.startsWith(ENTRY_FEATURE))
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
		System.out.println("Loading lexica.");
		
		s_lsfs = UTInput.getStringSet(fin);
		m_ambi = UTInput.getStringMap(fin, " ");
		
		fin.close();
	}

	@Override
	public void saveModels(ZipOutputStream zout)
	{
		try
		{
			saveFeatureTemplates (zout, ENTRY_FEATURE);
			saveStatisticalModels(zout, ENTRY_MODEL);
			saveLexica(zout);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void saveLexica(ZipOutputStream zout) throws Exception
	{
		zout.putNextEntry(new JarArchiveEntry(ENTRY_LEXICA));
		PrintStream fout = new PrintStream(new BufferedOutputStream(zout));
		System.out.println("Saving lexica.");
		
		UTOutput.printSet(fout, s_lsfs);
		UTOutput.printMap(fout, m_ambi, " ");
		
		fout.close();
		zout.closeEntry();
	}
	
//	====================================== GETTERS AND SETTERS ======================================

	@Override
	public Object[] getLexica()
	{
		Object[] lexica = new Object[2];
		
		lexica[LEXICA_LOWER_SIMPLIFIED_FORMS] = s_lsfs;
		lexica[LEXICA_AMBIGUITY_CLASSES] = (i_flag == FLAG_LEXICA) ? getAmbiguityClasses() : m_ambi;
		
		return lexica;
	}
	
	@Override
	public Object[] getGoldTags()
	{
		return g_tags;
	}
	
	/** {@link AbstractComponent#FLAG_LEXICA}. */
	public Set<String> getLowerSimplifiedForms()
	{
		return s_lsfs;
	}
	
	/** {@link AbstractComponent#FLAG_LEXICA}. */
	public void clearLowerSimplifiedForms()
	{
		s_lsfs.clear();
	}
	
	/** Called by {@link CPOSTagger#getLexica()}. */
	private Map<String,String> getAmbiguityClasses()
	{
		double threshold = f_xmls[0].getAmbiguityClassThreshold();
		Map<String,String> mAmbi = new HashMap<String,String>();
		StringDoublePair[] ps;
		StringBuilder build;
				
		for (String key : p_ambi.keySet())
		{
			build = new StringBuilder();
			ps = p_ambi.getProb1D(key);
			Arrays.sort(ps);
			
			for (StringDoublePair p : ps)
			{
				if (p.d <= threshold)	break;
				
				build.append(AbstractColumnReader.BLANK_COLUMN);
				build.append(p.s);
			}
			
			if (build.length() > 0)
				mAmbi.put(key, build.substring(1));				
		}
		
		return mAmbi;
	}
	
//	====================================== PROCESS ======================================
	
	@Override
	public void countAccuracy(int[] counts)
	{
		int i;
		
		counts[0] += t_size - 1;

		for (i=1; i<t_size; i++)
		{
			if (d_tree.get(i).pos.equals(g_tags[i]))
				counts[1]++;
		}
	}
	
	@Override
	public void process(DEPTree tree)
	{
		init(tree);
		processAux();
	}
	
	/** Called by {@link CPOSTagger#process(DEPTree)}. */
	protected void init(DEPTree tree)
	{
	 	d_tree = tree;
	 	t_size = tree.size();

	 	if (i_flag != FLAG_DECODE)
	 		g_tags = d_tree.getPOSTags();
	 	
	 	if (d_tree.get(1) != null)
	 		d_tree.clearPOSTags();
	 	
	 	EngineProcess.normalizeForms(d_tree);
	}
	
	/** Called by {@link CPOSTagger#process(DEPTree)}. */
	protected void processAux()
	{
		if (i_flag == FLAG_LEXICA)
			addLexica();
		else
			tagLR();
	}
	
	/** Called by {@link CPOSTagger#processAux()}. */
	protected void addLexica()
	{
		DEPNode node;
		int i;

		for (i=1; i<t_size; i++)
		{
			node = d_tree.get(i);
			
			if (s_lsfs.contains(node.lowerSimplifiedForm))			
				p_ambi.add(node.simplifiedForm, g_tags[i]);
		}
	}
	
	/** Called by {@link CPOSTagger#processAux()}. */
	protected void tagLR()
	{
		DEPNode input;
		
		for (i_input=1; i_input<t_size; i_input++)
		{
			input = d_tree.get(i_input);
			input.pos = getLabel();
		}
	}
	
	/** Called by {@link CPOSTagger#processAux()}. */
	protected void tagRL()
	{
		DEPNode input;
		
		for (i_input=t_size-1; i_input>0; i_input--)
		{
			input = d_tree.get(i_input);
			input.pos = getLabel();
		}
	}
	
	/** Called by {@link CPOSTagger#tagLR()}. */
	protected String getLabel()
	{
		StringFeatureVector vector = getFeatureVector(f_xmls[0]);
		String label = null;
		
		if (i_flag == FLAG_TRAIN)
		{
			label = getGoldLabel();
			if (vector.size() > 0) s_spaces[0].addInstance(label, vector);
		}
		else if (i_flag == FLAG_DECODE || i_flag == FLAG_DEVELOP)
		{
			label = getAutoLabel(vector);
		}
		
		return label;
	}
	
	/** Called by {@link CPOSTagger#getLabel()}. */
	private String getGoldLabel()
	{
		return g_tags[i_input];
	}
	
	/** Called by {@link CPOSTagger#getLabel()}. */
	private String getAutoLabel(StringFeatureVector vector)
	{
		StringPrediction p = s_models[0].predictBest(vector);
		return p.label;
	}

//	====================================== FEATURE EXTRACTION ======================================

	@Override
	protected String getField(FtrToken token)
	{
		DEPNode node = getNodeInput(token);
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(JointFtrXml.F_SIMPLIFIED_FORM))
		{
			return (s_lsfs.contains(node.lowerSimplifiedForm)) ? node.simplifiedForm : null;
		}
		else if (token.isField(JointFtrXml.F_LOWER_SIMPLIFIED_FORM))
		{
			return (s_lsfs.contains(node.lowerSimplifiedForm)) ? node.lowerSimplifiedForm : null;
		}
		else if (token.isField(JointFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(JointFtrXml.F_AMBIGUITY_CLASS))
		{
			return m_ambi.get(node.simplifiedForm);
		}
		else if ((m = JointFtrXml.P_BOOLEAN.matcher(token.field)).find())
		{
			int field = Integer.parseInt(m.group(1));
			
			switch (field)
			{
			case  0: return UTString.isAllUpperCase(node.simplifiedForm) ? token.field : null;
			case  1: return UTString.isAllLowerCase(node.simplifiedForm) ? token.field : null;
			case  2: return UTString.beginsWithUpperCase(node.simplifiedForm) ? token.field : null;
			case  3: return UTString.getNumOfCapitalsNotAtBeginning(node.simplifiedForm) == 1 ? token.field : null;
			case  4: return UTString.getNumOfCapitalsNotAtBeginning(node.simplifiedForm)  > 1 ? token.field : null;
			case  5: return node.simplifiedForm.contains(".") ? token.field : null;
			case  6: return UTString.containsDigit(node.simplifiedForm) ? token.field : null;
			case  7: return node.simplifiedForm.contains("-") ? token.field : null;
			case  8: return (i_input == t_size-1) ? token.field : null;
			case  9: return (i_input == 1) ? token.field : null;
			default: throw new IllegalArgumentException("Unsupported feature: "+field);
			}
		}
		else if ((m = JointFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		
		return null;
	}
	
	@Override
	protected String[] getFields(FtrToken token)
	{
		DEPNode node = getNodeInput(token);
		if (node == null)	return null;
		Matcher m;
		
		if ((m = JointFtrXml.P_PREFIX.matcher(token.field)).find())
		{
			String[] fields = UTString.getPrefixes(node.lowerSimplifiedForm, Integer.parseInt(m.group(1)));
			return fields.length == 0 ? null : fields;
		}
		else if ((m = JointFtrXml.P_SUFFIX.matcher(token.field)).find())
		{
			String[] fields = UTString.getSuffixes(node.lowerSimplifiedForm, Integer.parseInt(m.group(1)));
			return fields.length == 0 ? null : fields;
		}
		
		return null;
	}
	
//	====================================== NODE GETTER ======================================
	
	/** @return a node specified by the feature token. */
	protected DEPNode getNodeInput(FtrToken token)
	{
		int index = i_input + token.offset;
		return (0 < index && index < t_size) ? d_tree.get(index) : null;
	}
}
