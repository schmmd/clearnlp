package edu.colorado.clear.classification.model;



import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import edu.colorado.clear.classification.algorithm.AbstractAlgorithm;
import edu.colorado.clear.classification.model.SparseModel;
import edu.colorado.clear.classification.prediction.StringPrediction;
import edu.colorado.clear.classification.vector.SparseFeatureVector;

public class SparseModelTest
{
	@Test
	public void testSparseModelMultiClassification()
	{
		SparseModel model = new SparseModel();
		String[] labels   = {"A", "B", "C"};
		int[]    features = {1, 2, 3, 4};

		for (String label : labels)
			model.addLabel(label);
		
		model.initLabelArray();
		model.addFeatures(features);

		assertEquals(3, model.getLabelSize());
		assertEquals(5, model.getFeatureSize());
		
		for (int i=0; i<labels.length; i++)
			assertEquals(i, model.getLabelIndex(labels[i]));

		double[][] weights = {{1,0.1,0.01,0.001,0.0001},{3,0.3,0.03,0.003,0.0003},{2,0.2,0.02,0.002,0.0002}};
		model.initWeightVector();
		
		for (int i=0; i<weights.length; i++)
			model.copyWeightVector(i, weights[i]);
		
		testSparseModelMultiClassificationAux(model);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.setSolver(AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L1_SV);
		model.save(new PrintStream(out));
		
		model = new SparseModel(new BufferedReader(new StringReader(out.toString())));
		testSparseModelMultiClassificationAux(model);
	}
	
	private void testSparseModelMultiClassificationAux(SparseModel model)
	{
		SparseFeatureVector x = new SparseFeatureVector();
		
		x.addFeature(1);
		x.addFeature(3);
		
		assertEquals("1 3", x.toString());
		
		StringPrediction p = model.predictBest(x);
		assertEquals("B", p.label);
		assertEquals(true, p.score == 3.303);
		
		List<StringPrediction> list = model.predictAll(x);
		
		p = list.get(1);
		assertEquals("C", p.label);
		assertEquals(true, p.score == 2.202);
		
		p = list.get(2);
		assertEquals("A", p.label);
		assertEquals(true, p.score == 1.101);
		
		x = new SparseFeatureVector(true);
		
		x.addFeature(1, 2);
		x.addFeature(3, 4);
		
		p = model.predictAll(x).get(2);
		assertEquals("A", p.label);
		assertEquals(true, 1.204 == p.score);
	}
	
	@Test
	public void testSparseModelBinaryClassification()
	{
		SparseModel model = new SparseModel();
		String[] labels   = {"A", "B"};
		int[]    features = {1, 2, 3, 4};

		for (String label : labels)
			model.addLabel(label);
		
		model.initLabelArray();
		model.addFeatures(features);
		
		double[] weights = {1,0.1,0.01,0.001,0.0001};
		
		model.initWeightVector();
		model.copyWeightVector(weights);
		
		SparseFeatureVector vector = new SparseFeatureVector();
		
		vector.addFeature(1);
		vector.addFeature(3);
		
		StringPrediction p = model.predictBest(vector);
		assertEquals("A", p.label);
		assertEquals(true, p.score == 1.101);
		
		List<StringPrediction> list = model.predictAll(vector);
		
		p = list.get(1);
		assertEquals("B", p.label);
		assertEquals(true, p.score == -1.101);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.setSolver(AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L1_SV);
		model.save(new PrintStream(out));
		
		model = new SparseModel(new BufferedReader(new StringReader(out.toString())));
		vector = new SparseFeatureVector(true);
		
		vector.addFeature(1, 2);
		vector.addFeature(3, 4);
		
		p = model.predictBest(vector);
		assertEquals("A", p.label);
		assertEquals(true, 1.204 == p.score);
	}
}
