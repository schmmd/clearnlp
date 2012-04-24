/**
* Copyright (c) 2011, Regents of the University of Colorado
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
package edu.colorado.clear.experiment;

import java.io.FileInputStream;
import java.util.Set;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.dependency.DEPParser;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.feature.xml.DEPFtrXml;
import edu.colorado.clear.reader.DEPReader;
import edu.colorado.clear.run.AbstractRun;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTXml;
import edu.colorado.clear.util.pair.Pair;
import edu.colorado.clear.util.pair.StringIntPair;

/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPCross extends AbstractRun
{
	protected final int    MODEL_SIZE      = 2;
	protected final String ENTRY_FEATURE   = "FEATURE";
	protected final String ENTRY_MODEL     = "MODEL";
	protected final String ENTRY_THRESHOLD = "THRESHOLD";
	
	@Option(name="-i", usage="the training file (input; required)", required=true, metaVar="<filefile>")
	private String s_trainFile;
	@Option(name="-c", usage="the configuration file (input; required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-f", usage="the feature file (input; required)", required=true, metaVar="<filename>")
	private String s_featureXml;
	
	public DEPCross() {}
	
	public DEPCross(String[] args)
	{
		initArgs(args);
		
		
		try
		{
			run(s_configXml, s_featureXml, s_trainFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	DEPTree[][] getDPTrees(Element eConfig, String trnFile)
	{
		DEPReader reader = (DEPReader)getReader(eConfig);
		DEPTree[][] trees = new DEPTree[10][];
		DEPTree tree;
		int i, j = 0;
		
		reader.open(UTInput.createBufferedFileReader(trnFile));
		
		for (i=0; (tree = reader.next()) != null; i++)
		{
			if (i%10 == 0)
			{
				j = i / 10;
				trees[j] = new DEPTree[10];
			}
			
			trees[j][i%10] = tree;
		}
		
		return trees;
	}
	
	private void run(String configXml, String featureXml, String trnFile) throws Exception
	{
		Element eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		DEPFtrXml xml = new DEPFtrXml(new FileInputStream(featureXml));
		int devId;
		Pair<StringModel,Double> model = new Pair<StringModel,Double>(null, 0d);
		int[] pCount, cCount, tCount = {0,0,0,0};
		
		for (devId=0; devId<10; devId++)
		{
			int i = 0;
			
			Set<String> sPunc = getLexica(xml, getDPTrees(eConfig, trnFile), devId);
			cCount = develop(eConfig, xml, sPunc, getDPTrees(eConfig, trnFile), devId, model, i++);
			
			do
			{
				pCount = cCount;
				cCount = develop(eConfig, xml, sPunc, getDPTrees(eConfig, trnFile), devId, model, i++);
			}
			while (cCount[1] > pCount[1]);
			
			for (int j=0; j<4; j++)
				tCount[j] += pCount[j];
		}
		
		System.out.println("Average");
		System.out.printf("LAS: %5.2f (%d/%d)\n", 100d*tCount[1]/tCount[0], tCount[1], tCount[0]);
		System.out.printf("UAS: %5.2f (%d/%d)\n", 100d*tCount[2]/tCount[0], tCount[2], tCount[0]);
		System.out.printf("LS : %5.2f (%d/%d)\n", 100d*tCount[3]/tCount[0], tCount[3], tCount[0]);
	}
	
	protected Set<String> getLexica(DEPFtrXml xml, DEPTree[][] trees, int devId)
	{
		DEPParser parser = new DEPParser(xml);
		int i, size = trees.length;
		
		System.out.print("Collecting: ");
		
		for (i=0; i<size; i++)
		{
			if (i == devId)	continue;
			
			for (DEPTree tree : trees[i])
				parser.collectLexica(tree);
			
			System.out.print(".");			
		}
		
		System.out.println();
		return parser.getPunctuationSet();
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	protected int[] develop(Element eConfig, DEPFtrXml xml, Set<String> sPunc, DEPTree[][] trees, int devId, Pair<StringModel,Double> model, int boost) throws Exception
	{
		StringTrainSpace space = new StringTrainSpace(false, xml.getLabelCutoff(0), xml.getFeatureCutoff(0));
		int[] counts = {0,0,0,0};
		StringIntPair[] gHeads;
		DEPParser parser;
		int i, size = trees.length;
		
		if (model.o1 == null)	parser = new DEPParser(xml, sPunc, space);
		else					parser = new DEPParser(xml, sPunc, model.o1, space); 
		
		System.out.println("=== Boot: "+boost);
		System.out.print("Training: ");
		
		for (i=0; i<size; i++)
		{
			if (i == devId)	continue;
			
			for (DEPTree tree : trees[i])
				parser.parse(tree);
			
			System.out.print(".");
		}
		
		System.out.println();

		model.o1 = null;
		model.o1 = (StringModel)getModel(UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN), space, 0);
		parser   = new DEPParser(xml, sPunc, model.o1);
		
		System.out.print("Predicting: ");
		for (DEPTree tree : trees[devId])
		{
			gHeads = tree.getHeads();
			parser.parse(tree);
			tree.addScoreCounts(gHeads, counts);
			if (i%1000 == 0)	System.out.print(".");
		}
		System.out.println();
		
		System.out.printf("LAS: %5.2f (%d/%d)\n", 100d*counts[1]/counts[0], counts[1], counts[0]);
		System.out.printf("UAS: %5.2f (%d/%d)\n", 100d*counts[2]/counts[0], counts[2], counts[0]);
		System.out.printf("LS : %5.2f (%d/%d)\n", 100d*counts[3]/counts[0], counts[3], counts[0]);

		model.o2 = 100d*counts[1]/counts[0];
		
		return counts;
	}
	
	static public void main(String[] args)
	{
		new DEPCross(args);
	}
}
