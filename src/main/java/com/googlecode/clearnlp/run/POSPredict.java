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

import com.googlecode.clearnlp.feature.xml.POSFtrXml;
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
	
	@Option(name="-i", usage="the input path (input; required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-o", usage="the output file (default: <input_path>.tagged)", required=false, metaVar="<filename>")
	private String s_outputFile = null;
	@Option(name="-c", usage="the configuration file (input; required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-m", usage="the model file (input; required)", required=true, metaVar="<filename>")
	private String s_modelFile;
	@Option(name="-t", usage="the similarity threshold (default: -1)", required=false, metaVar="<double>")
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
		
		Pair<POSTagger[],Double> p = getTaggers(modelFile, threshold);
		
		if (new File(inputPath).isFile())
		{
			if (outputFile == null)	outputFile = inputPath + EXT;
			predict(inputPath, outputFile, reader, p.o1, p.o2);	
		}
		else
		{
			for (String filename : UTFile.getSortedFileList(inputPath))
				predict(filename, filename+EXT, reader, p.o1, p.o2);
		}		
	}
	
	static public Pair<POSTagger[],Double> getTaggers(String modelFile, double threshold) throws Exception
	{
		ZipInputStream zin = new ZipInputStream(new FileInputStream(modelFile));
		POSFtrXml xml = null;
		BufferedReader fin;
		ZipEntry zEntry;
		String name;
		int modId;
		
		POSTagger[] taggers = new POSTagger[POSTrain.MODEL_SIZE];
		
		while ((zEntry = zin.getNextEntry()) != null)
		{
			name = zEntry.getName();
						
			if (threshold < 0 && name.equals(POSTrain.ENTRY_THRESHOLD))
			{
				fin = new BufferedReader(new InputStreamReader(zin));
				threshold = Double.parseDouble(fin.readLine());
				System.out.println("Threshold: "+threshold);
			}
			else if (name.equals(ENTRY_FEATURE))
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
				
				xml = new POSFtrXml(new ByteArrayInputStream(build.toString().getBytes()));
			}
			else if (name.startsWith(ENTRY_MODEL))
			{
				fin = new BufferedReader(new InputStreamReader(zin));
				modId = Integer.parseInt(name.substring(ENTRY_MODEL.length()));
				taggers[modId] = new POSTagger(xml, fin);
			}
		}
		
		zin.close();
		
		return new Pair<POSTagger[],Double>(taggers, threshold);
	}
	
	static public void predict(String inputFile, String outputFile, POSReader reader, POSTagger[] taggers, double threshold) 
	{
		double sum = 0;
		POSNode[] nodes;
		int i, n = 0;
		int counts[] = {0, 0};
		
		System.out.println("Predicting: "+inputFile);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		
		for (i=0; (nodes = reader.next()) != null; i++)
		{
			sum += predict(nodes, taggers, threshold, counts);
			n   += nodes.length;

			if (i%1000 == 0)	System.out.print(".");
			fout.println(UTArray.join(nodes, AbstractColumnReader.DELIM_SENTENCE) + AbstractColumnReader.DELIM_SENTENCE);
		}
		
		System.out.println();
		reader.close();
		fout.close();
		
		System.out.printf("Overall tagging time  : %f (sec. for %d tokens)\n", sum/1000, n);
		System.out.printf("Average tagging time  : %f (ms/token)\n", sum/n);
		System.out.printf("Generalized model used: %f (%d/%d)\n", (double)counts[1]/i, counts[1], i);
	}
	
	static double predict(POSNode[] nodes, POSTagger[] taggers, double threshold, int[] counts)
	{
		double st, et;
		
		st = System.currentTimeMillis();
		POSLib.normalizeForms(nodes);
		
		if (threshold < taggers[0].getCosineSimilarity(nodes))
		{
			taggers[0].tag(nodes);
			counts[0]++;
		}
		else
		{
			taggers[1].tag(nodes);
			counts[1]++;
		}

		et = System.currentTimeMillis();
		return et - st;
	}
		
	static public void main(String[] args)
	{
		new POSPredict(args);
	}
}
