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
import com.googlecode.clearnlp.dependency.srl.AbstractSRLabeler;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.predicate.AbstractPredIdentifier;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class DemoSRLabeler
{
	final String language = AbstractReader.LANG_EN;
	
	public DemoSRLabeler(String dictionaryFile, String posModelFile, String depModelFile, String predModelFile, String srlModelFile, String inputFile, String outputFile) throws Exception
	{
		AbstractTokenizer tokenizer = EngineGetter.getTokenizer(language, dictionaryFile);
		AbstractMPAnalyzer analyzer = EngineGetter.getMPAnalyzer(language, dictionaryFile);
		Pair<POSTagger[],Double> taggers = EngineGetter.getPOSTaggers(posModelFile);
		AbstractDEPParser parser = EngineGetter.getDEPParser(depModelFile);
		AbstractPredIdentifier identifier = EngineGetter.getPredIdentifier(predModelFile);
		AbstractSRLabeler labeler = EngineGetter.getSRLabeler(srlModelFile);
		
		String sentence = "I'd like to meet Mr. Choi.";
		label(tokenizer, analyzer, taggers, parser, identifier, labeler, sentence);
		label(tokenizer, analyzer, taggers, parser, identifier, labeler, UTInput.createBufferedFileReader(inputFile), UTOutput.createPrintBufferedFileStream(outputFile));
	}
	
	public void label(AbstractTokenizer tokenizer, AbstractMPAnalyzer analyzer, Pair<POSTagger[],Double> taggers, AbstractDEPParser parser, AbstractPredIdentifier identifier, AbstractSRLabeler labeler, String sentence)
	{
		DEPTree tree = EngineProcess.getDEPTree(tokenizer, taggers, analyzer, parser, identifier, labeler, sentence);
		System.out.println(tree.toStringSRL()+"\n");
	}
	
	public void label(AbstractTokenizer tokenizer, AbstractMPAnalyzer analyzer, Pair<POSTagger[],Double> taggers, AbstractDEPParser parser, AbstractPredIdentifier identifier, AbstractSRLabeler labeler, BufferedReader reader, PrintStream fout)
	{
		AbstractSegmenter segmenter = EngineGetter.getSegmenter(language, tokenizer);
		DEPTree tree;
		
		for (List<String> tokens : segmenter.getSentences(reader))
		{
			tree = EngineProcess.getDEPTree(taggers, analyzer, parser, identifier, labeler, tokens);
			fout.println(tree.toStringSRL()+"\n");	
		}
		
		fout.close();
	}

	public static void main(String[] args)
	{
		String dictionaryFile = args[0];	// e.g., dictionary-1.1.0.zip
		String posModelFile   = args[1];	// e.g., ontonotes-en-pos-1.1.0g.jar
		String depModelFile   = args[2];	// e.g., ontonotes-en-dep-1.1.0b3.jar
		String predModelFile  = args[3];	// e.g., ontonotes-en-pred-1.2.0.jar
		String srlModelFile   = args[4];	// e.g., ontonotes-en-srl-1.2.0b3.jar
		String inputFile      = args[5];
		String outputFile     = args[6];

		try
		{
			new DemoSRLabeler(dictionaryFile, posModelFile, depModelFile, predModelFile, srlModelFile, inputFile, outputFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
