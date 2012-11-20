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
package com.googlecode.clearnlp.bin;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.googlecode.clearnlp.classification.model.AbstractModel;
import com.googlecode.clearnlp.classification.train.AbstractTrainSpace;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.dependency.AbstractDEPParser;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.predicate.AbstractPredIdentifier;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.run.LiblinearTrain;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
abstract public class AbstractBin
{
	final public String TAG_READER					= "reader";
	final public String TAG_READER_TYPE				= "type";
	final public String TAG_READER_COLUMN			= "column";
	final public String TAG_READER_COLUMN_INDEX		= "index";
	final public String TAG_READER_COLUMN_FIELD		= "field";
	
	final public String TAG_LEXICA					= "lexica";
	final public String TAG_LEXICA_LEXICON			= "lexicon";
	final public String TAG_LEXICA_LEXICON_TYPE		= "type";
	final public String TAG_LEXICA_LEXICON_LABEL	= "label";
	final public String TAG_LEXICA_LEXICON_CUTOFF	= "cutoff";
	
	final public String TAG_TRAIN					= "train";
	final public String TAG_TRAIN_ALGORITHM			= "algorithm";
	final public String TAG_TRAIN_ALGORITHM_NAME	= "name";
	final public String TAG_TRAIN_THREADS			= "threads";
	
	final public String TAG_LANGUAGE				= "language";
	final public String TAG_MODE					= "mode";
	final public String TAG_DICTIONARY				= "dictionary";
	final public String TAG_POS_MODEL				= "pos_model";
	final public String TAG_DEP_MODEL				= "dep_model";
	final public String TAG_PRED_MODEL				= "pred_model";
	final public String TAG_SRL_MODEL				= "srl_model";
	
	/** Initializes arguments using args4j. */
	protected void initArgs(String[] args)
	{
		CmdLineParser cmd = new CmdLineParser(this);
		
		try
		{
			cmd.parseArgument(args);
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
			System.exit(1);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	// ============================= getter: language =============================
	
	protected String getLanguage(Element eConfig)
	{
		Element eLanguage = UTXml.getFirstElementByTagName(eConfig, TAG_LANGUAGE);
		return UTXml.getTrimmedTextContent(eLanguage);
	}
	
	protected String getMode(Element eConfig)
	{
		Element eMode = UTXml.getFirstElementByTagName(eConfig, TAG_MODE);
		return UTXml.getTrimmedTextContent(eMode);
	}
	
	// ============================= getter: components =============================
	
	protected AbstractSegmenter getSegmenter(Element eConfig)
	{
		String language = getLanguage(eConfig);
		String dictFile = UTXml.getTrimmedTextContent(UTXml.getFirstElementByTagName(eConfig, TAG_DICTIONARY));

		return EngineGetter.getSegmenter(language, EngineGetter.getTokenizer(language, dictFile));
	}
	
	protected AbstractTokenizer getTokenizer(Element eConfig)
	{
		String language = getLanguage(eConfig);
		String dictFile = UTXml.getTrimmedTextContent(UTXml.getFirstElementByTagName(eConfig, TAG_DICTIONARY));

		return EngineGetter.getTokenizer(language, dictFile);
	}
	
	protected AbstractMPAnalyzer getMPAnalyzer(Element eConfig)
	{
		String language = getLanguage(eConfig);
		String dictFile = UTXml.getTrimmedTextContent(UTXml.getFirstElementByTagName(eConfig, TAG_DICTIONARY));

		return EngineGetter.getMPAnalyzer(language, dictFile);
	}
	
	protected Pair<POSTagger[],Double> getPOSTaggers(Element eConfig)
	{
		try
		{
			String modelFile = UTXml.getTrimmedTextContent(UTXml.getFirstElementByTagName(eConfig, TAG_POS_MODEL));
			return EngineGetter.getPOSTaggers(modelFile);
		}
		catch (Exception e) {e.printStackTrace();}
		
		return null;
	}
	
	protected AbstractDEPParser getDEPParser(Element eConfig)
	{
		String modelFile = UTXml.getTrimmedTextContent(UTXml.getFirstElementByTagName(eConfig, TAG_DEP_MODEL));
		AbstractDEPParser parser = null;
		
		try
		{
			parser = EngineGetter.getDEPParser(modelFile);
		}
		catch (IOException e) {e.printStackTrace();}
		
		return parser;
	}
	
	protected AbstractPredIdentifier getPredIdentifier(Element eConfig)
	{
		String modelFile = UTXml.getTrimmedTextContent(UTXml.getFirstElementByTagName(eConfig, TAG_PRED_MODEL));
		AbstractPredIdentifier identifier = null;
		
		try
		{
			identifier = EngineGetter.getPredIdentifier(modelFile);
		}
		catch (IOException e) {e.printStackTrace();}
		
		return identifier;
	}
	
	// ============================= getter: readers =============================
	
	protected JointReader getCDEPReader(Element eReader)
	{
		ObjectIntOpenHashMap<String> map = getFieldMap(eReader);
		
		int iId		= map.get(AbstractColumnReader.FIELD_ID)	 - 1;
		int iForm	= map.get(AbstractColumnReader.FIELD_FORM)	 - 1;
		int iLemma	= map.get(AbstractColumnReader.FIELD_LEMMA)	 - 1;
		int iPos	= map.get(AbstractColumnReader.FIELD_POS)	 - 1;
		int iFeats	= map.get(AbstractColumnReader.FIELD_FEATS)	 - 1;
		int iHeadId	= map.get(AbstractColumnReader.FIELD_HEADID) - 1;
		int iDeprel	= map.get(AbstractColumnReader.FIELD_DEPREL) - 1;
		int iSHeads = map.get(AbstractColumnReader.FIELD_SHEADS) - 1;
		
		return new JointReader(iId, iForm, iLemma, iPos, iFeats, iHeadId, iDeprel, iSHeads);
	}
	
	/** Called by {@link AbstractBin#getCDEPReader(Element, String)}. */
	private ObjectIntOpenHashMap<String> getFieldMap(Element eReader)
	{
		NodeList list = eReader.getElementsByTagName(TAG_READER_COLUMN);
		int i, index, size = list.getLength();
		Element element;
		String field;
		
		ObjectIntOpenHashMap<String> map = new ObjectIntOpenHashMap<String>();
		
		for (i=0; i<size; i++)
		{
			element = (Element)list.item(i);
			field   = UTXml.getTrimmedAttribute(element, TAG_READER_COLUMN_FIELD);
			index   = Integer.parseInt(element.getAttribute(TAG_READER_COLUMN_INDEX));
			
			map.put(field, index);
		}
		
		return map;
	}
	
	// ============================= classification =============================
	
	protected StringTrainSpace mergeTrainSpaces(List<StringTrainSpace> spaces, int labelCutoff, int featureCutoff)
	{
		StringTrainSpace space;
		
		if (spaces.size() == 1)
		{
			space = spaces.get(0);
		}
		else
		{
			System.out.println("Merging training instances:");
			space = new StringTrainSpace(false, labelCutoff, featureCutoff);
			
			for (StringTrainSpace s : spaces)
			{
				space.appendSpace(s);
				System.out.print(".");
				s.clear();
			}
			
			System.out.println();			
		}
		
		return space;
	}
	
	protected AbstractModel getModel(Element eTrain, AbstractTrainSpace space, int index)
	{
		NodeList list = eTrain.getElementsByTagName(TAG_TRAIN_ALGORITHM);
		int numThreads = getNumOfThreads(eTrain);
		Element  eAlgorithm;
		String   name;
		
		eAlgorithm = (Element)list.item(index);
		name       = UTXml.getTrimmedAttribute(eAlgorithm, TAG_TRAIN_ALGORITHM_NAME);
		
		if (name.equals("liblinear"))
		{
			byte solver = Byte  .parseByte  (UTXml.getTrimmedAttribute(eAlgorithm, "solver"));
			double cost = Double.parseDouble(UTXml.getTrimmedAttribute(eAlgorithm, "cost"));
			double eps  = Double.parseDouble(UTXml.getTrimmedAttribute(eAlgorithm, "eps"));
			double bias = Double.parseDouble(UTXml.getTrimmedAttribute(eAlgorithm, "bias"));

			return getLiblinearModel(space, numThreads, solver, cost, eps, bias);
		}
		
		return null;
	}
	
	/** Called by {@link AbstractBin#getModel(Element, AbstractTrainSpace, int)}. */
	protected AbstractModel getLiblinearModel(AbstractTrainSpace space, int numThreads, byte solver, double cost, double eps, double bias)
	{
		space.build();
		System.out.println("Liblinear:");
		System.out.printf("- solver=%d, cost=%f, eps=%f, bias=%f\n", solver, cost, eps, bias);
		return LiblinearTrain.getModel(space, numThreads, solver, cost, eps, bias);
	}
	
	protected int getNumOfThreads(Element eTrain)
	{
		Element eThreads = UTXml.getFirstElementByTagName(eTrain, TAG_TRAIN_THREADS); 
		return Integer.parseInt(UTXml.getTrimmedTextContent(eThreads));
	}
	
	protected void printTime(String message, long st, long et)
	{
		long millis = et - st;
		long mins   = TimeUnit.MILLISECONDS.toMinutes(millis);
		long secs   = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(mins);

		System.out.println(message);
		System.out.println(String.format("- %d mins, %d secs", mins, secs));
	}
}