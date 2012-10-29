package com.googlecode.clearnlp.demo;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.List;

import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.pos.POSNode;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.pair.Pair;

public class DemoPOSTagger
{
	final String language = AbstractReader.LANG_EN;
	
	public DemoPOSTagger(String dictionaryFile, String posModelFile, String inputFile, String outputFile) throws Exception
	{
		AbstractTokenizer tokenizer = EngineGetter.getTokenizer(language, dictionaryFile);
		Pair<POSTagger[],Double> taggers = EngineGetter.getPOSTaggers(posModelFile);
		
		String sentence = "I'd like to meet Mr. Choi.";
		tag(tokenizer, taggers, sentence);
		tag(tokenizer, taggers, UTInput.createBufferedFileReader(inputFile), UTOutput.createPrintBufferedFileStream(outputFile));
	}
	
	public void tag(AbstractTokenizer tokenizer, Pair<POSTagger[],Double> taggers, String sentence)
	{
		POSNode[] nodes = EngineProcess.getPOSNodes(tokenizer, taggers, sentence);
		System.out.println(UTArray.join(nodes,"\n")+"\n");
	}
	
	public void tag(AbstractTokenizer tokenizer, Pair<POSTagger[],Double> taggers, BufferedReader reader, PrintStream fout)
	{
		AbstractSegmenter segmenter = EngineGetter.getSegmenter(language, tokenizer);
		POSNode[] nodes;
		
		for (List<String> tokens : segmenter.getSentences(reader))
		{
			nodes = EngineProcess.getPOSNodes(taggers, tokens);
			fout.println(UTArray.join(nodes,"\n")+"\n");	
		}
		
		fout.close();
	}

	public static void main(String[] args)
	{
		String dictionaryFile = args[0];	// e.g., dictionary-1.1.0.zip
		String posModelFile   = args[1];	// e.g., ontonotes-en-pos-1.1.0g.jar
		String inputFile      = args[2];
		String outputFile     = args[3];

		try
		{
			new DemoPOSTagger(dictionaryFile, posModelFile, inputFile, outputFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
