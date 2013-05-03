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
package com.googlecode.clearnlp.classification.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.DoubleArrayList;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.googlecode.clearnlp.classification.algorithm.AbstractAlgorithm;
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
public class ONStringModel extends StringModel
{
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	/** The weight vector for all labels. */
	protected List<DoubleArrayList> d_weights;
	/** The list of all labels. */
	protected List<String> a_labels;
	/** Update counts for AdaGrad. */
	protected List<DoubleArrayList> d_gs;
	/** AdaGrad parameters. */
	protected double d_alpha, d_rho;

	// ----------------------------------- CONSTRUCTORS -----------------------------------
	
	public ONStringModel(double alpha, double rho)
	{
		initModel();
		initAdaGrad(alpha, rho);
	}
	
	/**
	 * Constructs an abstract model for decoding.
	 * @param reader the reader to load the model from.
	 */
	public ONStringModel(BufferedReader reader, double alpha, double rho)
	{
		load(reader);
		initAdaGrad(alpha, rho);
	}
	
	private void initModel()
	{
		n_labels   = 0;
		n_features = 1;
		d_weights  = new ArrayList<DoubleArrayList>();
		d_gs       = new ArrayList<DoubleArrayList>();
		a_labels   = new ArrayList<String>();
		m_labels   = new ObjectIntOpenHashMap<String>();
		m_features = new HashMap<String,ObjectIntOpenHashMap<String>>();
		i_solver   = AbstractAlgorithm.SOLVER_ADAGRAD_HINGE;
		
		d_weights.add(getBlankDoubleArrayList(n_labels));
	}
	
	private void initAdaGrad(double alpha, double rho)
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
	
	@Override
	public void load(BufferedReader reader)
	{
		LOG.info("Loading model:");
		
		try
		{
			i_solver = Byte.parseByte(reader.readLine());
			loadLabels(reader);
			loadFeatures(reader);
			loadWeightVector(reader);			
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	@Override
	protected void loadLabels(BufferedReader fin) throws IOException
	{
		n_labels = Integer.parseInt(fin.readLine());
		
		String[] labels = fin.readLine().split(" ");
		a_labels = UTArray.toList(labels);
		
		m_labels = new ObjectIntOpenHashMap<String>();
		
		int i; for (i=0; i<n_labels; i++)
			m_labels.put(labels[i], i+1);
	}
	
	@Override
	protected void loadFeatures(BufferedReader fin) throws IOException
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
	
	@Override
	protected void loadWeightVector(BufferedReader fin) throws Exception
	{
		int[] buffer = new int[128];
		DoubleArrayList weight;
		int i, j, b, ch;
		
		d_weights = new ArrayList<DoubleArrayList>(n_features);
		Integer.parseInt(fin.readLine());	// for compatibility
		
		for (i=0; i<n_features; i++)
		{
			if (i%100000 == 0)	LOG.debug(".");
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
	
		LOG.debug("\n");
		fin.readLine();
	}
	
	// ----------------------------------- SAVE MODELS -----------------------------------
	
	@Override
	public void save(PrintStream fout)
	{
		LOG.info("Saving model:");
		
		try
		{
			fout.println(i_solver);
			saveLabels(fout);
			saveFeatures(fout);
			saveWeightVector(fout);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	@Override
	protected void saveLabels(PrintStream fout)
	{
		fout.println(n_labels);
		fout.println(UTArray.join(a_labels, " "));
	}
	
	@Override
	protected void saveFeatures(PrintStream fout)
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

	@Override
	protected void saveWeightVector(PrintStream fout)
	{
		DoubleArrayList weight;
		StringBuilder build;
		int i, j;
		
		fout.println(n_labels * n_features);
		
		for (i=0; i<n_features; i++)
		{
			if (i%100000 == 0)	LOG.debug(".");
			weight = d_weights.get(i);
			build  = new StringBuilder();
			
			for (j=0; j<n_labels; j++)
			{
				build.append(weight.get(j));
				build.append(' ');
			}
			
			fout.print(build.toString());
		}
		
		LOG.debug("\n");
		fout.println();
	}
	
	// ----------------------------------- SETTERS -----------------------------------
	
	@Override
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
	
	@Override
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
	
	public void addFeatures(StringFeatureVector vector)
	{
		int i, size = vector.size();
		
		for (i=0; i<size; i++)
			addFeature(vector.getType(i), vector.getValue(i));
	}
	
	// ----------------------------------- PREDICT SPARSE -----------------------------------
	
	@Override
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
	
	@Override
	public List<StringPrediction> getPredictions(SparseFeatureVector x)
	{
		List<StringPrediction> list = new ArrayList<StringPrediction>(n_labels);
		double[] scores = getScores(x);
		int i;
		
		for (i=0; i<n_labels; i++)
			list.add(new StringPrediction(a_labels.get(i), scores[i]));
		
		return list;		
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
