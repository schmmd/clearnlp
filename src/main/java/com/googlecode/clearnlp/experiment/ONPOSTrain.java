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
package com.googlecode.clearnlp.experiment;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.component.pos.CPOSTagger;
import com.googlecode.clearnlp.component.pos.ONPOSTagger;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.nlp.NLPDevelop;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;

public class ONPOSTrain extends NLPDevelop
{
	public ONPOSTrain(String[] args)
	{
		super();
		initArgs(args);
		
		try
		{
			develop(s_configFile, s_featureFiles.split(DELIM_FILES), s_trainDir, s_devDir, s_mode);
		}
		catch (Exception e) {e.printStackTrace();}
	}

	public void develop(String configFile, String[] featureFiles, String trainDir, String devDir, String mode) throws Exception
	{
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		JointFtrXml[]  xmls = getFeatureTemplates(featureFiles);
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, ".*", true);
		String[]   devFiles = UTFile.getSortedFileListBySize(devDir, ".*", true);
		JointReader  reader = getJointReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
		
		AbstractStatisticalComponent component = new CPOSTagger(xmls, getLowerSimplifiedForms(reader, xmls[0], trainFiles, -1));
		Object[] lexica = (component != null) ? getLexica(component, reader, xmls, trainFiles, -1) : null;
		
		ONPOSTagger tagger = new ONPOSTagger(xmls, lexica, 0.01, 0.1);
		List<DEPTree> trainTrees = getTrees(reader, trainFiles);
		List<DEPTree> devTrees = getTrees(reader, devFiles);
		int i, size = trainTrees.size(), cut = size / 10;
		double prevScore = -1, currScore;
		
		tagger.train(trainTrees, 0, cut);
				
		for (i=cut; i<size; i++)
			tagger.train(trainTrees.get(i));
		
		currScore = decode(devTrees, tagger);
		
		while (prevScore < currScore)
		{
			prevScore = currScore;
			
			for (i=0; i<size; i++)
				tagger.train(trainTrees.get(i));
			
			currScore = decode(devTrees, tagger);
		}
	}
	
	protected List<DEPTree> getTrees(JointReader reader, String[] inputFiles)
	{
		List<DEPTree> trees = new ArrayList<DEPTree>();
		DEPTree tree;
		
		for (String inputFile : inputFiles)
		{
			reader.open(UTInput.createBufferedFileReader(inputFile));
			
			while ((tree = reader.next()) != null)
				trees.add(tree);	

			reader.close();
		}

		return trees;
	}
	
	protected double decode(List<DEPTree> devTrees, ONPOSTagger tagger)
	{
		int[] counts = getCounts(NLPLib.MODE_POS);
		
		for (DEPTree tree : devTrees)
		{
			tagger.develop(tree);
			tagger.countAccuracy(counts);
			tagger.resetGold();
		}
		
		return getScore(NLPLib.MODE_POS, counts);
	}
	
	static public void main(String[] args)
	{
		new ONPOSTrain(args);
	}
}
