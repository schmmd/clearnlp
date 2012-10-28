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
package com.googlecode.clearnlp.experiment;

import java.io.FileInputStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLEval;
import com.googlecode.clearnlp.feature.xml.SRLFtrXml;
import com.googlecode.clearnlp.predicate.PredIdentifier;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.run.AbstractRun;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTXml;

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
		Element  eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		DEPReader reader = (DEPReader)getReader(eConfig).o1;
		SRLFtrXml    xml = new SRLFtrXml(new FileInputStream(featureXml));
		
		develop(eConfig, reader, xml, trnFile, devFile);
	}
	
	/** @param devId if {@code -1}, train the models using all training files. */
	protected void develop(Element eConfig, DEPReader reader, SRLFtrXml xml, String trnFile, String devFile) throws Exception
	{
		StringTrainSpace space = new StringTrainSpace(false, xml.getLabelCutoff(0), xml.getFeatureCutoff(0));
		PredIdentifier pred = new PredIdentifier(xml, space);
		int[] scores = {0,0,0};
		String[] gRolesets;
		DEPTree tree;
		int i;
		
		reader.open(UTInput.createBufferedFileReader(trnFile));
		System.out.print("Training: ");
		
		for (i=0; (tree = reader.next()) != null; i++)
		{
			pred.identify(tree);
			if (i%1000 == 0)	System.out.print(".");
		}
		System.out.println(i);
		reader.close();

		StringModel model = (StringModel)getModel(UTXml.getFirstElementByTagName(eConfig, TAG_TRAIN), space, 0);
		pred = new PredIdentifier(xml, model);
		reader.open(UTInput.createBufferedFileReader(devFile));
	//	PrintStream fout = UTOutput.createPrintBufferedFileStream(devFile+".roleset");
		
		System.out.print("Predicting: ");
		for (i=0; (tree = reader.next()) != null; i++)
		{
			gRolesets = getGoldRolesets(tree);
			pred.identify(tree);
	//		fout.println(tree+"\n");
			countScores(tree, gRolesets, scores);
			if (i%1000 == 0)	System.out.print(".");
		}
		System.out.println();
	//	fout.close();
		reader.close();
		
		double precision = 100d * scores[0] / scores[1];
		double recall    = 100d * scores[0] / scores[2];
		double f1        = SRLEval.getF1(precision, recall);
		
		System.out.printf("P: %5.2f (%d/%d)\n", precision, scores[0], scores[1]);
		System.out.printf("R: %5.2f (%d/%d)\n", recall   , scores[0], scores[2]);
		System.out.printf("F: %5.2f\n", f1);
	}
	
	String[] getGoldRolesets(DEPTree tree)
	{
		int i, size = tree.size();
		String[] rolesets = new String[size];
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			rolesets[i] = node.getFeat(DEPLib.FEAT_PB);
		}
		
		return rolesets;
	}
	
	void countScores(DEPTree tree, String[] gRolesets, int[] scores)
	{
		int i, size = tree.size();
		DEPNode node;
		String sRoleset, gRoleset;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			gRoleset = gRolesets[i];
			sRoleset = node.getFeat(DEPLib.FEAT_PB);
			
			if (sRoleset != null)	scores[1]++;
			
			if (gRoleset != null)
			{
				scores[2]++;
				if (sRoleset != null)	scores[0]++;
			}
		}
	}
	
	static public void main(String[] args)
	{
		new PredDevelop(args);
	}
}