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

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;

import com.carrotsearch.hppc.IntObjectOpenHashMap;

import edu.colorado.clear.constituent.CTLibEn;
import edu.colorado.clear.constituent.CTNode;
import edu.colorado.clear.constituent.CTReader;
import edu.colorado.clear.constituent.CTTree;
import edu.colorado.clear.conversion.AbstractC2DConverter;
import edu.colorado.clear.conversion.EnglishC2DConverter;
import edu.colorado.clear.dependency.DEPFeat;
import edu.colorado.clear.dependency.DEPLib;
import edu.colorado.clear.dependency.DEPLibEn;
import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.headrule.HeadRuleMap;
import edu.colorado.clear.io.FileExtFilter;
import edu.colorado.clear.morphology.AbstractMPAnalyzer;
import edu.colorado.clear.propbank.PBArg;
import edu.colorado.clear.propbank.PBInstance;
import edu.colorado.clear.propbank.PBLib;
import edu.colorado.clear.propbank.PBLoc;
import edu.colorado.clear.reader.AbstractReader;
import edu.colorado.clear.run.AbstractRun;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;
import edu.colorado.clear.util.pair.StringIntPair;

public class C2DConvert extends AbstractRun
{
	@Option(name="-i", usage="the input path containing constituent trees (input; required)", required=true, metaVar="<filepath>")
	private String s_inputPath;
	@Option(name="-h", usage="the name of a headrule file (input; required)", required=true, metaVar="<filename>")
	private String s_headruleFile;
	@Option(name="-d", usage="the name of a dictionary file for lemmatization (for English)", required=false, metaVar="<extension>")
	private String s_dictFile = null;
	@Option(name="-et", usage="the parse-file extension (default: parse)", required=false, metaVar="<extension>")
	private String s_parseExt = "parse";
	@Option(name="-ep", usage="the propb-file extension (default: prop)", required=false, metaVar="<extension>")
	private String s_propExt = "prop";
	@Option(name="-es", usage="the sense-file extension (default: sense)", required=false, metaVar="<extension>")
	private String s_senseExt = "sense";
	@Option(name="-en", usage="the name-file extension (default: name)", required=false, metaVar="<extension>")
	private String s_nameExt = "name";
	@Option(name="-ed", usage="the output-file extension (default: dep)", required=false, metaVar="<extension>")
	private String s_outputExt = "dep";
	@Option(name="-l", usage="language (default: "+AbstractReader.LANG_EN+")", required=false, metaVar="<language>")
	private String s_language = AbstractReader.LANG_EN;
	
	private boolean b_verbs_only = false;
	
	public C2DConvert() {}

	public C2DConvert(String[] args)
	{
		initArgs(args);
		convert(s_headruleFile, s_dictFile, s_language, s_inputPath, s_parseExt, s_propExt, s_senseExt, s_nameExt, s_outputExt);
	}
	
	public void convert(String headruleFile, String dictFile, String language, String inputPath, String parseExt, String propExt, String senseExt, String nameExt, String outputExt)
	{
		AbstractMPAnalyzer morph = null;
		AbstractC2DConverter c2d = null;
		
		if (s_language.equals(AbstractReader.LANG_EN))
		{
			c2d   = new EnglishC2DConverter(new HeadRuleMap(UTInput.createBufferedFileReader(headruleFile)));
			if (dictFile != null)	morph = AbstractRun.getMPAnalyzerEn(dictFile);
		}
		
		convertRec(c2d, morph, language, inputPath, parseExt, propExt, senseExt, nameExt, outputExt);
	}
	
	private void convertRec(AbstractC2DConverter c2d, AbstractMPAnalyzer morph, String language, String inputPath, String parseExt, String propExt, String senseExt, String nameExt, String outputExt)
	{
		File file = new File(inputPath);
		
		if (file.isDirectory())
		{
			for (String filePath : file.list())
				convertRec(c2d, morph, language, inputPath+File.separator+filePath, parseExt, propExt, senseExt, nameExt, outputExt);
		}
		else if (inputPath.endsWith(parseExt))
		{
			System.out.println(inputPath);
			IntObjectOpenHashMap<List<PBInstance>>    mProp  = null;
			IntObjectOpenHashMap<List<StringIntPair>> mSense = null;
			IntObjectOpenHashMap<List<String>>        mName  = null;
			
			try
			{
				mProp  = getPBInstances(FileExtFilter.replaceExt(inputPath, propExt));
				mSense = getWordSenses (FileExtFilter.replaceExt(inputPath, senseExt));
				mName  = getNames      (FileExtFilter.replaceExt(inputPath, nameExt));
			}
			catch (Exception e) {e.printStackTrace();}
			
			PrintStream fout = UTOutput.createPrintBufferedFileStream(FileExtFilter.replaceExt(inputPath, outputExt));
			CTReader reader = new CTReader(UTInput.createBufferedFileReader(inputPath));
			CTTree cTree; DEPTree dTree; int n;
			List<PBInstance> instances = null;
			
			for (n=0; (cTree = reader.nextTree()) != null; n++)
			{
				if (language.equals(AbstractReader.LANG_EN))
					CTLibEn.preprocessTree(cTree);
				
				if (mProp != null)
				{
					instances = mProp.get(n);
					addPBInstances(cTree, instances);
				}
				
				dTree = c2d.toDEPTree(cTree);
				
				if (dTree == null)
				{
					fout.println(getNullTree()+"\n");
				}
				else
				{
					if (morph != null)	morph.lemmatize(dTree);
					addRolesets(cTree, dTree, instances);
					if (mSense != null)	addWordSenses(cTree, dTree, mSense.get(n));
					if (mName  != null)	addNames(cTree, dTree, mName.get(n));
					
					fout.println(dTree+"\n");					
				}
			}
			
			fout.close();
			reader.close();
		}
	}
	
	private IntObjectOpenHashMap<List<PBInstance>> getPBInstances(String propFile)
	{
		if (!new File(propFile).isFile())	return null;
		IntObjectOpenHashMap<List<PBInstance>> map = new IntObjectOpenHashMap<List<PBInstance>>();
		List<PBInstance> list;
		
		for (PBInstance inst : PBLib.getPBInstanceList(propFile))
		{
			if (map.containsKey(inst.treeId))
				list = map.get(inst.treeId);
			else
			{
				list = new ArrayList<PBInstance>();
				map.put(inst.treeId, list);
			}
			
			list.add(inst);
		}
		
		return map;
	}
	
	private void addPBInstances(CTTree cTree, List<PBInstance> instances)
	{
		if (instances == null)	return;
		initPBArgs(cTree.getRoot());
		int    predTokenId;
		String label;
		CTNode cNode;
		
		for (PBInstance instance : instances)
		{
			if (isPBSkip(instance, cTree))	continue;
			predTokenId = cTree.getTerminal(instance.predId).getTokenId() + 1;
			
			for (PBArg arg : instance.getArgs())
			{
				if (arg.label.startsWith(PBLib.SRL_LINK))
					continue;
				
				if (arg.label.endsWith("UNDEF"))
					continue;
				
				label = arg.isLabel(PBLib.SRL_REL) ? PBLib.SRL_C_V : "A"+arg.label.substring(3);
				
				for (PBLoc loc : arg.getLocs())
				{
					if (arg.isLabel(PBLib.SRL_REL) && loc.terminalId == instance.predId)
						continue;
					
					cNode = cTree.getNode(loc);
					
					if (!cNode.isEmptyCategoryRec())
						cNode.pbArgs.add(new StringIntPair(label, predTokenId));
				}
			}
		}
	}
	
	private boolean isPBSkip(PBInstance instance, CTTree cTree)
	{
		if (PBLib.WRONG_ROLESET.matcher(instance.roleset).find())
			return true;
		
		if (b_verbs_only)
		{
			if (!instance.type.endsWith("-v") && !instance.isLVNounPredicate(cTree))
				return true;
		}
		
		return false;
	}
	
	private void initPBArgs(CTNode node)
	{
		node.pbArgs = new ArrayList<StringIntPair>();
		
		for (CTNode child : node.getChildren())
			initPBArgs(child);
	}
	
	private IntObjectOpenHashMap<List<StringIntPair>> getWordSenses(String senseFile) throws Exception
	{
		if (!new File(senseFile).isFile())	return null;
		IntObjectOpenHashMap<List<StringIntPair>> map = new IntObjectOpenHashMap<List<StringIntPair>>();
		BufferedReader fin = UTInput.createBufferedFileReader(senseFile);
		List<StringIntPair> list;
		String line, sense;
		int treeId, wordId;
		String[] tmp;
		
		while ((line = fin.readLine()) != null)
		{
			tmp    = line.split(" ");
			treeId = Integer.parseInt(tmp[1]);
			wordId = Integer.parseInt(tmp[2]);
			sense  = tmp[3].substring(0, tmp[3].length()-2)+"."+tmp[4];
			
			if (map.containsKey(treeId))
				list = map.get(treeId);
			else
			{
				list = new ArrayList<StringIntPair>();
				map.put(treeId, list);
			}
			
			list.add(new StringIntPair(sense, wordId));
		}
		
		fin.close();
		return map;
	}
	
	private IntObjectOpenHashMap<List<String>> getNames(String nameFile) throws Exception
	{
		if (!new File(nameFile).isFile())	return null;
		IntObjectOpenHashMap<List<String>> map = new IntObjectOpenHashMap<List<String>>();
		BufferedReader fin = UTInput.createBufferedFileReader(nameFile);
		List<String> list;
		int treeId, i;
		String[] tmp;
		String line;
		
		while ((line = fin.readLine()) != null)
		{
			tmp    = line.split(" ");
			treeId = Integer.parseInt(tmp[1]);
			list   = new ArrayList<String>();
			
			for (i=2; i<tmp.length; i++)
				list.add(tmp[i]);

			map.put(treeId, list);
		}
		
		fin.close();
		return map;
	}
	
	private void addRolesets(CTTree cTree, DEPTree dTree, List<PBInstance> instances)
	{
		if (instances == null)	return;
		DEPNode pred;
		
		for (PBInstance inst : instances)
		{
			if (isPBSkip(inst, cTree))	continue;
			pred = dTree.get(cTree.getTerminal(inst.predId).getTokenId()+1);
			pred.addFeat(DEPLib.FEAT_PB, inst.roleset);
			
			if (s_language.equals(AbstractReader.LANG_EN))
				pred.lemma = inst.roleset.substring(0, inst.roleset.lastIndexOf("."));
		}
	}
	
	private void addWordSenses(CTTree cTree, DEPTree dTree, List<StringIntPair> senses)
	{
		if (senses == null)	return;
		DEPNode node;
		
		for (StringIntPair sense : senses)
		{
			node = dTree.get(cTree.getTerminal(sense.i).getTokenId()+1);
			node.addFeat(DEPLibEn.FEAT_WS, sense.s);
		}
	}
	
	private void addNames(CTTree cTree, DEPTree dTree, List<String> names)
	{
		if (names == null)	return;
		String[] t0, t1;
		int bIdx, eIdx, i;
		String ex;
		DEPNode node;
		
		for (String name : names)
		{
			t0 = name.split("-");
			ex = t0[1];
			t1 = t0[0].split(":");
			bIdx = Integer.parseInt(t1[0]);
			eIdx = Integer.parseInt(t1[1]);
			
			node = dTree.get(cTree.getTerminal(bIdx).getTokenId()+1);
			node.namex = "B-"+ex;
			
			for (i=bIdx+1; i<=eIdx; i++)
			{
				node = dTree.get(cTree.getTerminal(i).getTokenId()+1);
				node.namex = "I-"+ex;
			}
		}
	}
	
	private DEPTree getNullTree()
	{
		DEPTree tree = new DEPTree();
		
		DEPNode dummy = new DEPNode(1, "DUMMY", "dummy", "NN", new DEPFeat());
		dummy.setHead(tree.get(0), DEPLib.DEP_DEP);
		
		tree.add(dummy);
		return tree;
	}

	public static void main(String[] args)
	{
		new C2DConvert(args);
	}
}
