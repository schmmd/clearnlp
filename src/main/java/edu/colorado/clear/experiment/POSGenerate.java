package edu.colorado.clear.experiment;

import java.io.File;
import java.io.FileInputStream;

import org.w3c.dom.Element;

import com.carrotsearch.hppc.IntOpenHashSet;

import edu.colorado.clear.feature.xml.POSFtrXml;
import edu.colorado.clear.morphology.EnglishMPAnalyzer;
import edu.colorado.clear.pos.POSTagger;
import edu.colorado.clear.reader.POSReader;
import edu.colorado.clear.run.POSPredict;
import edu.colorado.clear.run.POSTrain;
import edu.colorado.clear.util.UTXml;

public class POSGenerate extends POSTrain
{
	public POSGenerate(String configXml, String featureXml, String dictFile, String trnDir, String outDir, double threshold) throws Exception
	{
		Element    eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		POSReader   reader = (POSReader)getReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
		POSFtrXml      xml = new POSFtrXml(new FileInputStream(featureXml));
		String[]  trnFiles = getSortedFileList(trnDir);
		EnglishMPAnalyzer morph = getMPAnalyzerEn(dictFile);

		IntOpenHashSet sDev = new IntOpenHashSet();
		int devId, size = trnFiles.length;
		POSTagger[] taggers;
		String devFile;
		
		for (devId=0; devId<size; devId++)
		{
			devFile = trnFiles[devId];
			devFile = devFile.substring(devFile.lastIndexOf(File.separator)+1);
			
			System.out.println("Cross validation: "+devFile);
			sDev.clear();	sDev.add(devId);
			
			taggers = getTrainedTaggers(eConfig, reader, xml, trnFiles, sDev);
			POSPredict.predict(trnFiles[devId], outDir+File.separator+devFile+".tagged", reader, taggers, threshold, morph);
		}
	}
	
	static public void main(String[] args)
	{
		String configXml  = args[0];
		String featureXml = args[1];
		String dictFile   = args[2];
		String trnDir     = args[3];
		String outDir     = args[4];
		double threshold  = Double.parseDouble(args[5]);
		
		try
		{
			new POSGenerate(configXml, featureXml, dictFile, trnDir, outDir, threshold);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
