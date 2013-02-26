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
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.List;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.nlp.NLPDecode;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class DemoNLPDecoder
{
	final String language = AbstractReader.LANG_EN;
	
	public DemoNLPDecoder(String dictFile, String posModelFile, String depModelFile, String predModelFile, String roleModelFile, String vnModelFile, String srlModelFile, String inputFile, String outputFile) throws Exception
	{
		AbstractTokenizer tokenizer  = EngineGetter.getTokenizer(language, new FileInputStream(dictFile));
		AbstractComponent tagger     = EngineGetter.getComponent(new FileInputStream(posModelFile) , language, NLPLib.MODE_POS);
		AbstractComponent analyzer   = EngineGetter.getComponent(new FileInputStream(dictFile)     , language, NLPLib.MODE_MORPH);
		AbstractComponent parser     = EngineGetter.getComponent(new FileInputStream(depModelFile) , language, NLPLib.MODE_DEP);
		AbstractComponent identifier = EngineGetter.getComponent(new FileInputStream(predModelFile), language, NLPLib.MODE_PRED);
		AbstractComponent classifier = EngineGetter.getComponent(new FileInputStream(roleModelFile), language, NLPLib.MODE_ROLE);
		AbstractComponent verbnet    = EngineGetter.getComponent(new FileInputStream(vnModelFile)  , language, NLPLib.MODE_SENSE+"_vn");
		AbstractComponent labeler    = EngineGetter.getComponent(new FileInputStream(srlModelFile) , language, NLPLib.MODE_SRL);
		
		AbstractComponent[] components = {tagger, analyzer, parser, identifier, classifier, verbnet, labeler};
		
		String sentence = "I'd like to meet Dr. Choi.";
		process(tokenizer, components, sentence);
		process(tokenizer, components, UTInput.createBufferedFileReader(inputFile), UTOutput.createPrintBufferedFileStream(outputFile));
	}
	
	public void process(AbstractTokenizer tokenizer, AbstractComponent[] components, String sentence)
	{
		NLPDecode nlp = new NLPDecode();
		DEPTree tree = nlp.toDEPTree(tokenizer.getTokens(sentence));
		
		for (AbstractComponent component : components)
			component.process(tree);

		System.out.println(tree.toStringSRL()+"\n");
	}
	
	public void process(AbstractTokenizer tokenizer, AbstractComponent[] components, BufferedReader reader, PrintStream fout)
	{
		AbstractSegmenter segmenter = EngineGetter.getSegmenter(language, tokenizer);
		NLPDecode nlp = new NLPDecode();
		DEPTree tree;
		
		for (List<String> tokens : segmenter.getSentences(reader))
		{
			tree = nlp.toDEPTree(tokens);
			
			for (AbstractComponent component : components)
				component.process(tree);
			
			fout.println(tree.toStringSRL()+"\n");
		}
		
		fout.close();
	}

	public static void main(String[] args)
	{
		String dictFile      = args[0];	// e.g., dictionary-1.3.1.zip
		String posModelFile  = args[1];	// e.g., ontonotes-en-pos-1.3.0.jar
		String depModelFile  = args[2];	// e.g., ontonotes-en-dep-1.3.0.jar
		String predModelFile = args[3];	// e.g., ontonotes-en-pred-1.3.0.jar
		String roleModelFile = args[4];	// e.g., ontonotes-en-role-1.3.0.jar
		String vnModelFile   = args[5];	// e.g., ontonotes-en-sense-vn-1.3.1.jar
		String srlModelFile  = args[6];	// e.g., ontonotes-en-srl-1.3.0.jar
		String inputFile     = args[7];
		String outputFile    = args[8];

		try
		{
			new DemoNLPDecoder(dictFile, posModelFile, depModelFile, predModelFile, roleModelFile, vnModelFile, srlModelFile, inputFile, outputFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
