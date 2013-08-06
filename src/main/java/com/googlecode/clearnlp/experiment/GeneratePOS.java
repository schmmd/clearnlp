package com.googlecode.clearnlp.experiment;

import java.io.File;
import java.io.PrintStream;

import com.googlecode.clearnlp.constituent.CTNode;
import com.googlecode.clearnlp.constituent.CTReader;
import com.googlecode.clearnlp.constituent.CTTree;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;

public class GeneratePOS
{
	public GeneratePOS(String inputDir, String inputExt, String outputDir)
	{
		File file = new File(inputDir);
		String outputFile;
		PrintStream fout;
		
		for (File section : file.listFiles())
		{
			if (section.isDirectory())
			{
				outputFile = outputDir+"/"+section.getName()+".pos";
				fout = UTOutput.createPrintBufferedFileStream(outputFile);
				System.out.println(outputFile);
				
				for (String inputFile : UTFile.getSortedFileList(section.getAbsolutePath()))
					printTree(fout, inputFile);
				
				fout.close();
			}
		}
	}
	
	private void printTree(PrintStream fout, String inputFile)
	{
		CTReader reader = new CTReader();
		StringBuilder build;
		CTTree tree;
		
		reader.open(UTInput.createBufferedFileReader(inputFile));
		
		while ((tree = reader.nextTree()) != null)
		{
			build = new StringBuilder();
			
			for (CTNode node : tree.getTerminals())
			{
				build.append(node.form);
				build.append("\t");
				build.append(node.pTag);
				build.append("\n");
			}
			
			fout.println(build.toString());
		}
		
		reader.close();
	}
	
	static public void main(String[] args)
	{
		new GeneratePOS(args[0], args[1], args[2]);
	}
}
