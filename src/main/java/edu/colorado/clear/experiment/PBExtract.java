package edu.colorado.clear.experiment;

import java.io.PrintStream;

import edu.colorado.clear.constituent.CTNode;
import edu.colorado.clear.constituent.CTTree;
import edu.colorado.clear.propbank.PBInstance;
import edu.colorado.clear.propbank.PBLib;
import edu.colorado.clear.util.UTOutput;

public class PBExtract
{
	public PBExtract(String propFile, String treeDir, String outFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outFile);
		
		for (PBInstance instance : PBLib.getPBInstances(propFile, treeDir, false))
		{
			if (instance.getArgSize() < 2)	continue;
			
			StringBuilder build = new StringBuilder();
			CTTree tree = instance.getTree();
			
			build.append(instance.treePath);	build.append("\t");
			build.append(instance.treeId);		build.append("\t");
			build.append(instance.predId);		build.append("\t");
			build.append(instance.roleset);		build.append("\t");
			build.append(tree.getTerminal(instance.predId).getTokenId());
			build.append("\t");
			build.append(getRawLine(tree, instance.predId));
			
			fout.println(build.toString());
		}
		
		fout.close();
	}
	
	private String getRawLine(CTTree tree, int predId)
	{
		StringBuilder build = new StringBuilder();
		
		for (CTNode node : tree.getTokens())
		{
			build.append(" ");
			
			if (node.getTerminalId() == predId)
			{
				build.append("[");
				build.append(node.form);
				build.append("]");
			}
			else
				build.append(node.form);
		}
		
		return build.substring(1);
	}

	public static void main(String[] args)
	{
		String propFile = args[0];
		String treeDir  = args[1];
		String outFile  = args[2];
		
		new PBExtract(propFile, treeDir, outFile);
	}

}
