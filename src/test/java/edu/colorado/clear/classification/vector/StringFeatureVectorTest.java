package edu.colorado.clear.classification.vector;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.colorado.clear.classification.vector.StringFeatureVector;

public class StringFeatureVectorTest
{
	@Test
	public void testStringFeatureVector()
	{
		// features without weights
		StringFeatureVector vector = new StringFeatureVector();

		vector.addFeature("0", "A");
		vector.addFeature("1:B");
		
		assertEquals("0", vector.getType(0));
		assertEquals("B", vector.getValue(1));
		assertEquals(2, vector.size());
		assertEquals("0:A 1:B", vector.toString());
		
		// features with weights
		vector = new StringFeatureVector(true);
		
		vector.addFeature("0", "A", 0.1);
		vector.addFeature("1:B:0.2");
		
		assertEquals("0", vector.getType(0));
		assertEquals("B", vector.getValue(1));
		assertEquals(true, 0.2 == vector.getWeight(1));
		assertEquals(2, vector.size());
		assertEquals("0:A:0.1 1:B:0.2", vector.toString());
	}
}
