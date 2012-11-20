package com.googlecode.clearnlp.bin;

import java.io.FileInputStream;
import java.io.PrintStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.component.CDEPParser0;
import com.googlecode.clearnlp.component.CPOSTagger0;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
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
		model_size = featureFiles.length;
		
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		JointFtrXml[]  xmls = getFeatureTemplates(featureFiles);
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, ".*", true);
		String[]   devFiles = UTFile.getSortedFileListBySize(devDir, ".*", true);
		JointReader  reader = getCDEPReader(eConfig);
		String         mode = getMode(eConfig);
		
		if      (mode.equals(COMLib.MODE_POS))
			developPOSTagger(eConfig, reader, xmls, trainFiles, devFiles);
		else if (mode.equals(COMLib.MODE_DEP))
			developDEPParser(eConfig, reader, xmls, trainFiles, devFiles);
	}
	
	protected void developPOSTagger(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles) throws Exception
	{
		CPOSTagger0 tagger = getTrainedPOSTagger(eConfig, reader, xmls, trainFiles, -1);
		predict(reader, tagger, devFiles);
	}
	
	protected void developDEPParser(Element eConfig, JointReader reader, JointFtrXml[] xmls, String[] trainFiles, String[] devFiles) throws Exception
	{
		Object[] lexica = getLexica(new CDEPParser0(xmls), reader, xmls, trainFiles, -1);
		double prevScore, currScore = 0;
		StringModel[] models = null;
		CDEPParser0 parser = null;
		
		do
		{
			prevScore = currScore;
			
			parser = (CDEPParser0)getTrainedComponent(eConfig, xmls, trainFiles, models, lexica, COMLib.MODE_DEP, -1);
			models = parser.getModels();

			currScore = predict(reader, parser, devFiles);
		}
		while (prevScore < currScore);
	}
	
	protected double predict(JointReader reader, AbstractComponent component, String[] devFiles) throws Exception
	{
		int[] counts = new int[2];
		DEPTree tree;
		
		System.out.println("Predicting:");
		
		for (String devFile : devFiles)
		{
			PrintStream fout = UTOutput.createPrintBufferedFileStream(devFile+".pos");
			reader.open(UTInput.createBufferedFileReader(devFile));
			
			while ((tree = reader.next()) != null)
			{
				component.process(tree);
				component.countAccuracy(counts);
				fout.println(tree.toStringPOS()+"\n");
			}
			
			fout.close();
			reader.close();
			System.out.print(".");
		}
		
		System.out.println();
		
		double score = 100d * counts[1] / counts[0];
		System.out.printf("Accuracy: %5.2f (%d/%d)\n", score, counts[1], counts[0]);
		
		return score;
	}
	
	static public void main(String[] args)
	{
		new COMDevelop(args);
	}
}
