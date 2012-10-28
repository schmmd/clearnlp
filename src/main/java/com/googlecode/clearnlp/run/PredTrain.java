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
package com.googlecode.clearnlp.run;

import java.io.FileInputStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineSetter;
import com.googlecode.clearnlp.feature.xml.SRLFtrXml;
import com.googlecode.clearnlp.predicate.PredIdentifier;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;

/**
 * @since 1.2.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class PredTrain extends AbstractRun
{
	@Option(name="-i", usage="the directory containg training files (required)", required=true, metaVar="<directory>")
	protected String s_trainDir;
	@Option(name="-c", usage="the configuration file (required)", required=true, metaVar="<filename>")
	protected String s_configXml;
	@Option(name="-f", usage="the feature file for predicate identification (required)", required=true, metaVar="<filename>")
	protected String s_featureXml;
	@Option(name="-m", usage="the model file (output; required)", required=true, metaVar="<filename>")
	protected String s_modelFile;
	
	public PredTrain() {}
	
	public PredTrain(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_featureXml, s_trainDir, s_modelFile);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void run(String configXml, String featureXml, String trainDir, String modelFile) throws Exception
	{
		Element   eConfig    = UTXml.getDocumentElement(new FileInputStream(configXml));
		DEPReader reader     = (DEPReader)getReader(eConfig).o1;
		SRLFtrXml xml        = new SRLFtrXml(new FileInputStream(featureXml));
		String[]  trainFiles = UTFile.getSortedFileList(trainDir);
		
		PredIdentifier identifier = getTrainedIdentifier(eConfig, reader, xml, trainFiles, -1);
		EngineSetter.setPredIdentifier(modelFile, featureXml, identifier);
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	public PredIdentifier getTrainedIdentifier(Element eConfig, DEPReader reader, SRLFtrXml xml, String[] trainFiles, int devId) throws Exception
	{
		StringTrainSpace space = new StringTrainSpace(false, xml.getLabelCutoff(0), xml.getFeatureCutoff(0));
		PredIdentifier identifier = new PredIdentifier(xml, space);
		int i, size = trainFiles.length;
		DEPTree tree;
		
		System.out.println("Collecting training instances:");
		
		for (i=0; i<size; i++)
		{
			if (devId == i)	continue;
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			
			while ((tree = reader.next()) != null)
				identifier.identify(tree);
			
			System.out.print(".");
			reader.close();
		}
		
		System.out.println();
		StringModel model = (StringModel)getModel(UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN), space, 0);
		
		return new PredIdentifier(xml, model);
	}
	
	static public void main(String[] args)
	{
		new PredTrain(args);
	}
}
