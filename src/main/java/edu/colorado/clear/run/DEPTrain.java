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
import java.util.Set;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.dependency.DEPParser;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.feature.xml.DEPFtrXml;
import edu.colorado.clear.reader.DEPReader;
import edu.colorado.clear.util.UTFile;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTXml;

/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPTrain extends AbstractRun
{
	protected final String ENTRY_FEATURE = "FEATURE";
	protected final String ENTRY_MODEL   = "MODEL";
	
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
	
	public DEPTrain() {}
	
	public DEPTrain(String[] args)
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
		Element   eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		DEPReader reader = (DEPReader)getReader(eConfig);
		DEPFtrXml xml = new DEPFtrXml(new FileInputStream(featureXml));
		String[]  trainFiles = UTFile.getSortedFileList(trainDir);
		DEPParser parser;
		int i = 0;
		
		Set<String> sPunc = getLexica(reader, xml, trainFiles, -1);
		parser = getTrainedParser(eConfig, reader, xml, sPunc, trainFiles, null, -1);
		saveModels(modelFile, featureXml, parser);
		
		for (i=1; i<=nBoot; i++)
		{
			parser = getTrainedParser(eConfig, reader, xml, sPunc, trainFiles, parser.getModel(), -1);
			saveModels(modelFile+"."+i, featureXml, parser);
		}
	}
	
	public void saveModels(String modelFile, String featureXml, DEPParser parser) throws Exception
	{
		JarArchiveOutputStream zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
		PrintStream fout;
		
		zout.putArchiveEntry(new JarArchiveEntry(ENTRY_FEATURE));
		IOUtils.copy(new FileInputStream(featureXml), zout);
		zout.closeArchiveEntry();
		
		zout.putArchiveEntry(new JarArchiveEntry(ENTRY_MODEL));
		fout = new PrintStream(new BufferedOutputStream(zout));
		parser.saveModel(fout);
		fout.close();
		zout.closeArchiveEntry();
		
		zout.close();
	}
	
	protected Set<String> getLexica(DEPReader reader, DEPFtrXml xml, String[] trainFiles, int devId)
	{
		DEPParser parser = new DEPParser(xml);
		int i, size = trainFiles.length;
		DEPTree tree;
		
		System.out.println("Collecting lexica:");
		
		for (i=0; i<size; i++)
		{
			if (i == devId)	continue;
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			
			while ((tree = reader.next()) != null)
				parser.collectLexica(tree);
			
			System.out.print(".");
			reader.close();
		}
		
		System.out.println();
		return parser.getPunctuationSet();
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	public DEPParser getTrainedParser(Element eConfig, DEPReader reader, DEPFtrXml xml, Set<String> sPunc, String[] trainFiles, StringModel model, int devId) throws Exception
	{
		StringTrainSpace space = new StringTrainSpace(false, xml.getLabelCutoff(0), xml.getFeatureCutoff(0));
		int i, size = trainFiles.length;
		DEPParser parser;
		DEPTree tree;
		
		if (model == null)	parser = new DEPParser(xml, sPunc, space);
		else				parser = new DEPParser(xml, sPunc, model, space); 
		
		System.out.println("Collecting training instances:");
		
		for (i=0; i<size; i++)
		{
			if (devId == i)	continue;
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			
			while ((tree = reader.next()) != null)
				parser.parse(tree);
			
			System.out.print(".");
			reader.close();
		}
		
		System.out.println();
		model = null;
		model = (StringModel)getModel(UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN), space, 0);
		
		return new DEPParser(xml, sPunc, model);
	}
	
	static public void main(String[] args)
	{
		new DEPTrain(args);
	}
}
