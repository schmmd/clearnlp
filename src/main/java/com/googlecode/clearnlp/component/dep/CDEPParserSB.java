/**
* Copyright 2012-2013 University of Massachusetts Amherst
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
*   
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.googlecode.clearnlp.component.dep;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.googlecode.clearnlp.classification.algorithm.AbstractAlgorithm;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.component.AbstractStatisticalComponent;
import com.googlecode.clearnlp.dependency.DEPHead;
import com.googlecode.clearnlp.dependency.DEPLabel;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPLibEn;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPState;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.nlp.NLPLib;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;
import com.googlecode.clearnlp.util.map.Prob1DMap;
import com.googlecode.clearnlp.util.pair.ObjectDoublePair;
import com.googlecode.clearnlp.util.pair.Pair;
import com.googlecode.clearnlp.util.pair.StringIntPair;
import com.googlecode.clearnlp.util.triple.Triple;

/**
 * Dependency parser using selectional branching.
 * @since 1.3.2
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class CDEPParserSB extends AbstractStatisticalComponent
{
	private final Logger LOG = Logger.getLogger(this.getClass());
	
	protected final String ENTRY_CONFIGURATION = NLPLib.MODE_DEP_BACK + NLPLib.ENTRY_CONFIGURATION;
	protected final String ENTRY_FEATURE	   = NLPLib.MODE_DEP_BACK + NLPLib.ENTRY_FEATURE;
	protected final String ENTRY_LEXICA		   = NLPLib.MODE_DEP_BACK + NLPLib.ENTRY_LEXICA;
	protected final String ENTRY_MODEL		   = NLPLib.MODE_DEP_BACK + NLPLib.ENTRY_MODEL;
	
	protected final int LEXICA_PUNCTUATION = 0;
	
	protected final String LB_LEFT		= "L";
	protected final String LB_RIGHT		= "R";
	protected final String LB_NO		= "N";
	protected final String LB_SHIFT		= "S";
	protected final String LB_REDUCE	= "R";
	protected final String LB_PASS		= "P";
	
	protected IntOpenHashSet	s_reduce;
	protected Prob1DMap			p_punc;		// only for collecting
	protected Set<String>		s_punc;
	protected StringIntPair[]	g_heads;
	protected int				i_lambda, i_beta;

	protected Map<String,Pair<DEPLabel,DEPLabel>> m_labels;
	protected List<List<DEPHead>> l_2nd;
	protected int                 n_trans, n_beams;
	protected double              d_score, d_margin;
	protected boolean             b_first;
	
//	====================================== CONSTRUCTORS ======================================

	/** Constructs a dependency parser for collecting lexica. */
	public CDEPParserSB(JointFtrXml[] xmls)
	{
		super(xmls);
		p_punc = new Prob1DMap();
	}
	
	/** Constructs a dependency parsing for training. */
	public CDEPParserSB(JointFtrXml[] xmls, StringTrainSpace[] spaces, Object[] lexica, double margin, int beams)
	{
		super(xmls, spaces, lexica);
		d_margin = margin;
		n_beams  = beams;
	}
	
	/** Constructs a dependency parsing for developing. */
	public CDEPParserSB(JointFtrXml[] xmls, StringModel[] models, Object[] lexica, double margin, int beams)
	{
		super(xmls, models, lexica);
		d_margin = margin;
		n_beams  = beams;
	}
	
	/** Constructs a dependency parser for bootsrapping. */
	public CDEPParserSB(JointFtrXml[] xmls, StringTrainSpace[] spaces, StringModel[] models, Object[] lexica, double margin, int beams)
	{
		super(xmls, spaces, models, lexica);
		d_margin = margin;
		n_beams  = beams;
	}
	
	/** Constructs a dependency parser for decoding. */
	public CDEPParserSB(ZipInputStream in)
	{
		super(in);
	}
	
	@Override @SuppressWarnings("unchecked")
	protected void initLexia(Object[] lexica)
	{
		s_punc = (Set<String>)lexica[LEXICA_PUNCTUATION];
	}
	
//	====================================== LOAD/SAVE MODELS ======================================
	
	@Override
	public void loadModels(ZipInputStream zin)
	{
		int fLen = ENTRY_FEATURE.length(), mLen = ENTRY_MODEL.length();
		f_xmls   = new JointFtrXml[1];
		s_models = null;
		ZipEntry zEntry;
		String   entry;
				
		try
		{
			while ((zEntry = zin.getNextEntry()) != null)
			{
				entry = zEntry.getName();
				
				if      (entry.equals(ENTRY_CONFIGURATION))
					loadConfiguration(zin);
				else if (entry.startsWith(ENTRY_FEATURE))
					loadFeatureTemplates(zin, Integer.parseInt(entry.substring(fLen)));
				else if (entry.startsWith(ENTRY_MODEL))
					loadStatisticalModels(zin, Integer.parseInt(entry.substring(mLen)));
				else if (entry.equals(ENTRY_LEXICA))
					loadLexica(zin);
			}		
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	protected void loadConfiguration(ZipInputStream zin) throws Exception
	{
		BufferedReader fin = UTInput.createBufferedReader(zin);
		LOG.info("Loading configuration.\n");
		
		s_models = new StringModel[Integer.parseInt(fin.readLine())];
		n_beams  = Integer.parseInt(fin.readLine());
		d_margin = Double.parseDouble(fin.readLine());
	}
	
	protected void loadLexica(ZipInputStream zin) throws Exception
	{
		BufferedReader fin = new BufferedReader(new InputStreamReader(zin));
		LOG.info("Loading lexica.\n");

		s_punc = UTInput.getStringSet(fin);
	}

	@Override
	public void saveModels(ZipOutputStream zout)
	{
		try
		{
			saveConfiguration    (zout, ENTRY_CONFIGURATION);
			saveFeatureTemplates (zout, ENTRY_FEATURE);
			saveLexica           (zout);
			saveStatisticalModels(zout, ENTRY_MODEL);
			zout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	protected void saveConfiguration(ZipOutputStream zout, String entryName) throws Exception
	{
		zout.putNextEntry(new ZipEntry(entryName));
		PrintStream fout = UTOutput.createPrintBufferedStream(zout);
		LOG.info("Saving configuration.\n");
		
		fout.println(s_models.length);
		fout.println(n_beams);
		fout.println(d_margin);
		
		fout.flush();
		zout.closeEntry();
	}
	
	protected void saveLexica(ZipOutputStream zout) throws Exception
	{
		zout.putNextEntry(new ZipEntry(ENTRY_LEXICA));
		PrintStream fout = UTOutput.createPrintBufferedStream(zout);
		LOG.info("Saving lexica.\n");
		
		UTOutput.printSet(fout, s_punc);	fout.flush();
		zout.closeEntry();
	}
	
//	====================================== GETTERS AND SETTERS ======================================
	
	@Override
	public Object[] getLexica()
	{
		Object[] lexica = new Object[1];

		lexica[LEXICA_PUNCTUATION] = (i_flag == FLAG_LEXICA) ? p_punc.toSet(f_xmls[0].getPunctuationCutoff()) : s_punc;
		return lexica;
	}
	
	@Override
	public void countAccuracy(int[] counts)
	{
		int i, las = 0, uas = 0, ls = 0;
		StringIntPair p;
		DEPNode node;
		
		for (i=1; i<t_size; i++)
		{
			node = d_tree.get(i);
			
			p = g_heads[i];
			
			if (node.isDependentOf(d_tree.get(p.i)))
			{
				uas++;
				if (node.isLabel(p.s)) las++;
			}
			
			if (node.isLabel(p.s)) ls++;
		}
		
		counts[0] += t_size - 1;
		counts[1] += las;
		counts[2] += uas;
		counts[3] += ls;
	}
	
	public void setMargin(double margin)
	{
		d_margin = margin;
	}
	
	public void setBeams(int beams)
	{
		n_beams = beams;
	}
	
//	================================ PROCESS ================================
	
	@Override
	public void process(DEPTree tree)
	{
		init(tree);
		processAux();
	}
	
	/** Called by {@link CDEPParserSB#process(DEPTree)}. */
	protected void init(DEPTree tree)
	{
	 	d_tree  = tree;
	 	t_size  = tree.size();
	 	b_first = true;
	 	
	 	m_labels = new HashMap<String,Pair<DEPLabel,DEPLabel>>();
	 	l_2nd = new ArrayList<List<DEPHead>>();
	 	
	 	int i; for (i=0; i<t_size; i++)
	 		l_2nd.add(new ArrayList<DEPHead>());
	 	
	 	if (i_flag != FLAG_DECODE)
	 	{
	 		g_heads = tree.getHeads();
	 		tree.clearHeads();	
	 	}
	 	
	 	initAux();
	}
	
	protected void initAux()
	{
		i_lambda = 0;
	 	i_beta   = 1;
	 	d_score  = 0;
	 	n_trans  = 0;
	 	s_reduce = new IntOpenHashSet();
	}
	
	/** Called by {@link CDEPParserSB#process(DEPTree)}. */
	protected void processAux()
	{
		if (i_flag == FLAG_LEXICA)
			addLexica();
		else
		{
			List<Pair<String,StringFeatureVector>> insts = parse();
			
			if (i_flag == FLAG_TRAIN || i_flag == FLAG_BOOTSTRAP)
			{
				for (Pair<String,StringFeatureVector> inst : insts)
					s_spaces[0].addInstance(inst.o1, inst.o2);				
			}
		}
	}
	
	/** Called by {@link CDEPParserSB#processAux()}. */
	private void addLexica()
	{
		String puncLabel = f_xmls[0].getPunctuationLabel();
		StringIntPair head;
		DEPNode node;
		int i;

		for (i=1; i<t_size; i++)
		{
			node = d_tree.get(i);
			head = g_heads[i];
			
			if (head.s.equals(puncLabel))
				p_punc.add(node.form);
		}
	}
	
	/** Called by {@link CDEPParserSB#processAux()}. */
	protected List<Pair<String,StringFeatureVector>> parse()
	{
		List<Pair<String,StringFeatureVector>> insts = (i_flag == FLAG_TRAIN) ? parseMain().o2 : parseBranches();
	
		if (i_flag == FLAG_DECODE || i_flag == FLAG_DEVELOP)
			postProcess();
		
		return insts;
	}
	
	protected Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>> parseMain()
	{
		List<Pair<String,StringFeatureVector>> insts = new ArrayList<Pair<String,StringFeatureVector>>();
		List<DEPState> states = new ArrayList<DEPState>();
		DEPLabel label;
		
		while (i_beta < t_size)
		{
			if (i_lambda < 0)
			{
				noShift();
				continue;
			}
		
			label = getLabel(insts, states);
			parseAux(label);
		}
		
		if (states.size() > n_beams - 1)
		{
			Collections.sort(states);
			states = states.subList(0, n_beams - 1);
		}
		
		return new Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>(d_tree.getHeads(), insts, states);
	}
	
	protected void parseAux(DEPLabel label)
	{
		DEPNode lambda = d_tree.get(i_lambda);
		DEPNode beta   = d_tree.get(i_beta);
		
		d_score += label.score;
		n_trans++;
		
		if (label.isArc(LB_LEFT))
		{
			if (i_lambda == DEPLib.ROOT_ID)
				noShift();
			else if (beta.isDescendentOf(lambda))
				noPass();
			else if (label.isList(LB_REDUCE))
				leftReduce(lambda, beta, label.deprel);
			else
				leftPass(lambda, beta, label.deprel);
		}
		else if (label.isArc(LB_RIGHT))
		{
			if (lambda.isDescendentOf(beta))
				noPass();
			else if (label.isList(LB_SHIFT))
				rightShift(lambda, beta, label.deprel);
			else
				rightPass(lambda, beta, label.deprel);
		}
		else
		{
			if (label.isList(LB_SHIFT))
				noShift();
			else if (label.isList(LB_REDUCE) && lambda.hasHead())
				noReduce();
			else
				noPass();
		}
	}
	
	/** Called by {@link CDEPParserSB#parse()}. */
	protected DEPLabel getLabel(List<Pair<String,StringFeatureVector>> insts, List<DEPState> states)
	{
		StringFeatureVector vector = getFeatureVector(f_xmls[0]);
		DEPLabel label = null;
		
		if (i_flag == FLAG_TRAIN)
		{
			label = getGoldLabel();
			insts.add(new Pair<String,StringFeatureVector>(label.toString(), vector));
		}
		else if (i_flag == FLAG_DECODE || i_flag == FLAG_DEVELOP)
		{
			label = getAutoLabel(vector, insts, states);
		}
		else if (i_flag == FLAG_BOOTSTRAP)
		{
			label = getAutoLabel(vector, insts, states);
			insts.add(new Pair<String,StringFeatureVector>(getGoldLabel().toString(), vector));
		}

		return label;
	}
	
	/** Called by {@link CDEPParserSB#getLabel()}. */
	protected DEPLabel getGoldLabel()
	{
		DEPLabel label = getGoldLabelArc();
		
		if (label.isArc(LB_LEFT))
			label.list = isGoldReduce(true) ? LB_REDUCE : LB_PASS;
		else if (label.isArc(LB_RIGHT))
			label.list = isGoldShift() ? LB_SHIFT : LB_PASS;
		else
		{
			if      (isGoldShift())			label.list = LB_SHIFT;
			else if (isGoldReduce(false))	label.list = LB_REDUCE;
			else							label.list = LB_PASS;
		}
		
		return label;
	}
	
	/** Called by {@link CDEPParserSB#getGoldLabel()}. */
	private DEPLabel getGoldLabelArc()
	{
		StringIntPair head = g_heads[i_lambda];
		
		if (head.i == i_beta)
			return new DEPLabel(LB_LEFT, head.s);
		
		head = g_heads[i_beta];
		
		if (head.i == i_lambda)
			return new DEPLabel(LB_RIGHT, head.s);
		
		return new DEPLabel(LB_NO, "");
	}
	
	/** Called by {@link CDEPParserSB#getGoldLabel()}. */
	private boolean isGoldShift()
	{
		if (g_heads[i_beta].i < i_lambda)
			return false;
		
		int i;
		
		for (i=i_lambda-1; i>0; i--)
		{
			if (s_reduce.contains(i))
				continue;
			
			if (g_heads[i].i == i_beta)
				return false;
		}
		
		return true;
	}
	
	/** Called by {@link CDEPParserSB#getGoldLabel()}. */
	private boolean isGoldReduce(boolean hasHead)
	{
		if (!hasHead && !d_tree.get(i_lambda).hasHead())
			return false;
		
		int i, size = d_tree.size();
		
		for (i=i_beta+1; i<size; i++)
		{
			if (g_heads[i].i == i_lambda)
				return false;
		}
		
		return true;
	}
	
	/** Called by {@link CDEPParserSB#getLabel()}. */
	private DEPLabel getAutoLabel(StringFeatureVector vector, List<Pair<String,StringFeatureVector>> insts, List<DEPState> states)
	{
		String key = vector.toString();
		Pair<DEPLabel,DEPLabel> val = m_labels.get(key);
		DEPLabel fst, snd;
		List<DEPHead> p;
		
		if (val != null)
		{
			fst = val.o1;
			snd = val.o2;
		}
		else
		{
			List<StringPrediction> ps = s_models[0].predictAll(vector);
			AbstractAlgorithm.normalize(ps);

			fst = new DEPLabel(ps.get(0).label, ps.get(0).score);
			snd = new DEPLabel(ps.get(1).label, ps.get(1).score);
			
			m_labels.put(key, new Pair<DEPLabel,DEPLabel>(fst, snd));			
		}
		
		if (fst.score - snd.score < d_margin)
		{
			if (fst.isArc(LB_NO))
			{
				if (snd.isArc(LB_LEFT))
				{
					p = l_2nd.get(i_lambda);
					p.add(new DEPHead(i_beta, snd.deprel, snd.score));
				}
				else if (snd.isArc(LB_RIGHT))
				{
					p = l_2nd.get(i_beta);
					p.add(new DEPHead(i_lambda, snd.deprel, snd.score));
				}
			}
			
			if (b_first)
				states.add(new DEPState(i_lambda, i_beta, n_trans, d_score, snd, d_tree.getHeads(), s_reduce.clone()));
		}
		
		return fst;
	}
	
	/** Called by {@link CDEPParserSB#depParseAux()}. */
	protected void leftReduce(DEPNode lambda, DEPNode beta, String deprel)
	{
		leftArc(lambda, beta, deprel);
		reduce();
	}
	
	/** Called by {@link CDEPParserSB#depParseAux()}. */
	protected void leftPass(DEPNode lambda, DEPNode beta, String deprel)
	{
		leftArc(lambda, beta, deprel);
		pass();
	}
	
	/** Called by {@link CDEPParserSB#depParseAux()}. */
	protected void rightShift(DEPNode lambda, DEPNode beta, String deprel)
	{
		rightArc(lambda, beta, deprel);
		shift();
	}
	
	/** Called by {@link CDEPParserSB#depParseAux()}. */
	protected void rightPass(DEPNode lambda, DEPNode beta, String deprel)
	{
		rightArc(lambda, beta, deprel);
		pass();
	}
	
	/** Called by {@link CDEPParserSB#depParseAux()}. */
	protected void noShift()
	{
		shift();
	}
	
	/** Called by {@link CDEPParserSB#depParseAux()}. */
	protected void noReduce()
	{
		reduce();
	}
	
	/** Called by {@link CDEPParserSB#depParseAux()}. */
	protected void noPass()
	{
		pass();
	}
	
	private void leftArc(DEPNode lambda, DEPNode beta, String deprel)
	{
		lambda.setHead(beta, deprel);
	}
	
	private void rightArc(DEPNode lambda, DEPNode beta, String deprel)
	{
		beta.setHead(lambda, deprel);
	}
	
	private void shift()
	{
		i_lambda = i_beta++;
	}
	
	private void reduce()
	{
		s_reduce.add(i_lambda);
		passAux();
	}
	
	private void pass()
	{
		passAux();
	}
	
	private void passAux()
	{
		int i;
		
		for (i=i_lambda-1; i>=0; i--)
		{
			if (!s_reduce.contains(i))
			{
				i_lambda = i;
				return;
			}
		}
		
		i_lambda = i;
	}
	
	protected void postProcess()
	{
		Triple<DEPNode,String,Double> max = new Triple<DEPNode,String,Double>(null, null, -1d);
		DEPNode root = d_tree.get(DEPLib.ROOT_ID);
		int i, size = d_tree.size();
		List<DEPHead> list;
		DEPNode node, head;
		
		for (i=1; i<size; i++)
		{
			node = d_tree.get(i);
			
			if (!node.hasHead())
			{
				if (!(list = l_2nd.get(node.id)).isEmpty())
				{
					for (DEPHead p : list)
					{
						head = d_tree.get(p.headId);
						
						if (!head.isDescendentOf(node))
						{
							node.setHead(head, p.deprel);
							break;
						}
					}
				}
				
				if (!node.hasHead())
				{
					max.set(root, DEPLibEn.DEP_ROOT, -1d);
					
					postProcessAux(node, -1, max);
					postProcessAux(node, +1, max);
					
					node.setHead(max.o1, max.o2);
				}
			}
		}
	}
	
	protected void postProcessAux(DEPNode node, int dir, Triple<DEPNode,String,Double> max)
	{
		StringFeatureVector vector;
		List<StringPrediction> ps;
		int i, size = d_tree.size();
		DEPNode head;
		
		if (dir < 0)	i_beta   = node.id;
		else			i_lambda = node.id;
		
		for (i=node.id+dir; 0<=i && i<size; i+=dir)
		{
			head = d_tree.get(i);			
			if (head.isDescendentOf(node))	continue;
			
			if (dir < 0)	i_lambda = i;
			else			i_beta   = i;
			
			vector = getFeatureVector(f_xmls[0]);
			ps = s_models[0].predictAll(vector);
			AbstractAlgorithm.normalize(ps);
			
			for (StringPrediction p : ps)
			{
				if (p.score <= max.o3)
					break;
				
				if ((dir < 0 && p.label.startsWith(LB_RIGHT)) || (dir > 0 && p.label.startsWith(LB_LEFT)))
				{
					max.set(head, new DEPLabel(p.label).deprel, p.score);
					break;
				}
			}
		}
	}

//	================================ FEATURE EXTRACTION ================================

	@Override
	protected String getField(FtrToken token)
	{
		DEPNode node = getNode(token);
		if (node == null)	return null;
		Matcher m;
		
		if (token.isField(JointFtrXml.F_FORM))
		{
			return node.form;
		}
		else if (token.isField(JointFtrXml.F_LEMMA))
		{
			return node.lemma;
		}
		else if (token.isField(JointFtrXml.F_POS))
		{
			return node.pos;
		}
		else if (token.isField(JointFtrXml.F_DEPREL))
		{
			return node.getLabel();
		}
		else if (token.isField(JointFtrXml.F_DISTANCE))
		{
			int dist = i_beta - i_lambda;
			return (dist > 6) ? "6" : Integer.toString(dist);
		}
		else if (token.isField(JointFtrXml.F_LEFT_VALENCY))
		{
			return Integer.toString(d_tree.getLeftValency(node.id));
		}
		else if (token.isField(JointFtrXml.F_RIGHT_VALENCY))
		{
			return Integer.toString(d_tree.getRightValency(node.id));
		}
		else if (token.isField(JointFtrXml.F_LNPL))
		{
			return getLeftNearestPunctuation (0, i_lambda);
		}
		else if (token.isField(JointFtrXml.F_RNPL))
		{
			return getRightNearestPunctuation(i_lambda, i_beta);
		}
		else if (token.isField(JointFtrXml.F_LNPB))
		{
			return getLeftNearestPunctuation (i_lambda, i_beta);
		}
		else if (token.isField(JointFtrXml.F_RNPB))
		{
			return getRightNearestPunctuation(i_beta, d_tree.size());
		}
		else if ((m = JointFtrXml.P_BOOLEAN.matcher(token.field)).find())
		{
			int field = Integer.parseInt(m.group(1));
			
			switch (field)
			{
			case  0: return (i_lambda == 1) ? token.field : null;
			case  1: return (i_beta == t_size-1) ? token.field : null;
			case  2: return (i_lambda+1 == i_beta) ? token.field : null;
			case  3: return s_punc.contains(node.form) ? token.field : null;
			default: throw new IllegalArgumentException("Unsupported feature: "+field);
			}
		}
		else if ((m = JointFtrXml.P_FEAT.matcher(token.field)).find())
		{
			return node.getFeat(m.group(1));
		}
		
		return null;
	}
	
	@Override
	protected String[] getFields(FtrToken token)
	{
		return null;
	}
	
	/** Called by {@link CDEPParserSB#getField(FtrToken)}. */
	private String getLeftNearestPunctuation(int lIdx, int rIdx)
	{
		String form;
		int i;
		
		for (i=rIdx-1; i>lIdx; i--)
		{
			form = d_tree.get(i).form;
			
			if (s_punc.contains(form))
				return form;
		}
		
		return null;
	}
	
	/** Called by {@link CDEPParserSB#getField(FtrToken)}. */
	private String getRightNearestPunctuation(int lIdx, int rIdx)
	{
		String form;
		int i;
		
		for (i=lIdx+1; i<rIdx; i++)
		{
			form = d_tree.get(i).form;
			
			if (s_punc.contains(form))
				return form;
		}
		
		return null;
	}
	
//	================================ NODE GETTER ================================
	
	/** Called by {@link CDEPParserSB#getField(FtrToken)}. */
	private DEPNode getNode(FtrToken token)
	{
		DEPNode node = null;
		
		switch (token.source)
		{
		case JointFtrXml.S_STACK : node = getNodeStack(token);	break;
		case JointFtrXml.S_LAMBDA: node = getNodeLambda(token);	break;
		case JointFtrXml.S_BETA  : node = getNodeBeta(token);	break;
		}
		
		if (node == null)	return null;
		
		if (token.relation != null)
		{
			     if (token.isRelation(JointFtrXml.R_H))		node = node.getHead();
			else if (token.isRelation(JointFtrXml.R_H2))	node = node.getGrandHead();
			else if (token.isRelation(JointFtrXml.R_LMD))	node = d_tree.getLeftMostDependent  (node.id);
			else if (token.isRelation(JointFtrXml.R_RMD))	node = d_tree.getRightMostDependent (node.id);
			else if (token.isRelation(JointFtrXml.R_LMD2))	node = d_tree.getLeftMostDependent  (node.id, 1);
			else if (token.isRelation(JointFtrXml.R_RMD2))	node = d_tree.getRightMostDependent (node.id, 1);
			else if (token.isRelation(JointFtrXml.R_LNS))	node = d_tree.getLeftNearestSibling (node.id);
			else if (token.isRelation(JointFtrXml.R_RNS))	node = d_tree.getRightNearestSibling(node.id);
		}
		
		return node;
	}
	
	/** Called by {@link AbstractDEPParser#getNode(FtrToken)}. */
	private DEPNode getNodeStack(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_lambda);
		
		int offset = Math.abs(token.offset), i;
		int dir    = (token.offset < 0) ? -1 : 1;
					
		for (i=i_lambda+dir; 0<i && i<i_beta; i+=dir)
		{
			if (!s_reduce.contains(i) && --offset == 0)
				return d_tree.get(i);
		}
		
		return null;
	}

	/** Called by {@link AbstractDEPParser#getNode(FtrToken)}. */
	private DEPNode getNodeLambda(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_lambda);
		
		int cIndex = i_lambda + token.offset;
		
		if (0 < cIndex && cIndex < i_beta)
			return d_tree.get(cIndex);
		
		return null;
	}
	
	/** Called by {@link AbstractDEPParser#getNode(FtrToken)}. */
	private DEPNode getNodeBeta(FtrToken token)
	{
		if (token.offset == 0)
			return d_tree.get(i_beta);
		
		int cIndex = i_beta + token.offset;
		
		if (i_lambda < cIndex && cIndex < t_size)
			return d_tree.get(cIndex);
		
		return null;
	}
	
//	================================ BACKTRACK ================================
	
	@SuppressWarnings("unchecked")
	public List<Pair<String,StringFeatureVector>> parseBranches()
	{
		List<ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>>> list;
		Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>> t0 = parseMain();
		if ((i_flag == FLAG_DECODE || i_flag == FLAG_DEVELOP) && t0.o3.isEmpty())	return null;
		
		Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>> tm;
		double s0 = d_score / n_trans; 
		b_first = false;
		
		list = new ArrayList<ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>>>();
		list.add(new ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>>(t0, s0));
		backTrackDEP(list, t0.o3);
		
		if (i_flag == FLAG_DECODE || i_flag == FLAG_DEVELOP)
		{
			tm = (Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>)getMax(list).o;
			d_tree.resetHeads(tm.o1);
			return null;
		}
		else
		{
			List<Pair<String,StringFeatureVector>> insts = new ArrayList<Pair<String,StringFeatureVector>>(t0.o2);
			setGoldScores(list);
			
			tm = (Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>)getMax(list).o;
			insts.addAll(tm.o2);
			
			return insts;
		}
	}
	
	private void backTrackDEP(List<ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>>> list, List<DEPState> states)
	{
		Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>> t1;
		double s1;
		
		for (DEPState state : states)
		{
			resetState(state);
			t1 = parseMain();
			s1 = d_score / n_trans;
			list.add(new ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>>(t1, s1));
		}
	}
	
	private void resetState(DEPState state)
	{
		i_lambda = state.lambda;
		i_beta   = state.beta;
		n_trans  = state.trans;
		d_score  = state.score;
		s_reduce = state.reduce;
		d_tree.resetHeads(state.heads);
		parseAux(state.label);
	}
	
	private ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>> getMax(List<ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>>> dList) 
	{
		ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>> max = dList.get(0), t;
		int i, size = dList.size();
		
		for (i=1; i<size; i++)
		{
			t = dList.get(i);
			if (max.d < t.d) max = t;
		}
		
		return max;
	}
	
	@SuppressWarnings("unchecked")
	private void setGoldScores(List<ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>>> list)
	{
		StringIntPair gHead, sHead;
		StringIntPair[] sHeads;
		int i, c;
		
		for (ObjectDoublePair<Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>> p : list)
		{
			sHeads = ((Triple<StringIntPair[],List<Pair<String,StringFeatureVector>>,List<DEPState>>)p.o).o1;
			
			for (i=1,c=0; i<t_size; i++)
			{
				gHead = g_heads[i];
				sHead =  sHeads[i];
				
				if (gHead.i == sHead.i && gHead.s.equals(sHead.s))
					c++;
			}
			
			p.d = c;
		}
	}
}
