/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
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

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import org.w3c.dom.Element;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.googlecode.clearnlp.feature.xml.POSFtrXml;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.reader.POSReader;
import com.googlecode.clearnlp.run.POSPredict;
import com.googlecode.clearnlp.run.POSTrain;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTXml;


public class POSGenerate extends POSTrain
{
	public POSGenerate(String configXml, String featureXml, String trnDir, String outDir, double threshold) throws Exception
	{
		Element    eConfig = UTXml.getDocumentElement(new FileInputStream(configXml));
		POSReader   reader = (POSReader)getReader(eConfig);
		POSFtrXml      xml = new POSFtrXml(new FileInputStream(featureXml));
		String[]  trnFiles = UTFile.getSortedFileList(trnDir);

		IntOpenHashSet sDev = new IntOpenHashSet();
		int devId, size = trnFiles.length;
		long[] counts = new long[4];
		String devFile;
		
		for (devId=0; devId<size; devId++)
		{
			devFile = trnFiles[devId];
			devFile = devFile.substring(devFile.lastIndexOf(File.separator)+1);
			
			System.out.println("Cross validation: "+devFile);
			sDev.clear();	sDev.add(devId);
			Arrays.fill(counts, 0);
			
		//	POSTagger[] taggers = {getTrainedTagger(eConfig, reader, xml, trnFiles, sDev, FLAG_GENERAL)};
			POSTagger[] taggers = getTrainedTaggers(eConfig, reader, xml, trnFiles, sDev);
			POSPredict.predict(trnFiles[devId], outDir+File.separator+devFile+".tagged", reader, taggers, threshold, counts);
		}
	}
	
	static public void main(String[] args)
	{
		String configXml  = args[0];
		String featureXml = args[1];
		String trnDir     = args[2];
		String outDir     = args[3];
		double threshold  = Double.parseDouble(args[4]);
		
		try
		{
			new POSGenerate(configXml, featureXml, trnDir, outDir, threshold);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
