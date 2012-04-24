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
package edu.colorado.clear.run;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import edu.colorado.clear.dependency.DEPParser;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.feature.xml.DEPFtrXml;
import edu.colorado.clear.reader.AbstractColumnReader;
import edu.colorado.clear.reader.DEPReader;
import edu.colorado.clear.util.UTFile;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;
import edu.colorado.clear.util.UTXml;

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
		DEPParser  parser = getParser(modelFile);
		
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
	
	static public DEPParser getParser(String modelFile) throws Exception
	{
		ZipInputStream zin = new ZipInputStream(new FileInputStream(modelFile));
		DEPFtrXml xml = null;
		BufferedReader fin;
		ZipEntry zEntry;
		String name;
		
		DEPParser parser = null;
		
		while ((zEntry = zin.getNextEntry()) != null)
		{
			name = zEntry.getName();
						
			if (name.equals(ENTRY_FEATURE))
			{
				System.out.println("Loading feature template.");
				fin = new BufferedReader(new InputStreamReader(zin));
				StringBuilder build = new StringBuilder();
				String string;

				while ((string = fin.readLine()) != null)
				{
					build.append(string);
					build.append("\n");
				}
				
				xml = new DEPFtrXml(new ByteArrayInputStream(build.toString().getBytes()));
			}
			else if (name.startsWith(ENTRY_MODEL))
			{
				fin = new BufferedReader(new InputStreamReader(zin));
				parser = new DEPParser(xml, fin);
			}
		}
		
		zin.close();
		return parser;
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	static public void predict(String inputFile, String outputPath, DEPReader reader, DEPParser parser) throws Exception
	{
		double[] time = new double[10];
		int[] nTotal = new int[10];
		double st, et, dTotal = 0;
		int n, index;
		DEPTree tree;
		
		System.out.println("Predicting: "+inputFile);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputPath);
		
		for (n=0; (tree = reader.next()) != null; n++)
		{
			st = System.currentTimeMillis();
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
		
		for (int i=0; i<9; i++)
			System.out.printf("<= %2d: %4.2f (%1.0f/%d)\n", (i+1)*10, time[i]/nTotal[i], time[i], nTotal[i]);
		
		System.out.printf(" > %2d: %4.2f (%1.0f/%d)\n", 90, time[9]/nTotal[9], time[9], nTotal[9]);
		System.out.printf("\nAverage parsing time: %4.2f (ms) (%1.0f/%d)\n", dTotal/n, dTotal, n);
	}
	
	static public void main(String[] args)
	{
		new DEPPredict(args);
	}
}
