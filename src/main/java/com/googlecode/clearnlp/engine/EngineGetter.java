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
package com.googlecode.clearnlp.engine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.googlecode.clearnlp.bin.COMLib;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.component.pos.CPOSTagger;
import com.googlecode.clearnlp.component.srl.CRolesetClassifier;
import com.googlecode.clearnlp.conversion.AbstractC2DConverter;
import com.googlecode.clearnlp.conversion.EnglishC2DConverter;
import com.googlecode.clearnlp.dependency.AbstractDEPParser;
import com.googlecode.clearnlp.dependency.DEPParser;
import com.googlecode.clearnlp.dependency.srl.AbstractSRLabeler;
import com.googlecode.clearnlp.dependency.srl.SRLabeler;
import com.googlecode.clearnlp.feature.xml.DEPFtrXml;
import com.googlecode.clearnlp.feature.xml.POSFtrXml;
import com.googlecode.clearnlp.feature.xml.SRLFtrXml;
import com.googlecode.clearnlp.headrule.HeadRuleMap;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.morphology.DefaultMPAnalyzer;
import com.googlecode.clearnlp.morphology.EnglishMPAnalyzer;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.predicate.AbstractPredIdentifier;
import com.googlecode.clearnlp.predicate.PredIdentifier;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.segmentation.EnglishSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.tokenization.EnglishTokenizer;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class EngineGetter implements EngineLib
{
	// ============================= getter: constituent-to-dependency converter =============================
	
	static public AbstractC2DConverter getC2DConverter(String language, String headruleFile, String mergeLabels)
	{
		HeadRuleMap headrules = new HeadRuleMap(UTInput.createBufferedFileReader(headruleFile));
		
		if (language.equals(AbstractReader.LANG_EN))
			return new EnglishC2DConverter(headrules, mergeLabels);
		
		throw new IllegalArgumentException("The requested language '"+language+"' is not currently supported.");
	}
	
	// ============================= getter: word tokenizer =============================
	
	static public AbstractTokenizer getTokenizer(String language, String dictFile)
	{
		AbstractTokenizer tokenizer = null;
		
		try
		{
			tokenizer = getTokenizer(language, new FileInputStream(dictFile));
		}
		catch (Exception e) {e.printStackTrace();}
		
		return tokenizer;
	}
	
	static public AbstractTokenizer getTokenizer(String language, InputStream stream)
	{
		if (language.equals(AbstractReader.LANG_EN))
			return new EnglishTokenizer(new ZipInputStream(stream));
		
		throw new IllegalArgumentException("The requested language '"+language+"' is not currently supported.");
	}
	
	// ============================= getter: sentence segmenter =============================
	
	static public AbstractSegmenter getSegmenter(String language, AbstractTokenizer tokenizer)
	{
		if (language.equals(AbstractReader.LANG_EN))
			return new EnglishSegmenter(tokenizer);
		
		throw new IllegalArgumentException("The requested language '"+language+"' is not currently supported.");
	}
	
	// ============================= getter: morphological analyzer =============================
	
	static public AbstractMPAnalyzer getMPAnalyzer(String language, String dictFile)
	{
		if (language.equals(AbstractReader.LANG_EN))
			return new EnglishMPAnalyzer(dictFile);
		
		return new DefaultMPAnalyzer();
	}
	
	static public AbstractMPAnalyzer getMPAnalyzer(String language, InputStream stream)
	{
		if (language.equals(AbstractReader.LANG_EN))
			return new EnglishMPAnalyzer(stream);
		
		return new DefaultMPAnalyzer();
	}
	
	// ============================= getter: part-of-speech tagger =============================
	
	static public Pair<POSTagger[],Double> getPOSTaggers(String modelFile) throws Exception
	{
		return getPOSTaggers(new FileInputStream(modelFile));
	}
	
	static public Pair<POSTagger[],Double> getPOSTaggers(InputStream stream) throws Exception
	{
		ZipInputStream zin = new ZipInputStream(stream);
		POSTagger[] taggers = null;
		double threshold = -1;
		POSFtrXml xml = null;
		BufferedReader fin;
		ZipEntry zEntry;
		String entry;
		int modId;
		
		while ((zEntry = zin.getNextEntry()) != null)
		{
			entry = zEntry.getName();
			fin   = new BufferedReader(new InputStreamReader(zin));
			
			if (entry.equals(EngineGetter.ENTRY_CONFIGURATION))
			{
				taggers   = new POSTagger[Integer.parseInt(fin.readLine())];
				threshold = Double.parseDouble(fin.readLine());
			}
			else if (entry.equals(ENTRY_FEATURE))
			{
				xml = new POSFtrXml(getFeatureTemplates(fin));
			}
			else if (entry.startsWith(ENTRY_MODEL))
			{
				modId = Integer.parseInt(entry.substring(ENTRY_MODEL.length()));
				taggers[modId] = new POSTagger(xml, fin);
			}			
		}
		
		zin.close();
		return new Pair<POSTagger[],Double>(taggers, threshold);
	}
	
	// ============================= getter: dependency parser =============================
	
	static public AbstractDEPParser getDEPParser(String modelFile) throws IOException
	{
		return getDEPParser(new FileInputStream(modelFile));
	}
	
	static public AbstractDEPParser getDEPParser(InputStream stream) throws IOException
	{
		ZipInputStream zin = new ZipInputStream(stream);
		AbstractDEPParser parser = null;
		DEPFtrXml xml = null;
		BufferedReader fin;
		ZipEntry zEntry;
		String entry;
		
		while ((zEntry = zin.getNextEntry()) != null)
		{
			entry = zEntry.getName();
			fin   = new BufferedReader(new InputStreamReader(zin));
			
			if (entry.equals(ENTRY_FEATURE))
				xml = new DEPFtrXml(getFeatureTemplates(fin));
			else if (entry.startsWith(ENTRY_MODEL))
				parser = new DEPParser(xml, fin);
		}
		
		zin.close();
		return parser;
	}
	
	// ============================= getter: predicate identifier =============================
	
	static public AbstractPredIdentifier getPredIdentifier(String modelFile) throws IOException
	{
		return getPredIdentifier(new FileInputStream(modelFile));
	}
	
	static public AbstractPredIdentifier getPredIdentifier(InputStream stream) throws IOException
	{
		ZipInputStream zin = new ZipInputStream(stream);
		AbstractPredIdentifier identifier = null;
		SRLFtrXml xml = null;
		BufferedReader fin;
		ZipEntry zEntry;
		String entry;
			
		while ((zEntry = zin.getNextEntry()) != null)
		{
			entry = zEntry.getName();
			fin   = new BufferedReader(new InputStreamReader(zin));
				
			if (entry.equals(ENTRY_FEATURE))
				xml = new SRLFtrXml(getFeatureTemplates(fin));
			else if (entry.startsWith(ENTRY_MODEL))
				identifier = new PredIdentifier(xml, fin);
		}
			
		zin.close();
		return identifier;
	}
	
	// ============================= getter: component =============================
	
	static public AbstractStatisticalComponent getComponent(String modelFile, String mode) throws IOException
	{
		return getComponent(new FileInputStream(modelFile), mode);
	}
	
	static public AbstractStatisticalComponent getComponent(InputStream stream, String mode) throws IOException
	{
		ZipInputStream zin = new ZipInputStream(stream);
		
		if      (mode.equals(COMLib.MODE_POS))
			return new CPOSTagger(zin);
		else if (mode.equals(COMLib.MODE_ROLE))
			return new CRolesetClassifier(zin);
		
		return null;
	}
	
	// ============================= getter: semantic role labeler =============================
	
	static public AbstractSRLabeler getSRLabeler(String modelFile) throws Exception
	{
		return getSRLabeler(new FileInputStream(modelFile));
	}
	
	static public AbstractSRLabeler getSRLabeler(InputStream stream) throws Exception
	{
		ZipInputStream zin = new ZipInputStream(stream);
		StringModel[] models = new StringModel[SRLabeler.MODEL_SIZE];
		Set<String> sDown = null, sUp = null;
		SRLFtrXml xml = null;
		BufferedReader fin;
		ZipEntry zEntry;
		String entry;
		
		while ((zEntry = zin.getNextEntry()) != null)
		{
			entry = zEntry.getName();
			fin   = new BufferedReader(new InputStreamReader(zin));
						
			if (entry.equals(EngineGetter.ENTRY_FEATURE))
				xml = new SRLFtrXml(getFeatureTemplates(fin));
			else if (entry.equals(ENTRY_SET_DOWN))
				sDown = UTInput.getStringSet(fin);
			else if (entry.equals(ENTRY_SET_UP))
				sUp = UTInput.getStringSet(fin);
			else if (entry.startsWith(ENTRY_MODEL+SRLabeler.MODEL_LEFT))
				models[SRLabeler.MODEL_LEFT] = new StringModel(fin);
			else if (entry.startsWith(ENTRY_MODEL+SRLabeler.MODEL_RIGHT))
				models[SRLabeler.MODEL_RIGHT] = new StringModel(fin);
		}
		
		zin.close();
		return new SRLabeler(xml, models, sDown, sUp);
	}

	
	// ============================= utilities =============================
	
	static ByteArrayInputStream getFeatureTemplates(BufferedReader fin) throws IOException
	{
		StringBuilder build = new StringBuilder();
		String line;

		System.out.println("Loading feature templates.");
		
		while ((line = fin.readLine()) != null)
		{
			build.append(line);
			build.append("\n");
		}
		
		return new ByteArrayInputStream(build.toString().getBytes());
	}
}
