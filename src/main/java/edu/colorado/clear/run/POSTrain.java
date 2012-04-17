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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.cursors.ObjectCursor;

import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.feature.xml.POSFtrXml;
import edu.colorado.clear.morphology.MPLib;
import edu.colorado.clear.pos.POSLib;
import edu.colorado.clear.pos.POSNode;
import edu.colorado.clear.pos.POSTagger;
import edu.colorado.clear.reader.POSReader;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTXml;
import edu.colorado.clear.util.list.SortedDoubleArrayList;
import edu.colorado.clear.util.map.Prob1DMap;
import edu.colorado.clear.util.pair.Pair;

/**
 * Trains a part-of-speech tagging model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class POSTrain extends AbstractRun
{
	static final int    MODEL_SIZE      = 2;
	static final String ENTRY_THRESHOLD = "THRESHOLD";
	
	@Option(name="-i", usage="the directory containg training files (input; required)", required=true, metaVar="<directory>")
	protected String s_trainDir;
	@Option(name="-c", usage="the configuration file (input; required)", required=true, metaVar="<filename>")
	protected String s_configXml;
	@Option(name="-f", usage="the feature file (input; required)", required=true, metaVar="<filename>")
	protected String s_featureXml;
	@Option(name="-m", usage="the model file (output; required)", required=true, metaVar="<filename>")
	protected String s_modelFile;
	@Option(name="-t", usage="the similarity threshold (default: -1)", required=false, metaVar="<double>")
	protected double d_threshold = -1;
	
	public POSTrain() {}
	
	public POSTrain(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_featureXml, s_trainDir, s_modelFile, d_threshold);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void run(String configXml, String featureXml, String trainDir, String modelFile, double threshold) throws Exception
	{
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		POSReader    reader = (POSReader)getReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
		POSFtrXml       xml = new POSFtrXml(new FileInputStream(featureXml));
		String[] trainFiles = getSortedFileList(trainDir);
		
		if (threshold < 0)
			threshold = crossValidate(trainFiles, reader, xml, eConfig);
		
		POSTagger[] taggers = getTrainedTaggers(eConfig, reader, xml, trainFiles, null);
		saveModels(modelFile, featureXml, taggers, threshold);
	}
	
	public POSTagger[] getTrainedTaggers(Element eConfig, POSReader reader, POSFtrXml xml, String[] trnFiles, IntOpenHashSet sDev) throws Exception
	{
		POSTagger[] taggers = new POSTagger[MODEL_SIZE];
		Pair<Set<String>, Map<String,String>> p;
		StringTrainSpace space;
		Set<String> sLemmas;
		StringModel model;
		int modId;
		
		for (modId=0; modId<MODEL_SIZE; modId++)
		{
			System.out.printf("===== Training model %d =====\n", modId);

			sLemmas = getLemmaSet  (reader, xml, modId, trnFiles, sDev);
			p       = getLexica    (reader, xml, modId, sLemmas, trnFiles, sDev);
			space   = getTrainSpace(reader, xml, modId, sLemmas, p.o1, p.o2, trnFiles, sDev); 
			model   = (StringModel)getModel(UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN), space, modId);
			
			taggers[modId] = new POSTagger(xml, sLemmas, p.o1, p.o2, model);
		}
		
		return taggers;
	}
	
	public void saveModels(String modelFile, String featureXml, POSTagger[] taggers, double threshold) throws Exception
	{
		JarArchiveOutputStream zout = new JarArchiveOutputStream(new FileOutputStream(modelFile));
		PrintStream fout;
		
		zout.putArchiveEntry(new JarArchiveEntry(ENTRY_THRESHOLD));
		fout = new PrintStream(zout);
		fout.println(threshold);
		fout.close();
		zout.closeArchiveEntry();
		
		zout.putArchiveEntry(new JarArchiveEntry(ENTRY_FEATURE));
		IOUtils.copy(new FileInputStream(featureXml), zout);
		zout.closeArchiveEntry();
		
		for (int i=0; i<MODEL_SIZE; i++)
		{
			zout.putArchiveEntry(new JarArchiveEntry(ENTRY_MODEL+i));
			fout = new PrintStream(new BufferedOutputStream(zout));
			taggers[i].saveModel(fout);
			fout.close();
			zout.closeArchiveEntry();
		}
		
		zout.close();
	}

	/** Called by {@link POSTrain#getTrainedTaggers(Element, POSReader, POSFtrXml, String[], IntOpenHashSet)}. */
	private Set<String> getLemmaSet(POSReader reader, POSFtrXml xml, int modId, String[] trnFiles, IntOpenHashSet sDev) throws Exception
	{
		int dfCutoff = xml.getDocumentFrequency(modId);
		Prob1DMap map = new Prob1DMap();
		int i, size = trnFiles.length;
		Set<String> set;
		POSNode[] nodes;
		String    lemma;
		
		System.out.println("Collecting n-gram set:");
		System.out.println("- document frequency cutoff: "+dfCutoff);
		
		for (i=0; i<size; i++)
		{
			if (sDev != null && sDev.contains(i))	continue;
			reader.open(UTInput.createBufferedFileReader(trnFiles[i]));
			set = new HashSet<String>();
			
			while ((nodes = reader.next()) != null)
			{
				MPLib.normalizeForms(nodes);
				
				for (POSNode node : nodes)
					set.add(node.lemma);
			}
			
			reader.close();
			for (String s : set)	map.add(s);
		}
		
		set = new HashSet<String>();
		
		for (ObjectCursor<String> cur : map.keys())
		{
			lemma = cur.value;
			
			if (map.get(lemma) > dfCutoff)
				set.add(lemma);
		}
		
		System.out.printf("- lemma reduction: %d -> %d\n", map.size(), set.size());
		return set;
	}
	
	/** Called by {@link POSTrain#getTrainedTaggers(Element, POSReader, POSFtrXml, String[], IntOpenHashSet)}. */
	private Pair<Set<String>,Map<String,String>> getLexica(POSReader reader, POSFtrXml xml, int xmlId, Set<String> sLemmas, String[] trnFiles, IntOpenHashSet sDev)
	{
		POSTagger tagger = new POSTagger(sLemmas);
		int i, size = trnFiles.length;
		POSNode[] nodes;
		int featureCutoff = xml.getFeatureCutoff(xmlId);
		double ambiguityThreshold = xml.getAmbiguityThreshold(xmlId);
		
		System.out.println("Collecting lexica:");
		System.out.println("- lexica cutoff: "+featureCutoff);
		System.out.println("- ambiguity class threshold: "+ambiguityThreshold);
		
		for (i=0; i<size; i++)
		{
			if (sDev != null && sDev.contains(i))	continue;
			reader.open(UTInput.createBufferedFileReader(trnFiles[i]));
			
			while ((nodes = reader.next()) != null)
				tagger.tag(nodes);
			
			reader.close();
		}
		
		Set<String> sForms = tagger.getFormSet(featureCutoff);
		Map<String,String> mAmbi = tagger.getAmbiguityMap(ambiguityThreshold);
		System.out.println("- # of word-forms: "+sForms.size());
		System.out.println("- # of word-forms with ambiguity classes: "+mAmbi.size());
		
		return new Pair<Set<String>,Map<String,String>>(sForms, mAmbi);
	}
	
	/** Called by {@link POSTrain#getTrainedTaggers(Element, POSReader, POSFtrXml, String[], IntOpenHashSet)}. */
	private StringTrainSpace getTrainSpace(POSReader reader, POSFtrXml xml, int modId, Set<String> sLemmas, Set<String> sForms, Map<String, String> ambiguityMap, String[] trnFiles, IntOpenHashSet sDev)
	{
		StringTrainSpace space = new StringTrainSpace(false, xml.getLabelCutoff(modId), xml.getFeatureCutoff(modId));
		POSTagger tagger = new POSTagger(xml, sLemmas, sForms, ambiguityMap, space);
		int i, size = trnFiles.length;
		POSNode[] nodes;
		
		System.out.println("Collecting training instances:");
		
		for (i=0; i<size; i++)
		{
			if (sDev != null && sDev.contains(i))	continue;
			reader.open(UTInput.createBufferedFileReader(trnFiles[i]));
			
			while ((nodes = reader.next()) != null)
				tagger.tag(nodes);
			
			reader.close();
			System.out.print(".");
		}
		
		System.out.println();
		return space;
	}
	
	public double crossValidate(String[] trnFiles, POSReader reader, POSFtrXml xml, Element eConfig) throws Exception
	{
		SortedDoubleArrayList list = new SortedDoubleArrayList();
		int devId, size = trnFiles.length;
		POSTagger[] taggers;
		IntOpenHashSet sDev = new IntOpenHashSet();
		
		for (devId=0; devId<size; devId++)
		{
			System.out.printf("<== Cross validation %d ==>\n", devId);
			sDev.clear();	sDev.add(devId);
			
			taggers = getTrainedTaggers(eConfig, reader, xml, trnFiles, sDev);
			crossValidatePredict(trnFiles[devId], reader, taggers, list);
		}
		
		int n = (int)Math.round(list.size() * 0.05);
		double threshold = (double)Math.ceil(list.get(n)*1000)/1000;
		
		System.out.println("Out-of-domain validation:");
		System.out.println("- threshold: "+threshold);
	
		return threshold;
	}
	
	/** Called by {@link POSTrain#crossValidate(String[], POSReader, POSFtrXml, Element)}. */
	private void crossValidatePredict(String devFile, POSReader reader, POSTagger[] taggers, SortedDoubleArrayList list)
	{
		int[] local   = new int[MODEL_SIZE];
		int[] correct = new int[MODEL_SIZE];
		POSNode[] nodes;
		String[]  gold;
		int modId, total = 0;
		
		System.out.println("Predicting: "+devFile);
		reader.open(UTInput.createBufferedFileReader(devFile));
		
		while ((nodes = reader.next()) != null)
		{
			gold = POSLib.getLabels(nodes);
			total += gold.length;
			
			for (modId=0; modId<MODEL_SIZE; modId++)
			{
				taggers[modId].tag(nodes);
				local[modId] = countCorrect(nodes, gold);
				correct[modId] += local[modId];
			}
			
			if (local[0] > local[1])
				list.add(taggers[0].getCosineSimilarity(nodes));
		}
		
		reader.close();
		
		double accuracy;
		
		for (modId=0; modId<MODEL_SIZE; modId++)
		{
			accuracy = 100d * correct[modId] / total;
			System.out.printf("- accuracy %d: %7.5f (%d/%d)\n", modId, accuracy, correct[modId], total);
		}
	}
	
	/** Called by {@link POSTrain#crossValidatePredict(String, POSReader, POSTagger[], SortedDoubleArrayList)}. */
	private int countCorrect(POSNode[] nodes, String[] gold)
	{
		int i, correct = 0, n = nodes.length;
		
		for (i=0; i<n; i++)
		{
			if (gold[i].equals(nodes[i].pos))
				correct++;
		}
		
		return correct;
	}
		
	static public void main(String[] args)
	{
		new POSTrain(args);
	}
}
