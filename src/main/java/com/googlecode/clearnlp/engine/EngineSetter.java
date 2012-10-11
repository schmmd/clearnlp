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

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.googlecode.clearnlp.dependency.srl.SRLParser;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.run.POSTrain;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class EngineSetter
{
	// ============================= setters: general =============================

	static public void saveModel(String modelFile, String featureXml, AbstractEngine engine) throws Exception
	{
		JarArchiveOutputStream zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
		PrintStream fout;
		
		zout.putArchiveEntry(new JarArchiveEntry(EngineGetter.ENTRY_FEATURE));
		IOUtils.copy(new FileInputStream(featureXml), zout);
		zout.closeArchiveEntry();
		
		zout.putArchiveEntry(new JarArchiveEntry(EngineGetter.ENTRY_MODEL));
		fout = new PrintStream(new BufferedOutputStream(zout));
		engine.saveModel(fout);
		fout.close();
		zout.closeArchiveEntry();
		
		zout.close();
	}
	
	// ============================= setter: part-of-speech tagger =============================

	static public void setPOSTaggers(String modelFile, String featureXml, POSTagger[] taggers, double threshold) throws Exception
	{
		JarArchiveOutputStream zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
		PrintStream fout;
		
		zout.putArchiveEntry(new JarArchiveEntry(EngineGetter.ENTRY_CONFIGURATION));
		fout = new PrintStream(zout);
		fout.println(threshold);
		fout.close();
		zout.closeArchiveEntry();
		
		zout.putArchiveEntry(new JarArchiveEntry(EngineGetter.ENTRY_FEATURE));
		IOUtils.copy(new FileInputStream(featureXml), zout);
		zout.closeArchiveEntry();
		
		int modId;
		
		for (modId=0; modId<POSTrain.MODEL_SIZE; modId++)
		{
			zout.putArchiveEntry(new JarArchiveEntry(EngineGetter.ENTRY_MODEL+modId));
			fout = new PrintStream(new BufferedOutputStream(zout));
			taggers[modId].saveModel(fout);
			fout.close();
			zout.closeArchiveEntry();			
		}
		
		zout.close();
	}
	
	// ============================= setter: semantic role labeler =============================

	static public void setSRLabeler(String modelFile, String featureXml, SRLParser parser) throws Exception
	{
		JarArchiveOutputStream zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
		PrintStream fout;
		int modId;
		
		zout.putArchiveEntry(new JarArchiveEntry(EngineGetter.ENTRY_FEATURE));
		IOUtils.copy(new FileInputStream(featureXml), zout);
		zout.closeArchiveEntry();
		
		zout.putArchiveEntry(new JarArchiveEntry(EngineGetter.ENTRY_SET_DOWN));
		fout = new PrintStream(new BufferedOutputStream(zout));
		parser.saveDownSet(fout);
		fout.close();
		zout.closeArchiveEntry();
		
		zout.putArchiveEntry(new JarArchiveEntry(EngineGetter.ENTRY_SET_UP));
		fout = new PrintStream(new BufferedOutputStream(zout));
		parser.saveUpSet(fout);
		fout.close();
		zout.closeArchiveEntry();
		
		for (modId=0; modId<SRLParser.MODEL_SIZE; modId++)
		{
			zout.putArchiveEntry(new JarArchiveEntry(EngineGetter.ENTRY_MODEL+modId));
			fout = new PrintStream(new BufferedOutputStream(zout));
			parser.saveModel(fout, modId);
			fout.close();
			zout.closeArchiveEntry();			
		}
		
		zout.close();
	}
}
