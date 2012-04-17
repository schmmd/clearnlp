package edu.colorado.clear.run;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;

import edu.colorado.clear.classification.model.AbstractModel;
import edu.colorado.clear.classification.train.AbstractTrainSpace;
import edu.colorado.clear.morphology.EnglishMPAnalyzer;
import edu.colorado.clear.reader.AbstractColumnReader;
import edu.colorado.clear.reader.AbstractReader;
import edu.colorado.clear.reader.DAGReader;
import edu.colorado.clear.reader.DEPReader;
import edu.colorado.clear.reader.POSReader;
import edu.colorado.clear.reader.SRLReader;
import edu.colorado.clear.util.UTXml;

abstract public class AbstractRun
{
	static protected final String ENTRY_FEATURE	= "FEATURE";
	static protected final String ENTRY_MODEL	= "MODEL";
	
	static final public String TAG_READER				= "reader";
	static final public String TAG_READER_TYPE			= "type";
	static final public String TAG_READER_COLUMN		= "column";
	static final public String TAG_READER_COLUMN_INDEX	= "index";
	static final public String TAG_READER_COLUMN_FIELD	= "field";
	
	static final public String TAG_TRAIN				= "train";
	static final public String TAG_TRAIN_ALGORITHM		= "algorithm";
	static final public String TAG_TRAIN_ALGORITHM_NAME	= "name";
	static final public String TAG_TRAIN_THREADS		= "threads";
	
	public void initArgs(String[] args)
	{
		CmdLineParser cmd = new CmdLineParser(this);
		
		try
		{
			cmd.parseArgument(args);
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			cmd.printUsage(System.err);
			System.exit(1);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	protected AbstractReader<?> getReader(Element eReader)
	{
		String type = UTXml.getTrimmedAttribute(eReader, TAG_READER_TYPE);
		
		if (type.equals(AbstractReader.TYPE_POS))
			return getPOSReader(eReader);
		else if (type.equals(AbstractReader.TYPE_DEP))
			return getDEPReader(eReader);
		else if (type.equals(AbstractReader.TYPE_DAG))
			return getDAGReader(eReader);
		else if (type.equals(AbstractReader.TYPE_SRL))
			return getSRLReader(eReader);
		
		return null;
	}
	
	/** Called by {@link AbstractRun#getReader(Element)}. */
	private POSReader getPOSReader(Element eReader)
	{
		ObjectIntOpenHashMap<String> map = getFieldMap(eReader);
		
		int iForm = map.get(AbstractColumnReader.FIELD_FORM) - 1;
		int iPos  = map.get(AbstractColumnReader.FIELD_POS)  - 1;
		
		if (iForm < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_FORM);
			System.exit(1);
		}
		
		return new POSReader(iForm, iPos);
	}
	
	private DEPReader getDEPReader(Element eReader)
	{
		ObjectIntOpenHashMap<String> map = getFieldMap(eReader);
		
		int iId		= map.get(AbstractColumnReader.FIELD_ID)	 - 1;
		int iForm	= map.get(AbstractColumnReader.FIELD_FORM)	 - 1;
		int iLemma	= map.get(AbstractColumnReader.FIELD_LEMMA)	 - 1;
		int iPos	= map.get(AbstractColumnReader.FIELD_POS)	 - 1;
		int iFeats	= map.get(AbstractColumnReader.FIELD_FEATS)	 - 1;
		int iHeadId	= map.get(AbstractColumnReader.FIELD_HEADID) - 1;
		int iDeprel	= map.get(AbstractColumnReader.FIELD_DEPREL) - 1;
		
		if (iId < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_ID);
			System.exit(1);
		}
		else if (iForm < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_FORM);
			System.exit(1);
		}
		else if (iLemma < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_LEMMA);
			System.exit(1);
		}
		else if (iPos < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_POS);
			System.exit(1);
		}
		else if (iFeats < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_FEATS);
			System.exit(1);
		}
		
		return new DEPReader(iId, iForm, iLemma, iPos, iFeats, iHeadId, iDeprel);
	}
	
	private DAGReader getDAGReader(Element eReader)
	{
		ObjectIntOpenHashMap<String> map = getFieldMap(eReader);
		
		int iId		= map.get(AbstractColumnReader.FIELD_ID)	 - 1;
		int iForm	= map.get(AbstractColumnReader.FIELD_FORM)	 - 1;
		int iLemma	= map.get(AbstractColumnReader.FIELD_LEMMA)	 - 1;
		int iPos	= map.get(AbstractColumnReader.FIELD_POS)	 - 1;
		int iFeats	= map.get(AbstractColumnReader.FIELD_FEATS)	 - 1;
		int iXheads	= map.get(AbstractColumnReader.FIELD_XHEADS) - 1;
		
		if (iId < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_ID);
			System.exit(1);
		}
		else if (iForm < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_FORM);
			System.exit(1);
		}
		else if (iLemma < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_LEMMA);
			System.exit(1);
		}
		else if (iPos < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_POS);
			System.exit(1);
		}
		else if (iFeats < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_FEATS);
			System.exit(1);
		}
		
		return new DAGReader(iId, iForm, iLemma, iPos, iFeats, iXheads);
	}
	
	private SRLReader getSRLReader(Element eReader)
	{
		ObjectIntOpenHashMap<String> map = getFieldMap(eReader);
		
		int iId		= map.get(AbstractColumnReader.FIELD_ID)	 - 1;
		int iForm	= map.get(AbstractColumnReader.FIELD_FORM)	 - 1;
		int iLemma	= map.get(AbstractColumnReader.FIELD_LEMMA)	 - 1;
		int iPos	= map.get(AbstractColumnReader.FIELD_POS)	 - 1;
		int iFeats	= map.get(AbstractColumnReader.FIELD_FEATS)	 - 1;
		int iHeadId	= map.get(AbstractColumnReader.FIELD_HEADID) - 1;
		int iDeprel	= map.get(AbstractColumnReader.FIELD_DEPREL) - 1;
		int iSheads	= map.get(AbstractColumnReader.FIELD_SHEADS) - 1;
		
		if (iId < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_ID);
			System.exit(1);
		}
		else if (iForm < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_FORM);
			System.exit(1);
		}
		else if (iLemma < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_LEMMA);
			System.exit(1);
		}
		else if (iPos < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_POS);
			System.exit(1);
		}
		else if (iFeats < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_FEATS);
			System.exit(1);
		}
		else if (iHeadId < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_HEADID);
			System.exit(1);
		}
		else if (iDeprel < 0)
		{
			System.out.printf("The '%s' field must be specified in the configuration file.\n", AbstractColumnReader.FIELD_DEPREL);
			System.exit(1);
		}
		
		return new SRLReader(iId, iForm, iLemma, iPos, iFeats, iHeadId, iDeprel, iSheads);
	}
	
	/** Called by {@link AbstractRun#getReader(Element)}. */
	private ObjectIntOpenHashMap<String> getFieldMap(Element eReader)
	{
		NodeList list = eReader.getElementsByTagName(TAG_READER_COLUMN);
		int i, index, size = list.getLength();
		String field;
		Element element;
		
		ObjectIntOpenHashMap<String> map = new ObjectIntOpenHashMap<String>();
		
		for (i=0; i<size; i++)
		{
			element = (Element)list.item(i);
			field   = UTXml.getTrimmedAttribute(element, TAG_READER_COLUMN_FIELD);
			index   = Integer.parseInt(element.getAttribute(TAG_READER_COLUMN_INDEX));
			
			map.put(field, index);
		}
		
		return map;
	}
	
	protected String[] getSortedFileList(String fileDir)
	{
		List<String> list = new ArrayList<String>();
		
		for (String filepath : new File(fileDir).list())
		{
			filepath = fileDir + File.separator + filepath;
			
			if (new File(filepath).isFile())
				list.add(filepath);
		}
		
		String[] filelist = new String[list.size()];
		list.toArray(filelist);
		Arrays.sort(filelist);
		
		return filelist;
	}
	
	protected AbstractModel getModel(Element eTrain, AbstractTrainSpace space, int index)
	{
		Element eThreads   = UTXml.getFirstElementByTagName(eTrain, TAG_TRAIN_THREADS); 
		int     numThreads = Integer.parseInt(UTXml.getTrimmedTextContent(eThreads));
		
		NodeList list = eTrain.getElementsByTagName(TAG_TRAIN_ALGORITHM);
		Element  eAlgorithm;
		String   name;
		
		eAlgorithm = (Element)list.item(index);
		name       = UTXml.getTrimmedAttribute(eAlgorithm, TAG_TRAIN_ALGORITHM_NAME);
		
		if (name.equals("liblinear"))
		{
			byte solver = Byte  .parseByte  (UTXml.getTrimmedAttribute(eAlgorithm, "solver"));
			double cost = Double.parseDouble(UTXml.getTrimmedAttribute(eAlgorithm, "cost"));
			double eps  = Double.parseDouble(UTXml.getTrimmedAttribute(eAlgorithm, "eps"));
			double bias = Double.parseDouble(UTXml.getTrimmedAttribute(eAlgorithm, "bias"));
			
			return getLiblinearModel(space, numThreads, solver, cost, eps, bias);
		}
		
		return null;
	}
	
	static public AbstractModel getLiblinearModel(AbstractTrainSpace space, int numThreads, byte solver, double cost, double eps, double bias)
	{
		space.build();
		System.out.println("Liblinear:");
		System.out.printf("- solver=%d, cost=%f, eps=%f, bias=%f\n", solver, cost, eps, bias);
		return LiblinearTrain.getModel(space, numThreads, solver, cost, eps, bias);
	}
	
	static public EnglishMPAnalyzer getMPAnalyzerEn(String dictFile)
	{
		try
		{
			return new EnglishMPAnalyzer(new ZipInputStream(new FileInputStream(dictFile)));
		}
		catch (Exception e) {e.printStackTrace();}
		
		return null;
	}
}