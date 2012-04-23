package edu.colorado.clear.classification.model;



import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import edu.colorado.clear.classification.algorithm.AbstractAlgorithm;
import edu.colorado.clear.classification.model.StringModel;
import edu.colorado.clear.classification.prediction.StringPrediction;
import edu.colorado.clear.classification.vector.SparseFeatureVector;
import edu.colorado.clear.classification.vector.StringFeatureVector;

public class StringModelTest
{
	@Test
	public void testStringModelMultiClassification()
	{
		StringModel model    = new StringModel();
		String[]    labels   = {"A", "B", "C"};
		String[][]  features = {{"F00","F01"},{"F10"},{"F20","F21","F22"}};

		for (String label : labels)
			model.addLabel(label);
		
		model.addLabel("B");
		model.initLabelArray();
		
		for (int i=0; i<features.length; i++)
			for (String ftr : features[i])
				model.addFeature(Integer.toString(i), ftr);

		model.addFeature("0", "F00");
		model.addFeature("2", "F22");
					
		assertEquals(3, model.getLabelSize());
		assertEquals(7, model.getFeatureSize());
		
		for (int i=0; i<labels.length; i++)
			assertEquals(i, model.getLabelIndex(labels[i]));
		
		double[][] weights = {{1,0.1,0.01,0.001,0.0001,0.00001,0.000001},{3,0.3,0.03,0.003,0.0003,0.00003,0.000003},{2,0.2,0.02,0.002,0.0002,0.00002,0.000002}};
		model.initWeightVector();
		
		for (int i=0; i<weights.length; i++)
			model.copyWeightVector(i, weights[i]);
		
		testStringModelMultiClassificationAux(model);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.setSolver(AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L1_SV);
		model.save(new PrintStream(out));
		
		model = new StringModel(new BufferedReader(new StringReader(out.toString())));
		testStringModelMultiClassificationAux(model);
	}
	
	private void testStringModelMultiClassificationAux(StringModel model)
	{
		StringFeatureVector vector = new StringFeatureVector();
		
		vector.addFeature("0", "F00");
		vector.addFeature("1", "F10");
		vector.addFeature("2", "F21");
		vector.addFeature("2", "F22");
		vector.addFeature("2", "F23");
		vector.addFeature("3", "F00");

		SparseFeatureVector x = model.toSparseFeatureVector(vector);
		assertEquals("1 3 5 6", x.toString());
		
		StringPrediction p = model.predictBest(vector);
		assertEquals("B", p.label);
		assertEquals(true, p.score == 3.303033);
		
		List<StringPrediction> list = model.predictAll(vector);
		
		p = list.get(1);
		assertEquals("C", p.label);
		assertEquals(true, p.score == 2.202022);
		
		p = list.get(2);
		assertEquals("A", p.label);
		assertEquals(true, p.score == 1.101011);
		
		vector = new StringFeatureVector(true);
		
		vector.addFeature("0", "F00", 1);
		vector.addFeature("1", "F10", 2);
		vector.addFeature("2", "F21", 3);
		vector.addFeature("2", "F22", 4);
		
		p = model.predictAll(vector).get(2);
		assertEquals("A", p.label);
		assertEquals(true, 1.102034 == p.score);
	}
	
	@Test
	public void testStringModelBinaryClassification()
	{
		StringModel model    = new StringModel();
		String[]    labels   = {"A", "B"};
		String[][]  features = {{"F00","F01"},{"F10"},{"F20","F21","F22"}};

		for (String label : labels)
			model.addLabel(label);
		
		model.initLabelArray();
		
		for (int i=0; i<features.length; i++)
			for (String ftr : features[i])
				model.addFeature(Integer.toString(i), ftr);

		double[] weights = {1,0.1,0.01,0.001,0.0001,0.00001,0.000001};
		
		model.initWeightVector();
		model.copyWeightVector(weights);
		
		StringFeatureVector vector = new StringFeatureVector();
		
		vector.addFeature("0", "F00");
		vector.addFeature("1", "F10");
		vector.addFeature("2", "F21");
		vector.addFeature("2", "F22");
		vector.addFeature("2", "F23");
		vector.addFeature("3", "F00");
		
		StringPrediction p = model.predictBest(vector);
		assertEquals("A", p.label);
		assertEquals(true, p.score == 1.101011);
		
		List<StringPrediction> list = model.predictAll(vector);
		
		p = list.get(1);
		assertEquals("B", p.label);
		assertEquals(true, p.score == -1.101011);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.setSolver(AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L1_SV);
		model.save(new PrintStream(out));
		
		model  = new StringModel(new BufferedReader(new StringReader(out.toString())));
		vector = new StringFeatureVector(true);
		
		vector.addFeature("0", "F00", 1);
		vector.addFeature("1", "F10", 2);
		vector.addFeature("2", "F21", 3);
		vector.addFeature("2", "F22", 4);
		
		p = model.predictBest(vector);
		assertEquals("A", p.label);
		assertEquals(true, 1.102034 == p.score);
	}
}
