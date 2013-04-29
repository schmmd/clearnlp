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
package com.googlecode.clearnlp.engine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.component.dep.CDEPParser;
import com.googlecode.clearnlp.component.morph.CDefaultMPAnalyzer;
import com.googlecode.clearnlp.component.morph.CEnglishMPAnalyzer;
import com.googlecode.clearnlp.component.pos.CPOSTagger;
import com.googlecode.clearnlp.component.srl.CPredIdentifier;
import com.googlecode.clearnlp.component.srl.CRolesetClassifier;
import com.googlecode.clearnlp.component.srl.CSRLabeler;
import com.googlecode.clearnlp.component.srl.CSenseClassifier;
import com.googlecode.clearnlp.conversion.AbstractC2DConverter;
import com.googlecode.clearnlp.conversion.EnglishC2DConverter;
import com.googlecode.clearnlp.headrule.HeadRuleMap;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.segmentation.EnglishSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.tokenization.EnglishTokenizer;
import com.googlecode.clearnlp.util.UTInput;

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
	
	// ============================= getter: component =============================
	
	static public AbstractComponent getComponent(InputStream stream, String language, String mode) throws IOException
	{
		ZipInputStream zin = new ZipInputStream(stream);
		
		if      (mode.equals(NLPLib.MODE_POS))
			return new CPOSTagger(zin);
		else if (mode.equals(NLPLib.MODE_MORPH))
			return getCMPAnalyzer(zin, language);
		else if (mode.equals(NLPLib.MODE_DEP))
			return new CDEPParser(zin);
		else if (mode.equals(NLPLib.MODE_PRED))
			return new CPredIdentifier(zin);
		else if (mode.equals(NLPLib.MODE_ROLE))
			return new CRolesetClassifier(zin);
		else if (mode.startsWith(NLPLib.MODE_SENSE))
			return new CSenseClassifier(zin, mode.substring(mode.lastIndexOf("_")+1));
		else if (mode.equals(NLPLib.MODE_SRL))
			return new CSRLabeler(zin);
		
		throw new IllegalArgumentException("The requested mode '"+mode+"' is not supported.");
	}
	
	static private AbstractComponent getCMPAnalyzer(ZipInputStream zin, String language) throws IOException
	{
		if (language.equals(AbstractReader.LANG_EN))
			return new CEnglishMPAnalyzer(zin);
		
		return new CDefaultMPAnalyzer();
	}
}
