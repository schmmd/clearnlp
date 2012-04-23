package edu.colorado.clear.morphology;

import static org.junit.Assert.*;

import org.junit.Test;

public class MPLibEnTest
{
	@Test
	public void testIsBe()
	{
		assertEquals(true , MPLibEn.isBe("'m"));
		assertEquals(false, MPLibEn.isBe("become"));
	}
}
