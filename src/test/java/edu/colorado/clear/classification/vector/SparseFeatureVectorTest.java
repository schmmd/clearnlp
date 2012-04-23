package edu.colorado.clear.classification.vector;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import edu.colorado.clear.classification.vector.SparseFeatureVector;

public class SparseFeatureVectorTest
{
	@Test
	public void testSparseFeatureVector()
	{
		// features without weights
		SparseFeatureVector vector = new SparseFeatureVector();

		vector.addFeature(0);
		vector.addFeature("1");
		
		assertEquals(0, vector.getIndex(0));
		assertEquals(2, vector.size());
		assertEquals("0 1", vector.toString());
		
		// features with weights
		vector = new SparseFeatureVector(true);
		
		vector.addFeature(0, 0.1);
		vector.addFeature("1:0.2");
		
		assertEquals(0, vector.getIndex(0));
		assertEquals(true, 0.2 == vector.getWeight(1));
		assertEquals(2, vector.size());
		assertEquals("0:0.1 1:0.2", vector.toString());
	}
}
