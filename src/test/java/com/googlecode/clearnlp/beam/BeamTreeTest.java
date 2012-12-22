package com.googlecode.clearnlp.beam;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.googlecode.clearnlp.classification.prediction.StringPrediction;

public class BeamTreeTest
{
	@Test	
	public void test1BeamTree()
	{
		List<List<StringPrediction>> predictions = new ArrayList<List<StringPrediction>>(); 
		BeamTree<Object> tree = new BeamTree<Object>(4);
		List<StringPrediction> preds;
		
		// 1st predictions
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("A", 1));
		preds.add(new StringPrediction("B", 2));
		Collections.sort(preds);
		predictions.add(preds);
		
		tree.setBeam(predictions);
		predictions.clear();
		assertEquals(tree.toString(), "B:2.0\nA:1.0");
		
		// 2nd predictions
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("B1", 1));
		preds.add(new StringPrediction("B2", 3));
		Collections.sort(preds);
		predictions.add(preds);
		
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("A1", 2));
		preds.add(new StringPrediction("A2", 4));
		Collections.sort(preds);
		predictions.add(preds);
		
		tree.setBeam(predictions);
		predictions.clear();
		assertEquals(tree.toString(), "A:1.0 -> A2:4.0\nB:2.0 -> B2:3.0\nA:1.0 -> A1:2.0\nB:2.0 -> B1:1.0");
		
		// 3rd predictions
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("A21", 1));
		preds.add(new StringPrediction("A22", 5));
		Collections.sort(preds);
		predictions.add(preds);
		
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("B21", 6));
		preds.add(new StringPrediction("B22", 2));
		Collections.sort(preds);
		predictions.add(preds);
		
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("A11", 7));
		preds.add(new StringPrediction("A12", 3));
		Collections.sort(preds);
		predictions.add(preds);
		
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("B11", 4));
		preds.add(new StringPrediction("B12", 8));
		Collections.sort(preds);
		predictions.add(preds);

		tree.setBeam(predictions);
		predictions.clear();
		assertEquals(tree.toString(), "B:2.0 -> B1:1.0 -> B12:8.0\nA:1.0 -> A1:2.0 -> A11:7.0\nB:2.0 -> B2:3.0 -> B21:6.0\nA:1.0 -> A2:4.0 -> A22:5.0");
		
		// 4th predictions
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("B121", 1));
		preds.add(new StringPrediction("B122", 7));
		Collections.sort(preds);
		predictions.add(preds);
		
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("A111", 2));
		preds.add(new StringPrediction("A112", 6));
		Collections.sort(preds);
		predictions.add(preds);

		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("B211", 4));
		preds.add(new StringPrediction("B212", 8));
		Collections.sort(preds);
		predictions.add(preds);
		
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("A221", 3));
		preds.add(new StringPrediction("A222", 5));
		Collections.sort(preds);
		predictions.add(preds);

		tree.setBeam(predictions);
		predictions.clear();
		assertEquals(tree.toString(), "B:2.0 -> B2:3.0 -> B21:6.0 -> B212:8.0\nB:2.0 -> B1:1.0 -> B12:8.0 -> B122:7.0\nA:1.0 -> A1:2.0 -> A11:7.0 -> A112:6.0\nA:1.0 -> A2:4.0 -> A22:5.0 -> A222:5.0");
	}
	
	@Test	
	public void test2BeamTree()
	{
		List<List<StringPrediction>> predictions = new ArrayList<List<StringPrediction>>(); 
		BeamTree<Object> tree = new BeamTree<Object>(4);
		List<StringPrediction> preds;
		
		// 1st predictions
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("A", 1));
		preds.add(new StringPrediction("B", 2));
		preds.add(new StringPrediction("C", 3));
		preds.add(new StringPrediction("D", 4));
		preds.add(new StringPrediction("E", 5));
		Collections.sort(preds);
		predictions.add(preds);
		
		tree.setBeam(predictions);
		predictions.clear();
		assertEquals(tree.toString(), "E:5.0\nD:4.0\nC:3.0\nB:2.0");
		
		// 2nd predictions
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("E1", 1));
		preds.add(new StringPrediction("E2", 5));
		preds.add(new StringPrediction("E3", 9));
		preds.add(new StringPrediction("E4", 13));
		preds.add(new StringPrediction("E5", 17));
		Collections.sort(preds);
		predictions.add(preds);
		
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("D1", 2));
		preds.add(new StringPrediction("D2", 6));
		preds.add(new StringPrediction("D3", 10));
		preds.add(new StringPrediction("D4", 14));
		preds.add(new StringPrediction("D5", 18));
		Collections.sort(preds);
		predictions.add(preds);
		
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("C1", 3));
		preds.add(new StringPrediction("C2", 7));
		preds.add(new StringPrediction("C3", 11));
		preds.add(new StringPrediction("C4", 15));
		preds.add(new StringPrediction("C5", 19));
		Collections.sort(preds);
		predictions.add(preds);
		
		preds = new ArrayList<StringPrediction>();
		preds.add(new StringPrediction("B1", 4));
		preds.add(new StringPrediction("B2", 8));
		preds.add(new StringPrediction("B3", 12));
		preds.add(new StringPrediction("B4", 16));
		preds.add(new StringPrediction("B5", 20));
		Collections.sort(preds);
		predictions.add(preds);
		
		tree.setBeam(predictions);
		predictions.clear();
		assertEquals(tree.toString(), "B:2.0 -> B5:20.0\nC:3.0 -> C5:19.0\nD:4.0 -> D5:18.0\nE:5.0 -> E5:17.0");
	}
}
