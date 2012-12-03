package com.googlecode.clearnlp.bin;

import java.io.FileInputStream;
import java.util.Set;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.carrotsearch.hppc.IntDoubleMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.component.CDEPPassParser;
import com.googlecode.clearnlp.component.CPOSTagger;
import com.googlecode.clearnlp.component.CRolesetClassifier;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLEval;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;

public class COMDevelop extends COMTrain
{
	@Option(name="-d", usage="the directory containing development files (required)", required=true, metaVar="<directory>")
	private String s_devDir;
	
	public COMDevelop(String[] args)
	{
		initArgs(args);
		
		try
		{
			develop(s_configFile, s_featureFiles.split(DELIM_FILES), s_trainDir, s_devDir);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void develop(String configFile, String[] featureFiles, String trainDir, String devDir) throws Exception
	{
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		JointFtrXml[]  xmls = getFeatureTemplates(featureFiles);
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, ".*", true);
		String[]   devFiles = UTFile.getSortedFileListBySize(devDir, ".*", true);
		JointReader  reader = getJointReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
		String         mode = getMode(eConfig);
		
		if      (mode.equals(COMLib.MODE_POS))
			developPOSTagger(eConfig, reader, xmls, trainFiles, devFiles);
		else if (mode.equals(COMLib.MODE_DEP_PASS))
			developDEPParser(eConfig, reader, xmls, trainFiles, devFiles);
		else if (mode.equals(COMLib.MODE_ROLESET))
			developRolesetClassifier(eConfig, reader, xmls, trainFiles, devFiles);
	}
	
	protected void developPOSTagger(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles) throws Exception
	{
		Set<String> sLsfs = getLowerSimplifiedForms(reader, xmls[0], trainFiles, -1);
		Object[]   lexica = getLexica(new CPOSTagger(xmls, sLsfs), reader, xmls, trainFiles, -1);
		String       mode = COMLib.MODE_POS;
		
		StringTrainSpace[] spaces = getStringTrainSpaces(eConfig, xmls, trainFiles, null, lexica, mode, -1);
		Element eTrain = UTXml.getFirstElementByTagName(eConfig, mode);
		int i, mSize = spaces.length, nUpdate = 1;
		CPOSTagger tagger;
		
		IntObjectOpenHashMap<IntDoubleMap> mMargin = new IntObjectOpenHashMap<IntDoubleMap>();
		StringModel[] models = new StringModel[mSize];
		double prevScore, currScore = 0;
		
		do
		{
			prevScore = currScore;

			for (i=0; i<mSize; i++)
			{
				updateModel(eTrain, spaces[i], mMargin, nUpdate++, i);
				models[i] = (StringModel)spaces[i].getModel();
			}

			tagger = (CPOSTagger)getComponent(xmls, models, lexica, mode);
			currScore = predict(reader, tagger, devFiles, mode);
		}
		while (prevScore < currScore);
	}
	
	protected void developPOSTagger2(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles) throws Exception
	{
		CPOSTagger tagger = getTrainedPOSTagger(eConfig, reader, xmls, trainFiles, -1);
		predict(reader, tagger, devFiles, COMLib.MODE_POS);
	}
	
	protected void developDEPParser(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles) throws Exception
	{
		Object[] lexica = getLexica(new CDEPPassParser(xmls), reader, xmls, trainFiles, -1);
		double prevScore, currScore = 0;
		StringModel[] models = null;
		CDEPPassParser parser = null;
		
		do
		{
			prevScore = currScore;
			
			parser = (CDEPPassParser)getTrainedComponent(eConfig, xmls, trainFiles, models, lexica, COMLib.MODE_DEP_PASS, -1);
			models = parser.getModels();

			currScore = predict(reader, parser, devFiles, COMLib.MODE_DEP_PASS);
		}
		while (prevScore < currScore);
	}
	
	protected void developRolesetClassifier(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles) throws Exception
	{
		CRolesetClassifier tagger = getTrainedRolesetClassifier(eConfig, reader, xmls, trainFiles, -1);
		predict(reader, tagger, devFiles, COMLib.MODE_ROLESET);
	}
	
	protected double predict(JointReader reader, AbstractComponent component, String[] devFiles, String mode) throws Exception
	{
		int[] counts = getCounts(mode);
		DEPTree tree;
		
	//	System.out.print("Predicting:");
		
		for (String devFile : devFiles)
		{
	//		PrintStream fout = UTOutput.createPrintBufferedFileStream(devFile+".role");
			reader.open(UTInput.createBufferedFileReader(devFile));
			
			while ((tree = reader.next()) != null)
			{
				component.process(tree);
				component.countAccuracy(counts);
	//			fout.println(tree.toStringDEP()+"\n");
			}
			
	//		fout.close();
			reader.close();
	//		System.out.print(".");
		}
		
	//	System.out.println();
		return getScore(mode, counts);
	}
	
	protected int[] getCounts(String mode)
	{
		if (mode.equals(COMLib.MODE_POS) || mode.equals(COMLib.MODE_DEP_PASS) || mode.equals(COMLib.MODE_ROLESET))
			return new int[2];
		else if (mode.equals(COMLib.MODE_DEP_1))
			return new int[3];
		
		return null;
	}
	
	protected double getScore(String mode, int[] counts)
	{
		double score = 0;
		
		if (mode.equals(COMLib.MODE_POS) || mode.equals(COMLib.MODE_ROLESET))
		{
			score = 100d * counts[1] / counts[0];
			System.out.printf("- ACC: %5.2f (%d/%d)\n", score, counts[1], counts[0]);
		}
		else if (mode.equals(COMLib.MODE_DEP_PASS))
		{
			score = 100d * counts[1] / counts[0];
			System.out.printf("LAS: %5.2f (%d/%d)\n", score, counts[1], counts[0]);
		}
		else if (mode.equals(COMLib.MODE_DEP_1))
		{
			double p = 100d * counts[0] / counts[1];
			double r = 100d * counts[0] / counts[2];
			score = SRLEval.getF1(p, r);
			
			System.out.printf("P : %5.2f\n", p);
			System.out.printf("R : %5.2f\n", r);
			System.out.printf("F1: %5.2f\n", score);
		}
		
		return score;
	}
	
	static public void main(String[] args)
	{
		new COMDevelop(args);
	}
	
/*	protected IntObjectOpenHashMap<IntDoubleMap> getDela(JointReader reader, AbstractComponent component, String[] trainFiles, String mode) throws Exception
	{
		IntObjectOpenHashMap<IntDoubleMap> delta = new IntObjectOpenHashMap<IntDoubleMap>();
		Prob1DMap map = new Prob1DMap();
		DEPTree tree;
		
		for (String trainFile : trainFiles)
		{
			reader.open(UTInput.createBufferedFileReader(trainFile));
			
			while ((tree = reader.next()) != null)
			{
				component.process(tree);
				updateDeltas(component, tree, map);
			}
			
			reader.close();
		}
		
		Pattern p = Pattern.compile("_");
		IntDoubleMap smap;
		String[] tmp;
		int i1, i2;
		
		for (ObjectCursor<String> cur : map.keys())
		{
			tmp = p.split(cur.value);
			i1  = Integer.parseInt(tmp[0]);
			i2  = Integer.parseInt(tmp[1]);
			
			if (delta.containsKey(i1))
				smap = delta.get(i1);
			else
			{
				smap = new IntDoubleOpenHashMap();
				delta.put(i1, smap);
			}
				
			smap.put(i2, 1 + map.getProb(cur.value));
		//	System.out.printf("%10s: %f\n", cur.value, map.getProb(cur.value));
		}
		
		return delta;
	}
	
	protected void updateDeltas(AbstractComponent component, DEPTree tree, Prob1DMap map)
	{
		String[] gHeads = (String[])component.getGoldTags();
		AbstractModel model = component.getModels()[0];
		int i, size = tree.size();
		String key;
		
		boolean[] match = new boolean[size];
		
		for (i=1; i<size; i++)
			match[i] = tree.get(i).pos.equals(gHeads[i]);
		
		for (i=1; i<size-1; i++)
		{
			if (!match[i] && !match[i+1])
			{
				key = model.getLabelIndex(gHeads[i])+"_"+model.getLabelIndex(tree.get(i).pos);
				map.add(key);
				
				if (i+2 < size && !match[i+2])
					map.add(key);
			}
		}
	}*/
}
