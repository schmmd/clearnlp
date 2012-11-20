package com.googlecode.clearnlp.feature.xml;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;

import org.junit.Test;

import com.googlecode.clearnlp.util.UTInput;

public class DEPFtrXmlTest
{
	@Test
	public void testDEPFtrXml() throws Exception
	{
		DEPFtrXml xml1 = new DEPFtrXml(new FileInputStream("src/main/resources/feature/feature_en_dep.xml"));
		String s1 = xml1.toString();
		DEPFtrXml xml2 = new DEPFtrXml(UTInput.toInputStream(s1));
		assertEquals(s1, xml2.toString());
	}
}
