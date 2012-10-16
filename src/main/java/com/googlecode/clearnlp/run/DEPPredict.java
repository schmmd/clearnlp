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
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.dependency.DEPParser;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.pos.POSNode;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.reader.LineReader;
import com.googlecode.clearnlp.reader.POSReader;
import com.googlecode.clearnlp.reader.RawReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPPredict extends AbstractRun
{
	@Option(name="-i", usage="input path (required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-ie", usage="input file extension (default: .*)", required=false, metaVar="<regex>")
	private String s_inputExt = ".*";
	@Option(name="-oe", usage="output file extension (default: pos)", required=false, metaVar="<string>")
	private String s_outputExt = "parsed";
	@Option(name="-c", usage="configuration file (input; required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-m", usage="model file (input; required)", required=true, metaVar="<filename>")
	private String s_modelFile;
	
	public DEPPredict() {}
	
	public DEPPredict(String[] args)
	{
		initArgs(args);
		
		try
		{
			Element eConfig = UTXml.getDocumentElement(new FileInputStream(s_configXml));
			
			List<String[]> filenames = getFilenames(s_inputPath, s_inputExt, s_outputExt);
			Pair<AbstractReader<?>, String> reader = getReader(eConfig);
			
			AbstractSegmenter segmenter = null;
			AbstractTokenizer tokenizer = null;
			Pair<POSTagger[],Double> taggers = null;
			AbstractMPAnalyzer analyzer = null;
			
			if (reader.o2.equals(AbstractReader.TYPE_RAW ))
			{
				segmenter = getSegmenter(eConfig);
				taggers   = getPOSTaggers(eConfig);
				analyzer  = getMPAnalyzer(eConfig);
			}
			else if (reader.o2.equals(AbstractReader.TYPE_LINE))
			{
				tokenizer = getTokenizer(eConfig);
				taggers   = getPOSTaggers(eConfig);
				analyzer  = getMPAnalyzer(eConfig);
			}
			else if (reader.o2.equals(AbstractReader.TYPE_POS))
			{
				taggers  = getPOSTaggers(eConfig);
				analyzer = getMPAnalyzer(eConfig);
			}
				
			DEPParser parser = EngineGetter.getDEPParser(s_modelFile);
			
			for (String[] io : filenames)
			{
				if      (reader.o2.equals(AbstractReader.TYPE_RAW))
					predict(segmenter, taggers, analyzer, parser, (RawReader)reader.o1, io[0], io[1]);
				else if (reader.o2.equals(AbstractReader.TYPE_LINE))
					predict(tokenizer, taggers, analyzer, parser, (LineReader)reader.o1, io[0], io[1]);
				else if (reader.o2.equals(AbstractReader.TYPE_POS))
					predict(taggers, analyzer, parser, (POSReader)reader.o1, io[0], io[1]);
				else if (reader.o2.equals(AbstractReader.TYPE_DEP))
					predict(parser, (DEPReader)reader.o1, io[0], io[1]);
				else
				{
					new Exception("Invalid reader type: "+reader.o2);
					break;
				}
			}
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void predict(AbstractSegmenter segmenter, Pair<POSTagger[],Double> taggers, AbstractMPAnalyzer analyzer, DEPParser parser, RawReader fin, String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		fin.open(UTInput.createBufferedFileReader(inputFile));
		DEPTree tree;
		int i = 0;
		
		System.out.print(inputFile+": ");
		
		for (List<String> tokens : segmenter.getSentences(fin.getBufferedReader()))
		{
			tree = EngineProcess.getDEPTree(taggers, analyzer, parser, tokens);
			fout.println(tree.toStringDEP() + AbstractColumnReader.DELIM_SENTENCE);
			
			if (++i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		
		fin.close();
		fout.close();
	}
	
	public void predict(AbstractTokenizer tokenizer, Pair<POSTagger[],Double> taggers, AbstractMPAnalyzer analyzer, DEPParser parser, LineReader fin, String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		fin.open(UTInput.createBufferedFileReader(inputFile));
		DEPTree tree;
		String line;
		int i = 0;
		
		System.out.print(inputFile+": ");
		
		while ((line = fin.next()) != null)
		{
			tree = EngineProcess.getDEPTree(tokenizer, taggers, analyzer, parser, line);
			fout.println(tree.toStringDEP() + AbstractColumnReader.DELIM_SENTENCE);
			
			if (++i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		
		fin.close();
		fout.close();
	}
	
	public void predict(Pair<POSTagger[],Double> taggers, AbstractMPAnalyzer analyzer, DEPParser parser, POSReader fin, String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		fin.open(UTInput.createBufferedFileReader(inputFile));
		POSNode[] nodes;
		DEPTree tree;
		int i = 0;
		
		System.out.print(inputFile+": ");
		
		while ((nodes = fin.next()) != null)
		{
			tree = EngineProcess.getDEPTree(analyzer, parser, nodes);
			fout.println(tree.toStringDEP() + AbstractColumnReader.DELIM_SENTENCE);

			if (++i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		
		fin.close();
		fout.close();
	}
	
	public void predict(DEPParser parser, DEPReader fin, String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		fin.open(UTInput.createBufferedFileReader(inputFile));
		DEPTree tree;
		int i = 0;
		
		System.out.print(inputFile+": ");
		
		while ((tree = fin.next()) != null)
		{
			parser.parse(tree);
			fout.println(tree.toStringDEP() + AbstractColumnReader.DELIM_SENTENCE);

			if (++i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		
		fin.close();
		fout.close();
	}
	
	static public void main(String[] args)
	{
		new DEPPredict(args);
	}
}
