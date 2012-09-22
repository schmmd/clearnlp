package com.googlecode.clearnlp.tokenization;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.googlecode.clearnlp.morphology.MPLib;

public class DefaultTokenizer
{
	Pattern[] P_MARKERS;
	
	final String M_ELIPSIS = "ELIPSIS_MARKER_";
	
	public DefaultTokenizer()
	{
		initPatterns();
	}
	
	private void initPatterns()
	{
		String[] markers = {".","-","*","!","~"};
		int i, size = markers.length;
		
		P_MARKERS = new Pattern[size];
		
		for (i=0; i<size; i++)
			P_MARKERS[i] = Pattern.compile(String.format("(\\%s\\%s+)", markers[i]));
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
	
	public String tokenize(String str)
	{
	//	str = protectPatterns(str, P_ELIPSIS, M_ELIPSIS);
		
		
		return str;
	}
		
	private String protectPatterns(String str, Pattern p, String protect)
	{
		Matcher m;
		
		while (true)
		{
			m = p.matcher(str);
			
			if (m.find())	str = m.replaceFirst(" "+protect+m.group(0).length()+" ");
			else			break;
		}
		
		return str;
	}
}
