package edu.colorado.clear.morphology;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.colorado.clear.morphology.MPLib;

public class MPLibTest
{
	@Test
	public void containsURLTest()
	{
		assertEquals(true , MPLib.containsURL("http://colorado"));
		assertEquals(false, MPLib.containsURL("htp://colorado"));
		assertEquals(false, MPLib.containsURL("http://"));
		assertEquals(true , MPLib.containsURL("colorado.edu"));
		assertEquals(false, MPLib.containsURL(".edu"));
		assertEquals(false, MPLib.containsURL("korea.ac"));
		assertEquals(true , MPLib.containsURL("korea.ac.kr"));
		assertEquals(false, MPLib.containsURL(".ac.kr"));
		assertEquals(true , MPLib.containsURL("index.html"));
		assertEquals(false, MPLib.containsURL(".html"));
		assertEquals(false, MPLib.containsURL("index.java"));
	}
	
	@Test
	public void normalizePunctuationTest()
	{
		String form = "...!!!???---***===~~~,,,";
		assertEquals(".!?-*=~,", MPLib.normalizePunctuation(form));
	}
	
	@Test
	public void containsPunctuationTest()
	{
		String form = "a@b.com";
		assertEquals(true , MPLib.containsAnyPunctuation(form));
		assertEquals(false, MPLib.containsOnlyPunctuation(form));
		assertEquals(true , MPLib.containsAnySpecificPunctuation(form, '@', '?'));	
		assertEquals(false, MPLib.containsAnySpecificPunctuation(form, '!', '?'));
		
		form = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
		assertEquals(true, MPLib.containsAnyPunctuation(form));
		assertEquals(true, MPLib.containsOnlyPunctuation(form));
		assertEquals(true, MPLib.containsAnySpecificPunctuation(form, '@', '?'));
		
		form = "abcde";
		assertEquals(false, MPLib.containsAnyPunctuation(form));
	}
	
	@Test
	public void normalizeDigitsTest()
	{
		String form = "10%";
		assertEquals("0", MPLib.normalizeDigits(form));
		
		form = "$10";
		assertEquals("0", MPLib.normalizeDigits(form));
		
		form = "A.01";
		assertEquals("A.0", MPLib.normalizeDigits(form));
		
		form = ".01";
		assertEquals("0", MPLib.normalizeDigits(form));
		
		form = "12.34";
		assertEquals("0", MPLib.normalizeDigits(form));
		
		form = "12,34,56";
		assertEquals("0", MPLib.normalizeDigits(form));
		
		form = "12:34:56";
		assertEquals("0", MPLib.normalizeDigits(form));
		
		form = "12-34-56";
		assertEquals("0", MPLib.normalizeDigits(form));
		
		form = "A:01";
		assertEquals("A:0", MPLib.normalizeDigits(form));
		
		form = "12/34/56";
		assertEquals("0", MPLib.normalizeDigits(form));
		
		form = "A/01";
		assertEquals("A/0", MPLib.normalizeDigits(form));
		
		form = "$10.23,45:67-89/10%";
		assertEquals("0", MPLib.normalizeDigits(form));
	}
	
	@Test
	public void testNormalize()
	{
		String tmp = "...!!!???---***===~~~,,,";
		assertEquals(".!?-*=~,", MPLib.normalizePunctuation(tmp));
		
		tmp = "10%";
		assertEquals("0", MPLib.normalizeDigits(tmp));
		tmp = "$10";
		assertEquals("0", MPLib.normalizeDigits(tmp));
		tmp = "A.01";
		assertEquals("A.0", MPLib.normalizeDigits(tmp));
		tmp = ".01";
		assertEquals("0", MPLib.normalizeDigits(tmp));
		tmp = "12.34";
		assertEquals("0", MPLib.normalizeDigits(tmp));
		tmp = "12,34,56";
		assertEquals("0", MPLib.normalizeDigits(tmp));
		tmp = "12:34:56";
		assertEquals("0", MPLib.normalizeDigits(tmp));
		tmp = "12-34-56";
		assertEquals("0", MPLib.normalizeDigits(tmp));
		tmp = "A:01";
		assertEquals("A:0", MPLib.normalizeDigits(tmp));
		tmp = "12/34/56";
		assertEquals("0", MPLib.normalizeDigits(tmp));
		tmp = "A/01";
		assertEquals("A/0", MPLib.normalizeDigits(tmp));
		tmp = "$10.23,45:67-89/10%";
		assertEquals("0", MPLib.normalizeDigits(tmp));
	}
	
	@Test
	public void revertBracketTest()
	{
		assertEquals("(", MPLib.revertBracket("-LRB-"));
		assertEquals(")", MPLib.revertBracket("-RRB-"));
		assertEquals("[", MPLib.revertBracket("-LSB-"));
		assertEquals("]", MPLib.revertBracket("-RSB-"));
		assertEquals("{", MPLib.revertBracket("-LCB-"));
		assertEquals("}", MPLib.revertBracket("-RCB-"));
		
		assertEquals("(0)", MPLib.revertBracket("-LRB-0-RRB-"));
		assertEquals(":-)", MPLib.revertBracket(":--RRB-"));
	}
}
