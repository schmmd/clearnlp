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
package com.googlecode.clearnlp.online;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTString;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * Part-of-speech tagger using document frequency cutoffs.
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class ONPOSTagger extends AbstractStatisticalComponent
{
	private final String ENTRY_CONFIGURATION = NLPLib.MODE_POS + NLPLib.ENTRY_CONFIGURATION;
	private final String ENTRY_FEATURE		 = NLPLib.MODE_POS + NLPLib.ENTRY_FEATURE;
	private final String ENTRY_LEXICA		 = NLPLib.MODE_POS + NLPLib.ENTRY_LEXICA;
	private final String ENTRY_MODEL		 = NLPLib.MODE_POS + NLPLib.ENTRY_MODEL;
	
	protected final int LEXICA_LOWER_SIMPLIFIED_FORMS = 0;
	protected final int LEXICA_AMBIGUITY_CLASSES      = 1;
	
	protected ONStringModel 		s_model;
	protected Set<String>			s_lsfs;		// lower simplified forms
	protected Map<String,String>	m_ambi;		// ambiguity classes
	protected String[]          	g_tags;		// gold-standard part-of-speech tags
	protected int 					i_input;
	
//	====================================== CONSTRUCTORS ======================================

	public ONPOSTagger(ZipInputStream zin, double alpha, double rho)
	{
		loadModels(zin, alpha, rho);
	}
	
//	====================================== LOAD/SAVE MODELS ======================================
	
	public void loadModels(ZipInputStream zin, double alpha, double rho)
	{
		int fLen = ENTRY_FEATURE.length();
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
					loadStatisticalModels(zin, alpha, rho);
				else if (entry.equals(ENTRY_LEXICA))
					loadLexica(zin);
			}		
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	protected void loadStatisticalModels(ZipInputStream zin, double alpha, double rho) throws Exception
	{
		BufferedReader fin = UTInput.createBufferedReader(zin);
		s_model = new ONStringModel(fin, alpha, rho);
	}
	
	private void loadLexica(ZipInputStream zin) throws Exception
	{
		BufferedReader fin = UTInput.createBufferedReader(zin);
		System.out.println("Loading lexica.");
		
		s_lsfs = UTInput.getStringSet(fin);
		m_ambi = UTInput.getStringMap(fin, " ");
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
	
	@Override
	protected void saveStatisticalModels(ZipOutputStream zout, String entryName) throws Exception
	{
		zout.putNextEntry(new ZipEntry(entryName+"0"));
		PrintStream fout = UTOutput.createPrintBufferedStream(zout);
		s_model.save(fout);
		fout.flush();
		zout.closeEntry();
	}
	
	private void saveLexica(ZipOutputStream zout) throws Exception
	{
		zout.putNextEntry(new ZipEntry(ENTRY_LEXICA));
		PrintStream fout = UTOutput.createPrintBufferedStream(zout);
		System.out.println("Saving lexica.");
		
		UTOutput.printSet(fout, s_lsfs);		fout.flush();
		UTOutput.printMap(fout, m_ambi, " ");	fout.flush();
		
		zout.closeEntry();
	}
	
//	====================================== GETTERS AND SETTERS ======================================
	
	@Override
	public Object[] getGoldTags()
	{
		return g_tags;
	}
	
	@Override
	public void countAccuracy(int[] counts)
	{
		int i, correct = 0;
		
		for (i=1; i<t_size; i++)
		{
			if (d_tree.get(i).pos.equals(g_tags[i]))
				correct++;
		}
		
		counts[0] += t_size - 1;
		counts[1] += correct;
	}
	
//	====================================== PROCESS ======================================
	
	@Override
	public void process(DEPTree tree)
	{
		init(tree);
		processAux();
	}
	
	/** Called by {@link ONPOSTagger#process(DEPTree)}. */
	protected void init(DEPTree tree)
	{
	 	d_tree = tree;
	 	t_size = tree.size();

	 	EngineProcess.normalizeForms(tree);
	}
	
	/** Called by {@link ONPOSTagger#process(DEPTree)}. */
	protected void processAux()
	{
		DEPNode input;
		
		for (i_input=1; i_input<t_size; i_input++)
		{
			input = d_tree.get(i_input);
			input.pos = getAutoLabel(getFeatureVector(f_xmls[0]));
		}
	}
	
	private String getAutoLabel(StringFeatureVector vector)
	{
		StringPrediction p = s_model.predictBest(vector);
		return p.label;
	}
	
	public void trainHard(DEPTree tree)
	{
		List<Pair<String,StringFeatureVector>> insts;
		g_tags = tree.getPOSTags();
		int i, j, correct;
		init(tree);
		
		for (i=0; i<10; i++)
		{
			tree.clearPOSTags();
			insts = getInstances();
			correct = 0;
			
			for (j=1; j<t_size; j++)
			{	
				if (tree.get(j).isPos(g_tags[j]))
					correct++;
			}
			
			System.out.println("== "+i+" ==\n"+tree.toStringPOS()+"\n");
			
			if (correct == t_size-1)
				break;
			
			s_model.updateWeights(insts);
		}
	}
	
	private List<Pair<String,StringFeatureVector>> getInstances()
	{
		List<Pair<String,StringFeatureVector>> insts = new ArrayList<Pair<String,StringFeatureVector>>();
		StringFeatureVector vector;
		DEPNode input;
		String  label;
		
		for (i_input=1; i_input<t_size; i_input++)
		{
			input  = d_tree.get(i_input);
			label  = getGoldLabel();
			vector = getFeatureVector(f_xmls[0]);
			
			input.pos = getAutoLabel(vector);
			insts.add(new Pair<String,StringFeatureVector>(label, vector));
		}
		
		return insts;
	}
	
	/** Called by {@link ONPOSTagger#tag()}. */
	protected String getBootLabel()
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
	
	/** Called by {@link ONPOSTagger#getLabel()}. */
	private String getGoldLabel()
	{
		return g_tags[i_input];
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
		else if ((m = JointFtrXml.P_PREFIX.matcher(token.field)).find())
		{
			int n = Integer.parseInt(m.group(1)), len = node.lowerSimplifiedForm.length();
			return (n <= len) ? node.lowerSimplifiedForm.substring(0, n) : null;
		}
		else if ((m = JointFtrXml.P_SUFFIX.matcher(token.field)).find())
		{
			int n = Integer.parseInt(m.group(1)), len = node.lowerSimplifiedForm.length();
			return (n <= len) ? node.lowerSimplifiedForm.substring(len-n, len) : null;
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

	@Override
	protected void initLexia(Object[] lexica) {}

	@Override
	public void loadModels(ZipInputStream zin) {}

	@Override
	public Object[] getLexica() {return null;}
}
