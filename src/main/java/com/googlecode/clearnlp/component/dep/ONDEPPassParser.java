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
package com.googlecode.clearnlp.component.dep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.googlecode.clearnlp.classification.model.ONStringModel;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * Dependency parser using *-pass transitions.
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class ONDEPPassParser extends CDEPParser
{
	private ONStringModel o_model;
	
//	====================================== CONSTRUCTORS ======================================

	public ONDEPPassParser(JointFtrXml[] xmls, Object[] lexica, double alpha, double rho)
	{
		f_xmls   = xmls;
		s_models = new ONStringModel[]{new ONStringModel(alpha, rho)};
		initLexia(lexica);
		initOnline();
	}
	
	public ONDEPPassParser(ZipInputStream zin, double alpha, double rho)
	{
		loadModels(zin, alpha, rho);
		initOnline();
	}
	
	private void initOnline()
	{
		i_flag  = FLAG_DECODE;
		o_model = (ONStringModel)s_models[0];
	}
	
//	====================================== LOAD/SAVE MODELS ======================================
	
	public void loadModels(ZipInputStream zin, double alpha, double rho)
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
					loadOnlineModels(zin, Integer.parseInt(entry.substring(mLen)), alpha, rho);
				else if (entry.equals(ENTRY_LEXICA))
					loadLexica(zin);
			}		
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
//	====================================== TRAIN ======================================
	
	private void initOnline(DEPTree tree)
	{
		i_lambda = 0;
	 	i_beta   = 1;
	 	
	 	s_reduce.clear();
	 	
	 	int i; for (i=0; i<t_size; i++)
	 	{
	 		lm_deps[i] = null;
	 		rm_deps[i] = null;
	 		ln_sibs[i] = null;
	 		rn_sibs[i] = null;
	 		
	 		l_2nd.get(i).clear();
	 	}
	 	
	 	tree.clearHeads();
	}
	
	public void trainHard(DEPTree tree, int maxIter)
	{
		List<Pair<String,StringFeatureVector>> insts;
		int[] counts = new int[4];
		byte flag = i_flag;
		int i;
		
		i_flag = FLAG_BOOTSTRAP;
		init(tree);
		
		for (i=0; i<maxIter; i++)
		{
			initOnline(tree);
			Arrays.fill(counts, 0);
			
			insts = parse();
			countAccuracy(counts);
			
			if (counts[0] == counts[1])	break;
			o_model.updateWeights(insts);
		}
		
		i_flag = flag;
	}
	
	public void train(List<DEPTree> trees)
	{
		List<Pair<String,StringFeatureVector>> insts = new ArrayList<Pair<String,StringFeatureVector>>(), tmp;
		int[] counts = new int[4];
		byte flag = i_flag;
		
		i_flag = FLAG_BOOTSTRAP;
		
		for (DEPTree tree : trees)
		{
			init(tree);
			Arrays.fill(counts, 0);
			
			tmp = parse();
			countAccuracy(counts);
			
			if (counts[0] != counts[1])
				insts.addAll(tmp);
		}

		o_model.updateWeights(insts);
		i_flag = flag;		
	}
	
	public void develop(DEPTree tree)
	{
		byte flag = i_flag;
		
		i_flag = FLAG_DECODE;
		process(tree);
		
		i_flag = flag;
	}
}
