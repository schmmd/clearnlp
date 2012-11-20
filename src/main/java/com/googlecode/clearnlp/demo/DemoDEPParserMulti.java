/**
 * Copyright 2012 University of Massachusetts Amherst
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.googlecode.clearnlp.demo;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.List;

import com.googlecode.clearnlp.dependency.AbstractDEPParser;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class DemoDEPParserMulti
{
	final String language = AbstractReader.LANG_EN;
	
	public DemoDEPParserMulti(String dictionaryFile, String posModelFile, String depModelFile, String inputDir, int beginIndex, int length) throws Exception
	{
		AbstractTokenizer tokenizer = EngineGetter.getTokenizer(language, dictionaryFile);
		AbstractSegmenter segmenter = EngineGetter.getSegmenter(language, tokenizer);
		AbstractMPAnalyzer analyzer = EngineGetter.getMPAnalyzer(language, dictionaryFile);
		Pair<POSTagger[],Double> taggers = EngineGetter.getPOSTaggers(posModelFile);
		AbstractDEPParser parser = EngineGetter.getDEPParser(depModelFile);
		String[] inputFiles = UTFile.getSortedFileList(inputDir);
		int i, endIndex = beginIndex + length;
		String inputFile, outputFile;
		
		if (endIndex > inputFiles.length)
			endIndex = inputFiles.length;
		
		for (i=beginIndex; i<endIndex; i++)
		{
			inputFile  = inputFiles[i];
			outputFile = inputFile + ".parsed";
			
			System.out.println(outputFile);
			parse(segmenter, analyzer, taggers, parser, inputFile, outputFile);
		}
	}
	
	public void parse(AbstractSegmenter segmenter, AbstractMPAnalyzer analyzer, Pair<POSTagger[],Double> taggers, AbstractDEPParser parser, String inputFile, String outputFile) throws Exception
	{
		BufferedReader reader = UTInput.createBufferedFileReader(inputFile);
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		DEPTree tree;
		
		for (List<String> tokens : segmenter.getSentences(reader))
		{
			tree = EngineProcess.getDEPTree(taggers, analyzer, parser, tokens);
			fout.println(tree.toStringDEP()+"\n");
		}
		
		reader.close();
		fout.close();
	}
	
	public static void main(String[] args)
	{
		String dictionaryFile = args[0];	// e.g., dictionary-1.1.0.zip
		String posModelFile   = args[1];	// e.g., ontonotes-en-pos-1.1.0g.jar
		String depModelFile   = args[2];	// e.g., ontonotes-en-dep-1.1.0b3.jar
		String inputDir       = args[3];
		int    beginIndex     = Integer.parseInt(args[4]);
		int    length         = Integer.parseInt(args[5]);
				
		try
		{
			new DemoDEPParserMulti(dictionaryFile, posModelFile, depModelFile, inputDir, beginIndex, length);
		}
		catch (Exception e) {e.printStackTrace();}
	}

}
