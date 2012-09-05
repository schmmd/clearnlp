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
package com.googlecode.clearnlp.run;

import java.io.File;
import java.io.PrintStream;

import org.kohsuke.args4j.Option;

import com.googlecode.clearnlp.constituent.CTLibEn;
import com.googlecode.clearnlp.constituent.CTReader;
import com.googlecode.clearnlp.constituent.CTTree;
import com.googlecode.clearnlp.conversion.AbstractC2DConverter;
import com.googlecode.clearnlp.conversion.EnglishC2DConverter;
import com.googlecode.clearnlp.dependency.DEPFeat;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.headrule.HeadRuleMap;
import com.googlecode.clearnlp.io.FileExtFilter;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.morphology.EnglishMPAnalyzer;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;


public class C2DConvert extends AbstractRun
{
	@Option(name="-i", usage="the input path containing constituent trees (input; required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-h", usage="the name of a headrule file (required)", required=true, metaVar="<filename>")
	private String s_headruleFile;
	@Option(name="-d", usage="the name of a dictionary file for lemmatization (optional)", required=false, metaVar="<filename>")
	private String s_dictFile = null;
	@Option(name="-ie", usage="the input file extension (default: .*)", required=false, metaVar="<regex>")
	private String s_inputExt = "parse";
	@Option(name="-oe", usage="the output file extension (default: dep)", required=false, metaVar="<string>")
	private String s_outputExt = "dep";
	@Option(name="-l", usage="language (default: "+AbstractReader.LANG_EN+")", required=false, metaVar="<language>")
	private String s_language = AbstractReader.LANG_EN;
	@Option(name="-m", usage="merge labels (default: null)", required=false, metaVar="<string>")
	private String s_mergeLabels = null;
	
	public C2DConvert() {}

	public C2DConvert(String[] args)
	{
		initArgs(args);
		convert(s_headruleFile, s_dictFile, s_language, s_mergeLabels, s_inputPath, s_inputExt, s_outputExt);
	}
	
	public void convert(String headruleFile, String dictFile, String language, String mergeLabels, String inputPath, String inputExt, String outputExt)
	{
		AbstractMPAnalyzer morph = null;
		AbstractC2DConverter c2d = null;
		
		if (s_language.equals(AbstractReader.LANG_EN))
		{
			c2d   = new EnglishC2DConverter(new HeadRuleMap(UTInput.createBufferedFileReader(headruleFile)), mergeLabels);
			if (dictFile != null)	morph = new EnglishMPAnalyzer(dictFile);
		}
		
		File f = new File(inputPath);
		
		if (f.isDirectory())
		{
			for (String inputFile : f.list(new FileExtFilter(inputExt)))
				convertAux(c2d, morph, language, inputPath+File.separator+inputFile, outputExt);
		}
		else
			convertAux(c2d, morph, language, inputPath, outputExt);
	}
	
	private void convertAux(AbstractC2DConverter c2d, AbstractMPAnalyzer morph, String language, String inputFile, String outputExt)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(inputFile+"."+outputExt);
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(inputFile));
		CTTree cTree; DEPTree dTree; int n;
		
		for (n=0; (cTree = reader.nextTree()) != null; n++)
		{
			if (language.equals(AbstractReader.LANG_EN))
				CTLibEn.preprocessTree(cTree);
			
			dTree = c2d.toDEPTree(cTree);
			
			if (dTree == null)
			{
				fout.println(getNullTree()+"\n");
			}
			else
			{
				if (morph != null)	morph.lemmatize(dTree);
				fout.println(dTree.toStringDAG()+"\n");
			}
		}
		
		fout.close();
		reader.close();
		
		System.out.printf("%s -> %d trees\n", inputFile.substring(inputFile.lastIndexOf(File.separator)), n);
	}
	
	private DEPTree getNullTree()
	{
		DEPTree tree = new DEPTree();
		
		DEPNode dummy = new DEPNode(1, "NULL", "NULL", "NULL", new DEPFeat());
		dummy.setHead(tree.get(0), "NULL");
		
		tree.add(dummy);
		tree.initXHeads();
		tree.initSHeads();
		
		return tree;
	}

	public static void main(String[] args)
	{
		new C2DConvert(args);
	}
}
