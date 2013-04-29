/**
* Copyright 2012-2013 University of Massachusetts Amherst
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
import java.util.List;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.dependency.DEPFeat;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPLibEn;
import com.googlecode.clearnlp.dependency.DEPNode;
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
public class DemoCMDDecoder
{
	final String language = AbstractReader.LANG_EN;
	
	public DemoCMDDecoder(String dictFile, String posModelFile, String depModelFile, String predModelFile, String roleModelFile, String vnModelFile, String srlModelFile) throws Exception
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
		
		String sentence = "Say \"Hello\".";
		process(tokenizer, components, sentence);
		
		sentence = "Ask yourself a question.";
		process(tokenizer, components, sentence);
		
		sentence = "Say {Hello, I am ready to help you.}";
		process(tokenizer, components, sentence);
		
		sentence = "Ask {What is your username?}"; 
		process(tokenizer, components, sentence);
	}
	
	public void process(AbstractTokenizer tokenizer, AbstractComponent[] components, String sentence)
	{
		List<String> tokens = tokenizer.getTokens(sentence);
		boolean bCmd = isCommand(tokens);
		
		NLPDecode nlp = new NLPDecode();
		DEPTree tree = bCmd ? nlp.toDEPTree(tokens.subList(2, tokens.size()-1)) : nlp.toDEPTree(tokens);
		
		for (AbstractComponent component : components)
			component.process(tree);

		if (bCmd) tree = mergeCommand(tree, tokens.get(0));
		System.out.println(tree.toStringSRL()+"\n");
	}
	
	public boolean isCommand(List<String> tokens)
	{
		if (tokens.size() >= 4)
		{
			final String[] COMMANDS = {"say", "ask"};
			
			String fst = tokens.get(0).toLowerCase();
			String snd = tokens.get(1);
			String lst = tokens.get(tokens.size()-1);
			
			for (String cmd : COMMANDS)
				if (fst.equals(cmd) && snd.equals("{") && lst.equals("}"))
					return true;
		}
		
		return false;
	}
	
	public DEPTree mergeCommand(DEPTree oTree, String cmd)
	{
		final String CMD_TAG = "CMD";
		
		DEPNode nRoot = new DEPNode(1, cmd, cmd.toLowerCase(), CMD_TAG, new DEPFeat());
		DEPNode oRoot = oTree.get(DEPLib.ROOT_ID);
		DEPTree nTree = new DEPTree();
		DEPNode oNode;
		
		nRoot.setHead(nTree.get(DEPLib.ROOT_ID), DEPLibEn.DEP_ROOT);
		nRoot.initSHeads();
		nTree.add(nRoot);
		
		int i, size = oTree.size();
		
		for (i=1; i<size; i++)
		{
			oNode = oTree.get(i);
			oNode.id++;
			
			if (oNode.isDependentOf(oRoot))
				oNode.setHead(nRoot, CMD_TAG);
			
			nTree.add(oNode);
		}
		
		return nTree;
	}

	public static void main(String[] args)
	{
		String dictFile      = args[0];	// e.g., dictionary-1.3.1.zip
		String posModelFile  = args[1];	// e.g., ontonotes-en-pos-1.3.0.jar
		String depModelFile  = args[2];	// e.g., ontonotes-en-dep-1.3.0.jar
		String predModelFile = args[3];	// e.g., ontonotes-en-pred-1.3.0.jar
		String roleModelFile = args[4];	// e.g., ontonotes-en-role-1.3.0.jar
		String vnModelFile   = args[5];	// e.g., ontonotes-en-sense-vn-1.3.1b.jar
		String srlModelFile  = args[6];	// e.g., ontonotes-en-srl-1.3.0.jar

		try
		{
			new DemoCMDDecoder(dictFile, posModelFile, depModelFile, predModelFile, roleModelFile, vnModelFile, srlModelFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
