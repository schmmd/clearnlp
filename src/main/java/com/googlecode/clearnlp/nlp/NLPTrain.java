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
import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.component.dep.CDEPBackParser;
import com.googlecode.clearnlp.component.dep.CDEPPassParser;
import com.googlecode.clearnlp.component.pos.CPOSBackTagger;
import com.googlecode.clearnlp.component.pos.CPOSTagger;
import com.googlecode.clearnlp.component.srl.CPredIdentifier;
import com.googlecode.clearnlp.component.srl.CRolesetClassifier;
import com.googlecode.clearnlp.component.srl.CSRLabeler;
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
public class NLPTrain extends AbstractNLP
{
	protected final String DELIM_FILES	= ":";
	@Option(name="-c", usage="configuration file (required)", required=true, metaVar="<filename>")
	protected String s_configFile;
	@Option(name="-f", usage="feature template files delimited by '"+DELIM_FILES+"' (required)", required=true, metaVar="<filename>")
	protected String s_featureFiles;
	@Option(name="-i", usage="input directory containing training files (required)", required=true, metaVar="<directory>")
	protected String s_trainDir;
	@Option(name="-m", usage="model file (output; required)", required=true, metaVar="<filename>")
	protected String s_modelFile;
	@Option(name="-n", usage="bootstrapping level (default: 2)", required=false, metaVar="<integer>")
	protected int n_boot = 0;
	@Option(name="-z", usage="mode (pos|morph|dep|pred|role|srl)", required=true, metaVar="<string>")
	protected String s_mode;
	@Option(name="-margin", usage="margin between the 1st and 2nd predictions (default: 0.5)", required=false, metaVar="<double>")
	protected double d_margin = 0.5;
	
	public NLPTrain() {}
	
	public NLPTrain(String[] args)
	{
		initArgs(args);
		
		try
		{
			train(s_configFile, s_featureFiles.split(DELIM_FILES), s_trainDir, s_modelFile, s_mode);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void train(String configFile, String[] featureFiles, String trainDir, String modelFile, String mode) throws Exception
	{
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		JointFtrXml[]  xmls = getFeatureTemplates(featureFiles);
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, ".*", true);
		JointReader  reader = getJointReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));

		AbstractStatisticalComponent component = getComponent(eConfig, reader, xmls, trainFiles, -1, mode);
		component.saveModels(new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(modelFile))));
	}
	
	//	====================================== GETTERS/SETTERS ======================================

	protected AbstractStatisticalComponent getComponent(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, int devId, String mode)
	{
		if      (mode.equals(NLPLib.MODE_POS))
			return getTrainedComponent(eConfig, reader, xmls, trainFiles, new CPOSTagger(xmls, getLowerSimplifiedForms(reader, xmls[0], trainFiles, devId)), mode, devId);
		else if (mode.equals(NLPLib.MODE_DEP))
			return getTrainedComponent(eConfig, reader, xmls, trainFiles, new CDEPPassParser(xmls), mode, devId);
		else if (mode.equals(NLPLib.MODE_PRED))
			return getTrainedComponent(eConfig, xmls, trainFiles, null, null, mode, 0, devId);
		else if (mode.equals(NLPLib.MODE_ROLE))
			return getTrainedComponent(eConfig, reader, xmls, trainFiles, new CRolesetClassifier(xmls), mode, devId);
		else if (mode.equals(NLPLib.MODE_SRL))
			return getTrainedComponent(eConfig, reader, xmls, trainFiles, new CSRLabeler(xmls), mode, devId);
		else if (mode.equals(NLPLib.MODE_POS_BACK))
			return getTrainedComponent(eConfig, reader, xmls, trainFiles, new CPOSTagger(xmls, getLowerSimplifiedForms(reader, xmls[0], trainFiles, devId)), mode, devId);
		else if (mode.equals(NLPLib.MODE_DEP_BACK))
			return getTrainedComponent(eConfig, reader, xmls, trainFiles, new CDEPBackParser(xmls), mode, devId);
		
		throw new IllegalArgumentException("The requested mode '"+mode+"' is not supported.");
	}
	
	/** @return a component for developing. */
	protected AbstractStatisticalComponent getComponent(JointFtrXml[] xmls, StringModel[] models, Object[] lexica, String mode)
	{
		if      (mode.equals(NLPLib.MODE_POS))
			return new CPOSTagger(xmls, models, lexica);
		else if (mode.equals(NLPLib.MODE_DEP))
			return new CDEPPassParser(xmls, models, lexica);
		else if (mode.equals(NLPLib.MODE_PRED))
			return new CPredIdentifier(xmls, models, lexica);
		else if (mode.equals(NLPLib.MODE_ROLE))
			return new CRolesetClassifier(xmls, models, lexica);
		else if (mode.equals(NLPLib.MODE_SRL))
			return new CSRLabeler(xmls, models, lexica);
		else if (mode.equals(NLPLib.MODE_POS_BACK))
			return new CPOSBackTagger(xmls, models, lexica, d_margin);
		else if (mode.equals(NLPLib.MODE_DEP_BACK))
			return new CDEPBackParser(xmls, models, lexica, d_margin);
		
		throw new IllegalArgumentException("The requested mode '"+mode+"' is not supported.");
	}
	
	protected AbstractStatisticalComponent getTrainedComponent(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, AbstractStatisticalComponent component, String mode, int devId) 
	{
		Object[] lexica = getLexica(component, reader, xmls, trainFiles, devId);
		AbstractStatisticalComponent processor = null;
		StringModel[] models = null;
		int boot;
		
		for (boot=0; boot<=n_boot; boot++)
		{
			processor = getTrainedComponent(eConfig, xmls, trainFiles, models, lexica, mode, boot, devId);
			models = processor.getModels();
		}
		
		return processor;
	}
	
	protected JointFtrXml[] getFeatureTemplates(String[] featureFiles) throws Exception
	{
		int i, size = featureFiles.length;
		JointFtrXml[] xmls = new JointFtrXml[size];
		
		for (i=0; i<size; i++)
			xmls[i] = new JointFtrXml(new FileInputStream(featureFiles[i]));
		
		return xmls;
	}
	
	protected Object[] getLexica(AbstractStatisticalComponent component, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, int devId)
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
		}
		
		System.out.println();
		return component.getLexica();
	}
	
	//	====================================== PART-OF-SPEECH TAGGING ======================================
	
	/** Called by {@link NLPTrain#trainPOSTagger(Element, JointFtrXml[], String[], JointReader)}. */
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
	
	//	====================================== TRAINING ======================================
	
	/** Called by {@link NLPTrain#getTrainedJointPD(String, String[], String, String)}. */
	protected AbstractStatisticalComponent getTrainedComponent(Element eConfig, JointFtrXml[] xmls, String[] trainFiles, StringModel[] models, Object[] lexica, String mode, int boot, int devId)
	{
		StringTrainSpace[] spaces = getStringTrainSpaces(eConfig, xmls, trainFiles, models, lexica, mode, boot, devId);
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		
		int i, mSize = spaces.length;
		models = new StringModel[mSize];
		
		for (i=0; i<mSize; i++)
		{
			if (mode.equals(NLPLib.MODE_ROLE))
				models[i] = (StringModel)getModel(eTrain, spaces[i], 0, boot);
			else
				models[i] = (StringModel)getModel(eTrain, spaces[i], i, boot);

			spaces[i].clear();
		}
		
		return getComponent(xmls, models, lexica, mode);
	}
	
	protected StringTrainSpace[] getStringTrainSpaces(Element eConfig, JointFtrXml[] xmls, String[] trainFiles, StringModel[] models, Object[] lexica, String mode, int boot, int devId)
	{
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		int i, j, mSize = 1, size = trainFiles.length;
		int numThreads = getNumOfThreads(eTrain);
		
		List<StringTrainSpace[]> lSpaces = new ArrayList<StringTrainSpace[]>();
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		StringTrainSpace[] spaces;
		
		System.out.println("Collecting training instances:");
		
		for (i=0; i<size; i++)
		{
			if (devId != i)
			{
				lSpaces.add(spaces = getStringTrainSpaces(xmls, lexica, mode, boot));
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
				
				System.out.println();
			}
		}
		
		return spaces;
	}
	
	protected AbstractStatisticalComponent getComponent(JointFtrXml[] xmls, StringTrainSpace[] spaces, StringModel[] models, Object[] lexica, String mode)
	{
		if      (mode.equals(NLPLib.MODE_POS))
			return new CPOSTagger(xmls, spaces, lexica);
		else if (mode.equals(NLPLib.MODE_DEP))
			return (models == null) ? new CDEPPassParser(xmls, spaces, lexica) : new CDEPPassParser(xmls, spaces, models, lexica);
		else if (mode.equals(NLPLib.MODE_PRED))
			return new CPredIdentifier(xmls, spaces, lexica);	
		else if (mode.equals(NLPLib.MODE_ROLE))
			return new CRolesetClassifier(xmls, spaces, lexica);
		else if (mode.equals(NLPLib.MODE_SRL))
			return (models == null) ? new CSRLabeler(xmls, spaces, lexica) : new CSRLabeler(xmls, spaces, models, lexica);
		else if (mode.equals(NLPLib.MODE_POS_BACK))
			return (models == null) ? new CPOSBackTagger(xmls, spaces, lexica, d_margin) : new CPOSBackTagger(xmls, spaces, models, lexica, d_margin);
		else if (mode.equals(NLPLib.MODE_DEP_BACK))
			return (models == null) ? new CDEPBackParser(xmls, spaces, lexica, d_margin) : new CDEPBackParser(xmls, spaces, models, lexica, d_margin);
		
		throw new IllegalArgumentException("The requested mode '"+mode+"' is not supported.");
	}
	
	@SuppressWarnings("unchecked")
	/** Called by {@link COMTrain#getStringTrainSpaces(Element, JointFtrXml[], String[], StringModel[], Object[], String, int)}. */
	protected StringTrainSpace[] getStringTrainSpaces(JointFtrXml[] xmls, Object[] lexica, String mode, int boot)
	{
		if      (mode.equals(NLPLib.MODE_ROLE))
			return getStringTrainSpaces(xmls[0], ((ObjectIntOpenHashMap<String>)lexica[1]).size());
		else if (mode.equals(NLPLib.MODE_SRL))
			return getStringTrainSpaces(xmls[0], 2);
		else
			return getStringTrainSpaces(xmls);
	}
	
	/** Called by {@link NLPTrain#getStringTrainSpaces(JointFtrXml[], Object[], String)}. */
	private StringTrainSpace[] getStringTrainSpaces(JointFtrXml[] xmls)
	{
		int i, size = xmls.length;
		StringTrainSpace[] spaces = new StringTrainSpace[size];
		
		for (i=0; i<size; i++)
			spaces[i] = new StringTrainSpace(false, xmls[i].getLabelCutoff(0), xmls[i].getFeatureCutoff(0));
		
		return spaces;
	}
	
	/** Called by {@link NLPTrain#getStringTrainSpaces(JointFtrXml[], Object[], String)}. */
	private StringTrainSpace[] getStringTrainSpaces(JointFtrXml xml, int size)
	{
		StringTrainSpace[] spaces = new StringTrainSpace[size];
		int i;
		
		for (i=0; i<size; i++)
			spaces[i] = new StringTrainSpace(false, xml.getLabelCutoff(0), xml.getFeatureCutoff(0));
		
		return spaces;
	}
	
	/** Called by {@link NLPTrain#getStringTrainSpaces(Element, JointFtrXml[], String[], StringModel[], Object[], String, int)}. */
	private class TrainTask implements Runnable
	{
		AbstractStatisticalComponent j_component;
		JointReader j_reader;
		
		public TrainTask(Element eConfig, String trainFile, AbstractStatisticalComponent component)
		{
			j_reader = getJointReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
			j_reader.open(UTInput.createBufferedFileReader(trainFile));
			j_component = component;
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
		new NLPTrain(args);
	}
}
