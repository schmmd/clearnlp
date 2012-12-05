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

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.component.CDEPPassParser;
import com.googlecode.clearnlp.component.CPOSTagger;
import com.googlecode.clearnlp.component.CRolesetClassifier;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.map.Prob1DMap;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class COMTrain extends AbstractBin
{
	protected final String DELIM_FILES	= ":";
	@Option(name="-c", usage="configuration file (required)", required=true, metaVar="<filename>")
	protected String s_configFile;
	@Option(name="-f", usage="feature template files delimited by "+DELIM_FILES+" (required)", required=true, metaVar="<filename>")
	protected String s_featureFiles;
	@Option(name="-i", usage="input directory containing training files (required)", required=true, metaVar="<directory>")
	protected String s_trainDir;
	@Option(name="-m", usage="model file (output; required)", required=true, metaVar="<filename>")
	protected String s_modelFile;
	@Option(name="-n", usage="bootstrapping level (default: 2)", required=false, metaVar="<integer>")
	protected int n_boot = 2;
	
	public COMTrain() {}
	
	public COMTrain(String[] args)
	{
		initArgs(args);
		
		try
		{
			train(s_configFile, s_featureFiles.split(DELIM_FILES), s_trainDir, s_modelFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void train(String configFile, String[] featureFiles, String trainDir, String modelFile) throws Exception
	{
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		JointFtrXml[]  xmls = getFeatureTemplates(featureFiles);
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, ".*", true);
		JointReader  reader = getJointReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
		String         mode = getMode(eConfig);
		AbstractComponent component = null;
		
		if      (mode.equals(COMLib.MODE_POS))
			component = getTrainedPOSTagger(eConfig, reader, xmls, trainFiles, -1);
		else if (mode.equals(COMLib.MODE_DEP_PASS))
			component = getTrainedDEPPassParser(eConfig, reader, xmls, trainFiles, -1);
		else if (mode.equals(COMLib.MODE_ROLESET))
			component = getTrainedRolesetClassifier(eConfig, reader, xmls, trainFiles, -1);
		
		component.saveModels(new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(modelFile))));
	}
	
//	====================================== GETTERS/SETTERS ======================================
	
	/** Called by {@link COMTrain#getTrainedComponent(Element, JointFtrXml[], String[], StringModel[], Object[], String, int)}. */
	protected AbstractComponent getComponent(JointFtrXml[] xmls, StringTrainSpace[] spaces, StringModel[] models, Object[] lexica, String mode)
	{
		if      (mode.equals(COMLib.MODE_POS))
			return (models == null) ? new CPOSTagger(xmls, spaces, lexica) : new CPOSTagger(xmls, spaces, models, lexica);
		else if (mode.equals(COMLib.MODE_DEP_PASS))
			return (models == null) ? new CDEPPassParser(xmls, spaces, lexica) : new CDEPPassParser(xmls, spaces, models, lexica);
		else if (mode.equals(COMLib.MODE_ROLESET))
			return new CRolesetClassifier(xmls, spaces, lexica);
		
		return null;
	}
	
	/** Called by {@link COMTrain#getTrainedComponent(Element, JointFtrXml[], String[], StringModel[], Object[], String, int)}. */
	protected AbstractComponent getComponent(JointFtrXml[] xmls, StringModel[] models, Object[] lexica, String mode)
	{
		if      (mode.equals(COMLib.MODE_POS))
			return new CPOSTagger(xmls, models, lexica);
		else if (mode.equals(COMLib.MODE_DEP_PASS))
			return new CDEPPassParser(xmls, models, lexica);
		else if (mode.equals(COMLib.MODE_ROLESET))
			return new CRolesetClassifier(xmls, models, lexica);
		
		return null;
	}
	
	protected JointFtrXml[] getFeatureTemplates(String[] featureFiles) throws Exception
	{
		int i, size = featureFiles.length;
		JointFtrXml[] xmls = new JointFtrXml[size];
		
		for (i=0; i<size; i++)
			xmls[i] = new JointFtrXml(new FileInputStream(featureFiles[i]));
		
		return xmls;
	}
	
	protected Object[] getLexica(AbstractComponent component, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, int devId)
	{
		int i, size = trainFiles.length;
		DEPTree tree;
		
		System.out.println("Collecting lexica:");
		
		for (i=0; i<size; i++)
		{
			if (devId == i)	continue;
			
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			
			while ((tree = reader.next()) != null)
				component.process(tree);
			
			reader.close();
			
			System.out.print(".");
		}	System.out.println();
		
		return component.getLexica();
	}
	
//	====================================== PART-OF-SPEECH TAGGING ======================================
	
	protected CPOSTagger getTrainedPOSTagger(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, int devId) 
	{
		Set<String> sLsfs = getLowerSimplifiedForms(reader, xmls[0], trainFiles, devId);
		Object[]   lexica = getLexica(new CPOSTagger(xmls, sLsfs), reader, xmls, trainFiles, devId);
		
		return (CPOSTagger)getTrainedComponent(eConfig, xmls, trainFiles, null, lexica, COMLib.MODE_POS, devId);
	}
	
	/** Called by {@link COMTrain#trainPOSTagger(Element, JointFtrXml[], String[], JointReader)}. */
	protected Set<String> getLowerSimplifiedForms(JointReader reader, JointFtrXml xml, String[] trainFiles, int devId)
	{
		Set<String> set = new HashSet<String>();
		int i, j, len, size = trainFiles.length;
		Prob1DMap map = new Prob1DMap();
		DEPTree tree;
		
		System.out.println("Collecting word-forms:");
		
		for (i=0; i<size; i++)
		{
			if (devId == i)	continue;
			
			reader.open(UTInput.createBufferedFileReader(trainFiles[i]));
			set.clear();
			
			while ((tree = reader.next()) != null)
			{
				EngineProcess.normalizeForms(tree);
				len = tree.size();
				
				for (j=1; j<len; j++)
					set.add(tree.get(j).lowerSimplifiedForm);
			}
			
			reader.close();
			map.addAll(set);
			System.out.print(".");
		}	System.out.println();
		
		return map.toSet(xml.getDocumentFrequencyCutoff());
	}
	
//	====================================== DEPENDENCY PARSING ======================================

	protected CDEPPassParser getTrainedDEPPassParser(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, int devId) 
	{
		Object[] lexica = getLexica(new CDEPPassParser(xmls), reader, xmls, trainFiles, devId);
		StringModel[] models = null;
		CDEPPassParser parser = null;
		int boot;
		
		for (boot=0; boot<n_boot; boot++)
		{
			parser = (CDEPPassParser)getTrainedComponent(eConfig, xmls, trainFiles, models, lexica, COMLib.MODE_DEP_PASS, devId);
			models = parser.getModels();
		}
		
		return parser;
	}
	
//	====================================== ROLESET CLASSIFICATION ======================================
	
	protected CRolesetClassifier getTrainedRolesetClassifier(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, int devId) 
	{
		Object[] lexica = getLexica(new CRolesetClassifier(xmls), reader, xmls, trainFiles, devId);
		return (CRolesetClassifier)getTrainedComponent(eConfig, xmls, trainFiles, null, lexica, COMLib.MODE_ROLESET, devId);
	}
	
//	====================================== TRAIN ======================================
	
	/** Called by {@link COMTrain#getTrainedJointPD(String, String[], String, String)}. */
	protected AbstractComponent getTrainedComponent(Element eConfig, JointFtrXml[] xmls, String[] trainFiles, StringModel[] models, Object[] lexica, String mode, int devId)
	{
		StringTrainSpace[] spaces = getStringTrainSpaces(eConfig, xmls, trainFiles, models, lexica, mode, devId);
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		
		int i, mSize = spaces.length;
		models = new StringModel[mSize];
		
		for (i=0; i<mSize; i++)
		{
			if (mode.equals(COMLib.MODE_ROLESET))
				models[i] = (StringModel)getModel(eTrain, spaces[i], 0);
			else
				models[i] = (StringModel)getModel(eTrain, spaces[i], i);

			spaces[i].clear();
		}
		
		return getComponent(xmls, models, lexica, mode);
	}
	
	@SuppressWarnings("unchecked")
	protected StringTrainSpace[] getStringTrainSpaces(Element eConfig, JointFtrXml[] xmls, String[] trainFiles, StringModel[] models, Object[] lexica, String mode, int devId)
	{
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		int numThreads = getNumOfThreads(eTrain);
		int i, j, mSize = 1, size = trainFiles.length;
		
		List<StringTrainSpace[]> lSpaces = new ArrayList<StringTrainSpace[]>();
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		StringTrainSpace[] spaces;
		
		System.out.println("Collecting training instances:");
		
		if (mode.equals(COMLib.MODE_ROLESET))
			mSize = ((ObjectIntOpenHashMap<String>)lexica[1]).size();
		
		for (i=0; i<size; i++)
		{
			if (devId != i)
			{
				if (mode.equals(COMLib.MODE_ROLESET))
					spaces = getTrainSpaces(xmls[0], mSize);
				else
					spaces = getTrainSpaces(xmls);
				
				lSpaces.add(spaces);
				executor.execute(new TrainTask(eConfig, trainFiles[i], getComponent(xmls, spaces, models, lexica, mode)));
			}
		}
		
		executor.shutdown();
		
		try
		{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException e) {e.printStackTrace();}
		
		System.out.println();
		
		mSize = lSpaces.get(0).length;
		spaces = new StringTrainSpace[mSize];
		StringTrainSpace sp;

		for (i=0; i<mSize; i++)
		{
			spaces[i] = lSpaces.get(0)[i];
			
			if ((size = lSpaces.size()) > 1)
			{
				System.out.println("Merging training instances:");
				
				for (j=1; j<size; j++)
				{
					spaces[i].appendSpace(sp = lSpaces.get(j)[i]);
					sp.clear();
					System.out.print(".");
				}				
			}
		}	System.out.println();
		
		return spaces;
	}
	
	protected StringModel[] getModels(Element eTrain, StringTrainSpace[] spaces)
	{
		int i, size = spaces.length;
		StringModel[] models = new StringModel[size];
		
		for (i=0; i<size; i++)
			models[i] = (StringModel)getModel(eTrain, spaces[i], i);
		
		return models;
	}
	
	/** Called by {@link COMTrain#getTrainedComponent(Element, JointFtrXml[], String[], StringModel[], Object[], int)}. */
	protected StringTrainSpace[] getTrainSpaces(JointFtrXml[] xmls)
	{
		int i, size = xmls.length;
		StringTrainSpace[] spaces = new StringTrainSpace[size];
		
		for (i=0; i<size; i++)
			spaces[i] = new StringTrainSpace(false, xmls[i].getLabelCutoff(0), xmls[i].getFeatureCutoff(0));
		
		return spaces;
	}
	
	/** Called by {@link COMTrain#getTrainedComponent(Element, JointFtrXml[], String[], StringModel[], Object[], int)}. */
	protected StringTrainSpace[] getTrainSpaces(JointFtrXml xml, int size)
	{
		StringTrainSpace[] spaces = new StringTrainSpace[size];
		int i;
		
		for (i=0; i<size; i++)
			spaces[i] = new StringTrainSpace(false, xml.getLabelCutoff(0), xml.getFeatureCutoff(0));
		
		return spaces;
	}
	
	/** Called by {@link COMTrain#getTrainedComponent(Element, JointFtrXml[], String[], StringModel[], Object[], int)}. */
	private class TrainTask implements Runnable
	{
		AbstractComponent j_component;
		JointReader       j_reader;
		
		public TrainTask(Element eConfig, String trainFile, AbstractComponent component)
		{
			j_component = component;
			j_reader    = getJointReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
			j_reader.open(UTInput.createBufferedFileReader(trainFile));
		}
		
		public void run()
		{
			DEPTree tree;
			
			while ((tree = j_reader.next()) != null)
				j_component.process(tree);
			
			j_reader.close();
			System.out.print(".");
		}
	}
	
	static public void main(String[] args)
	{
		new COMTrain(args);
	}
}
