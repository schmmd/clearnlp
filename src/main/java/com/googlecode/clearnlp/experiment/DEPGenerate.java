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
import java.io.PrintStream;
import java.util.Set;

import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.dependency.DEPParser;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.DEPFtrXml;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.run.DEPTrain;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.Pair;
import com.googlecode.clearnlp.util.pair.StringIntPair;


/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPGenerate extends DEPTrain
{
	public DEPGenerate() {}
	
	public DEPGenerate(String[] args)
	{
		initArgs(args);
		
		String[] trnFiles = UTFile.getSortedFileList(s_trainDir);
		int i, size = trnFiles.length;
		
		try
		{
			for (i=0; i<size; i++)
			{
				System.out.printf("===== Cross validation: %d =====\n", i);
				run(s_configXml, s_featureXml, trnFiles, i);
			}
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void run(String configXml, String featureXml, String[] trainFiles, int devId) throws Exception
	{
		Element   eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		DEPReader  reader = (DEPReader)getReader(eConfig);
		DEPFtrXml     xml = new DEPFtrXml(new FileInputStream(featureXml));
		
		Pair<StringModel,Double> model = new Pair<StringModel,Double>(null, 0d);
		double prevScore;	int i = 0;
		
		Set<String> sPunc = getLexica(reader, xml, trainFiles, devId);
		develop(eConfig, reader, xml, sPunc, trainFiles, devId, model, i++);
		
		do
		{
			prevScore = model.o2;
			develop(eConfig, reader, xml, sPunc, trainFiles, devId, model, i++);
		}
		while (model.o2 > prevScore);
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	protected void develop(Element eConfig, DEPReader reader, DEPFtrXml xml, Set<String> sPunc, String[] trainFiles, int devId, Pair<StringModel,Double> model, int boost) throws Exception
	{
		int[] counts = {0,0,0,0};
		StringIntPair[] gHeads;
		DEPParser parser;
		DEPTree tree;
		int i;
		
		parser = getTrainedParser(eConfig, reader, xml, sPunc, trainFiles, model.o1, devId);
		model.o1 = parser.getModel();
		
		reader.open(UTInput.createBufferedFileReader(trainFiles[devId]));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(trainFiles[devId]+".parse."+boost);
		
		System.out.print("Predicting: ");
		
		for (i=0; (tree = reader.next()) != null; i++)
		{
			gHeads = tree.getHeads();
			parser.parse(tree);
			fout.println(tree.toStringDEP()+"\n");
			tree.addScoreCounts(gHeads, counts);
			if (i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		reader.close();
		fout.close();
		
		System.out.printf("LAS: %5.2f (%d/%d)\n", 100d*counts[1]/counts[0], counts[1], counts[0]);
		System.out.printf("UAS: %5.2f (%d/%d)\n", 100d*counts[2]/counts[0], counts[2], counts[0]);
		System.out.printf("LS : %5.2f (%d/%d)\n", 100d*counts[3]/counts[0], counts[3], counts[0]);

		model.o2 = 100d*counts[1]/counts[0];
	}
	
	static public void main(String[] args)
	{
		new DEPGenerate(args);
	}
}
