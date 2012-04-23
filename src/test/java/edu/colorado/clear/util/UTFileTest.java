package edu.colorado.clear.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class UTFileTest
{
	@Test
	public void replaceExtensionTest()
	{
		assertEquals("a.b.jpg", UTFile.replaceExtension("a.b.txt", "jpg"));
		assertEquals("a.jpg"  , UTFile.replaceExtension("a.b"    , "jpg"));
		assertEquals("a."     , UTFile.replaceExtension("a.b"    , ""));
		assertEquals("a.jpg"  , UTFile.replaceExtension("a"      , "jpg"));
		
		assertEquals("a.jpg", UTFile.replaceExtension("a.txt", "txt", "jpg"));
		assertEquals("a.txt", UTFile.replaceExtension("a.txt", "bmp", "jpg"));
	}
	
	public void getSortedFileListTest()
	{
		for (String filename : UTFile.getSortedFileList("/home/choijd/"))
			System.out.println(filename);

		System.out.println();
				
		for (String filename : UTFile.getSortedFileList("/home/choijd/", "pdf"))
			System.out.println(filename);
	}
}
