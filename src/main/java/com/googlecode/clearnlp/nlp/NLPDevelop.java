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
package com.googlecode.clearnlp.nlp;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.component.dep.CDEPParserSB;
import com.googlecode.clearnlp.component.dep.CDEPParser;
import com.googlecode.clearnlp.component.pos.CPOSTaggerSB;
import com.googlecode.clearnlp.component.pos.CPOSTagger;
import com.googlecode.clearnlp.component.srl.CRolesetClassifier;
import com.googlecode.clearnlp.component.srl.CSRLabeler;
import com.googlecode.clearnlp.component.srl.CSenseClassifier;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLEval;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.ObjectDoublePair;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class NLPDevelop extends NLPTrain
{
	@Option(name="-d", usage="the directory containing development files (required)", required=true, metaVar="<directory>")
	protected String s_devDir;
	@Option(name="-r", usage="the random seed", required=false, metaVar="<directory>")
	protected int i_rand = 0;
	@Option(name="-g", usage="if set, generate files", required=false, metaVar="<boolean>")
	protected boolean b_generate = false;
	
	public NLPDevelop() {}
	
	public NLPDevelop(String[] args)
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
		
		if      (mode.equals(NLPLib.MODE_POS))
			developComponent(eConfig, reader, xmls, trainFiles, devFiles, new CPOSTagger(xmls, getLowerSimplifiedForms(reader, xmls[0], trainFiles, -1)), mode, -1);
		else if (mode.equals(NLPLib.MODE_DEP))
			developComponentBoot(eConfig, reader, xmls, trainFiles, devFiles, new CDEPParser(xmls), mode, -1);
		else if (mode.equals(NLPLib.MODE_PRED))
			decode(reader, getTrainedComponent(eConfig, xmls, trainFiles, null, null, mode, 0, -1), devFiles, mode, mode);
		else if (mode.equals(NLPLib.MODE_ROLE))
			decode(reader, getTrainedComponent(eConfig, reader, xmls, trainFiles, new CRolesetClassifier(xmls), mode, -1), devFiles, mode, mode);
		else if (mode.startsWith(NLPLib.MODE_SENSE))
			decode(reader, getTrainedComponent(eConfig, reader, xmls, trainFiles, new CSenseClassifier(xmls, mode.substring(mode.lastIndexOf("_")+1)), mode, -1), devFiles, mode, mode);
		else if (mode.equals(NLPLib.MODE_SRL))
			developComponentBoot(eConfig, reader, xmls, trainFiles, devFiles, new CSRLabeler(xmls), mode, -1);
		else if (mode.equals(NLPLib.MODE_POS_BACK))
			developComponentBoot(eConfig, reader, xmls, trainFiles, devFiles, new CPOSTaggerSB(xmls, getLowerSimplifiedForms(reader, xmls[0], trainFiles, -1)), mode, -1);
		else if (mode.equals(NLPLib.MODE_DEP_BACK))
			developComponentBoot(eConfig, reader, xmls, trainFiles, devFiles, new CDEPParserSB(xmls), mode, -1);
	}
	
	protected double developComponent(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles, Object[] lexica, String mode, int devId) throws Exception
	{
		StringTrainSpace[] spaces = getStringTrainSpaces(eConfig, xmls, trainFiles, null, lexica, mode, 0, devId);
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		int i, mSize = spaces.length, nUpdate = 1;
		AbstractStatisticalComponent processor;
		
		StringModel[] models = new StringModel[mSize];
		double prevScore = -1, currScore = 0;
		Random rand = new Random(i_rand);
		int iter = 0;
		
		do
		{
			prevScore = currScore;
			
			for (i=0; i<mSize; i++)
			{
				updateModel(eTrain, spaces[i], rand, nUpdate++, i);
				models[i] = (StringModel)spaces[i].getModel();
			}

			processor = getComponent(xmls, models, lexica, mode);
			currScore = decode(reader, processor, devFiles, mode, Integer.toString(iter));
			iter++;
		}
		while (prevScore < currScore);
		
		return prevScore;
	}
	
	protected double developComponent(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles, AbstractStatisticalComponent component, String mode, int devId) throws Exception
	{
		Object[] lexica = (component != null) ? getLexica(component, reader, xmls, trainFiles, devId) : null;
		return developComponent(eConfig, reader, xmls, trainFiles, devFiles, lexica, mode, devId);
	}
	
	protected void developComponentBoot(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles, AbstractStatisticalComponent component, String mode, int devId) throws Exception
	{
		Object[] lexica = getLexica(component, reader, xmls, trainFiles, devId);
		ObjectDoublePair<StringModel[]> p;
		double prevScore, currScore = 0;
		StringModel[] models = null;
		int boot = 0;
		
		do
		{
			prevScore = currScore;
			p = developComponent(eConfig, reader, xmls, trainFiles, devFiles, lexica, models, mode, boot, devId);
			models = (StringModel[])p.o;
			currScore = p.d;
			boot++;
		}
		while (-0.01 < currScore - prevScore);
	}
	
	protected void developComponentBoot2(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles, AbstractStatisticalComponent component, String mode, int devId) throws Exception
	{
		Object[] lexica = getLexica(component, reader, xmls, trainFiles, devId);
		ObjectDoublePair<StringModel[]> p;
		double prevScore, currScore = 0;
		StringModel[] models = null;
		int boot = 0;
		
		do
		{
			prevScore = currScore;
			p = developComponent2(eConfig, reader, xmls, trainFiles, devFiles, lexica, models, mode, boot, devId);
			models = (StringModel[])p.o;
			currScore = p.d;
			boot++;
		}
		while (prevScore < currScore);
	}
	
	private ObjectDoublePair<StringModel[]> developComponent(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles, Object[] lexica, StringModel[] models, String mode, int boot, int devId) throws Exception
	{
		StringTrainSpace[] spaces = getStringTrainSpaces(eConfig, xmls, trainFiles, models, lexica, mode, boot, devId);
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		int i, mSize = spaces.length, nUpdate = 1;
		AbstractStatisticalComponent component;
		
		double prevScore = -1, currScore = 0;
		Random[] rands = new Random[mSize];
		models = new StringModel[mSize];

		double[][] prevWeights = new double[mSize][];
		double[] d;
		
		for (i=0; i<mSize; i++)
			rands[i] = new Random(i_rand);
		
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
				
				updateModel(eTrain, spaces[i], rands[i], nUpdate, i);
				models[i] = (StringModel)spaces[i].getModel();
			}
			
			component = getComponent(xmls, models, lexica, mode);
			currScore = decode(reader, component, devFiles, mode, boot+"."+nUpdate+"."+i_rand);
			nUpdate++;
		}
		while (prevScore < currScore);
		
		for (i=0; i<mSize; i++)
			models[i].setWeights(prevWeights[i]);
		
		return new ObjectDoublePair<StringModel[]>(models, prevScore);
	}
	
	private ObjectDoublePair<StringModel[]> developComponent2(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles, Object[] lexica, StringModel[] models, String mode, int boot, int devId) throws Exception
	{
		StringTrainSpace[] spaces = getStringTrainSpaces(eConfig, xmls, trainFiles, models, lexica, mode, boot, devId);
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		int nUpdate, i, j, mSize = spaces.length;
		AbstractStatisticalComponent component;
		double prevScore = -1, currScore;
		double[] prevWeights, d;
		StringModel[] tmp;
		Random rand;
		
		for (i=0; i<mSize; i++)
		{
			tmp = models;
			models = new StringModel[i+1];
			
			for (j=0; j<i; j++)
				models[j] = tmp[j];
			
			rand = new Random(i_rand);
			prevWeights = null;
			currScore = 0;
			nUpdate = 1;
			
			do
			{
				prevScore = currScore;
				
				if (models[i] != null)
				{
					d = models[i].getWeights();
					prevWeights = Arrays.copyOf(d, d.length);
				}
				
				updateModel(eTrain, spaces[i], rand, nUpdate, i);
				models[i] = (StringModel)spaces[i].getModel();

				component = getComponent(xmls, models, lexica, mode);
				currScore = decode(reader, component, devFiles, mode, Integer.toString(100*boot+nUpdate));
				nUpdate++;
			}
			while (prevScore < currScore);
			
			models[i].setWeights(prevWeights);
		}
		
		return new ObjectDoublePair<StringModel[]>(models, prevScore);
	}
	
	protected double decode(JointReader reader, AbstractStatisticalComponent component, String[] devFiles, String mode, String ext) throws Exception
	{
		int[] counts = getCounts(mode);
		PrintStream fout = null;
		DEPTree tree;
		
		for (String devFile : devFiles)
		{
			if (b_generate) fout = UTOutput.createPrintBufferedFileStream(devFile+"."+ext);
			reader.open(UTInput.createBufferedFileReader(devFile));
			
			while ((tree = reader.next()) != null)
			{
				component.process(tree);
				component.countAccuracy(counts);
				if (b_generate)	fout.println(toString(tree, mode)+"\n");
			}
			
			reader.close();
			if (b_generate)	fout.close();
		}

		return getScore(mode, counts);
	}
	
	protected int[] getCounts(String mode)
	{
		if      (mode.startsWith(NLPLib.MODE_POS) || mode.equals(NLPLib.MODE_ROLE) || mode.startsWith(NLPLib.MODE_SENSE))
			return new int[2];
		else if (mode.equals(NLPLib.MODE_DEP))
			return new int[4];
		else if (mode.equals(NLPLib.MODE_PRED) || mode.equals(NLPLib.MODE_SRL))
			return new int[3];
		else if (mode.equals(NLPLib.MODE_DEP_BACK))
			return new int[5];
		
		return null;
	}
	
	protected double getScore(String mode, int[] counts)
	{
		double score = 0;
		
		if (mode.startsWith(NLPLib.MODE_POS) || mode.equals(NLPLib.MODE_ROLE) || mode.startsWith(NLPLib.MODE_SENSE))
		{
			score = 100d * counts[1] / counts[0];
			System.out.printf("- ACC: %5.2f (%d/%d)\n", score, counts[1], counts[0]);
		}
		else if (mode.startsWith(NLPLib.MODE_DEP))
		{
			String[] labels = {"T","LAS","UAS","LS"};
			printScores(labels, counts);

			score = 100d * counts[1] / counts[0];
		}
		else if (mode.equals(NLPLib.MODE_PRED) || mode.equals(NLPLib.MODE_SRL))
		{
			double p = 100d * counts[0] / counts[1];
			double r = 100d * counts[0] / counts[2];
			score = SRLEval.getF1(p, r);
			
			System.out.printf("P: %5.2f ", p);
			System.out.printf("R: %5.2f ", r);
			System.out.printf("F1: %5.2f\n", score);
		}
		
		return score;
	}
	
	private void printScores(String[] labels, int[] counts)
	{
		int i, t = counts[0], size = counts.length;
		
		for (i=1; i<size; i++)
			System.out.printf("%3s: %5.2f (%d/%d)\n", labels[i], 100d*counts[i]/t, counts[i], t);
	}
	
	static public void main(String[] args)
	{
		new NLPDevelop(args);
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
