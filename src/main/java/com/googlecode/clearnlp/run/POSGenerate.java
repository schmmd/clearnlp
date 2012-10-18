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
package com.googlecode.clearnlp.run;

import java.io.File;
import java.io.FileInputStream;

import org.w3c.dom.Element;

import com.googlecode.clearnlp.feature.xml.POSFtrXml;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.reader.POSReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class POSGenerate extends POSTrain
{
	public POSGenerate(String[] args) throws Exception
	{
		initArgs(args);
		
		Element    eConfig = UTXml.getDocumentElement(new FileInputStream(s_configXml));
		POSReader   reader = (POSReader)getReader(eConfig).o1;
		POSFtrXml      xml = new POSFtrXml(new FileInputStream(s_featureXml));
		String[]  trnFiles = UTFile.getSortedFileList(s_trainDir);

		Pair<POSTagger[],Double> taggers = new Pair<POSTagger[],Double>(null, d_threshold);
		int devId, size = trnFiles.length;
		POSPredict p = new POSPredict();
		String devFile;
		
		for (devId=0; devId<size; devId++)
		{
			devFile = trnFiles[devId];
			devFile = devFile.substring(devFile.lastIndexOf(File.separator)+1);
			
			System.out.println("Cross validation: "+devFile);
			
			if (i_flag == FLAG_DYNAMIC)
				taggers.o1 = getTrainedTaggers(eConfig, reader, xml, trnFiles, devId);
			else
				taggers.o1 = new POSTagger[]{getTrainedTagger(eConfig, reader, xml, trnFiles, devId, FLAG_GENERAL)};
			
			p.predict(taggers, reader, trnFiles[devId], s_trainDir+File.separator+devFile+".tagged");
		}
	}
	
	static public void main(String[] args)
	{
		try
		{
			new POSGenerate(args);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
