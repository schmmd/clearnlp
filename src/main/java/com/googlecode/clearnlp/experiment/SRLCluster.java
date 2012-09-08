package com.googlecode.clearnlp.experiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.googlecode.clearnlp.clustering.Kmeans;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.reader.SRLReader;
import com.googlecode.clearnlp.run.AbstractRun;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.IntDoublePair;

public class SRLCluster extends AbstractRun
{
	public SRLCluster(String configXml, String inputDir, String outputDir, int K, double threshold) throws Exception
	{
		Kmeans km = new Kmeans();
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		SRLReader    reader = (SRLReader)getReader(eConfig);
		List<DEPTree> lTrees = new ArrayList<DEPTree>();
		List<List<IntDoublePair>> lClusters;
		DEPTree tree;
		int i;
		PrintStream fout;
		String[] inputFiles = UTFile.getSortedFileList(inputDir);
		
		for (String inputFile : inputFiles)
		{
			reader.open(UTInput.createBufferedFileReader(inputFile));
			System.out.println(inputFile);
			
			while ((tree = reader.next()) != null)
			{
				lTrees.add(tree);
				km.addUnit(tree);
			}
			
			reader.close();
		}
		
		lClusters = km.cluster(K, threshold);
		
		for (i=0; i<K; i++)
		{
			fout = UTOutput.createPrintBufferedFileStream(outputDir+File.separator+i);
			
			for (IntDoublePair cluster : lClusters.get(i))
				fout.println(lTrees.get(cluster.i).toStringSRL()+"\n");
		
			fout.close();
		}
	}

	static public void main(String[] args)
	{
		String configXml = args[0];
		String inputDir = args[1];
		String outputDir = args[2];
		int K  = Integer.parseInt(args[3]);
		double threshold = Double.parseDouble(args[4]);
		
		try {
			new SRLCluster(configXml, inputDir, outputDir, K, threshold);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
