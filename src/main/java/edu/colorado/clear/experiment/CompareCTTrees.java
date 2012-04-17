package edu.colorado.clear.experiment;

import java.io.File;

import edu.colorado.clear.constituent.CTReader;
import edu.colorado.clear.constituent.CTTree;
import edu.colorado.clear.io.FileExtFilter;
import edu.colorado.clear.util.UTInput;

public class CompareCTTrees
{
	public CompareCTTrees(String treeDir, String ext1, String ext2)
	{
		CTReader reader1, reader2;
		CTTree tree1, tree2;
		String filename2;
	//	int n;
		
		for (String filename1 : new File(treeDir).list(new FileExtFilter(ext1)))
		{
		//	System.out.print(filename1+": ");
			filename1 = treeDir + File.separator + filename1;
			filename2 = FileExtFilter.replaceExt(filename1, ext1, ext2);
			reader1   = new CTReader(UTInput.createBufferedFileReader(filename1));
			reader2   = new CTReader(UTInput.createBufferedFileReader(filename2));
			
		//	for (n=0; (tree1 = reader1.nextTree()) != null; n++)
			while ((tree1 = reader1.nextTree()) != null)
			{
				tree2 = reader2.nextTree();
				
				if (tree1.getTerminals().size() != tree2.getTerminals().size())
				{
					System.out.println(filename2);
					System.out.println(tree1+"\n"+tree2);
					return;
				}
			}
			
		//	System.out.println(n);
		}
	}
	
	static public void main(String[] args)
	{
		new CompareCTTrees(args[0], args[1], args[2]);
	}
}
