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

import com.googlecode.clearnlp.dependency.DEPParser;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;


/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPPredict extends AbstractRun
{
	final private String EXT = ".parsed";
	
	@Option(name="-i", usage="the input path (input; required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-o", usage="the output file (default: <input_path>.parsed)", required=false, metaVar="<filename>")
	private String s_outputFile;
	@Option(name="-c", usage="the configuration file (input; required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-m", usage="the model file (input; required)", required=true, metaVar="<filename>")
	private String s_modelFile;
	
	public DEPPredict() {}
	
	public DEPPredict(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_modelFile, s_inputPath, s_outputFile);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void run(String configXml, String modelFile, String inputPath, String outputFile) throws Exception
	{
		Element  eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		DEPReader reader = (DEPReader)getReader(eConfig);
		DEPParser parser = EngineGetter.getDEPParser(modelFile);
		
		if (new File(inputPath).isFile())
		{
			if (outputFile == null)	outputFile = inputPath + EXT;
			predict(inputPath, outputFile, reader, parser);
		}
		else
		{
			for (String filename : UTFile.getSortedFileList(inputPath))
				predict(filename, filename+EXT, reader, parser);
		}
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	static public void predict(String inputFile, String outputPath, DEPReader reader, DEPParser parser) throws Exception
	{
		long[] time = new long[10];
		int[] nTotal = new int[10];
		long st, et, dTotal = 0;
		int i, n, index;
		DEPTree tree;
		
		System.out.println("Predicting: "+inputFile);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputPath);
		
		for (n=0; true; n++)
		{
			st = System.currentTimeMillis();
			if ((tree = reader.next()) == null)	break;
			parser.parse(tree);
			et = System.currentTimeMillis();
			
			fout.println(tree.toStringDEP() + AbstractColumnReader.DELIM_SENTENCE);
			if (n%1000 == 0)	System.out.print(".");
			
			index = (tree.size() > 101) ? 9 : (tree.size()-2) / 10;
			time[index] += (et - st);
			dTotal      += (et - st);
			nTotal[index]++;
		}
		
		System.out.println();
		reader.close();
		fout.close();
		
		System.out.println("\nParsing time per sentence length");
		
		for (i=0; i<9; i++)
			System.out.printf("<= %2d: %4.2f (%d/%d)\n", (i+1)*10, (double)time[i]/nTotal[i], time[i], nTotal[i]);
		
		System.out.printf(" > %2d: %4.2f (%d/%d)\n", i*10, (double)time[9]/nTotal[9], time[9], nTotal[9]);
		System.out.printf("\nAverage parsing time: %4.2f (ms) (%d/%d)\n", (double)dTotal/n, dTotal, n);
	}
	
	static public void main(String[] args)
	{
		new DEPPredict(args);
	}
}
