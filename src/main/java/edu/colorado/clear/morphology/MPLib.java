/**
* Copyright (c) 2011, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package edu.colorado.clear.morphology;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.colorado.clear.util.pair.Pair;

/**
 * Morphology library.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class MPLib
{
	static final protected Pattern URL_URI       = Pattern.compile("(cvs|dns|file|ftp|http|https|imap|mms|pop|rsync|rtmp|ssh|sftp|smb|svn)://.+");
	static final protected Pattern URL_DOMAIN    = Pattern.compile(".+\\.(com|edu|gov|int|mil|net|org)");
	static final protected Pattern URL_DOMAIN_KR = Pattern.compile(".+\\.(co|ac|go|mil|ne|or)\\.kr");
	static final protected Pattern URL_WEB_EXT   = Pattern.compile(".+\\.(asp|aspx|htm|html|jsp|php|shtml|xml)");

	static final protected Pattern PUNCT_ANY	= Pattern.compile("\\p{Punct}");
	static final protected Pattern PUNCT_ONLY	= Pattern.compile("^\\p{Punct}+$");
	static final protected Pattern PUNCT_PERIOD	= Pattern.compile("^(\\.|\\?|\\!)+$");

	static final protected Pattern   DIGIT_SPAN	= Pattern.compile("\\d+");
	static final protected Pattern   DIGIT_ONLY	= Pattern.compile("^\\d+$");
	static final protected Pattern[] DIGIT_LIKE	= {Pattern.compile("\\d%"), Pattern.compile("\\$\\d"), Pattern.compile("(^|\\d)\\.\\d"), Pattern.compile("\\d,\\d"), Pattern.compile("\\d:\\d"), Pattern.compile("\\d-\\d"), Pattern.compile("\\d\\/\\d")};
	
	@SuppressWarnings("serial")
	static final protected List<Pair<Pattern, String>> BRACKET_LIST = new ArrayList<Pair<Pattern,String>>()
	{{
		add(new Pair<Pattern,String>(Pattern.compile("-LRB-"), "("));
		add(new Pair<Pattern,String>(Pattern.compile("-RRB-"), ")"));
		add(new Pair<Pattern,String>(Pattern.compile("-LSB-"), "["));
		add(new Pair<Pattern,String>(Pattern.compile("-RSB-"), "]"));
		add(new Pair<Pattern,String>(Pattern.compile("-LCB-"), "{"));
		add(new Pair<Pattern,String>(Pattern.compile("-RCB-"), "}"));
		
		trimToSize();
	}};
	
	@SuppressWarnings("serial")
	static final protected List<Pair<Pattern, String>> PUNC_REPEAT_LIST = new ArrayList<Pair<Pattern, String>>()
	{{
		final String[] PUNC_REPEAT_ARRAY = {".","!","?","-","*","=","~",","};
		
		for (String p : PUNC_REPEAT_ARRAY)
			add(new Pair<Pattern,String>(Pattern.compile(String.format("(\\%s)+", p)), p));
	}};
	
	/**
	 * Returns {@code true} if the specific word-form contains an URL.
	 * @param form the word-form to be compared (assumed to be all in lowercase).
	 * @return {@code true} if the specific form contains an URL.
	 */
	static public boolean containsURL(String form)
	{
		if      (URL_URI.matcher(form).find())
			return true;
		else if (URL_DOMAIN.matcher(form).find())
			return true;
		else if (URL_DOMAIN_KR.matcher(form).find())
			return true;
		else if (URL_WEB_EXT.matcher(form).find())
			return true;
		
		return false;
	}
	
	/**
	 * Reverts coded brackets to their original forms (e.g., from "-LBR-" to "(").
	 * @param form the word-form.
	 * @return the reverted form of coded brackets.
	 */
	static public String revertBracket(String form)
	{
		for (Pair<Pattern,String> p : BRACKET_LIST)
			form = p.o1.matcher(form).replaceAll(p.o2);
		
		return form;
	}
	
	/**
	 * Collapses redundant punctuation in the specific word-form (e.g., "!!!" -> "!").
	 * @param form the word-form to be normalized.
	 * @return normalized word-form.
	 */
	static public String normalizePunctuation(String form)
	{
		for (Pair<Pattern,String> p : PUNC_REPEAT_LIST)
			form = p.o1.matcher(form).replaceAll(p.o2);
		
		return form;		
	}
	
	/**
	 * Returns {@code true} if the specific word-form contains only punctuation.
	 * @param form the word-form to be compared.
	 * @return {@code true} if the specific word-form contains only punctuation.
	 */
	static public boolean containsAnyPunctuation(String form)
	{
		return PUNCT_ANY.matcher(form).find();
	}
	
	/**
	 * Returns {@code true} if the specific word-form contains only punctuation.
	 * @param form the word-form to be compared.
	 * @return {@code true} if the specific word-form contains only punctuation.
	 */
	static public boolean containsOnlyPunctuation(String form)
	{
		return PUNCT_ONLY.matcher(form).find();
	}

	/**
	 * Returns {@code true} if the specific word-form contains any of the specified punctuation.
	 * @param form the word-form.
	 * @param punctuation the array of punctuation.
	 * @return {@code true} if the specific word-form contains any of the specified punctuation.
	 */
	static public boolean containsAnySpecificPunctuation(String form, char... punctuation)
	{
		int i, size = form.length();
		
		for (i=0; i<size; i++)
		{
			for (char p : punctuation)
			{
				if (form.charAt(i) == p)
					return true;	
			}
		}
		
		return false;
	}
	
	/**
	 * Normalizes all digits to 0.
	 * @param form the word-form to be normalized.
	 * @return the normalized form.
	 */
	static public String normalizeDigits(String form)
	{
		for (Pattern p : DIGIT_LIKE)
			form = p.matcher(form).replaceAll("0");
		
		return DIGIT_SPAN.matcher(form).replaceAll("0");
	}
	
	/**
	 * Returns {@code true} if the specific word-form contains only digits.
	 * @param form the word-form to be compared.
	 * @return {@code true} if the specific word-form contains only digits.
	 */
	static public boolean containsOnlyDigits(String form)
	{
		return DIGIT_ONLY.matcher(form).find();
	}
	
	static public boolean isPeriodLike(String form)
	{
		if (PUNCT_PERIOD.matcher(form).find())
			return true;
		
		if (form.length() > 1 && form.charAt(0) == '/')
			return PUNCT_PERIOD.matcher(form.substring(1)).find();
		
		return false;
	}
}
