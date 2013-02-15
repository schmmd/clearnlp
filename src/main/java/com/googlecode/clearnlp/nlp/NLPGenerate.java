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

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.component.dep.CDEPPassParser;
import com.googlecode.clearnlp.component.pos.CPOSTagger;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTXml;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class NLPGenerate extends NLPDevelop
{
	@Option(name="-b", usage="the directory containing development files (required)", required=true, metaVar="<directory>")
	private int b_idx = -1;
	@Option(name="-e", usage="the directory containing development files (required)", required=true, metaVar="<directory>")
	private int e_idx = -1;
	@Option(name="-ie", usage="input file extension (default: .*)", required=false, metaVar="<regex>")
	private String s_inputExt = ".*";
	
	public NLPGenerate(String[] args)
	{
		initArgs(args);
		
		try
		{
			generate(s_configFile, s_featureFiles.split(DELIM_FILES), s_trainDir, s_inputExt, s_mode, b_idx, e_idx);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void generate(String configFile, String[] featureFiles, String trainDir, String inputExt, String mode, int bIdx, int eIdx) throws Exception
	{
		Element     eConfig = UTXml.getDocumentElement(new FileInputStream(configFile));
		JointFtrXml[]  xmls = getFeatureTemplates(featureFiles);
		String[] trainFiles = UTFile.getSortedFileListBySize(trainDir, inputExt, true), devFiles;
		JointReader  reader = getJointReader(UTXml.getFirstElementByTagName(eConfig, TAG_READER));
		int i;
		
		b_generate = true;
		
		for (i=bIdx; i<eIdx; i++)
		{
			devFiles = new String[]{trainFiles[i]};

			if      (mode.equals(NLPLib.MODE_POS))
				developComponent(eConfig, reader, xmls, trainFiles, devFiles, new CPOSTagger(xmls, getLowerSimplifiedForms(reader, xmls[0], trainFiles, i)), mode, i);
			else if (mode.equals(NLPLib.MODE_DEP))
				developComponentBoot(eConfig, reader, xmls, trainFiles, devFiles, new CDEPPassParser(xmls), mode, i);
		}
	}
		
	static public void main(String[] args)
	{
		new NLPGenerate(args);
	}
}
