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
package com.googlecode.clearnlp.run;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPParser;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineSetter;
import com.googlecode.clearnlp.feature.xml.DEPFtrXml;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.map.Prob1DMap;

/**
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPTrain extends AbstractRun
{
	protected final String LEXICON_PUNCTUATION = "punctuation"; 
	
	@Option(name="-i", usage="input directory containing training files (required)", required=true, metaVar="<directory>")
	protected String s_trainDir;
	@Option(name="-c", usage="configuration file (required)", required=true, metaVar="<filename>")
	protected String s_configXml;
	@Option(name="-f", usage="feature template file (required)", required=true, metaVar="<filename>")
	protected String s_featureXml;
	@Option(name="-m", usage="model file (output; required)", required=true, metaVar="<filename>")
	protected String s_modelFile;
	@Option(name="-n", usage="bootstrapping level (default: 2)", required=false, metaVar="<integer>")
	protected int n_boot = 2;
	@Option(name="-sb", usage="if set, save all intermediate bootstrapping models", required=false, metaVar="<boolean>")
	protected boolean b_saveAllModels = false;
	
	public DEPTrain() {}
	
	public DEPTrain(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_featureXml, s_trainDir, s_modelFile, n_boot);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void run(String configXml, String featureXml, String trainDir, String modelFile, int nBoot) throws Exception
	{
		Element eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		DEPFtrXml xml = new DEPFtrXml(new FileInputStream(featureXml));
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, ".*", true);
		Set<String> sPunc = getLexica(eConfig, xml, trainFiles, -1);
		DEPParser parser;
		int boot = 0;
		
		parser = getTrainedParser(eConfig, xml, sPunc, trainFiles, null, -1, boot);
		if (b_saveAllModels)	EngineSetter.setDEPParser(modelFile+"."+boot, featureXml, parser);
		
		for (boot=1; boot<=nBoot; boot++)
		{
			parser = getTrainedParser(eConfig, xml, sPunc, trainFiles, parser.getModel(), -1, boot);
			if (b_saveAllModels)	EngineSetter.setDEPParser(modelFile+"."+boot, featureXml, parser);
		}
		
		if (!b_saveAllModels)	EngineSetter.setDEPParser(modelFile, featureXml, parser);
	}
	
	protected Set<String> getLexica(Element eConfig, DEPFtrXml xml, String[] trainFiles, int devId) throws Exception
	{
		DEPReader reader = (DEPReader)getReader(eConfig).o1;
		Prob1DMap mPunct = new Prob1DMap();
		int i, size = trainFiles.length;
		DEPTree tree;
		
		System.out.println("Collecting lexica:");
		
		for (i=0; i<size; i++)
		{
			if (i == devId)	continue;
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			
			while ((tree = reader.next()) != null)
				collectLexica(tree, mPunct, xml.getPunctuationLabel());
			
			System.out.print(".");
			reader.close();
		}
		
		System.out.println();
		return mPunct.toSet(xml.getPunctuationCutoff());
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
	
	/** @param devId if {@code -1}, train the models using all training files. */
	public DEPParser getTrainedParser(Element eConfig, DEPFtrXml xml, Set<String> sPunc, String[] trainFiles, StringModel model, int devId, int boot) throws Exception
	{
		int i, size = trainFiles.length, labelCutoff = xml.getLabelCutoff(0), featureCutoff = xml.getFeatureCutoff(0);
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN);
		int numThreads = getNumOfThreads(eTrain);
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		List<StringTrainSpace> spaces = new ArrayList<StringTrainSpace>();
		StringTrainSpace space;
		
		System.out.println("Collecting training instances:");
		
		for (i=0; i<size; i++)
		{
			if (devId != i)
			{
				spaces.add(space = new StringTrainSpace(false, labelCutoff, featureCutoff));
				executor.execute(new TrainTask(eConfig, xml, sPunc, trainFiles[i], model, space));
			}
		}
		
		executor.shutdown();
		
		try
		{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e) {e.printStackTrace();}
		
		System.out.println();
		space = spaces.get(0);
		
		if ((size = spaces.size()) > 1)
		{
			System.out.println("Merging training instances:");
			
			for (i=1; i<size; i++)
			{
				space.appendSpace(spaces.get(i));
				spaces.get(i).clear();
				System.out.print(".");
			}
			
			System.out.println();			
		}
		
		model = null;
		model = (StringModel)getModel(eTrain, space, 0);
		
		return new DEPParser(xml, sPunc, model);
	}

	private class TrainTask implements Runnable
	{
		DEPParser d_parser;
		DEPReader d_reader;
		
		public TrainTask(Element eConfig, DEPFtrXml xml, Set<String> sPunc, String trainFile, StringModel model, StringTrainSpace space)
		{
			d_parser = (model == null) ? new DEPParser(xml, sPunc, space) : new DEPParser(xml, sPunc, model, space);
			d_reader = (DEPReader)getReader(eConfig).o1;
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
	
	protected void printScores(int[] counts)
	{
		System.out.printf("- LAS: %5.2f (%d/%d)\n", 100d*counts[1]/counts[0], counts[1], counts[0]);
		System.out.printf("- UAS: %5.2f (%d/%d)\n", 100d*counts[2]/counts[0], counts[2], counts[0]);
		System.out.printf("- LS : %5.2f (%d/%d)\n", 100d*counts[3]/counts[0], counts[3], counts[0]);
	}

	static public void main(String[] args)
	{
		new DEPTrain(args);
	}
}
