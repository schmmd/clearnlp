package edu.colorado.clear.dependency;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class SRLLib
{
	static public final String DELIM_PATH_UP	= "^";
	static public final String DELIM_PATH_DOWN	= "|";
	static public final String DELIM_SUBCAT		= "_";
	
	static private Pattern P_ARGN = Pattern.compile("^(A|C-A|R-A|ARG)\\d");
	
	static public boolean isNumberedArgument(String label)
	{
		return P_ARGN.matcher(label).find();
	}
	
	static public List<List<DEPArc>> getArgumentList(DEPTree tree)
	{
		int i, size = tree.size();
		List<DEPArc> args;
		DEPNode node;
		
		List<List<DEPArc>> list = new ArrayList<List<DEPArc>>();
		for (i=0; i<size; i++)	list.add(new ArrayList<DEPArc>());
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			for (DEPArc arc : node.getSHeads())
			{
				args = list.get(arc.getNode().id);
				args.add(new DEPArc(node, arc.getLabel()));
			}
		}
		
		return list;
	}
}
