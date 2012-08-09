/**
* Copyright (c) 2011, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package com.googlecode.clearnlp.run;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.googlecode.clearnlp.classification.model.AbstractModel;
import com.googlecode.clearnlp.classification.train.AbstractTrainSpace;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.morphology.DefaultMPAnalyzer;
import com.googlecode.clearnlp.morphology.EnglishMPAnalyzer;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.reader.DAGReader;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.reader.POSReader;
import com.googlecode.clearnlp.reader.SRLReader;
import com.googlecode.clearnlp.util.UTXml;


/**
 * Abstract run.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
abstract public class AbstractRun
{
	static final protected String ENTRY_FEATURE	= "FEATURE";
	static final protected String ENTRY_MODEL	= "MODEL";
	
	static final public String TAG_READER				= "reader";
	static final public String TAG_READER_TYPE			= "type";
	static final public String TAG_READER_COLUMN		= "column";
	static final public String TAG_READER_COLUMN_INDEX	= "index";
	static final public String TAG_READER_COLUMN_FIELD	= "field";
	
	static final public String TAG_TRAIN				= "train";
	static final public String TAG_TRAIN_ALGORITHM		= "algorithm";
	static final public String TAG_TRAIN_ALGORITHM_NAME	= "name";
	static final public String TAG_TRAIN_THREADS		= "threads";
	
	static final public String TAG_LANGUAGE		= "language";
	static final public String TAG_MORPH_DICT	= "morph_dict";
	
	/** Initializes arguments using args4j. */
	protected void initArgs(String[] args)
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
	
	protected String getLanguage(Element eConfig)
	{
		Element eLanguage = UTXml.getFirstElementByTagName(eConfig, TAG_LANGUAGE);
		return UTXml.getTrimmedTextContent(eLanguage);
	}
	
	protected AbstractReader<?> getReader(Element eConfig)
	{
		Element eReader = UTXml.getFirstElementByTagName(eConfig, TAG_READER);
		String  type    = UTXml.getTrimmedAttribute(eReader, TAG_READER_TYPE);
		
		if      (type.equals(AbstractReader.TYPE_POS))
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
	private ObjectIntOpenHashMap<String> getFieldMap(Element eReader)
	{
		NodeList list = eReader.getElementsByTagName(TAG_READER_COLUMN);
		int i, index, size = list.getLength();
		Element element;
		String field;
		
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
	
	/** Called by {@link AbstractRun#getReader(Element)}. */
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
	
	/** Called by {@link AbstractRun#getReader(Element)}. */
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
	
	/** Called by {@link AbstractRun#getModel(Element, AbstractTrainSpace, int)}. */
	private AbstractModel getLiblinearModel(AbstractTrainSpace space, int numThreads, byte solver, double cost, double eps, double bias)
	{
		space.build();
		System.out.println("Liblinear:");
		System.out.printf("- solver=%d, cost=%f, eps=%f, bias=%f\n", solver, cost, eps, bias);
		return LiblinearTrain.getModel(space, numThreads, solver, cost, eps, bias);
	}
	
	protected AbstractMPAnalyzer getMPAnalyzer(Element eConfig)
	{
		Element eMorphDict = UTXml.getFirstElementByTagName(eConfig, TAG_MORPH_DICT);
		String  language   = getLanguage(eConfig);
		String  morphDict  = UTXml.getTrimmedTextContent(eMorphDict);

		return getMPAnalyzer(language, morphDict);
	}
	
	/**
	 * Returns a morphological analyzer for the specific language.
	 * @param language the language.
	 * @param dictFile the dictionary filename.
	 * @return a morphological analyzer for the specific language.
	 */
	static public AbstractMPAnalyzer getMPAnalyzer(String language, String dictFile)
	{
		if (language.equals(AbstractReader.LANG_EN))
			return new EnglishMPAnalyzer(dictFile);
		
		return new DefaultMPAnalyzer();
	}
}