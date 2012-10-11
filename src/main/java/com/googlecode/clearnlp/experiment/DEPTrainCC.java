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
package com.googlecode.clearnlp.experiment;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPParserCC;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineSetter;
import com.googlecode.clearnlp.feature.xml.DEPFtrXml;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.run.AbstractRun;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.map.Prob1DMap;
import com.googlecode.clearnlp.util.pair.IntIntPair;
import com.googlecode.clearnlp.util.pair.StringIntPair;

/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPTrainCC extends AbstractRun
{
	protected final String DELIM_FEATURE_FILES = ":";
	protected final String LEXICON_PUNCTUATION = "punctuation"; 
	
	@Option(name="-i", usage="the directory containg training files (input; required)", required=true, metaVar="<directory>")
	protected String s_trainDir;
	@Option(name="-c", usage="the configuration file (input; required)", required=true, metaVar="<filename>")
	protected String s_configFile;
	@Option(name="-f", usage="the feature files (input; required)", required=true, metaVar="<filename>")
	protected String s_featureFiles;
	@Option(name="-m", usage="the model file (output; required)", required=true, metaVar="<filename>")
	protected String s_modelFile;
	@Option(name="-n", usage="the bootstrapping level (default: 2)", required=false, metaVar="<integer>")
	protected int n_boot = 2;
	@Option(name="-s", usage="if set, save all bootstrapping models", required=false, metaVar="<boolean>")
	protected boolean b_saveAllModels = false;
	@Option(name="-lb", usage="the lower bound (default: 0)", required=false, metaVar="<double>")
	private double d_lower = 0;
	@Option(name="-pb", usage="the precision bias (default: 1)", required=false, metaVar="<double>")
	private double d_pBias = 1;
	
	private int MODEL_SIZE = 0;
	
	public DEPTrainCC() {}
	
	public DEPTrainCC(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configFile, s_featureFiles, s_trainDir, s_modelFile, n_boot);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void run(String configFile, String featureFiles, String trainDir, String modelFile, int nBoot) throws Exception
	{
		Element      eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		DEPFtrXml[]     xmls = getFeatureTemplates(featureFiles);	// initialize MODEL_SIZE
		String[]  trainFiles = UTFile.getSortedFileList(trainDir);
		Set<String>    sPunc = getLexica(eConfig, trainFiles, -1);
		IntIntPair[] cutoffs = getCutoffs(xmls);
		DEPParserCC parser;
		int i = 0;
		
		parser = getTrainedParser(eConfig, xmls, sPunc, trainFiles, cutoffs, null, -1);
		if (b_saveAllModels)	EngineSetter.saveModel(modelFile+"."+i, featureFiles, parser);
		
		for (i=1; i<=nBoot; i++)
		{
			parser = getTrainedParser(eConfig, xmls, sPunc, trainFiles, cutoffs, parser.getModels(), -1);
			if (b_saveAllModels)	EngineSetter.saveModel(modelFile+"."+i, featureFiles, parser);
		}
		
		EngineSetter.saveModel(modelFile, featureFiles, parser);
	}
	
	protected DEPFtrXml[] getFeatureTemplates(String featureFiles) throws Exception
	{
		String[] filenames = featureFiles.split(DELIM_FEATURE_FILES);
		MODEL_SIZE = filenames.length;
		
		DEPFtrXml[] xmls = new DEPFtrXml[MODEL_SIZE];
		int i;
		
		for (i=0; i<MODEL_SIZE; i++)
			xmls[i] = new DEPFtrXml(new FileInputStream(filenames[i]));
		
		return xmls;
	}
	
	protected Set<String> getLexica(Element eConfig, String[] trainFiles, int devId) throws Exception
	{
		DEPReader reader = (DEPReader)getReader(eConfig);
		StringIntPair punctInfo = getPunctInfo(eConfig);
		Prob1DMap mPunct = new Prob1DMap();
		int i, size = trainFiles.length;
		DEPTree tree;
		
		System.out.println("Collecting lexica:");
		
		for (i=0; i<size; i++)
		{
			if (i == devId)	continue;
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			
			while ((tree = reader.next()) != null)
				collectLexica(tree, mPunct, punctInfo.s);
			
			System.out.print(".");
			reader.close();
		}
		
		System.out.println();
		return mPunct.toSet(punctInfo.i);
	}
	
	private StringIntPair getPunctInfo(Element eConfig)
	{
		Element eLexica = UTXml.getFirstElementByTagName(eConfig, TAG_LEXICA);
		NodeList list = eLexica.getElementsByTagName(TAG_LEXICA_LEXICON);
		int i, size = list.getLength(), cutoff;
		Element eLexicon;
		String label;
		
		for (i=0; i<size; i++)
		{
			eLexicon = (Element)list.item(i);
			
			if (UTXml.getTrimmedAttribute(eLexicon, TAG_LEXICA_LEXICON_TYPE).equals(LEXICON_PUNCTUATION))
			{
				label  = UTXml.getTrimmedAttribute(eLexicon, TAG_LEXICA_LEXICON_LABEL);
				cutoff = Integer.parseInt(UTXml.getTrimmedAttribute(eLexicon, TAG_LEXICA_LEXICON_CUTOFF));
				return new StringIntPair(label, cutoff);
			}
		}
		
		return new StringIntPair("", 0);
	}
	
	/**
	 * Retrieves lexica from the specific dependency tree and stores them to the specific map.
	 * @param tree the dependency tree to collect lexica from.
	 * @param mPunct the map to store lexica to.
	 * @param punctLabel punctuation dependency label.
	 */
	private void collectLexica(DEPTree tree, Prob1DMap mPunct, String punctLabel)
	{
		int i, size = tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			if (node.isLabel(punctLabel))
				mPunct.add(node.form);
		}
	}
	
	protected IntIntPair[] getCutoffs(DEPFtrXml[] xmls)
	{
		IntIntPair[] cutoffs = new IntIntPair[MODEL_SIZE];
		int i;
		
		for (i=0; i<MODEL_SIZE; i++)
			cutoffs[i] = new IntIntPair(xmls[i].getLabelCutoff(0), xmls[i].getFeatureCutoff(0));
		
		return cutoffs;
	}

	/** @param devId if {@code -1}, train the models using all training files. */
	public DEPParserCC getTrainedParser(Element eConfig, DEPFtrXml[] xmls, Set<String> sPunc, String[] trainFiles, IntIntPair[] cutoffs, StringModel[] models, int devId) throws Exception
	{
		List<StringTrainSpace[]> lSpaces = new ArrayList<StringTrainSpace[]>();
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN);
		int numThreads = getNumOfThreads(eTrain);
		int i, size = trainFiles.length;
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		StringTrainSpace[] spaces;
		
		System.out.println("Collecting training instances:");
		
		for (i=0; i<size; i++)
		{
			if (devId == i)	continue;
			
			lSpaces.add(spaces = getTrainSpaces(cutoffs));
			executor.execute(new TrainTask(eConfig, xmls, sPunc, trainFiles[i], models, spaces));
		}
		
		executor.shutdown();
		
		try
		{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e) {e.printStackTrace();}
		
		System.out.println();

		models = null;
		models = getModels(eTrain, lSpaces, cutoffs);
		
		return new DEPParserCC(xmls, sPunc, models, d_lower);
	}
	
	private StringTrainSpace[] getTrainSpaces(IntIntPair[] cutoffs)
	{
		StringTrainSpace[] spaces = new StringTrainSpace[MODEL_SIZE];
		int i;
		
		for (i=0; i<MODEL_SIZE; i++)
			spaces[i] = new StringTrainSpace(false, cutoffs[i].i1, cutoffs[i].i2);
		
		return spaces;
	}
	
	private class TrainTask implements Runnable
	{
		DEPParserCC d_parser;
		DEPReader d_reader;
		
		public TrainTask(Element eConfig, DEPFtrXml[] xmls, Set<String> sPunc, String trainFile, StringModel[] models, StringTrainSpace[] spaces)
		{
			d_parser = (models == null) ? new DEPParserCC(xmls, sPunc, spaces) : new DEPParserCC(xmls, sPunc, models, spaces, d_lower);
			d_reader = (DEPReader)getReader(eConfig);
			d_reader.open(UTInput.createBufferedFileReader(trainFile));
		}
		
		public void run()
		{
			DEPTree tree;
			
			while ((tree = d_reader.next()) != null)
				d_parser.parse(tree);
			
			d_reader.close();
			System.out.print(".");
		}
    }
	
	private StringModel[] getModels(Element eTrain, List<StringTrainSpace[]> lSpaces, IntIntPair[] cutoffs)
	{
		StringModel[] models = new StringModel[MODEL_SIZE];
		int i;
		
		for (i=0; i<MODEL_SIZE; i++)
			models[i] = (StringModel)getModel(eTrain, getTrainSpace(lSpaces, cutoffs, i), 0);
		
		return models;
	}
	
	private StringTrainSpace getTrainSpace(List<StringTrainSpace[]> lSpaces, IntIntPair[] cutoffs, int index)
	{
		StringTrainSpace gSpace = new StringTrainSpace(false, cutoffs[index].i1, cutoffs[index].i2), lSpace;
		
		for (StringTrainSpace[] s : lSpaces)
		{
			lSpace = s[index];
			gSpace.appendSpace(lSpace);
			lSpace.clear();
		}
		
		return gSpace;
	}
	
	protected void printScores(int[] counts)
	{
		System.out.printf("- LAS: %5.2f (%d/%d)\n", 100d*counts[1]/counts[0], counts[1], counts[0]);
		System.out.printf("- UAS: %5.2f (%d/%d)\n", 100d*counts[2]/counts[0], counts[2], counts[0]);
		System.out.printf("- LS : %5.2f (%d/%d)\n", 100d*counts[3]/counts[0], counts[3], counts[0]);
	}
	
	static public void main(String[] args)
	{
		new DEPTrainCC(args);
	}
}
