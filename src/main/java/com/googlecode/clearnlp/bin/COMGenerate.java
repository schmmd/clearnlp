package com.googlecode.clearnlp.bin;

import java.io.FileInputStream;
import java.io.PrintStream;

import org.w3c.dom.Element;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.component.CPOSTagger0;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;

public class COMGenerate extends COMTrain
{
	public COMGenerate(String[] args)
	{
		initArgs(args);
		
		try
		{
			generate(s_configFile, s_featureFiles.split(DELIM_FILES), s_trainDir);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void generate(String configFile, String[] featureFiles, String trainDir) throws Exception
	{
		model_size = featureFiles.length;
		
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		JointFtrXml[]  xmls = getFeatureTemplates(featureFiles);
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, ".*", true);
		JointReader   reader = getCDEPReader(eConfig);
		String         mode = getMode(eConfig);
		
		if (mode.equals(COMLib.MODE_POS))
			generatePOSTagger(eConfig, reader, xmls, trainFiles);
	}
	
	protected void generatePOSTagger(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles) throws Exception
	{
		int devId, size = trainFiles.length;
		CPOSTagger0 tagger;
		
		for (devId=0; devId<size; devId++)
		{
			tagger = getTrainedPOSTagger(eConfig, reader, xmls, trainFiles, devId);
			predict(reader, tagger, trainFiles[devId], COMLib.MODE_POS);
		}
	}
	
	protected void predict(JointReader reader, AbstractComponent component, String devFile, String mode) throws Exception
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(devFile+".pos");
		reader.open(UTInput.createBufferedFileReader(devFile));
		int[] counts = new int[2];
		DEPTree tree;
		
		System.out.println("Predicting: "+devFile);
		
		while ((tree = reader.next()) != null)
		{
			component.process(tree);
			component.countAccuracy(counts);
			
			if (mode.equals(COMLib.MODE_POS))
				fout.println(tree.toStringPOS()+"\n");
		}
		
		double score = 100d * counts[1] / counts[0];
		System.out.printf("Accuracy: %5.2f (%d/%d)\n", score, counts[1], counts[0]);
		
		reader.close();
		fout.close();
	}
	
	static public void main(String[] args)
	{
		new COMGenerate(args);
	}
}
