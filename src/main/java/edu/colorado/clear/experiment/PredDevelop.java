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
import java.io.PrintStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.dependency.DEPLib;
import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.dependency.srl.SRLEval;
import edu.colorado.clear.engine.PredIdentifier;
import edu.colorado.clear.feature.xml.DEPFtrXml;
import edu.colorado.clear.reader.AbstractColumnReader;
import edu.colorado.clear.reader.DEPReader;
import edu.colorado.clear.run.AbstractRun;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;
import edu.colorado.clear.util.UTXml;

/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class PredDevelop extends AbstractRun
{
	protected final int    MODEL_SIZE      = 2;
	protected final String ENTRY_FEATURE   = "FEATURE";
	protected final String ENTRY_MODEL     = "MODEL";
	protected final String ENTRY_THRESHOLD = "THRESHOLD";
	
	@Option(name="-i", usage="the training file (input; required)", required=true, metaVar="<filename>")
	private String s_trainFile;
	@Option(name="-d", usage="the directory containing development file (input; required)", required=true, metaVar="<filename>")
	private String s_devFile;
	@Option(name="-c", usage="the configuration file (input; required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-f", usage="the feature file (input; required)", required=true, metaVar="<filename>")
	private String s_featureXml;
	
	public PredDevelop() {}
	
	public PredDevelop(String[] args)
	{
		initArgs(args);
		
		try
		{
			run(s_configXml, s_featureXml, s_trainFile, s_devFile);	
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void run(String configXml, String featureXml, String trnFile, String devFile) throws Exception
	{
		Element   eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		DEPReader  reader = (DEPReader)getReader(eConfig);
		DEPFtrXml     xml = new DEPFtrXml(new FileInputStream(featureXml));
		
		develop(eConfig, reader, xml, trnFile, devFile);
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	protected void develop(Element eConfig, DEPReader reader, DEPFtrXml xml, String trnFile, String devFile) throws Exception
	{
		StringTrainSpace space = new StringTrainSpace(false, xml.getLabelCutoff(0), xml.getFeatureCutoff(0));
		PredIdentifier pred = new PredIdentifier(xml, space);
		DEPTree tree;
		int[] scores = {0,0,0};
		int i;
		
		reader.open(UTInput.createBufferedFileReader(trnFile));
		System.out.print("Training: ");
		
		for (i=0; (tree = reader.next()) != null; i++)
		{
			pred.identify(tree);
			if (i%1000 == 0)	System.out.print(".");
		}
		System.out.println();
		reader.close();

		StringModel model = (StringModel)getModel(UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN), space, 0);
		pred = new PredIdentifier(xml, model);
		reader.open(UTInput.createBufferedFileReader(devFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(devFile+".roleset");
		
		System.out.print("Predicting: ");
		for (i=0; (tree = reader.next()) != null; i++)
		{
			pred.identify(tree);
			fout.println(tree+"\n");
			countScores(tree, scores);
			if (i%1000 == 0)	System.out.print(".");
		}
		System.out.println();
		fout.close();
		reader.close();
		
		double precision = 100d * scores[0] / scores[1];
		double recall    = 100d * scores[0] / scores[2];
		double f1        = SRLEval.getF1(precision, recall);
		
		System.out.printf("P: %5.2f (%d/%d)\n", precision, scores[0], scores[1]);
		System.out.printf("R: %5.2f (%d/%d)\n", recall   , scores[0], scores[2]);
		System.out.printf("F: %5.2f\n", f1);
	}
	
	void countScores(DEPTree tree, int[] scores)
	{
		int i, size = tree.size();
		DEPNode node;
		String roleset;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			roleset = node.getFeat(DEPLib.FEAT_PB);
			
			if (roleset != null)	scores[1]++;
			
			if (!node.namex.equals(AbstractColumnReader.BLANK_COLUMN))
			{
				scores[2]++;
				if (roleset != null)	scores[0]++;
			}
		}
	}
	
	static public void main(String[] args)
	{
		new PredDevelop(args);
	}
}
