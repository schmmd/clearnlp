package com.googlecode.clearnlp.engine;

import java.io.BufferedReader;
import java.util.List;

import com.googlecode.clearnlp.dependency.AbstractDEPParser;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.pos.POSNode;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;

public class EngineProcess
{
	// ============================= input: reader =============================
	
	static public List<List<String>> getSentences(AbstractSegmenter segmenter, BufferedReader fin)
	{
		return segmenter.getSentences(fin);
	}
	
	// ============================= input: sentence =============================
	
	static public List<String> getTokens(AbstractTokenizer tokenizer, String sentence)
	{
		return tokenizer.getTokens(sentence);
	}
	
	static public POSNode[] getPOSNodes(AbstractTokenizer tokenizer, POSTagger tagger, String sentence)
	{
		POSNode[] nodes = toPOSNodes(getTokens(tokenizer, sentence));
		tagger.tag(nodes);

		return nodes;
	}
	
	static public POSNode[] getPOSNodesWithLemmas(AbstractTokenizer tokenizer, POSTagger tagger, AbstractMPAnalyzer analyzer, String sentence)
	{
		POSNode[] nodes = getPOSNodes(tokenizer, tagger, sentence);
		analyzer.lemmatize(nodes);
		
		return nodes;
	}
	
	static public DEPTree getDEPTree(AbstractTokenizer tokenizer, POSTagger tagger, AbstractMPAnalyzer analyzer, AbstractDEPParser parser, String sentence)
	{
		DEPTree tree = toDEPTree(getPOSNodesWithLemmas(tokenizer, tagger, analyzer, sentence));
		parser.parse(tree);
		
		return tree;
	}
	
	// ============================= utilities =============================
	
	static public POSNode[] toPOSNodes(List<String> tokens)
	{
		int i, size = tokens.size();
		POSNode[] nodes = new POSNode[size];
		
		for (i=0; i<size; i++)
			nodes[i].form = tokens.get(i);
		
		return nodes;
	}
	
	static public DEPTree toDEPTree(POSNode[] nodes)
	{
		DEPTree tree = new DEPTree();
		int i, size = nodes.length;
		
		for (i=0; i<size; i++)
			tree.add(new DEPNode(i+1, nodes[i]));
		
		return tree;
	}
}
