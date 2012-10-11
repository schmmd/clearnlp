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

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.pos.POSLib;
import com.googlecode.clearnlp.pos.POSNode;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.reader.POSReader;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.Pair;


/**
 * Predicts a part-of-speech tagging model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class POSPredict extends AbstractRun
{
	final private String EXT = ".tagged";
	
	static final private int COUNT_WC = 0;		// word count
	static final private int COUNT_SC = 1;		// sentence count
	static final private int COUNT_GC = 2;		// count how many times a generalized model is used
	static final private int COUNT_TC = 3;		// total time in milliseconds
	
	@Option(name="-i", usage="the input path (input; required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-o", usage="the output file (default: <input_path>.tagged)", required=false, metaVar="<filename>")
	private String s_outputFile = null;
	@Option(name="-c", usage="the configuration file (input; required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-m", usage="the model file (input; required)", required=true, metaVar="<filename>")
	private String s_modelFile;
	@Option(name="-t", usage="the similarity threshold (default: read from the model file)", required=false, metaVar="<double>")
	protected double d_threshold = -1;
	
	public POSPredict() {}
	
	public POSPredict(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_modelFile, d_threshold, s_inputPath, s_outputFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void run(String configXml, String modelFile, double threshold, String inputPath, String outputFile) throws Exception
	{
		Element  eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		POSReader reader = (POSReader)getReader(eConfig);
		
		Pair<POSTagger[],Double> p = EngineGetter.getPOSTaggers(modelFile, threshold);
		long counts[] = {0, 0, 0, 0};
		
		if (new File(inputPath).isFile())
		{
			if (outputFile == null)	outputFile = inputPath + EXT;
			predict(inputPath, outputFile, reader, p.o1, p.o2, counts);	
		}
		else
		{
			for (String filename : UTFile.getSortedFileList(inputPath))
				predict(filename, filename+EXT, reader, p.o1, p.o2, counts);
		}
	
		printScores(counts, p.o1.length);
	}
	
	private void printScores(long[] counts, int modelSize)
	{
		long wc = counts[COUNT_WC], sc = counts[COUNT_SC], gc = counts[COUNT_GC];
		double time = counts[COUNT_TC];
		
		System.out.println("Average tagging speed");
		System.out.printf(": %f (milliseconds/token)\n", time/wc);
		System.out.printf(": %f (milliseconds/sentence)\n", time/sc);
		
		if (modelSize > 1)
		{
			System.out.println("Generalized model used");
			System.out.printf(": %f (%d/%d)\n", (double)gc/sc, gc, sc);			
		}
	}
	
	static public void predict(String inputFile, String outputFile, POSReader reader, POSTagger[] taggers, double threshold, long[] counts) 
	{
		System.out.println("Predicting: "+inputFile);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);

		POSNode[] nodes;
		long sc;
		
		for (sc=0; (nodes = reader.next()) != null; sc++)
		{
			predict(nodes, taggers, threshold, counts);
			counts[COUNT_WC] += nodes.length;

			if (sc%1000 == 0)	System.out.print(".");
			fout.println(UTArray.join(nodes, AbstractColumnReader.DELIM_SENTENCE) + AbstractColumnReader.DELIM_SENTENCE);
		}
		
		System.out.println();
		counts[COUNT_SC] += sc;
		reader.close();
		fout.close();
	}
	
	static void predict(POSNode[] nodes, POSTagger[] taggers, double threshold, long[] counts)
	{
		long st, et;
		
		st = System.currentTimeMillis();
		POSLib.normalizeForms(nodes);

		if (taggers.length == 1 || threshold < taggers[0].getCosineSimilarity(nodes))
		{
			taggers[0].tag(nodes);
		}
		else
		{
			taggers[1].tag(nodes);
			counts[COUNT_GC]++;
		}

		et = System.currentTimeMillis();
		counts[COUNT_TC] += et - st;
	}
		
	static public void main(String[] args)
	{
		new POSPredict(args);
	}
}
