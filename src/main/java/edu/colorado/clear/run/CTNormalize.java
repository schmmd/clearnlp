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
package edu.colorado.clear.run;

import java.io.File;
import java.io.PrintStream;

import org.kohsuke.args4j.Option;

import edu.colorado.clear.constituent.CTLib;
import edu.colorado.clear.constituent.CTReader;
import edu.colorado.clear.constituent.CTTree;
import edu.colorado.clear.io.FileExtFilter;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;

/**
 * Normalizes indices of constituent trees.
 * @see CTReader#normalizeIndices(CTTree)
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class CTNormalize extends AbstractRun
{
	@Option(name="-i", usage="the input directory path (required)", required=true, metaVar="<dirpath>")
	private String s_inputDir;
	@Option(name="-o", usage="the output directory path (required)", required=true, metaVar="<dirpath>")
	private String s_outputDir;
	@Option(name="-ie", usage="the input treefile extension (required)", required=true, metaVar="<extension>")
	private String s_inputExt;
	@Option(name="-oe", usage="the output treefile extension (required)", required=true, metaVar="<extension>")
	private String s_outputExt;
	
	public CTNormalize() {}
	
	public CTNormalize(String[] args)
	{
		initArgs(args);
		normalize(s_inputDir, s_outputDir, s_inputExt, s_outputExt);
	}
	
	/**
	 * Normalizes indices of constituent trees.
	 * @param inputDir the directory containing unnormalized tree files.
	 * @param outputDir the directory to save normalized tree files.
	 * @param inputExt the tree file extension (e.g., {@code parse}).
	 */
	public void normalize(String inputDir, String outputDir, String inputExt, String outputExt)
	{
		CTReader    reader;
		CTTree      tree;
		PrintStream fout;
		
		File dir = new File(outputDir);
		if (!dir.exists())	dir.mkdirs();
		
		inputDir  += File.separator;
		outputDir += File.separator;
		
		int extLength = inputExt.length();
		
		for (String filename : new File(inputDir).list(new FileExtFilter(inputExt)))
		{
			reader = new CTReader(UTInput.createBufferedFileReader(inputDir + filename));
			fout   = UTOutput.createPrintBufferedFileStream(outputDir + filename.substring(0, filename.length()-extLength) + outputExt);
			
			while ((tree = reader.nextTree()) != null)
			{
				CTLib.normalizeIndices(tree);
				fout.println(tree.toString()+"\n");
			}
			
			reader.close();
			fout.close();
		}
	}
	
	static public void main(String[] args)
	{
		new CTNormalize(args);
	//	new CTNormalizeIndices().normalize(inputDir, outputDir, extension);
	}
}
