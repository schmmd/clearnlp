package com.googlecode.clearnlp.experiment;

import java.io.File;
import java.io.PrintStream;

import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;


public class DEPSplit
{
	static public void main(String[] args)
	{
		DEPReader reader = new DEPReader(0, 1, 3, 5, 6, 9, 10);
		String[] filelist = UTFile.getSortedFileList(args[0]);
		PrintStream[] fout = new PrintStream[10];
		DEPTree tree;
		int i;
		
		for (i=0; i<fout.length; i++)
			fout[i] = UTOutput.createPrintBufferedFileStream(args[1]+File.separator+i+".dep");
		
		for (String filename : filelist)
		{
			reader.open(UTInput.createBufferedFileReader(filename));
			
			while ((tree = reader.next()) != null)
			{
				i = (tree.size() > 101) ? 9 : (tree.size()-2) / 10;
				fout[i].println(tree.toStringCoNLL()+"\n");
			}	
			
			reader.close();
		}
		
		for (i=0; i<fout.length; i++)
			fout[i].close();
	}

}
