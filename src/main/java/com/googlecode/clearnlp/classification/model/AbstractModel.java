/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
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
import java.util.Arrays;
import java.util.List;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.vector.SparseFeatureVector;
import com.googlecode.clearnlp.util.UTArray;


/**
 * Abstract model.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
abstract public class AbstractModel
{
	static public String LABEL_TRUE  = "T";
	static public String LABEL_FALSE = "F";
	
	/** The total number of labels. */
	protected int      n_labels;
	/** The total number of features. */
	protected int      n_features;
	/** The weight vector for all labels. */
	protected double[] d_weights;
	/** The list of all labels. */
	protected String[] a_labels;
	/** The map between labels and their indices. */
	protected ObjectIntOpenHashMap<String> m_labels;
	protected byte i_solver;
	
	/** Constructs an abstract model for training. */
	public AbstractModel()
	{
		n_labels   = 0;
		m_labels   = new ObjectIntOpenHashMap<String>();
		n_features = 1;
	}
	
	/**
	 * Constructs an abstract model for decoding.
	 * @param reader the reader to load the model from.
	 */
	public AbstractModel(BufferedReader reader)
	{
		load(reader);
	}
	
	/**
	 * Constructs an abstract model by copying the specific model.
	 * @param model the model to be copied.
	 */
	protected AbstractModel(AbstractModel model)
	{
		n_labels   = model.n_labels;
		n_features = model.n_features;
		d_weights  = Arrays.copyOf(model.d_weights, model.d_weights.length);
		a_labels   = Arrays.copyOf(model.a_labels , model.a_labels.length);
		m_labels   = new ObjectIntOpenHashMap<String>(model.m_labels);
		i_solver   = model.i_solver;
	}
	
	public void setSolver(byte solver)
	{
		i_solver = solver;
	}
	
	/**
	 * Initializes the label array after adding all labels.
	 * @see StringModel#addLabel(String)
	 */
	public void initLabelArray()
	{
		a_labels = new String[n_labels];
		String label;
		
		for (ObjectCursor<String> cur : m_labels.keys())
		{
			label = cur.value;
			a_labels[getLabelIndex(label)] = label;
		}
	}
	
	/**
	 * Adds the specific label to this model.
	 * @param label the label to be added.
	 */
	public void addLabel(String label)
	{
		if (!m_labels.containsKey(label))
			m_labels.put(label, ++n_labels);
	}
	
	/**
	 * Returns the index of the specific label.
	 * Returns {@code -1} if the label is not found in this model.
	 * @param label the label to get the index for.
	 * @return the index of the specific label.
	 */
	public int getLabelIndex(String label)
	{
		return m_labels.get(label) - 1;
	}
	
	/**
	 * Returns {@code true} if this model contains only 2 labels.
	 * @return {@code true} if this model contains only 2 labels.
	 */
	public boolean isBinaryLabel()
	{
		return n_labels == 2;
	}
	
	/**
	 * Returns the total number of labels in this model.
	 * @return the total number of labels in this model.
	 */
	public int getLabelSize()
	{
		return n_labels;
	}
	
	/**
	 * Returns the total number of features in this model.
	 * @return the total number of features in this model.
	 */
	public int getFeatureSize()
	{
		return n_features;
	}
	
	public String[] getLabels()
	{
		return a_labels;
	}
	
	/** Initializes the weight vector given the label and feature sizes. */
	public void initWeightVector()
	{
		d_weights = isBinaryLabel() ? new double[n_features] : new double[n_features * n_labels];
	}
	
	/**
	 * Copies a weight vector for binary classification.
	 * @param vector the weight vector to be copied. 
	 */
	public void copyWeightVector(double[] vector)
	{
		System.arraycopy(vector, 0, d_weights, 0, n_features);
	}
	
	/**
	 * Copies a weight vector of the specific label (for multi-classification).
	 * @param label the label of the weight vector.
	 * @param weights the weight vector to be copied.
	 */
	public void copyWeightVector(int label, double[] weights)
	{
		int i;
		
		for (i=0; i<n_features; i++)
			d_weights[getWeightIndex(label, i)] = weights[i];
	}
	
	/**
	 * Returns the scores of all labels given the feature vector.
	 * For binary classification, this method calls {@link AbstractModel#getScoresBinary(SparseFeatureVector)}.
	 * For multi-classification, this method calls {@link AbstractModel#getScoresMulti(SparseFeatureVector)}.
	 * @param x the feature vector.
	 * @return the scores of all labels given the feature vector.
	 */
	public double[] getScores(SparseFeatureVector x)
	{
		return isBinaryLabel() ? getScoresBinary(x) : getScoresMulti(x);
	}
	
	/**
	 * Returns the scores of all labels given the feature vector.
	 * This method is used for binary classification.
	 * @param x the feature vector.
	 * @return the scores of all labels given the feature vector.
	 */
	public double[] getScoresBinary(SparseFeatureVector x)
	{
		double score = d_weights[0];
		int    i, index, size = x.size();
		
		for (i=0; i<size; i++)
		{
			index = x.getIndex(i);
			
			if (isRange(index))
			{
				if (x.hasWeight())
					score += d_weights[index] * x.getWeight(i);
				else
					score += d_weights[index];
			}
		}
		
		double[] scores = {score, -score};
		return scores;
	}
	
	/**
	 * Returns the scores of all labels given the feature vector.
	 * This method is used for multi-classification.
	 * @param x the feature vector.
	 * @return the scores of all labels given the feature vector.
	 */
	public double[] getScoresMulti(SparseFeatureVector x)
	{
		double[] scores = Arrays.copyOf(d_weights, n_labels);
		int      i, index, label, weightIndex, size = x.size();
		double   weight = 1;
		
		for (i=0; i<size; i++)
		{
			index = x.getIndex(i);
			if (x.hasWeight())	weight = x.getWeight(i);
			
			if (isRange(index))
			{
				for (label=0; label<n_labels; label++)
				{
					weightIndex = getWeightIndex(label, index);
					
					if (x.hasWeight())	scores[label] += d_weights[weightIndex] * weight;
					else				scores[label] += d_weights[weightIndex];
				}
			}
		}
		
		return scores;
	}
	
	/**
	 * Returns {@code true} if the specific feature index is within the range of this model.
	 * @param featureIndex the index of the feature.
	 * @return {@code true} if the specific feature index is within the range of this model.
	 */
	public boolean isRange(int featureIndex)
	{
		return 0 < featureIndex && featureIndex < n_features;
	}
	
	/** Returns the index of the weight vector given the label and the feature index. */
	protected int getWeightIndex(int label, int index)
	{
		return index * n_labels + label;
	}
	
	/** Loads labels from the specific reader. */
	protected void loadLabels(BufferedReader fin) throws IOException
	{
		n_labels = Integer.parseInt(fin.readLine());
		a_labels = fin.readLine().split(" ");
		m_labels = new ObjectIntOpenHashMap<String>();
		
		int i;
		for (i=0; i<n_labels; i++)
			m_labels.put(a_labels[i], i+1);
	}
	
	/** Saves labels to the specific reader. */
	protected void saveLabels(PrintStream fout)
	{
		fout.println(n_labels);
		fout.println(UTArray.join(a_labels, " "));
	}
	
	/**
	 * Loads the weight vector from the specific reader.
	 * @param fin the reader to load the weight vector from.
	 * @throws Exception
	 */
	protected void loadWeightVector(BufferedReader fin) throws Exception
	{
		int[] buffer = new int[128];
		int   i, b, size, ch;
		
		size = Integer.parseInt(fin.readLine());
		d_weights = new double[size];
		
		for (i=0; i<size; i++)
		{
			b = 0;
			
			while (true)
			{
				ch = fin.read();
				
				if (ch == ' ')	break;
				else			buffer[b++] = ch;
			}

			d_weights[i] = Double.parseDouble((new String(buffer, 0, b)));
			if (i%n_features == 0)	System.out.print(".");
		}
		
		fin.readLine();
	}

	/**
	 * Saves the weight vector to the specific stream.
	 * @param fout the output stream to save the weight vector to.
	 */
	protected void saveWeightVector(PrintStream fout)
	{
		StringBuilder build = new StringBuilder();
		int i, size = d_weights.length;
		
		fout.println(size);
		
		for (i=0; i<size; i++)
		{
			build.append(d_weights[i]);
			build.append(' ');
			
			if (i%n_features == 0)	System.out.print(".");
		}
		
		fout.println(build.toString());
	}
	
	public void toProbability(List<StringPrediction> ps)
	{
		double d;
		
		if (isBinaryLabel())
		{
			StringPrediction p = ps.get(0);
			d = 1 / (1 + Math.exp(-p.score));
			p.score = d;
			ps.get(1).score = 1 - d;
		}
		else
		{
			double sum = 0;
			
			for (StringPrediction p : ps)
			{
				d = 1 / (1 + Math.exp(-p.score));
				p.score = d;
				sum += d;
			}
			
			for (StringPrediction p : ps)
				p.score /= sum;
		}
	}
	
	/**
	 * Loads this model from the specific reader.
	 * @param reader the reader to load the model from.
	 */
	abstract public void load(BufferedReader reader);
	
	/**
	 * Saves this model to the specific stream.
	 * @param fout the stream to save this model to.
	 */
	abstract public void save(PrintStream fout);
}
