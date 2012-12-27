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
package com.googlecode.clearnlp.bin;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Random;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.component.dep.CDEPPassParser;
import com.googlecode.clearnlp.component.pos.CPOSTagger;
import com.googlecode.clearnlp.component.srl.CRolesetClassifier;
import com.googlecode.clearnlp.component.srl.CSRLabeler;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLEval;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.ObjectDoublePair;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class COMDevelop extends COMTrain
{
	@Option(name="-d", usage="the directory containing development files (required)", required=true, metaVar="<directory>")
	private String s_devDir;
	@Option(name="-r", usage="the random seed", required=false, metaVar="<directory>")
	private int i_rand = 0;
	
	public COMDevelop(String[] args)
	{
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
		
		if      (mode.equals(COMLib.MODE_POS))
			developComponent(eConfig, reader, xmls, trainFiles, devFiles, new CPOSTagger(xmls, getLowerSimplifiedForms(reader, xmls[0], trainFiles, -1)), mode);
		else if (mode.equals(COMLib.MODE_DEP))
			developComponentBoot(eConfig, reader, xmls, trainFiles, devFiles, new CDEPPassParser(xmls), mode);
		else if (mode.equals(COMLib.MODE_PRED))
			decode(reader, getTrainedComponent(eConfig, xmls, trainFiles, null, null, mode, 0, -1), devFiles, mode);
		else if (mode.equals(COMLib.MODE_ROLE))
			decode(reader, getTrainedComponent(eConfig, reader, xmls, trainFiles, new CRolesetClassifier(xmls), mode, -1), devFiles, mode);
		else if (mode.equals(COMLib.MODE_SRL))
			developComponentBoot(eConfig, reader, xmls, trainFiles, devFiles, new CSRLabeler(xmls), mode);
	}
	
	protected double developComponent(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles, AbstractStatisticalComponent component, String mode) throws Exception
	{
		Object[] lexica = (component != null) ? getLexica(component, reader, xmls, trainFiles, -1) : null;
		StringTrainSpace[] spaces = getStringTrainSpaces(eConfig, xmls, trainFiles, null, lexica, mode, -1);
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		int i, mSize = spaces.length, nUpdate = 1;
		AbstractStatisticalComponent processor;
		
		StringModel[] models = new StringModel[mSize];
		double prevScore = -1, currScore = 0;
		Random rand = new Random(i_rand);
		
		do
		{
			prevScore = currScore;
			
			for (i=0; i<mSize; i++)
			{
				updateModel(eTrain, spaces[i], rand, nUpdate++, i);
				models[i] = (StringModel)spaces[i].getModel();
			}

			processor = getComponent(xmls, models, lexica, mode);
			currScore = decode(reader, processor, devFiles, mode);
		}
		while (prevScore < currScore);
		
		return prevScore;
	}
	
	protected void developComponentBoot(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles, AbstractStatisticalComponent component, String mode) throws Exception
	{
		Object[] lexica = getLexica(component, reader, xmls, trainFiles, -1);
		ObjectDoublePair<StringModel[]> p;
		double prevScore, currScore = 0;
		StringModel[] models = null;
		
		do
		{
			prevScore = currScore;
			p = developComponent(eConfig, reader, xmls, trainFiles, devFiles, lexica, models, mode);
			models = (StringModel[])p.o;
			currScore = p.d;			
		}
		while (prevScore < currScore);
	}
	
	private ObjectDoublePair<StringModel[]> developComponent(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles, Object[] lexica, StringModel[] models, String mode) throws Exception
	{
		StringTrainSpace[] spaces = getStringTrainSpaces(eConfig, xmls, trainFiles, models, lexica, mode, -1);
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		int i, mSize = spaces.length, nUpdate = 1;
		AbstractStatisticalComponent component;
		
		double prevScore = -1, currScore = 0;
		Random rand = new Random(i_rand);
		models = new StringModel[mSize];

		double[][] prevWeights = new double[mSize][];
		double[] d;
		
		do
		{
			prevScore = currScore;
			
			for (i=0; i<mSize; i++)
			{
				if (models[i] != null)
				{
					d = models[i].getWeights();
					prevWeights[i] = Arrays.copyOf(d, d.length);
				}
				
				updateModel(eTrain, spaces[i], rand, nUpdate, i);
				models[i] = (StringModel)spaces[i].getModel();
			}

			nUpdate++;
			component = getComponent(xmls, models, lexica, mode);
			currScore = decode(reader, component, devFiles, mode);
		}
		while (prevScore < currScore);
		
		for (i=0; i<mSize; i++)
			models[i].setWeights(prevWeights[i]);
		
		return new ObjectDoublePair<StringModel[]>(models, prevScore);
	}
	
	protected double decode(JointReader reader, AbstractStatisticalComponent component, String[] devFiles, String mode) throws Exception
	{
		int[] counts = getCounts(mode);
		DEPTree tree;
		
		for (String devFile : devFiles)
		{
	//		PrintStream fout = UTOutput.createPrintBufferedFileStream(devFile+".cnlp");
			reader.open(UTInput.createBufferedFileReader(devFile));
			
			while ((tree = reader.next()) != null)
			{
				component.process(tree);
				component.countAccuracy(counts);
	//			fout.println(tree.toStringPOS()+"\n");
			}
			
			reader.close();
	//		fout.close();
		}

		return getScore(mode, counts);
	}
	
	protected int[] getCounts(String mode)
	{
		if      (mode.equals(COMLib.MODE_POS) || mode.equals(COMLib.MODE_ROLE))
			return new int[2];
		else if (mode.equals(COMLib.MODE_DEP))
			return new int[4];
		else if (mode.equals(COMLib.MODE_PRED) || mode.equals(COMLib.MODE_SRL))
			return new int[3];
		
		return null;
	}
	
	protected double getScore(String mode, int[] counts)
	{
		double score = 0;
		
		if (mode.equals(COMLib.MODE_POS) || mode.equals(COMLib.MODE_ROLE))
		{
			score = 100d * counts[1] / counts[0];
			System.out.printf("- ACC: %5.2f (%d/%d)\n", score, counts[1], counts[0]);
		}
		else if (mode.equals(COMLib.MODE_DEP))
		{
			score = 100d * counts[1] / counts[0];
			System.out.printf("- LAS: %5.2f (%d/%d)\n", score, counts[1], counts[0]);
			System.out.printf("- UAS: %5.2f (%d/%d)\n", 100d*counts[2]/counts[0], counts[2], counts[0]);
			System.out.printf("- LS : %5.2f (%d/%d)\n", 100d*counts[3]/counts[0], counts[3], counts[0]);
		}
		else if (mode.equals(COMLib.MODE_PRED) || mode.equals(COMLib.MODE_SRL))
		{
			double p = 100d * counts[0] / counts[1];
			double r = 100d * counts[0] / counts[2];
			score = SRLEval.getF1(p, r);
			
			System.out.printf("P : %5.2f\n", p);
			System.out.printf("R : %5.2f\n", r);
			System.out.printf("F1: %5.2f\n", score);
		}
		
		return score;
	}
	
	static public void main(String[] args)
	{
		new COMDevelop(args);
	}
	
/*	protected void developPOSTagger(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles) throws Exception
	{
		CPOSTagger tagger = getTrainedPOSTagger(eConfig, reader, xmls, trainFiles, -1);
		predict(reader, tagger, devFiles, COMLib.MODE_POS);
	}
	
	protected void developDEPParser(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles) throws Exception
	{
		Object[] lexica = getLexica(new CDEPPassParser(xmls), reader, xmls, trainFiles, -1);
		double prevScore, currScore = 0;
		StringModel[] models = null;
		CDEPPassParser parser = null;
		
		do
		{
			prevScore = currScore;
			
			parser = (CDEPPassParser)getTrainedComponent(eConfig, xmls, trainFiles, models, lexica, COMLib.MODE_DEP, -1);
			models = parser.getModels();

			currScore = decode(reader, parser, devFiles, COMLib.MODE_DEP);
		}
		while (prevScore < currScore);
	}
	
	protected void developSRLabeler(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles) throws Exception
	{
		Object[] lexica = getLexica(new CSRLabeler(xmls), reader, xmls, trainFiles, -1);
		AbstractStatisticalComponent labeler = null;
		double prevScore, currScore = 0;
		StringModel[] models = null;
		
		do
		{
			prevScore = currScore;
			
			labeler = getTrainedComponent(eConfig, xmls, trainFiles, models, lexica, COMLib.MODE_SRL, 0, -1);
			models  = labeler.getModels();

			currScore = decode(reader, labeler, devFiles, COMLib.MODE_SRL);
		}
		while (prevScore < currScore);
	}
	*/
}
