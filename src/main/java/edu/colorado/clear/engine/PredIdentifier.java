package edu.colorado.clear.engine;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.regex.Matcher;

import edu.colorado.clear.classification.model.AbstractModel;
import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.classification.vector.StringFeatureVector;
import edu.colorado.clear.dependency.DEPLib;
import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.feature.xml.DEPFtrXml;
import edu.colorado.clear.feature.xml.FtrToken;

public class PredIdentifier extends AbstractTool
{ 
	private byte				i_flag;
	private DEPFtrXml			f_xml;
	private StringTrainSpace	s_space;
	private StringModel			s_model;
	private DEPTree				d_tree;
	private int					i_beta;
	
	public PredIdentifier(DEPFtrXml xml)
	{
		i_flag = FLAG_LEXICA;
		f_xml  = xml;
	}
	
	/** Constructs a dependency parser for training. */
	public PredIdentifier(DEPFtrXml xml, StringTrainSpace space)
	{
		i_flag  = FLAG_TRAIN;
		f_xml   = xml;
		s_space = space;
	}
	
	/** Constructs a dependency parser for predicting. */
	public PredIdentifier(DEPFtrXml xml, StringModel model)
	{
		i_flag  = FLAG_PREDICT;
		f_xml   = xml;
		s_model = model;
	}
	
	public PredIdentifier(DEPFtrXml xml, BufferedReader fin)
	{
		i_flag  = FLAG_PREDICT;
		f_xml   = xml;
		s_model = new StringModel(fin);
	}
	
	/** Called by {@link PredIdentifier#parse(DEPTree)}. */
	public void init(DEPTree tree)
	{
		d_tree = tree;
		tree.setDependents();
	}
	
	public void saveModel(PrintStream fout)
	{
		s_model.save(fout);
	}
	
	public StringModel getModel()
	{
		return s_model;
	}
	
	public void identify(DEPTree tree)
	{
		init(tree);

		int size = d_tree.size();
		DEPNode node;
		
		for (i_beta=1; i_beta<size; i_beta++)
		{
			node = tree.get(i_beta);
			
			if (node.pos.startsWith("VB"))
				identifyAux(node);
		}
	}
	
	private void identifyAux(DEPNode node)
	{
		StringFeatureVector vector = getFeatureVector(f_xml);
		String label;
		
		if (i_flag == FLAG_TRAIN)
		{
			label = (node.getFeat(DEPLib.FEAT_PB) == null) ? AbstractModel.LABEL_FALSE :  AbstractModel.LABEL_TRUE;
			s_space.addInstance(label, vector);		
		}
		else if (i_flag == FLAG_PREDICT)
		{
			label = s_model.predictBest(vector).label;
			
			if (label.equals(AbstractModel.LABEL_TRUE))
				node.namex = node.lemma+".XX";
		}
	}
	
	protected String getField(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(DEPFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(DEPFtrXml.F_LEMMA))
		{
			return node.lemma;
		}
		else if (token.isField(DEPFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(DEPFtrXml.F_DEPREL))
		{
			return node.getLabel();
		}
		else if ((m = DEPFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		
		return null;
	}
	
	protected String[] getFields(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
	//	String[] fields;
		
	/*	if (token.isField(DepFtrXml.F_DEPRELS))
		{
			Set<String> set = new HashSet<String>();
			for (DPArc arc : node.getDependents())
				set.add(arc.getLabel());
			if (set.isEmpty())	return null;
			
			int i = 0, size = set.size();
			fields = new String[size];
			
			for (String deprel : set)
				fields[i++] = deprel;
			
			return fields;
		}*/
		
		return null;
	}
	
	private DEPNode getNode(FtrToken token)
	{
		DEPNode node = getNodeAux(token);
		if (node == null)	return null;
		
		if (token.relation != null)
		{
			     if (token.isRelation(DEPFtrXml.R_H))	node = node.getHead();
			else if (token.isRelation(DEPFtrXml.R_LMD))	node = node.getLeftNearestDependent();
			else if (token.isRelation(DEPFtrXml.R_RMD))	node = node.getRightNearestDependent();			     
		}
		
		return node;
	}
	
	/** Called by {@link PredIdentifier#getNode(FtrToken)}. */
	private DEPNode getNodeAux(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_beta);
		
		int cIndex = i_beta + token.offset;
		
		if (0 < cIndex && cIndex < d_tree.size())
			return d_tree.get(cIndex);
		
		return null;
	}
}
