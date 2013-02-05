/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
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
package com.googlecode.clearnlp.morphology;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jregex.MatchResult;
import jregex.Replacer;
import jregex.Substitution;
import jregex.TextBuffer;

import com.googlecode.clearnlp.util.pair.Pair;

/**
 * Morphology library.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class MPLib
{
	static final public Pattern PUNCT_CHAR   = Pattern.compile("\\p{Punct}");
	static final public Pattern PUNCT_ONLY   = Pattern.compile("^\\p{Punct}+$");
	static final public Pattern PUNCT_PERIOD = Pattern.compile("^(\\.|\\?|\\!)+$");
	static final public jregex.Pattern PUNCT_REPEAT = new jregex.Pattern("\\.{2,}|\\!{2,}|\\?{2,}|\\-{2,}|\\*{2,}|\\={2,}|\\~{2,}|\\,{2,}");	// ".","!","?","-","*","=","~",","
	static final public Replacer PUNCT_REPEAT_REPLACE = PUNCT_REPEAT.replacer(new Substitution()
	{
		public void appendSubstitution(MatchResult match, TextBuffer dest)
		{
			char c = match.group(0).charAt(0);
			dest.append(c);
			dest.append(c);
		}
	});
	
	static final public Pattern DIGIT_SPAN = Pattern.compile("\\d+");
	static final public Pattern DIGIT_ONLY = Pattern.compile("^\\d+$");
	static final public Pattern DIGIT_LIKE = Pattern.compile("\\d%|\\$\\d|(^|\\d)\\.\\d|\\d,\\d|\\d:\\d|\\d-\\d|\\d\\/\\d");
	
	static final public Pattern ALPHA_CHAR = Pattern.compile("\\p{Alpha}");
	static final public Pattern ALNUM_CHAR = Pattern.compile("\\p{Alnum}");
	static final public Pattern WHITE_SPAN = Pattern.compile("\\s+");
	
//	static final public jregex.Pattern URL_SPAN = new jregex.Pattern("((([A-Za-z]{3,9}:(?:\\/\\/)?)(?:[-;:&=\\+\\$,\\w]+@)?[A-Za-z0-9.-]+|(?:www.|[-;:&=\\+\\$,\\w]+@)[A-Za-z0-9.-]+)((?:\\/[\\+~%\\/.\\w-_]*)?\\??(?:[-\\+=&;%@.\\w_]*)#?(?:[.\\!\\/\\\\w]*))?)");
	static final public jregex.Pattern URL_SPAN = new jregex.Pattern("((([A-Za-z]{3,9}:(?:\\/\\/)?)(?:[-;:&=\\+\\$,\\w]+@)?[A-Za-z0-9.-]+|(?:www.|[-;:&=\\+\\$,\\w]+@)[A-Za-z0-9.-]+)((?:\\/[\\+~%\\/.\\w-_]*)?\\??(?:[-\\+=&;%@.\\w_]*)#?(?:[.\\!\\/\\\\w]*))?|(\\w+\\.)+(com|edu|gov|int|mil|net|org|biz)$)");
	static final public Pattern FILE_EXTS = Pattern.compile("\\S+\\.(3gp|7z|ace|ai(f){0,2}|amr|asf|asp(x)?|asx|avi|bat|bin|bmp|bup|cab|cbr|cd(a|l|r)|chm|dat|divx|dll|dmg|doc|dss|dvf|dwg|eml|eps|exe|fl(a|v)|gif|gz|hqx|(s)?htm(l)?|ifo|indd|iso|jar|jsp|jp(e)?g|lnk|log|m4(a|b|p|v)|mcd|mdb|mid|mov|mp(2|3|4)|mp(e)?g|ms(i|wmm)|ogg|pdf|php|png|pps|ppt|ps(d|t)?|ptb|pub|qb(b|w)|qxd|ra(m|r)|rm(vb)?|rtf|se(a|s)|sit(x)?|sql|ss|swf|tgz|tif|torrent|ttf|txt|vcd|vob|wav|wm(a|v)|wp(d|s)|xls|xml|xtm|zip)$");
	
	static public boolean containsURL(String str)
	{
		return URL_SPAN.matcher(str).find();
	}
	
	static public String[] splitWhiteSpaces(String str)
	{
		return WHITE_SPAN.split(str);
	}
	
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
	
	/**
	 * Returns a normalized form of the specific word-form.
	 * @see MPLib#containsURL(String)
	 * @see MPLib#normalizeDigits(String)
	 * @see MPLib#normalizePunctuation(String)
	 * @param form the word-form.
	 * @return a normalized form of the specific word-form.
	 */
	static public String normalizeBasic(String form)
	{
		if (MPLib.containsURL(form))	return "#url#";
		
		form = MPLib.normalizeDigits(form);
		form = MPLib.normalizePunctuation(form);
		
		return form;
	}
	
	/**
	 * Normalizes all digits to 0.
	 * @param form the word-form to be normalized.
	 * @return the normalized form.
	 */
	static public String normalizeDigits(String form)
	{
		form = DIGIT_LIKE.matcher(form).replaceAll("0");
		return DIGIT_SPAN.matcher(form).replaceAll("0");
	}
	
	/**
	 * Collapses redundant punctuation in the specific word-form (e.g., "!!!" -> "!").
	 * @param form the word-form to be normalized.
	 * @return normalized word-form.
	 */
	static public String normalizePunctuation(String form)
	{
		return PUNCT_REPEAT_REPLACE.replace(form);
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
	 * Returns {@code true} if the specific word-form contains only punctuation.
	 * @param form the word-form to be compared.
	 * @return {@code true} if the specific word-form contains only punctuation.
	 */
	static public boolean containsAnyPunctuation(String form)
	{
		return PUNCT_CHAR.matcher(form).find();
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
	
	static public boolean isAlpha(String form)
	{
		return ALPHA_CHAR.matcher(form).find();
	}
	
	static public boolean isAlnum(String form)
	{
		return ALNUM_CHAR.matcher(form).find();		
	}
}
