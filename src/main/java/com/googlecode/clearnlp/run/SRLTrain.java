/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package com.googlecode.clearnlp.run;

import java.io.FileInputStream;
import java.util.Set;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLabeler;
import com.googlecode.clearnlp.engine.EngineSetter;
import com.googlecode.clearnlp.feature.xml.SRLFtrXml;
import com.googlecode.clearnlp.reader.SRLReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.Pair;


/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class SRLTrain extends AbstractRun
{
	@Option(name="-i", usage="the directory containg training files (input; required)", required=true, metaVar="<directory>")
	protected String s_trainDir;
	@Option(name="-c", usage="the configuration file (input; required)", required=true, metaVar="<filename>")
	protected String s_configXml;
	@Option(name="-f", usage="the feature file (input; required)", required=true, metaVar="<filename>")
	protected String s_featureXml;
	@Option(name="-m", usage="the model file (output; required)", required=true, metaVar="<filename>")
	protected String s_modelFile;
	@Option(name="-n", usage="the bootstrapping level (default: 2)", required=false, metaVar="<integer>")
	protected int n_boot = 2;
	
	public SRLTrain() {}
	
	public SRLTrain(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_featureXml, s_trainDir, s_modelFile, n_boot);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void run(String configXml, String featureXml, String trainDir, String modelFile, int nBoot) throws Exception
	{
		Element   eConfig    = UTXml.getDocumentElement(new FileInputStream(configXml));
		SRLReader reader     = (SRLReader)getReader(eConfig).o1;
		SRLFtrXml xml        = new SRLFtrXml(new FileInputStream(featureXml));
		String[]  trainFiles = UTFile.getSortedFileList(trainDir);
		SRLabeler parser;
		
		Pair<Set<String>,Set<String>> p = getDownUpSets(reader, xml, trainFiles, -1);
		int i;
		
		parser = getTrainedParser(eConfig, reader, xml, trainFiles, null, p.o1, p.o2, -1);
		EngineSetter.setSRLabeler(modelFile, featureXml, parser);
		
		for (i=1; i<=nBoot; i++)
		{
			parser = getTrainedParser(eConfig, reader, xml, trainFiles, parser.getModels(), p.o1, p.o2, -1);
			EngineSetter.setSRLabeler(modelFile+"."+i, featureXml, parser);
		}
	}
	
	public Pair<Set<String>,Set<String>> getDownUpSets(SRLReader reader, SRLFtrXml xml, String[] trainFiles, int devId)
	{
		SRLabeler parser = new SRLabeler();
		int i, size = trainFiles.length;
		DEPTree tree;
		
		System.out.println("Collecting lexica:");
		
		for (i=0; i<size; i++)
		{
			if (devId == i)	continue;
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			
			while ((tree = reader.next()) != null)
				parser.label(tree);
			
			System.out.print(".");
			reader.close();
		}	System.out.println();
		
		Set<String> sDown = parser.getDownSet(xml.getDownCutoff());
		Set<String> sUp   = parser.getUpSet  (xml.getUpCutoff());
		System.out.printf("- down-paths: size = %d, cutoff = %d\n", sDown.size(), xml.getDownCutoff());
		System.out.printf("- up-paths  : size = %d, cutoff = %d\n", sUp  .size(), xml.getUpCutoff());
		
		return new Pair<Set<String>, Set<String>>(sDown, sUp);
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	public SRLabeler getTrainedParser(Element eConfig, SRLReader reader, SRLFtrXml xml, String[] trainFiles, StringModel[] models, Set<String> sDown, Set<String> sUp, int devId) throws Exception
	{
		StringTrainSpace[] spaces = new StringTrainSpace[SRLabeler.MODEL_SIZE];
		int i, size = trainFiles.length;
		SRLabeler parser;
		DEPTree tree;
		
		for (i=0; i<spaces.length; i++)
			spaces[i] = new StringTrainSpace(false, xml.getLabelCutoff(0), xml.getFeatureCutoff(0));
		
		if (models == null)	parser = new SRLabeler(xml, spaces, sDown, sUp);
		else				parser = new SRLabeler(xml, models, spaces, sDown, sUp); 
		
		System.out.println("Collecting training instances:");
		
		for (i=0; i<size; i++)
		{
			if (devId == i)	continue;
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			
			while ((tree = reader.next()) != null)
				parser.label(tree);
			
			System.out.print(".");
			reader.close();
		}
		
		System.out.println();
		models = new StringModel[spaces.length];
		
		for (i=0; i<models.length; i++)
			models[i] = (StringModel)getModel(UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN), spaces[i], i);
		
		return new SRLabeler(xml, models, sDown, sUp);
	}
	
	static public void main(String[] args)
	{
		new SRLTrain(args);
	}
}
