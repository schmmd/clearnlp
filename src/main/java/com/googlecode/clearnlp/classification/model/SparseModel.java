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
package com.googlecode.clearnlp.classification.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.googlecode.clearnlp.classification.algorithm.AbstractAlgorithm;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.vector.SparseFeatureVector;
import com.googlecode.clearnlp.util.UTArray;


/**
 * Sparse vector model.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class SparseModel extends AbstractModel
{
	/** Constructs a sparse model for training. */
	public SparseModel()
	{
		super();
	}
	
	/**
	 * Constructs a sparse model for decoding.
	 * @param reader the reader to load the model from.
	 */
	public SparseModel(BufferedReader reader)
	{
		load(reader);
	}
	
	/**
	 * Constructs a sparse model by copying the specific model.
	 * @param model the model to be copied.
	 */
	public SparseModel(SparseModel model)
	{
		super(model);
	}
	
	/* (non-Javadoc)
	 * @see edu.colorado.clear.classification.model.AbstractModel#load(java.io.Reader)
	 */
	public void load(BufferedReader reader)
	{
		System.out.print("Loading");
		
		try
		{
			i_solver = Byte.parseByte(reader.readLine());
			loadLabels(reader);
			loadFeatures(reader);
			loadWeightVector(reader);			
		}
		catch (Exception e) {e.printStackTrace();}
		
		System.out.println();
	}
	
	/* (non-Javadoc)
	 * @see edu.colorado.clear.classification.model.AbstractModel#save(java.io.PrintStream)
	 */
	public void save(PrintStream fout)
	{
		System.out.print("Saving");
		
		try
		{
			fout.println(i_solver);
			saveLabels(fout);
			saveFeatures(fout);
			saveWeightVector(fout);
		}
		catch (Exception e) {e.printStackTrace();}
		
		System.out.println();
	}
	
	private void loadFeatures(BufferedReader fin) throws IOException
	{
		n_features = Integer.parseInt(fin.readLine());
	}
	
	private void saveFeatures(PrintStream fout)
	{
		fout.println(n_features);
	}
	
	/**
	 * Adds the specific feature indices to this model.
	 * @param indices the feature indices.
	 */
	public void addFeatures(int[] indices)
	{
		n_features = Math.max(n_features, UTArray.max(indices)+1);
	}

	/**
	 * Returns the best prediction given the feature vector.
	 * @param x the feature vector.
	 * @return the best prediction given the feature vector.
	 */
	public StringPrediction predictBest(SparseFeatureVector x)
	{
		return predictAll(x).get(0);
	}
	
	/**
	 * Returns all predictions given the feature vector.
	 * Predictions are sorted in descending order by their scores. 
	 * @param x the feature vector.
	 * @return all predictions given the feature vector.
	 */
	public List<StringPrediction> predictAll(SparseFeatureVector x)
	{
	//	SortedStringPredictionArrayList list = new SortedStringPredictionArrayList(n_labels);
		List<StringPrediction> list = new ArrayList<StringPrediction>(n_labels);
		double[] scores = getScores(x);
		int i;
		
		for (i=0; i<n_labels; i++)
			list.add(new StringPrediction(a_labels[i], scores[i]));
		
		Collections.sort(list);
		if (i_solver == AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_LR)
			toProbability(list);
		
		return list;
	}
}
