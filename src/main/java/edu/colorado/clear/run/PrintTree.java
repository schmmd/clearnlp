package edu.colorado.clear.run;

import java.io.File;

import edu.colorado.clear.constituent.CTReader;
import edu.colorado.clear.constituent.CTTree;
import edu.colorado.clear.util.UTInput;

public class PrintTree
{
	static public void main(String[] args)
	{
		String treeDir = args[0];
		String treeFile = args[1];
		int treeId = Integer.parseInt(args[2]);
		
		CTReader reader = new CTReader(UTInput.createBufferedFileReader(treeDir+File.separator+treeFile));
		CTTree tree = null;
		int i;
		
		for (i=0; i<=treeId; i++)
			tree = reader.nextTree();
		
		System.out.println(tree.toString(true,true));
	}
}
