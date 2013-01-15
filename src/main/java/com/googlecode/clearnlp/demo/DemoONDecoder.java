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

import java.io.FileInputStream;
import java.util.zip.ZipInputStream;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.component.dep.ONDEPPassParser;
import com.googlecode.clearnlp.component.pos.ONPOSTagger;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.nlp.NLPDecode;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class DemoONDecoder
{
	final String language = AbstractReader.LANG_EN;
	
	public DemoONDecoder(String dictFile, String posModelFile, String depModelFile, String predModelFile, String roleModelFile, String srlModelFile) throws Exception
	{
		AbstractTokenizer tokenizer  = EngineGetter.getTokenizer(language, new FileInputStream(dictFile));
		AbstractComponent analyzer   = EngineGetter.getComponent(new FileInputStream(dictFile), language, NLPLib.MODE_MORPH);
		AbstractComponent identifier = EngineGetter.getComponent(new FileInputStream(predModelFile), language, NLPLib.MODE_PRED);
		AbstractComponent classifier = EngineGetter.getComponent(new FileInputStream(roleModelFile), language, NLPLib.MODE_ROLE);
		AbstractComponent labeler    = EngineGetter.getComponent(new FileInputStream(srlModelFile) , language, NLPLib.MODE_SRL);
		
		ONPOSTagger     tagger = new ONPOSTagger(new ZipInputStream(new FileInputStream(posModelFile)), 0.01, 0.1);
		ONDEPPassParser parser = new ONDEPPassParser(new ZipInputStream(new FileInputStream(depModelFile)), 0.01, 0.1);
				
		AbstractComponent[] components = {tagger, analyzer, parser, identifier, classifier, labeler};
		
		String sentence1, sentence2;
		DEPTree tree1, tree2;
		
		sentence1 = "The ping tests to the router are failing.";
		tree1 = process(tokenizer, components, sentence1);
		
		sentence2 = "Are ping tests failing?";
		tree2 = process(tokenizer, components, sentence2);
		
		tree1.get(3).pos = "NNS";
		tagger.trainHard(tree1, 10);
		
		process(tokenizer, components, sentence1);
		
	/*	sentence = "CUTE GIRL AT SAFEWAY JUST SMILED AT ME";
		tree = process(tokenizer, components, sentence);
		
		tree.get(1).pos = "JJ";
		tree.get(2).pos = "NN";
		tree.get(3).pos = "IN";
		tree.get(7).pos = "IN";
		tree.get(8).pos = "PRP";
		
		tagger.trainHard(tree, 10);
		tree = process(tokenizer, components, sentence);
		
		sentence = "Is this person's sex male or female?";
		tree = process(tokenizer, components, sentence);
		
		tree.get(3).setHead(tree.get(5), "poss");
		tree.get(5).setHead(tree.get(1), "nsubj");
		tree.get(6).setHead(tree.get(1), "attr");
		
		parser.trainHard(tree, 10);
		tree = process(tokenizer, components, sentence);*/
	}
	
	public DEPTree process(AbstractTokenizer tokenizer, AbstractComponent[] components, String sentence)
	{
		NLPDecode nlp = new NLPDecode();
		DEPTree tree = nlp.toDEPTree(tokenizer.getTokens(sentence));
		
		for (AbstractComponent component : components)
			component.process(tree);

		System.out.println(tree.toStringSRL()+"\n");
		return tree;
	}
	
	public static void main(String[] args)
	{
		String dictFile      = args[0];	// e.g., dictionary-1.2.0.zip
		String posModelFile  = args[1];	// e.g., ontonotes-en-pos-1.3.0.jar
		String depModelFile  = args[2];	// e.g., ontonotes-en-dep-1.3.0.jar
		String predModelFile = args[3];	// e.g., ontonotes-en-pred-1.3.0.jar
		String roleModelFile = args[4];	// e.g., ontonotes-en-role-1.3.0.jar
		String srlModelFile  = args[5];	// e.g., ontonotes-en-srl-1.3.0.jar

		try
		{
			new DemoONDecoder(dictFile, posModelFile, depModelFile, predModelFile, roleModelFile, srlModelFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
