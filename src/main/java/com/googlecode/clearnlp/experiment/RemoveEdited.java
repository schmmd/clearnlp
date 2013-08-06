package com.googlecode.clearnlp.experiment;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.clearnlp.constituent.CTLibEn;
import com.googlecode.clearnlp.constituent.CTNode;
import com.googlecode.clearnlp.constituent.CTReader;
import com.googlecode.clearnlp.constituent.CTTree;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;

public class RemoveEdited
{
	public RemoveEdited(String inputPath, String parseExt, String outputExt)
	{
		remove(inputPath, parseExt, outputExt);
	}
	
	public void remove(String inputPath, String parseExt, String outputExt)
	{
		File file = new File(inputPath);
		
		if (file.isDirectory())
		{
			for (String filePath : file.list())
				remove(inputPath+"/"+filePath, parseExt, outputExt);
		}
		else if (inputPath.endsWith(parseExt))
		{
			PrintStream fout = UTOutput.createPrintBufferedFileStream(UTFile.replaceExtension(inputPath, outputExt));
			CTReader reader = new CTReader(UTInput.createBufferedFileReader(inputPath));
			CTTree tree;
			
			while ((tree = reader.nextTree()) != null)
			{
				remove(inputPath, tree.getRoot());
				fout.println(tree.toString()+"\n");
			}
			
			fout.close();
			reader.close();
		} 
	}
	
	public void remove(String inputPath, CTNode curr)
	{
		List<CTNode> remove = new ArrayList<CTNode>();
		List<CTNode> children = curr.getChildren();
		
		for (CTNode child : children)
		{
			if (child.isPTag(CTLibEn.PTAG_EDITED) || (child.getChildrenSize() == 1 &&  child.getChild(0).isPTag(CTLibEn.PTAG_EDITED)))
				remove.add(child);
			else if (child.isPhrase())
				remove(inputPath, child);
		}
		
		if (remove.size() == children.size())
			System.out.println(inputPath+"\n"+curr.toString());
		
		for (CTNode child : remove)
			curr.removeChild(child);
	}

	static public void main(String[] args)
	{
		new RemoveEdited(args[0], args[1], args[2]);
	}
}
