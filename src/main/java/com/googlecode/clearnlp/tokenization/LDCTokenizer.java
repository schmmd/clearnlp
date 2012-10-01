package com.googlecode.clearnlp.tokenization;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.googlecode.clearnlp.morphology.MPLib;

public class LDCTokenizer
{
	protected final String[] S_MARKERS_O = {".","-","*","!","~"};
	protected final String[] S_MARKERS_R = {"MARKER_PERIOD_","MARKER_HYPHEN_","MARKER_ASTERISK_","MARKER_EXCLAMATION_","MARKER_TILDA_"};
	
	protected Pattern[] P_MARKERS;
	
	public LDCTokenizer()
	{
		initMarkers();
	}
	
	private void initMarkers()
	{
		int i, size = S_MARKERS_O.length;
		
		P_MARKERS = new Pattern[size];
		
		for (i=0; i<size; i++)
			P_MARKERS[i] = Pattern.compile(String.format("(\\%s\\%s+)", S_MARKERS_O[i], S_MARKERS_O[i]));
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
		str = protectMarkers(str);
		
		
		return str;
	}
	
	public String protectMarkers(String str)
	{
		int i, size = P_MARKERS.length;
		
		for (i=0; i<size; i++)
			str = protectMarkers(str, P_MARKERS[i], S_MARKERS_R[i]);
		
		return str;
	}
		
	/** Called by {@link LDCTokenizer#protectMarkers(String)}. */
	protected String protectMarkers(String str, Pattern p, String protect)
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
