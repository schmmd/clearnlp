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
package com.googlecode.clearnlp.classification.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.carrotsearch.hppc.DoubleArrayList;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.googlecode.clearnlp.classification.prediction.IntPrediction;
import com.googlecode.clearnlp.classification.prediction.StringPrediction;
import com.googlecode.clearnlp.classification.vector.SparseFeatureVector;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.pair.Pair;

/**
 * Abstract model.
 * @since 1.3.1
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class ONStringModel
{
	/** The total number of labels. */
	protected int n_labels;
	/** The total number of features. */
	protected int n_features;
	/** The weight vector for all labels. */
	protected List<DoubleArrayList> d_weights;
	/** Update counts. */
	protected List<DoubleArrayList> d_gs;
	/** The list of all labels. */
	protected List<String> a_labels;
	/** The map between labels and their indices. */
	protected ObjectIntOpenHashMap<String> m_labels;
	/** The map between features and their indices. */
	protected Map<String,ObjectIntOpenHashMap<String>> m_features;
	/** The solver type (e.g., AdaGrad). */
	protected byte i_solver;
	/** AdaGrad parameters. */
	protected double d_alpha, d_rho;

	// ----------------------------------- CONSTRUCTORS -----------------------------------
	
	/**
	 * Constructs an abstract model for decoding.
	 * @param reader the reader to load the model from.
	 */
	public ONStringModel(BufferedReader reader, double alpha, double rho)
	{
		load(reader);
		initAdaGrad(alpha, rho);
	}
	
	protected void initAdaGrad(double alpha, double rho)
	{
		d_gs = new ArrayList<DoubleArrayList>(n_features);
		
		int i; for (i=0; i<n_features; i++)
			d_gs.add(getBlankDoubleArrayList(n_labels));
		
		d_alpha = alpha;
		d_rho   = rho;
	}
	
	protected DoubleArrayList getBlankDoubleArrayList(int size)
	{
		DoubleArrayList list = new DoubleArrayList(size);
		int i;
		
		for (i=0; i<size; i++)
			list.add(0);
		
		return list;
	}
	
	// ----------------------------------- LOAD MODELS -----------------------------------
	
	/**
	 * Loads this model from the specific reader.
	 * @param reader the reader to load the model from.
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
	
	/** Loads labels from the specific reader. */
	protected void loadLabels(BufferedReader fin) throws IOException
	{
		n_labels = Integer.parseInt(fin.readLine());
		
		String[] labels = fin.readLine().split(" ");
		a_labels = Arrays.asList(labels);
		
		m_labels = new ObjectIntOpenHashMap<String>();
		
		int i; for (i=0; i<n_labels; i++)
			m_labels.put(labels[i], i+1);
	}
	
	public void loadFeatures(BufferedReader fin) throws IOException
	{
		ObjectIntOpenHashMap<String> map;
		int i, j, typeSize, valueSize;
		String[] tmp;
		String type;
		
		n_features = Integer.parseInt(fin.readLine());
		typeSize   = Integer.parseInt(fin.readLine());
		m_features = new HashMap<String, ObjectIntOpenHashMap<String>>();
		
		Pattern P_DELIM = Pattern.compile(" ");
		
		for (i=0; i<typeSize; i++)
		{
			map  = new ObjectIntOpenHashMap<String>();
			type = fin.readLine();
			valueSize = Integer.parseInt(fin.readLine());
			
			for (j=0; j<valueSize; j++)
			{
				tmp = P_DELIM.split(fin.readLine());
				map.put(tmp[0], Integer.parseInt(tmp[1]));
			}
			
			m_features.put(type, map);
		}
	}
	
	/**
	 * Loads the weight vector from the specific reader.
	 * @param fin the reader to load the weight vector from.
	 * @throws Exception
	 */
	protected void loadWeightVector(BufferedReader fin) throws Exception
	{
		int[] buffer = new int[128];
		DoubleArrayList weight;
		int i, j, b, ch;
		
		d_weights = new ArrayList<DoubleArrayList>(n_features);
		Integer.parseInt(fin.readLine());	// for compatibility
		
		for (i=0; i<n_features; i++)
		{
			if (i%100000 == 0)	System.out.print(".");
			weight = new DoubleArrayList(n_labels);
			
			for (j=0; j<n_labels; j++)
			{
				b = 0;
				
				while (true)
				{
					ch = fin.read();
					
					if (ch == ' ')	break;
					else			buffer[b++] = ch;
				}
				
				weight.add(Double.parseDouble((new String(buffer, 0, b))));
			}
			
			d_weights.add(weight);
		}
		
		fin.readLine();
	}
	
	// ----------------------------------- SAVE MODELS -----------------------------------
	
	/**
	 * Saves this model to the specific stream.
	 * @param fout the stream to save this model to.
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
	
	/** Saves labels to the specific reader. */
	protected void saveLabels(PrintStream fout)
	{
		fout.println(n_labels);
		fout.println(UTArray.join(a_labels, " "));
	}
	
	public void saveFeatures(PrintStream fout)
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
	 * Saves the weight vector to the specific stream.
	 * @param fout the output stream to save the weight vector to.
	 */
	protected void saveWeightVector(PrintStream fout)
	{
		DoubleArrayList weight;
		StringBuilder build;
		int i, j;
		
		fout.println(n_labels * n_features);
		
		for (i=0; i<n_features; i++)
		{
			if (i%100000 == 0)	System.out.print(".");
			weight = d_weights.get(i);
			build  = new StringBuilder();
			
			for (j=0; j<n_labels; j++)
			{
				build.append(weight.get(j));
				build.append(' ');
			}
			
			fout.print(build.toString());
		}
		
		fout.println();
	}
	
	// ----------------------------------- GETTERS -----------------------------------
	
	public byte getSolver()
	{
		return i_solver;
	}
	
	public String getLabel(int index)
	{
		return a_labels.get(index);
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
	
	// ----------------------------------- SETTERS -----------------------------------
	
	public void setSolver(byte solver)
	{
		i_solver = solver;
	}
	
	/**
	 * Adds the specific label to this model.
	 * @param label the label to be added.
	 */
	public void addLabel(String label)
	{
		if (!m_labels.containsKey(label))
		{
			a_labels.add(label);
			m_labels.put(label, ++n_labels);
			addLabelAux();
		}
	}
	
	private void addLabelAux()
	{
		int i; for (i=0; i<n_features; i++)
		{
			d_weights.get(i).add(0);
			d_gs.get(i).add(0);
		}
	}
	
	public void addFeatures(StringFeatureVector vector)
	{
		int i, size = vector.size();
		
		for (i=0; i<size; i++)
			addFeature(vector.getType(i), vector.getValue(i));
	}
	
	/**
	 * Adds the specific feature to this model.
	 * @param type the feature type.
	 * @param value the feature value.
	 */
	public void addFeature(String type, String value)
	{
		ObjectIntOpenHashMap<String> map = m_features.get(type);
		
		if (map == null)
		{
			map = new ObjectIntOpenHashMap<String>();
			m_features.put(type, map);
		}
		
		if (!map.containsKey(value))
		{
			map.put(value, n_features++);
			addFeatureAux();
		}
	}
	
	private void addFeatureAux()
	{
		d_weights.add(getBlankDoubleArrayList(n_labels));
		d_gs.add(getBlankDoubleArrayList(n_labels));
	}
	
	// ----------------------------------- PREDICT SPARSE -----------------------------------
	
	/**
	 * Returns the scores of all labels given the feature vector.
	 * For binary classification, this method calls {@link ONStringModel#getScoresBinary(SparseFeatureVector)}.
	 * For multi-classification, this method calls {@link ONStringModel#getScoresMulti(SparseFeatureVector)}.
	 * @param x the feature vector.
	 * @return the scores of all labels given the feature vector.
	 */
	public double[] getScores(SparseFeatureVector x)
	{
		double[] scores = d_weights.get(0).toArray();
		int i, index, label, size = x.size();
		DoubleArrayList weight;
		double value = 1;
		
		// features
		for (i=0; i<size; i++)
		{
			index = x.getIndex(i);
			if (x.hasWeight())	value = x.getWeight(i);
			
			if (isRange(index))
			{
				weight = d_weights.get(index);
				
				for (label=0; label<n_labels; label++)
				{
					if (x.hasWeight())	scores[label] += weight.get(label) * value;
					else				scores[label] += weight.get(label);
				}
			}
		}
		
		return scores;
	}
	
	/**
	 * Returns an unsorted list of predictions given the specific feature vector.
	 * @param x the feature vector.
	 * @return an unsorted list of predictions given the specific feature vector.
	 */
	public List<StringPrediction> getPredictions(SparseFeatureVector x)
	{
		List<StringPrediction> list = new ArrayList<StringPrediction>(n_labels);
		double[] scores = getScores(x);
		int i;
		
		for (i=0; i<n_labels; i++)
			list.add(new StringPrediction(a_labels.get(i), scores[i]));
		
		return list;		
	}
	
	public List<IntPrediction> getIntPredictions(SparseFeatureVector x)
	{
		List<IntPrediction> list = new ArrayList<IntPrediction>(n_labels);
		double[] scores = getScores(x);
		int i;
		
		for (i=0; i<n_labels; i++)
			list.add(new IntPrediction(i, scores[i]));
		
		return list;		
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
	
	public void normalizeScores(List<StringPrediction> ps)
	{
		double sum = 0, d;
		
		for (StringPrediction p : ps)
		{
			d = 1 / (1 + Math.exp(-p.score));
			p.score = d;
			sum += d;
		}
		
		for (StringPrediction p : ps)
			p.score /= sum;
	}
	
	/**
	 * Returns the best prediction given the feature vector.
	 * @param x the feature vector.
	 * @return the best prediction given the feature vector.
	 */
	public StringPrediction predictBest(SparseFeatureVector x)
	{
		List<StringPrediction> list = getPredictions(x);
		StringPrediction max = list.get(0), p;
		int i;
		
		for (i=1; i<n_labels; i++)
		{
			p = list.get(i);
			if (max.score < p.score) max = p;
		}
		
		return max;
	}
	
	/**
	 * Returns the first and second best predictions given the feature vector.
	 * @param x the feature vector.
	 * @return the first and second best predictions given the feature vector.
	 */
	public Pair<StringPrediction,StringPrediction> predictTwo(SparseFeatureVector x)
	{
		List<StringPrediction> list = getPredictions(x);
		StringPrediction fst = list.get(0), snd = list.get(1), p;
		int i;
		
		if (fst.score < snd.score)
		{
			fst = snd;
			snd = list.get(0);
		}
		
		for (i=2; i<n_labels; i++)
		{
			p = list.get(i);
			
			if (fst.score < p.score)
			{
				snd = fst;
				fst = p;
			}
			else if (snd.score < p.score)
				snd = p;
		}
		
		return new Pair<StringPrediction,StringPrediction>(fst, snd);
	}
	
	/**
	 * Returns a sorted list of predictions given the specific feature vector.
	 * @param x the feature vector.
	 * @return a sorted list of predictions given the specific feature vector.
	 */
	public List<StringPrediction> predictAll(SparseFeatureVector x)
	{
		List<StringPrediction> list = getPredictions(x);
		Collections.sort(list);
		
		return list;
	}
	
	// ----------------------------------- PREDICT STRING -----------------------------------
	
	public StringPrediction predictBest(StringFeatureVector x)
	{
		return predictBest(toSparseFeatureVector(x));
	}
	
	public Pair<StringPrediction,StringPrediction> predictTwo(StringFeatureVector x)
	{
		return predictTwo(toSparseFeatureVector(x));
	}
	
	public List<StringPrediction> predictAll(StringFeatureVector x)
	{
		return predictAll(toSparseFeatureVector(x));
	}
	
	public List<StringPrediction> getPredictions(StringFeatureVector x)
	{
		return getPredictions(toSparseFeatureVector(x));
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
	
	// ----------------------------------- UPDATE -----------------------------------
	
	public void updateWeights(List<Pair<String,StringFeatureVector>> instances)
	{
		for (Pair<String,StringFeatureVector> p : instances)
			updateWeights(p.o1, p.o2);
	}
	
	public void updateWeights(String label, StringFeatureVector vector)
	{
		addLabel(label);
		addFeatures(vector);
		
		SparseFeatureVector x = toSparseFeatureVector(vector);
		int i, y = getLabelIndex(label);
		double[] scores = getScores(x);
		scores[y] -= 1;
		
		IntPrediction max = new IntPrediction(0, scores[0]);
		
		for (i=1; i<n_labels; i++)
		{
			if (max.score < scores[i])
				max.set(i, scores[i]);
		}
		
		if (max.label != y)
		{
			updateCounts (y, max.label, x);
			updateWeights(y, max.label, x);
		}
	}
	
	private void updateCounts(int yp, int yn, SparseFeatureVector x)
	{
		int i, len = x.size();
		DoubleArrayList g;
		
		if (x.hasWeight())
		{
			double d;
			
			for (i=0; i<len; i++)
			{
				g = d_gs.get(x.getIndex(i));
				d = x.getWeight(i) * x.getWeight(i);

				add(g, yp, d);
				add(g, yn, d);
			}
		}
		else
		{
			for (i=0; i<len; i++)
			{
				g = d_gs.get(x.getIndex(i));
				
				add(g, yp, 1);
				add(g, yn, 1);
			}
		}
	}
	
	private void updateWeights(int yp, int yn, SparseFeatureVector x)
	{
		int i, xi, len = x.size();
		DoubleArrayList w;
		double vi;
		
		if (x.hasWeight())
		{
			for (i=0; i<len; i++)
			{
				xi = x.getIndex(i);
				vi = x.getWeight(i);
				w  = d_weights.get(xi);
				
				add(w, yp,  vi * getUpdate(yp, xi));
				add(w, yn, -vi * getUpdate(yn, xi));
			}
		}
		else
		{
			for (i=0; i<len; i++)
			{
				xi = x.getIndex(i);
				w  = d_weights.get(xi);
				
				add(w, yp,  getUpdate(yp, xi));
				add(w, yn, -getUpdate(yn, xi));
			}
		}
	}
	
	private void add(DoubleArrayList list, int index, double value)
	{
		list.set(index, list.get(index) + value);
	}
	
	private double getUpdate(int y, int x)
	{
		return d_alpha / (d_rho + Math.sqrt(d_gs.get(x).get(y)));
	}
}
