package edu.colorado.clear.run;

import java.io.BufferedReader;
import java.io.IOException;

import org.kohsuke.args4j.Option;

import edu.colorado.clear.dependency.DEPLib;
import edu.colorado.clear.dependency.SRLEval;
import edu.colorado.clear.reader.AbstractColumnReader;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.pair.StringIntPair;

public class SRLEvaluate extends AbstractRun
{
	@Option(name="-g", usage="the gold-standard file (input; required)", required=true, metaVar="<filename>")
	private String s_goldFile;
	@Option(name="-s", usage="the system file (input; required)", required=true, metaVar="<filename>")
	private String s_autoFile;
	@Option(name="-gi", usage="the column index of semantic arguments in the gold-standard file (input; required)", required=true, metaVar="<integer>")
	private int    i_goldIndex;
	@Option(name="-si", usage="the column index of semantic arguments in the sytem file (input; required)", required=true, metaVar="<integer>")
	private int    i_autoIndex;
	
	public SRLEvaluate() {}
	
	public SRLEvaluate(String[] args)
	{
		initArgs(args);
		run(s_goldFile, s_autoFile, i_goldIndex, i_autoIndex);
	}
	
	public void run(String goldFile, String autoFile, int goldIndex, int autoIndex)
	{
		BufferedReader fGold = UTInput.createBufferedFileReader(goldFile);
		BufferedReader fAuto = UTInput.createBufferedFileReader(autoFile);
		StringIntPair[] gHeads, aHeads;
		SRLEval eval = new SRLEval();
		String[] gold, auto;
		String line;
		
		try
		{
			while ((line = fGold.readLine()) != null)
			{
				gold = line.split(AbstractColumnReader.DELIM_COLUMN);
				auto = fAuto.readLine().split(AbstractColumnReader.DELIM_COLUMN);
				
				line = line.trim();
				if (line.isEmpty())	 continue;
				
				gHeads = toSHeads(gold[goldIndex]);
				aHeads = toSHeads(auto[autoIndex]);
				eval.evaluate(gHeads, aHeads);
			}
		}
		catch (IOException e) {e.printStackTrace();}
		
		eval.print();
	}
	
	private StringIntPair[] toSHeads(String sHeads)
	{
		if (sHeads.equals(AbstractColumnReader.BLANK_COLUMN))
			return new StringIntPair[0];
			
		String[] heads = sHeads.split(DEPLib.DELIM_HEADS), tmp;
		int i, size = heads.length, headId;
		
		StringIntPair[] p = new StringIntPair[size];
		String label;
		
		for (i=0; i<size; i++)
		{
			tmp    = heads[i].split(DEPLib.DELIM_HEADS_KEY);
			headId = Integer.parseInt(tmp[0]);
			label  = tmp[1];
			p[i]   = new StringIntPair(label, headId); 
		}
		
		return p;
	}

	static public void main(String[] args)
	{
		new SRLEvaluate(args);
	}
}
