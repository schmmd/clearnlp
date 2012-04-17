package edu.colorado.clear.experiment;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.colorado.clear.morphology.EnglishMPAnalyzer;
import edu.colorado.clear.reader.AbstractColumnReader;
import edu.colorado.clear.run.AbstractRun;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;

public class Merge
{
	public Merge(String[] args) throws Exception
	{
		EnglishMPAnalyzer morph = AbstractRun.getMPAnalyzerEn(args[0]);
		BufferedReader fin0 = UTInput.createBufferedFileReader(args[1]);
		BufferedReader fin1 = UTInput.createBufferedFileReader(args[2]);
		PrintStream fout = UTOutput.createPrintBufferedFileStream(args[3]);
		String line;
		String[] tmp0, tmp1;
		List<String> list;
		StringBuilder build;
		
		while ((line = fin0.readLine()) != null)
		{
			tmp0 = line.split(AbstractColumnReader.DELIM_COLUMN);
			tmp1 = fin1.readLine().split(AbstractColumnReader.DELIM_COLUMN);
			
			if (line.trim().isEmpty())
			{
				fout.println();
				continue;
			}
			
			list = new ArrayList<String>();
			Collections.addAll(list, tmp1);
			
			list.add(3, morph.getLemma(tmp0[0], tmp0[1]));
			list.add(5, tmp0[1]);
			
			build = new StringBuilder();
			build.append(list.get(0));
			
			for (int i=1; i<list.size(); i++)
			{
				build.append(AbstractColumnReader.DELIM_COLUMN);
				build.append(list.get(i));
			}
			
			fout.println(build.toString());
		}
		
		fout.close();
	}
	
	static public void main(String[] args)
	{
		try {
			new Merge(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
