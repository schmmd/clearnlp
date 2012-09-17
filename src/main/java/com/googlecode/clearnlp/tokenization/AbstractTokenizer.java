package com.googlecode.clearnlp.tokenization;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

import com.googlecode.clearnlp.morphology.MPLib;

abstract public class AbstractTokenizer
{
	final Pattern P_PERIODS = Pattern.compile("(\\.\\.+)");
	
	
	public AbstractTokenizer()
	{
		
	}
	
	public List<String> getTokens(BufferedReader fin)
	{
		List<String> tokens = new ArrayList<String>();
		String line;
		
		try
		{
			while ((line = fin.readLine()) != null)
			{
				for (String token : MPLib.splitWhiteSpaces(line))
					tokens.addAll(getTokens(token));
			}
		}
		catch (IOException e) {e.printStackTrace();}
		
		return tokens;
	}
	
	public List<String> getTokens(String str)
	{
		
		
		return null;
	}
	
	
	public void tokenizeApostropy(String str)
	{
		
	}
}
