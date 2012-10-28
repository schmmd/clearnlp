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

import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.w3c.dom.Element;

import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.predicate.AbstractPredIdentifier;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.UTXml;

/**
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class PredPredict extends AbstractRun
{
	@Option(name="-i", usage="input path (required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-ie", usage="input file extension (default: .*)", required=false, metaVar="<regex>")
	private String s_inputExt = ".*";
	@Option(name="-oe", usage="output file extension (default: labeled)", required=false, metaVar="<string>")
	private String s_outputExt = "pred";
	@Option(name="-c", usage="configuration file (input; required)", required=true, metaVar="<filename>")
	private String s_configXml;
	@Option(name="-m", usage="model file (input; required)", required=true, metaVar="<filename>")
	private String s_modelFile;
	
	public PredPredict() {}
	
	public PredPredict(String[] args)
	{
		initArgs(args);
		
		try
		{
			Element eConfig = UTXml.getDocumentElement(new FileInputStream(s_configXml));
			
			List<String[]> filenames = getFilenames(s_inputPath, s_inputExt, s_outputExt);
			DEPReader reader = (DEPReader)getReader(eConfig).o1;
		
			AbstractPredIdentifier identifier = EngineGetter.getPredIdentifier(s_modelFile);
			
			for (String[] io : filenames)
				predict(identifier, reader, io[0], io[1]);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void predict(AbstractPredIdentifier identifier, DEPReader fin, String inputFile, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		fin.open(UTInput.createBufferedFileReader(inputFile));
		DEPTree tree;
		int i = 0;
		
		System.out.print(inputFile+": ");
		
		while ((tree = fin.next()) != null)
		{
			identifier.identify(tree);
			fout.println(tree.toStringDEP() + AbstractColumnReader.DELIM_SENTENCE);

			if (++i%1000 == 0)	System.out.print(".");
		}
		
		System.out.println();
		
		fin.close();
		fout.close();
	}
	
	static public void main(String[] args)
	{
		new PredPredict(args);
	}
}
