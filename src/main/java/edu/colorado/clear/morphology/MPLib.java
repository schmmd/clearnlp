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

import com.carrotsearch.hppc.CharOpenHashSet;

import edu.colorado.clear.dependency.DEPNode;
import edu.colorado.clear.dependency.DEPTree;
import edu.colorado.clear.pos.POSNode;
import edu.colorado.clear.reader.AbstractReader;

/**
 * Morphology library.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class MPLib
{
	static final protected Pattern URL_URI       = Pattern.compile("(cvs|dns|file|ftp|http|https|imap|mms|pop|rsync|rtmp|ssh|sftp|smb|svn)://.+");
	static final protected Pattern URL_DOMAIN    = Pattern.compile(".+\\.(com|edu|gov|int|mil|net|org)");
	static final protected Pattern URL_DOMAIN_KR = Pattern.compile(".+\\.(co|ac|go|mil|ne|or)\\.kr");
	static final protected Pattern URL_WEB_EXT   = Pattern.compile(".+\\.(asp|aspx|htm|html|jsp|php|shtml|xml)");

	static final protected char[]   PUNC_ALL_ARRAY		= {'.','!','?',';',':',',','@','#','$','%','^','&','*','(',')','{','}','[',']','<','>','+','-','/','=','~','\\','_','|','"','\'','`'};
	static final protected char[]   PUNC_BOUNDARY_ARRAY	= {'.','!','?',';',':',',','(',')','{','}','[',']','"','`'};
	static final protected String[] PUNC_REPEAT_ARRAY	= {".","!","?","-","*","=","~",","};
	
	static final protected Pattern[] DIGIT_LIKE = {Pattern.compile("\\d%"), Pattern.compile("\\$\\d"), Pattern.compile("(^|\\d)\\.\\d"), Pattern.compile("\\d,\\d"), Pattern.compile("\\d:\\d"), Pattern.compile("\\d-\\d"), Pattern.compile("\\d\\/\\d")};
	static final protected Pattern   DIGITS = Pattern.compile("\\d+");
	
	@SuppressWarnings("serial")
	static final protected List<Pattern> PUNC_REPEAT = new ArrayList<Pattern>(PUNC_REPEAT_ARRAY.length)
	{{
		for (String p : PUNC_REPEAT_ARRAY)
			add(Pattern.compile(String.format("(\\%s)+", p)));
	}};
	
	static final protected CharOpenHashSet PUNC_ALL = new CharOpenHashSet()
	{{
		for (char p : PUNC_ALL_ARRAY)	add(p);
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
	 * Returns {@code true} if the specific character is punctuation.
	 * @param c the character to be compared.
	 * @return {@code true} if the specific character is punctuation.
	 */
	static public boolean isPunctuation(char c)
	{
		return PUNC_ALL.contains(c);
	}
	
	static public boolean containsOnlyPunctuation(String form)
	{
		int i, size = form.length();
		
		for (i=0; i<size; i++)
		{
			if (!isPunctuation(form.charAt(i)))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Collapses redundant punctuation in the specific word-form (e.g., "!!! -> !").
	 * @param form the word-form to be normalized.
	 * @return normalized form.
	 */
	static public String normalizePunctuation(String form)
	{
		int i, size = PUNC_REPEAT_ARRAY.length;
		
		for (i=0; i<size; i++)
			form = PUNC_REPEAT.get(i).matcher(form).replaceAll(PUNC_REPEAT_ARRAY[i]);
		
		return form;		
	}

	/**
	 * Normalizes digits.
	 * @param form the word-form to be normalized.
	 * @return normalized form.
	 */
	static public String normalizeDigits(String form)
	{
		for (Pattern p : DIGIT_LIKE)
			form = p.matcher(form).replaceAll("0");
		
		return DIGITS.matcher(form).replaceAll("0");
	}
	
	static public String trimPunctuation(String form)
	{
		int bIdx, eIdx, size = form.length();
		
		for (bIdx=0; bIdx<size; bIdx++)
		{
			if (!isPunctuation(form.charAt(bIdx)))
				break;
		}
		
		if (bIdx == size)	return form;
		
		for (eIdx=size-1; eIdx>bIdx; eIdx--)
		{
			if (!isPunctuation(form.charAt(bIdx)))
				break;
		}
		
		if (bIdx == eIdx)	return form;
		
		return form.substring(bIdx, eIdx+1);
	}
	
	static public void lemmatize(AbstractMPAnalyzer morph, POSNode[] nodes)
	{
		for (POSNode node : nodes)
			node.lemma = morph.getLemma(node.form, node.pos);
	}
	
	static public void lemmatize(AbstractMPAnalyzer morph, DEPTree tree)
	{
		int i, size = tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			node.lemma = morph.getLemma(node.form, node.pos);
		}
	}
	
	static public void normalizeForms(POSNode[] nodes)
	{
		if (!nodes[0].isSimplifiedForm(AbstractReader.DUMMY_TAG))
			return;
		
		for (POSNode node : nodes)
		{
			node.simplifiedForm = normalizeDigits(node.form);
			node.lemma = node.simplifiedForm.toLowerCase();
		}
	}
	
	static public boolean isPeriod(String form)
	{
		int size = form.length();
		
		if (size > 1 && form.charAt(0) == '/')
		{
			int i;	char c;
			
			for (i=1; i<size; i++)
			{
				c = form.charAt(i);
				
				if (c != '.' && c != '?' && c != '!')
					return false;
			}
			
			return true;
		}
		
		return false;
	}
}
