/**
* Copyright 2012 University of Massachusetts Amherst
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
*   
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.googlecode.clearnlp.nlp;

import java.io.FileInputStream;
import java.io.PrintStream;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class NLPGenerate extends NLPTrain
{
	@Option(name="-b", usage="the directory containing development files (required)", required=true, metaVar="<directory>")
	private int b_dev = -1;
	@Option(name="-e", usage="the directory containing development files (required)", required=true, metaVar="<directory>")
	private int e_dev = -1;
	
	public NLPGenerate(String[] args)
	{
		initArgs(args);
		
		try
		{
			generate(s_configFile, s_featureFiles.split(DELIM_FILES), s_trainDir, s_mode, b_dev, e_dev);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void generate(String configFile, String[] featureFiles, String trainDir, String mode, int bDev, int eDev) throws Exception
	{
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		JointFtrXml[]  xmls = getFeatureTemplates(featureFiles);
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, ".*", true);
		JointReader  reader = getJointReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
		int devId, size = trainFiles.length;
		
		AbstractStatisticalComponent component;
		PrintStream fout;
		
		for (devId=bDev; devId<eDev && devId<size; devId++)
		{
			System.out.println("Generate: "+trainFiles[devId]+"."+mode);
			
			component = getComponent(eConfig, reader, xmls, trainFiles, devId, mode);
			fout = UTOutput.createPrintBufferedFileStream(trainFiles[devId]+"."+mode);
			reader.open(UTInput.createBufferedFileReader(trainFiles[devId]));
			
			decode(reader, fout, component, s_mode);
			reader.close(); fout.close();	
		}
	}
	
	public void decode(JointReader reader, PrintStream fout, AbstractComponent component, String mode)
	{
		DEPTree tree;
		
		while ((tree = reader.next()) != null)
		{
			component.process(tree);
			fout.println(toString(tree, mode)+"\n");
		}
	}
	
	static public void main(String[] args)
	{
		new NLPGenerate(args);
	}
}
