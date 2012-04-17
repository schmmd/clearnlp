/**
* Copyright (c) 2011, Regents of the University of Colorado
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
package edu.colorado.clear.run;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.dependency.SRLParser;
import edu.colorado.clear.feature.xml.SRLFtrXml;
import edu.colorado.clear.reader.SRLReader;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTXml;

/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class SRLTrain extends AbstractRun
{
	protected final String ENTRY_FEATURE = "FEATURE";
	protected final String ENTRY_MODEL   = "MODEL";
	
	@Option(name="-i", usage="the directory containg training files (input; required)", required=true, metaVar="<directory>")
	protected String s_trainDir;
	@Option(name="-c", usage="the configuration file (input; required)", required=true, metaVar="<filename>")
	protected String s_configXml;
	@Option(name="-fc", usage="the feature file for argument classification (input; required)", required=true, metaVar="<filename>")
	protected String s_featureACXml;
	@Option(name="-fd", usage="the feature file for down-shift (input; required)", required=true, metaVar="<filename>")
	protected String s_featureDownXml;
	@Option(name="-fu", usage="the feature file for up-shift (input; required)", required=true, metaVar="<filename>")
	protected String s_featureUpXml;
	@Option(name="-m", usage="the model file (output; required)", required=true, metaVar="<filename>")
	protected String s_modelFile;
	@Option(name="-n", usage="the bootstrapping level (default: 2)", required=false, metaVar="<integer>")
	protected int n_boot = 2;
	
	public SRLTrain() {}
	
	public SRLTrain(String[] args)
	{
		initArgs(args);
		
		String[] featureXmls = new String[SRLParser.MODEL_SIZE];
		featureXmls[SRLParser.MODEL_LEFT]  = s_featureACXml;
		featureXmls[SRLParser.MODEL_RIGHT] = s_featureACXml;
		
		try
		{
			run(s_configXml, featureXmls, s_trainDir, s_modelFile, n_boot);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void run(String configXml, String[] featureXmls, String trainDir, String modelFile, int nBoot) throws Exception
	{
		Element   eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		SRLReader reader  = (SRLReader)getReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
		String[]  trainFiles = getSortedFileList(trainDir);
		SRLParser parser;
		
		SRLFtrXml[] xmls = new SRLFtrXml[featureXmls.length];
		int i;
		
		for (i=0; i<xmls.length; i++)
			xmls[i] = new SRLFtrXml(new FileInputStream(featureXmls[i]));

		parser = getTrainedParser(eConfig, reader, xmls, trainFiles, null, -1);
		saveModels(modelFile, featureXmls, parser);
		
		for (i=1; i<=nBoot; i++)
		{
			parser = getTrainedParser(eConfig, reader, xmls, trainFiles, parser.getModels(), -1);
			saveModels(modelFile+"."+i, featureXmls, parser);
		}
	}
	
	public void saveModels(String modelFile, String[] featureXmls, SRLParser parser) throws Exception
	{
		JarArchiveOutputStream zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
		int i, size = featureXmls.length;
		PrintStream fout;
		
		for (i=0; i<size; i++)
		{
			zout.putArchiveEntry(new JarArchiveEntry(ENTRY_FEATURE+"."+i));
			IOUtils.copy(new FileInputStream(featureXmls[i]), zout);
			zout.closeArchiveEntry();
			
			zout.putArchiveEntry(new JarArchiveEntry(ENTRY_MODEL+"."+i));
			fout = new PrintStream(new BufferedOutputStream(zout));
			parser.saveModel(fout, i);
			fout.close();
			zout.closeArchiveEntry();			
		}
		
		zout.close();
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	public SRLParser getTrainedParser(Element eConfig, SRLReader reader, SRLFtrXml[] xmls, String[] trainFiles, StringModel[] models, int devId) throws Exception
	{
		StringTrainSpace[] spaces = new StringTrainSpace[xmls.length];
		int i, size = trainFiles.length;
		SRLParser parser;
		DEPTree tree;
		
		for (i=0; i<spaces.length; i++)
			spaces[i] = new StringTrainSpace(false, xmls[i].getLabelCutoff(0), xmls[i].getFeatureCutoff(0));
		
		if (models == null)	parser = new SRLParser(xmls, spaces);
		else				parser = new SRLParser(xmls, models, spaces); 
		
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
			models[i] = (StringModel)getModel(UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN), spaces[i], 0);
		
		return new SRLParser(xmls, models);
	}
	
	static public void main(String[] args)
	{
		new SRLTrain(args);
	}
}
