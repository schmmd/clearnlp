package com.googlecode.clearnlp.experiment;

import java.io.PrintStream;

import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;


public class PrintNonProjective
{
	public PrintNonProjective(String inputFile, String outputFile)
	{
		DEPReader reader = new DEPReader(0, 1, 2, 4, 6, 7, 8);
		reader.open(UTInput.createBufferedFileReader(inputFile));
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		DEPTree tree;
		String mark;
		int i;
		
		while ((tree = reader.next()) != null)
		{
			tree.setDependents();
			mark = tree.getNonProjectiveSet().isEmpty() ? "P" : "N";
			
			for (i=tree.size(); i>1; i--)
				fout.println(mark);
			
			fout.println();
		}

		fout.close();
	}
	
	static public void main(String[] args)
	{
		new PrintNonProjective(args[0], args[1]);
	}
}
