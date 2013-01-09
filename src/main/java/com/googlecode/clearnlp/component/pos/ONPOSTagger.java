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
package com.googlecode.clearnlp.component.pos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.googlecode.clearnlp.classification.model.ONStringModel;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * Part-of-speech tagger using document frequency cutoffs.
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class ONPOSTagger extends CPOSTagger
{
	private ONStringModel	o_model;
	private int				n_maxIter;
	
//	====================================== CONSTRUCTORS ======================================

	public ONPOSTagger(ZipInputStream zin, int maxIter, double alpha, double rho)
	{
		loadModels(zin, alpha, rho);
		n_maxIter = maxIter;
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
					o_model = getOnlineModel(zin, alpha, rho);
				else if (entry.equals(ENTRY_LEXICA))
					loadLexica(zin);
			}		
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	@Override
	public void saveModels(ZipOutputStream zout)
	{
		try
		{
			saveDefaultConfiguration(zout, ENTRY_CONFIGURATION);
			saveFeatureTemplates    (zout, ENTRY_FEATURE);
			saveLexica              (zout);
			saveOnlineModel         (zout, ENTRY_MODEL+"0", o_model);
			zout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
//	====================================== PROCESS ======================================
	
	@Override
	public void process(DEPTree tree)
	{
		i_flag = FLAG_DECODE;
		
		init(tree);
		processOnline();
	}
	
	protected List<Pair<String,StringFeatureVector>> processOnline()
	{
		List<Pair<String,StringFeatureVector>> insts = new ArrayList<Pair<String,StringFeatureVector>>();
		StringFeatureVector vector;
		DEPNode input;
		
		for (i_input=1; i_input<t_size; i_input++)
		{
			input  = d_tree.get(i_input);
			vector = getFeatureVector(f_xmls[0]);
			
			input.pos = getAutoLabel(vector);
			if (i_flag != FLAG_DECODE)	insts.add(new Pair<String,StringFeatureVector>(getGoldLabel(), vector));
		}
		
		return insts;
	}
	
	public void trainHard(DEPTree tree)
	{
		List<Pair<String,StringFeatureVector>> insts;
		int[] counts = new int[2];
		int i;
		
		i_flag = FLAG_BOOTSTRAP;
		init(tree);
		
		for (i=0; i<n_maxIter; i++)
		{
			tree.clearPOSTags();
			Arrays.fill(counts, 0);
			
			insts = processOnline();
			countAccuracy(counts);
			System.out.println("== "+i+" ==\n"+tree.toStringPOS()+"\n");
			
			if (counts[0] == counts[1])	break;
			o_model.updateWeights(insts);
		}
	}
	
	private String getAutoLabel(StringFeatureVector vector)
	{
		StringPrediction p = o_model.predictBest(vector);
		return p.label;
	}
}
