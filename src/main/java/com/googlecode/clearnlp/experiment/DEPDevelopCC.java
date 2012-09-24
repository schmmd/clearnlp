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
package com.googlecode.clearnlp.experiment;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPParserCC;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.DEPFtrXml;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.run.DEPTrainCC;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.IntIntPair;
import com.googlecode.clearnlp.util.pair.Pair;
import com.googlecode.clearnlp.util.pair.StringIntPair;


/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPDevelopCC extends DEPTrainCC
{
	@Option(name="-d", usage="the directory containing development file (input; required)", required=true, metaVar="<filename>")
	private String s_devDir;
	
	public DEPDevelopCC() {}
	
	public DEPDevelopCC(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configFile, s_featureFiles, s_trainDir, s_devDir);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void run(String configFile, String featureFiles, String trainDir, String devDir) throws Exception
	{
		Element      eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		DEPReader     reader = (DEPReader)getReader(eConfig);
		DEPFtrXml[]     xmls = getFeatureTemplates(featureFiles);	// initialize MODEL_SIZE
		String[]  trainFiles = UTFile.getSortedFileList(trainDir);
		String[]    devFiles = UTFile.getSortedFileList(devDir); 
		Set<String>    sPunc = getLexica(eConfig, trainFiles, -1);
		IntIntPair[] cutoffs = getCutoffs(xmls);
		
		Pair<StringModel[],Double> model = new Pair<StringModel[],Double>(null, 0d);
		double prevScore;
		int i = 0;
			
		develop(eConfig, reader, xmls, sPunc, trainFiles, devFiles, cutoffs, model, i++);
		
		do
		{
			prevScore = model.o2;
			develop(eConfig, reader, xmls, sPunc, trainFiles, devFiles, cutoffs, model, i++);
		}
		while (model.o2 > prevScore);
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	protected void develop(Element eConfig, DEPReader reader, DEPFtrXml[] xmls, Set<String> sPunc, String[] trainFiles, String[] devFiles, IntIntPair[] cutoffs, Pair<StringModel[],Double> model, int boost) throws Exception
	{
		long st = System.currentTimeMillis();
		int[] lCounts = {0,0,0,0}, gCounts = {0,0,0,0}, counts;
		StringIntPair[] gHeads;
		DEPParserCC parser;
		DEPTree tree;
		int i;
		
		parser = getTrainedParser(eConfig, xmls, sPunc, trainFiles, cutoffs, model.o1, -1);
		model.o1 = parser.getModels();
		
		for (String devFile : devFiles)
		{
			reader.open(UTInput.createBufferedFileReader(devFile));
			Arrays.fill(lCounts, 0);
			
			System.out.println("Predicting: "+devFile);
			for (i=0; (tree = reader.next()) != null; i++)
			{
				gHeads = tree.getHeads();
				parser.parse(tree);
				counts = DEPLib.getScores(tree, gHeads);
				UTArray.add(lCounts, counts);
				if (i%1000 == 0)	System.out.print(".");
			}
			System.out.println();
			reader.close();
			
			System.out.printf("LAS: %5.2f (%d/%d)\n", 100d*lCounts[1]/lCounts[0], lCounts[1], lCounts[0]);
			System.out.printf("UAS: %5.2f (%d/%d)\n", 100d*lCounts[2]/lCounts[0], lCounts[2], lCounts[0]);
			System.out.printf("LS : %5.2f (%d/%d)\n", 100d*lCounts[3]/lCounts[0], lCounts[3], lCounts[0]);
			
			for (i=0; i<lCounts.length; i++)
				gCounts[i] += lCounts[i];
		}
		
		System.out.printf("1st model: %4.2f (%d/%d)\n", 100d*parser.n_1st/parser.n_total, parser.n_1st, parser.n_total);
		
		System.out.println("Total");
		System.out.printf("LAS: %5.2f (%d/%d)\n", 100d*gCounts[1]/gCounts[0], gCounts[1], gCounts[0]);
		System.out.printf("UAS: %5.2f (%d/%d)\n", 100d*gCounts[2]/gCounts[0], gCounts[2], gCounts[0]);
		System.out.printf("LS : %5.2f (%d/%d)\n", 100d*gCounts[3]/gCounts[0], gCounts[3], gCounts[0]);
		
		model.o2 = 100d*gCounts[1]/gCounts[0];
		long millis = System.currentTimeMillis() - st;
		String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(millis), TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
	}
	
	static public void main(String[] args)
	{
		new DEPDevelopCC(args);
	}
}
