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
package edu.colorado.clear.classification.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;

import edu.colorado.clear.classification.algorithm.AbstractAlgorithm;
import edu.colorado.clear.classification.prediction.StringPrediction;
import edu.colorado.clear.classification.vector.SparseFeatureVector;
import edu.colorado.clear.classification.vector.StringFeatureVector;

/**
 * String vector model.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class StringModel extends AbstractModel
{
	/** The map between features and their indices. */
	private Map<String,ObjectIntOpenHashMap<String>> m_features;
		
	/** Constructs a string model for training. */
	public StringModel()
	{
		super();
		m_features = new HashMap<String,ObjectIntOpenHashMap<String>>();
	}
	
	/**
	 * Constructs a string model for decoding.
	 * @param reader the reader to load the model from.
	 */
	public StringModel(BufferedReader reader)
	{
		super(reader);
	}
	
	/**
	 * Constructs a string model by copying the specific model.
	 * @param model the model to be copied.
	 */
	public StringModel(StringModel model)
	{
		super(model);
		m_features = new HashMap<String, ObjectIntOpenHashMap<String>>(model.m_features);
	}
	
	/* (non-Javadoc)
	 * @see edu.colorado.clear.classification.model.AbstractModel#load(java.io.Reader)
	 */
	public void load(BufferedReader reader)
	{
		System.out.println("Loading model:");
		
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
		System.out.println("Saving model:");
		
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
		ObjectIntOpenHashMap<String> map;
		int i, j, typeSize, valueSize;
		String[] tmp;
		String type;
		
		n_features = Integer.parseInt(fin.readLine());
		typeSize   = Integer.parseInt(fin.readLine());
		m_features = new HashMap<String, ObjectIntOpenHashMap<String>>();
		
		for (i=0; i<typeSize; i++)
		{
			map  = new ObjectIntOpenHashMap<String>();
			type = fin.readLine();
			valueSize = Integer.parseInt(fin.readLine());
			
			for (j=0; j<valueSize; j++)
			{
				tmp = fin.readLine().split(" ");
				map.put(tmp[0], Integer.parseInt(tmp[1]));
			}
			
			m_features.put(type, map);
		}
	}
	
	private void saveFeatures(PrintStream fout)
	{
		ObjectIntOpenHashMap<String> map;
		StringBuilder build;
		String value;
		
		fout.println(n_features);
		fout.println(m_features.size());
		
		for (String type : m_features.keySet())
		{
			map = m_features.get(type);
			fout.println(type);
			fout.println(map.size());
			
			for (ObjectCursor<String> cur : map.keys())
			{
				value = cur.value;
				build = new StringBuilder();
				
				build.append(value);
				build.append(" ");
				build.append(map.get(value));

				fout.println(build.toString());
			}
		}
	}
	
	/**
	 * Adds the specific feature to this model.
	 * @param type the feature type.
	 * @param value the feature value.
	 */
	public void addFeature(String type, String value)
	{
		ObjectIntOpenHashMap<String> map;
		
		if (m_features.containsKey(type))
		{
			map = m_features.get(type);
			if (!map.containsKey(value))
				map.put(value, n_features++);
		}
		else
		{
			map = new ObjectIntOpenHashMap<String>();
			map.put(value, n_features++);
			m_features.put(type, map);
		}
	}

	/**
	 * Returns the sparse feature vector converted from the string feature vector.
	 * During the conversion, discards features not found in this model.
	 * @param vector the string feature vector.
	 * @return the sparse feature vector converted from the string feature vector.
	 */
	public SparseFeatureVector toSparseFeatureVector(StringFeatureVector vector)
	{
		SparseFeatureVector sparse = new SparseFeatureVector(vector.hasWeight());
		ObjectIntOpenHashMap<String> map;
		int i, index, size = vector.size();
		String type, value;
		
		for (i=0; i<size; i++)
		{
			type  = vector.getType(i);
			value = vector.getValue(i);
			
			if ((map = m_features.get(type)) != null && (index = map.get(value)) > 0)
			{
				if (sparse.hasWeight())
					sparse.addFeature(index, vector.getWeight(i));
				else
					sparse.addFeature(index);
			}
		}
		
		sparse.trimToSize();
		return sparse;
	}
	
	/**
	 * Returns the best prediction given the feature vector.
	 * @param x the feature vector.
	 * @return the best prediction given the feature vector.
	 */
	public StringPrediction predictBest(StringFeatureVector x)
	{
		return predictAll(x).get(0);
	}
	
	/**
	 * Returns all predictions given the feature vector.
	 * Predictions are sorted in descending order by their scores. 
	 * @param x the feature vector.
	 * @return all predictions given the feature vector.
	 */
	public List<StringPrediction> predictAll(StringFeatureVector x)
	{
	//	SortedStringPredictionArrayList list = new SortedStringPredictionArrayList(n_labels);
		List<StringPrediction> list = new ArrayList<StringPrediction>(n_labels);
		double[] scores = getScores(toSparseFeatureVector(x));
		int i;
		
		for (i=0; i<n_labels; i++)
			list.add(new StringPrediction(a_labels[i], scores[i]));
		
		Collections.sort(list);
		if (i_solver == AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_LR)
			toProbability(list);
		
		return list;
	}
}
