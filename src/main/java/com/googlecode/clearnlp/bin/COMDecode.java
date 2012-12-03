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
package com.googlecode.clearnlp.bin;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.component.AbstractComponent;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.reader.JointReader;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class COMDecode extends AbstractBin
{
	@Option(name="-c", usage="configuration file (required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-i", usage="input path (required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-ie", usage="input file extension (default: .*)", required=false, metaVar="<regex>")
	private String s_inputExt = ".*";
	@Option(name="-oe", usage="output file extension (default: cnlp)", required=false, metaVar="<string>")
	private String s_outputExt = "cnlp";
	@Option(name="-md", usage="mode (required)", required=true, metaVar="<string>")
	private String s_mode;
	
	public COMDecode() {}
	
	public COMDecode(String[] args)
	{
		initArgs(args);
		
		try
		{
			List<String[]> filenames = getFilenames(s_inputPath, s_inputExt, s_outputExt);
			Element eConfig = UTXml.getDocumentElement(new FileInputStream(s_configXml));
			Element eReader = UTXml.getFirstElementByTagName(eConfig, TAG_READER);
			Element eModels = UTXml.getFirstElementByTagName(eConfig, TAG_MODELS);
			Pair<AbstractReader<?>,String> pr = getReader(eReader);
			
			AbstractComponent component = getComponent(eModels, s_mode);
			JointReader reader = (JointReader)pr.o1;
			
			for (String[] filename : filenames)
				decode(reader, filename[0], filename[1], component);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	protected void decode(JointReader reader, String inputFile, String outputFile, AbstractComponent component)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		DEPTree tree;
		
		while ((tree = reader.next()) != null)
		{
			component.process(tree);
			fout.println(tree.toStringDEP()+"\n");
		}
		
		fout.close();
	}
	
	static public void main(String[] args)
	{
		new COMDecode(args);
	}
}
