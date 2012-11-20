package com.googlecode.clearnlp.util;

import java.util.regex.Pattern;

public class UTRegex
{
	static public Pattern getORPattern(String... regex)
	{
		StringBuilder build = new StringBuilder();
		
		for (String r : regex)
		{
			build.append("|");
			build.append(r);
		}
		
		return Pattern.compile(build.substring(1));
	}
}
