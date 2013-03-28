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
package com.googlecode.clearnlp.nlp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.component.dep.CDEPBackParser;
import com.googlecode.clearnlp.component.dep.CDEPPassParser;
import com.googlecode.clearnlp.component.morph.CDefaultMPAnalyzer;
import com.googlecode.clearnlp.component.morph.CEnglishMPAnalyzer;
import com.googlecode.clearnlp.component.pos.CPOSTagger;
import com.googlecode.clearnlp.component.srl.CPredIdentifier;
import com.googlecode.clearnlp.component.srl.CRolesetClassifier;
import com.googlecode.clearnlp.component.srl.CSRLabeler;
import com.googlecode.clearnlp.component.srl.CSenseClassifier;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.reader.LineReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class NLPDecode extends AbstractNLP
{
	@Option(name="-c", usage="configuration file (required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-i", usage="input path (required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-ie", usage="input file extension (default: .*)", required=false, metaVar="<regex>")
	private String s_inputExt = ".*";
	@Option(name="-oe", usage="output file extension (default: cnlp)", required=false, metaVar="<string>")
	private String s_outputExt = "cnlp";
	@Option(name="-z", usage="mode (pos|morph|dep|srl|sense_vn)", required=true, metaVar="<string>")
	protected String s_mode;
	@Option(name="-twit", usage="if set, tokenize for twits", required=false, metaVar="<boolean>")
	protected boolean b_twit;
	@Option(name="-beams", usage="beam size (default: 1)", required=false, metaVar="<boolean>")
	protected int n_beams = 1;
	
	public NLPDecode() {}
	
	public NLPDecode(String[] args)
	{
		initArgs(args);
		
		try
		{
			decode(s_configXml, s_inputPath, s_inputExt, s_outputExt, s_mode);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void decode(String configXml, String inputPath, String inputExt, String outputExt, String mode) throws Exception
	{
		List<String[]> filenames = getFilenames(inputPath, inputExt, outputExt);
		Element eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		Element eReader = UTXml.getFirstElementByTagName(eConfig, TAG_READER);
		Element eModels = UTXml.getFirstElementByTagName(eConfig, TAG_MODELS);
		AbstractReader<?> reader = getReader(eReader);
		String readerType = reader.getType();
		PrintStream fout;
		
		AbstractSegmenter   segmenter  = readerType.equals(AbstractReader.TYPE_RAW)  ? getSegmenter(eModels, b_twit) : null;
		AbstractTokenizer   tokenizer  = readerType.equals(AbstractReader.TYPE_LINE) ? getTokenizer(eModels, b_twit) : null;
		AbstractComponent[] components = getComponent(eModels, getModes(readerType, mode));
		
		System.out.println("Decoding:");
		
		for (String[] filename : filenames)
		{
			reader.open(UTInput.createBufferedFileReader(filename[0]));
			fout = UTOutput.createPrintBufferedFileStream(filename[1]);
			System.out.println(filename[0]);
			
			decode(reader, fout, segmenter, tokenizer, components, mode);
			reader.close(); fout.close();
		}
	}
	
	//	===================================== decode ===================================== 
	
	public void decode(AbstractReader<?> reader, PrintStream fout, AbstractSegmenter segmenter, AbstractTokenizer tokenizer, AbstractComponent[] components, String mode) throws IOException
	{
		if      (segmenter != null)
			decode(reader.getBufferedReader(), fout, segmenter, components, mode);
		else if (tokenizer != null)
			decode((LineReader)reader, fout, tokenizer, components, mode);
		else
			decode((JointReader)reader, fout, components, mode);
	}
	
	public void decode(BufferedReader reader, PrintStream fout, AbstractSegmenter segmenter, AbstractComponent[] components, String mode) throws IOException
	{
		DEPTree tree;
		
		for (List<String> tokens : segmenter.getSentences(reader))
		{
			tree = toDEPTree(tokens);
			
			for (AbstractComponent component : components)
				component.process(tree);
			
			fout.println(toString(tree, mode)+"\n");
		}
	}
	
	public void decode(LineReader reader, PrintStream fout, AbstractTokenizer tokenizer, AbstractComponent[] components, String mode)
	{
		String sentence;
		DEPTree tree;
		
		while ((sentence = reader.next()) != null)
		{
			tree = toDEPTree(tokenizer.getTokens(sentence));
			
			for (AbstractComponent component : components)
				component.process(tree);
			
			fout.println(toString(tree, mode)+"\n");
		}
	}
	
	public void decode(JointReader reader, PrintStream fout, AbstractComponent[] components, String mode)
	{
		DEPTree tree;
		
		while ((tree = reader.next()) != null)
		{
			for (AbstractComponent component : components)
				component.process(tree);
			
			fout.println(toString(tree, mode)+"\n");
		}
	}
	
	public DEPTree toDEPTree(List<String> tokens)
	{
		DEPTree tree = new DEPTree();
		int i, size = tokens.size();
		
		for (i=0; i<size; i++)
			tree.add(new DEPNode(i+1, tokens.get(i)));
		
		return tree;
	}
	
	//	===================================== public methods =====================================
	
	public AbstractComponent getComponent(InputStream stream, String language, String mode) throws IOException
	{
		ZipInputStream zin = new ZipInputStream(stream);
		
		if      (mode.equals(NLPLib.MODE_POS))
			return new CPOSTagger(zin);
		else if (mode.equals(NLPLib.MODE_MORPH))
			return getMPAnalyzer(zin, language);
		else if (mode.equals(NLPLib.MODE_DEP))
			return new CDEPPassParser(zin);
		else if (mode.equals(NLPLib.MODE_PRED))
			return new CPredIdentifier(zin);
		else if (mode.equals(NLPLib.MODE_ROLE))
			return new CRolesetClassifier(zin);
		else if (mode.startsWith(NLPLib.MODE_SENSE))
			return new CSenseClassifier(zin, mode.substring(mode.lastIndexOf("_")+1));
		else if (mode.equals(NLPLib.MODE_SRL))
			return new CSRLabeler(zin);
		else if (mode.equals(NLPLib.MODE_DEP_BACK))
			return new CDEPBackParser(zin);
		
		throw new IllegalArgumentException("The requested mode '"+mode+"' is not supported.");
	}
	
	private AbstractComponent getMPAnalyzer(ZipInputStream zin, String language) throws IOException
	{
		if (language.equals(AbstractReader.LANG_EN))
			return new CEnglishMPAnalyzer(zin);
		
		return new CDefaultMPAnalyzer();
	}
	
	public List<String> getModes(String readerType, String mode)
	{
		List<String> modes = new ArrayList<String>();
		
		if (mode.equals(NLPLib.MODE_POS))
		{
			modes.add(NLPLib.MODE_POS);
		}
		if (mode.equals(NLPLib.MODE_MORPH))
		{
			if (readerType.equals(AbstractReader.TYPE_RAW) || readerType.equals(AbstractReader.TYPE_LINE) || readerType.equals(AbstractReader.TYPE_TOK))
				modes.add(NLPLib.MODE_POS);
			
			modes.add(NLPLib.MODE_MORPH);
		}
		else if (mode.equals(NLPLib.MODE_DEP))
		{
			if (readerType.equals(AbstractReader.TYPE_RAW) || readerType.equals(AbstractReader.TYPE_LINE) || readerType.equals(AbstractReader.TYPE_TOK))
			{
				modes.add(NLPLib.MODE_POS);
				modes.add(NLPLib.MODE_MORPH);
			}
			else if (readerType.equals(AbstractReader.TYPE_POS))
			{
				modes.add(NLPLib.MODE_MORPH);
			}
			
			modes.add(NLPLib.MODE_DEP);
		}
		else if (mode.equals(NLPLib.MODE_PRED) || mode.equals(NLPLib.MODE_ROLE) || mode.startsWith(NLPLib.MODE_SENSE))
		{
			modes.add(mode);
		}
		else if (mode.equals(NLPLib.MODE_SRL))
		{
			if (readerType.equals(AbstractReader.TYPE_RAW) || readerType.equals(AbstractReader.TYPE_LINE) || readerType.equals(AbstractReader.TYPE_TOK))
			{
				modes.add(NLPLib.MODE_POS);
				modes.add(NLPLib.MODE_MORPH);
				modes.add(NLPLib.MODE_DEP);
			}
			else if (readerType.equals(AbstractReader.TYPE_POS))
			{
				modes.add(NLPLib.MODE_MORPH);
				modes.add(NLPLib.MODE_DEP);
			}
			else if (readerType.equals(AbstractReader.TYPE_MORPH))
			{
				modes.add(NLPLib.MODE_DEP);
			}
			
			modes.add(NLPLib.MODE_PRED);
			modes.add(NLPLib.MODE_ROLE);
			modes.add(NLPLib.MODE_SRL);
		}
		else if (mode.equals(NLPLib.MODE_DEP_BACK))
		{
			if (readerType.equals(AbstractReader.TYPE_RAW) || readerType.equals(AbstractReader.TYPE_LINE) || readerType.equals(AbstractReader.TYPE_TOK))
			{
				modes.add(NLPLib.MODE_POS);
				modes.add(NLPLib.MODE_MORPH);
			}
			else if (readerType.equals(AbstractReader.TYPE_POS))
			{
				modes.add(NLPLib.MODE_MORPH);
			}
			
			modes.add(NLPLib.MODE_DEP_BACK);
		}
		
		return modes;
	}
	
//	===================================== getComponent: protected =====================================
	
	protected AbstractComponent[] getComponent(Element eModels, List<String> modes) throws Exception
	{
		AbstractComponent[] components = new AbstractComponent[modes.size()];
		NodeList list = eModels.getElementsByTagName(TAG_MODEL);
		ObjectIntOpenHashMap<String> map = getModeMap(modes);
		String language = getLanguage(eModels);
		int i, idx, size = list.getLength();
		Element eModel;
		String mode;
		
		for (i=0; i<size; i++)
		{
			eModel = (Element)list.item(i);
			mode   = UTXml.getTrimmedAttribute(eModel, TAG_MODE);
			
			if ((idx = map.get(mode) - 1) >= 0)
				components[idx] = getComponent(new FileInputStream(UTXml.getTrimmedAttribute(eModel, TAG_PATH)), language, mode);
		}
		
		return components;
	}
	
	protected AbstractSegmenter getSegmenter(Element eModels, boolean twit) throws IOException
	{
		AbstractTokenizer tokenizer = getTokenizer(eModels, twit);
		String language = getLanguage(eModels);
		
		return EngineGetter.getSegmenter(language, tokenizer);
	}
	
	protected AbstractTokenizer getTokenizer(Element eModels, boolean twit) throws IOException
	{
		String language   = getLanguage(eModels);
		String dictionary = getDictionary(eModels);
		
		AbstractTokenizer tokenizer = EngineGetter.getTokenizer(language, new FileInputStream(dictionary));
		tokenizer.setTwit(twit);
		
		return tokenizer;
	}
	
	/** Called by {@link NLPDecode#getComponent(Element, List)}. */
	private ObjectIntOpenHashMap<String> getModeMap(List<String> modes)
	{
		ObjectIntOpenHashMap<String> map = new ObjectIntOpenHashMap<String>();
		int i, size = modes.size();
		
		for (i=0; i<size; i++)
			map.put(modes.get(i), i+1);
		
		return map;
	}
	
	static public void main(String[] args)
	{
		new NLPDecode(args);
	}
}
