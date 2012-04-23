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
package edu.colorado.clear.experiment;

import java.io.FileInputStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import edu.colorado.clear.feature.xml.POSFtrXml;
import edu.colorado.clear.pos.POSLib;
import edu.colorado.clear.pos.POSNode;
import edu.colorado.clear.pos.POSTagger;
import edu.colorado.clear.reader.POSReader;
import edu.colorado.clear.run.POSTrain;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTXml;

/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class POSDevelop extends POSTrain
{
	@Option(name="-d", usage="the directory containing development files (input; required)", required=true, metaVar="<directory>")
	private String s_devDir;
	
	public POSDevelop() {}
	
	public POSDevelop(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_featureXml, s_trainDir, s_devDir, d_threshold);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void run(String configXml, String featureXml, String trnDir, String devDir, double threshold) throws Exception
	{
		Element    eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		POSReader   reader = (POSReader)getReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
		POSFtrXml      xml = new POSFtrXml(new FileInputStream(featureXml));
		String[]  trnFiles = getSortedFileList(trnDir);
		String[]  devFiles = getSortedFileList(devDir);
		
		if (threshold < 0)	threshold = crossValidate(trnFiles, reader, xml, eConfig);
		POSTagger[] taggers = getTrainedTaggers(eConfig, reader, xml, trnFiles, null);
		
		for (String devFile : devFiles)
			predict(devFile, reader, taggers, threshold);
	}

	protected double predict(String devFile, POSReader reader, POSTagger[] taggers, double threshold) 
	{
		int correct = 0, total = 0, n, cLocal, tLocal;
		POSNode[] nodes;
		String[]  gold;
		double macro = 0;
		
		System.out.println("Threshold : "+threshold);
		System.out.println("Predicting: "+devFile);
		reader.open(UTInput.createBufferedFileReader(devFile));
		
		for (n=0; (nodes = reader.next()) != null; n++)
		{
			gold = POSLib.getLabels(nodes);
			POSLib.normalizeForms(nodes);
			
			if (threshold < taggers[0].getCosineSimilarity(nodes))
				taggers[0].tag(nodes);
			else
				taggers[1].tag(nodes);

			cLocal   = countCorrect(nodes, gold);
			tLocal   = gold.length;
			correct += cLocal;
			total   += tLocal;
			
			macro += 100d * cLocal / tLocal;
		}
		
		reader.close();
		
		double accuracy = 100d * correct / total;
		System.out.printf("- micro accuracy: %7.5f (%d/%d)\n", accuracy, correct, total);
		
		accuracy = macro / n;
		System.out.printf("- macro accuracy: %7.5f (%d)\n", accuracy, n);
		
		return accuracy;
	}
	
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
		new POSDevelop(args);
	}
}
