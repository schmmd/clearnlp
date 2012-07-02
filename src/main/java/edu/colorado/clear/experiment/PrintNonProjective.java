package edu.colorado.clear.experiment;

import java.io.PrintStream;

import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.reader.DEPReader;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;

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
