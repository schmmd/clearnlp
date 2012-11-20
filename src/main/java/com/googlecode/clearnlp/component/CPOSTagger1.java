/**
* Copyright 2012 University of Massachusetts Amherst
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
package com.googlecode.clearnlp.component;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import com.googlecode.clearnlp.beam.BeamNode;
import com.googlecode.clearnlp.beam.BeamTree;
import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;

/**
 * Part-of-speech tagger using beam search.
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class CPOSTagger1 extends CPOSTagger0
{
	protected BeamTree<Object>	b_tree;		// beam tree
	protected int				n_beam;		// beam size
	protected int				n_window;	// left window
	
//	====================================== CONSTRUCTORS ======================================

	/** Constructs a part-of-speech tagger for developing. */
	public CPOSTagger1(CPOSTagger0 tagger, int beamSize)
	{
		super(tagger.f_xmls, tagger.s_models, tagger.getLexica());
		initDecoder(beamSize);
	}
	
	/** Constructs a part-of-speech tagger for developing. */
	public CPOSTagger1(JointFtrXml[] xmls, StringModel[] models, Object[] lexica, int beamSize)
	{
		super(xmls, models, lexica);
		initDecoder(beamSize);
	}
	
	/** Constructs a part-of-speech tagger for decoding. */
	public CPOSTagger1(ZipInputStream in, int beamSize)
	{
		super(in);
		initDecoder(beamSize);
	}
	
	private void initDecoder(int beamSize)
	{
		n_beam   = beamSize;
		n_window = Math.abs(f_xmls[0].getSourceWindow(JointFtrXml.S_INPUT).i1);
	}
	
//	====================================== PROCESS ======================================
		
	@Override
	public void process(DEPTree tree)
	{
		init(tree);
		processAux();
	}
	
	/** Called by {@link CPOSTagger1#process(DEPTree)}. */
	protected void init(DEPTree tree)
	{
	 	super.init(tree);
	 	
	 	if (i_flag == FLAG_DECODE || i_flag == FLAG_DEVELOP)
	 		b_tree = new BeamTree<Object>(n_beam);
	}
	
	/** Called by {@link CPOSTagger1#process(DEPTree)}. */
	protected void processAux()
	{
		EngineProcess.normalizeForms(d_tree);
		
		if      (i_flag == FLAG_LEXICA)
			addLexica();
		else if (i_flag == FLAG_TRAIN)
			posTag();
		else if (i_flag == FLAG_DECODE || i_flag == FLAG_DEVELOP)
			posBeam();
	}
	
	/** Called by {@link CPOSTagger1#processAux()}. */
	protected void posBeam()
	{
		for (i_input=1; i_input<t_size; i_input++)
			posBeamAux();

		setPOSTags(b_tree.getNode(0), t_size);
	}
	
	private void posBeamAux()
	{
		List<List<StringPrediction>> ps = new ArrayList<List<StringPrediction>>(n_beam);
		List<BeamNode<Object>> bNodes = b_tree.getCurrNodes();
		
		if (bNodes.isEmpty())
			ps.add(getPredictions());
		else
		{
			for (BeamNode<Object> bNode : bNodes)
			{
				setPOSTags(bNode, n_window);
				ps.add(getPredictions());
			}	
		}
		
		b_tree.setBeam(ps);
	}
	
	private List<StringPrediction> getPredictions()
	{
		List<StringPrediction> ps = s_models[0].predictAll(getFeatureVector(f_xmls[0]));
		boolean trim = false;
		
		if (ps.get(0).score > 0)
			trim = true;
		
		s_models[0].normalizeScores(ps);
		if (trim)	ps.subList(1, ps.size()).clear();
		
		return ps;
	}
	
	private void setPOSTags(BeamNode<Object> bNode, int length)
	{
		int i = 1;
		
		for (BeamNode<Object> node : b_tree.getSequence(bNode, length))
		{
			d_tree.get(i_input-i).pos = node.getLabel();
			i++;
		}
	}
}
