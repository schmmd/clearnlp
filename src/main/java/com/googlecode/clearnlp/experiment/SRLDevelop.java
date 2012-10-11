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
import java.util.Set;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLEval;
import com.googlecode.clearnlp.dependency.srl.SRLParser;
import com.googlecode.clearnlp.feature.xml.SRLFtrXml;
import com.googlecode.clearnlp.reader.SRLReader;
import com.googlecode.clearnlp.run.SRLTrain;
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
public class SRLDevelop extends SRLTrain
{
	@Option(name="-d", usage="the directory containing development file (input; required)", required=true, metaVar="<filename>")
	private String s_devDir;
	
	public SRLDevelop() {}
	
	public SRLDevelop(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_featureXml, s_trainDir, s_devDir);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void run(String configXml, String featureXml, String trainDir, String devDir) throws Exception
	{
		Element   eConfig    = UTXml.getDocumentElement(new FileInputStream(configXml));
		SRLReader reader     = (SRLReader)getReader(eConfig);
		SRLFtrXml xml        = new SRLFtrXml(new FileInputStream(featureXml));
		String[]  trainFiles = UTFile.getSortedFileList(trainDir);
		String[]  devFiles   = UTFile.getSortedFileList(devDir);
		
		Pair<Set<String>,Set<String>> p = getDownUpSets(reader, xml, trainFiles, -1);
		int i;
		
		Pair<StringModel[],Double> model = new Pair<StringModel[],Double>(null, 0d);
		double prevScore;
		
		i = 0;
		develop(eConfig, reader, xml, trainFiles, devFiles, model, p.o1, p.o2, i++);
		
		do
		{
			prevScore = model.o2;
			develop(eConfig, reader, xml, trainFiles, devFiles, model, p.o1, p.o2, i++);
		}
		while (model.o2 > prevScore);
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	protected void develop(Element eConfig, SRLReader reader, SRLFtrXml xml, String[] trainFiles, String[] devFiles, Pair<StringModel[],Double> model, Set<String> sDown, Set<String> sUp, int boost) throws Exception
	{
		SRLEval lEval, gEval = new SRLEval();
		StringIntPair[][] gHeads, sHeads;
		SRLParser parser;
		DEPTree tree;
		int i, n, size, N = 10;
		int[][] spaces = new int[2][N];
		IntIntPair p;
	//	IntIntPair lArgs, gArgs = new IntIntPair(0, 0);
		
		parser = getTrainedParser(eConfig, reader, xml, trainFiles, model.o1, sDown, sUp, -1);
		model.o1 = parser.getModels();
	//	PrintStream fout;
		
		for (String devFile : devFiles)
		{
			reader.open(UTInput.createBufferedFileReader(devFile));
	//		fout  = UTOutput.createPrintBufferedFileStream(devFile+".labeled."+boost);
			lEval = new SRLEval();
	//		lArgs = new IntIntPair(0, 0);
			
			System.out.println("Predicting: "+devFile);

			for (i=0; (tree = reader.next()) != null; i++)
			{
				gHeads = tree.getSHeads();
				parser.label(tree);
				sHeads = tree.getSHeads();
				
				lEval.evaluate(gHeads, sHeads);
				gEval.evaluate(gHeads, sHeads);
				if (i%1000 == 0)	System.out.print(".");
				
				size = tree.size();
				n = (size - 2) / N;
				if (n > N - 1)	n = N - 1;
				
				p = parser.getNumTransitions();
				spaces[0][n] += p.i1;
				spaces[1][n] += p.i2;
				
	//			p = parser.getArgCoverage(gHeads);
	//			lArgs.i1 += p.i1;
	//			lArgs.i2 += p.i2;
				
	//			fout.println(tree.toStringSRL()+"\n");
			}
			
			System.out.println();
			reader.close();
	//		fout.close();

	//		gArgs.i1 += lArgs.i1;
	//		gArgs.i2 += lArgs.i2;
			
	//		System.out.printf("Coverage: %5d %5d = %f\n", lArgs.i1, lArgs.i2, 100d*lArgs.i1/lArgs.i2);
			lEval.printOverall();
		}
		
		System.out.println("Average");
		gEval.printOverall();
		model.o2 = gEval.getF1(SRLEval.LAS);
		
//		System.out.printf("Coverage: %5d %5d = %f\n", gArgs.i1, gArgs.i2, 100d*gArgs.i1/gArgs.i2);
		
		for (i=0; i<N; i++)
			System.out.printf("%3d: %5d %5d\n", i, spaces[0][i], spaces[1][i]);
	}
	
	static public void main(String[] args)
	{
		new SRLDevelop(args);
	}
}