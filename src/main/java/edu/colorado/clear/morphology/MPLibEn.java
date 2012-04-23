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

import java.util.regex.Pattern;

import edu.colorado.clear.constituent.CTLibEn;

/**
 * Morphology library for English.
 * @since 0.1.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class MPLibEn extends MPLib
{
	/** Derivations of a verb "be". */
	static public Pattern RE_BE		= Pattern.compile("^(be|been|being|am|is|was|are|were|'m|'s|'re)$");
	/** Derivations of a verb "become". */
	static public Pattern RE_BECOME	= Pattern.compile("^(become|becomes|became|becoming)$");
	/** Derivations of a verb "get". */
	static public Pattern RE_GET	= Pattern.compile("^(get|gets|got|gotten|getting)$");
	/** Derivations of a verb "have". */
	static public Pattern RE_HAVE	= Pattern.compile("^(have|has|had|having|'ve|'d)$");
	/** Derivations of a verb "do". */
	static public Pattern RE_DO		= Pattern.compile("^(do|does|did|done|doing)$");
	
	/**
	 * Returns {@code true} if the specific word-form is a derivation of a verb "be".
	 * @param form the word-form to be compared.
	 * @return {@code true} if the specific form is a derivation of a verb "be".
	 */
	static public boolean isBe(String form)
	{
		return RE_BE.matcher(form.toLowerCase()).find();
	}
	
	/**
	 * Returns {@code true} if the specific word-form is a derivation of a verb "get".
	 * @param form the word-form to be compared.
	 * @return {@code true} if the specific form is a derivation of a verb "get".
	 */
	static public boolean isGet(String form)
	{
		return RE_GET.matcher(form.toLowerCase()).find();
	}
	
	/**
	 * Returns {@code true} if the specific word-form is a derivation of a verb "become".
	 * @param form the word-form to be compared.
	 * @return {@code true} if the specific form is a derivation of a verb "become".
	 */
	static public boolean isBecome(String form)
	{
		return RE_BECOME.matcher(form.toLowerCase()).find();
	}
	
	/**
	 * Returns {@code true} if the specific word-form is a derivation of a verb "have".
	 * @param form the word-form to be compared.
	 * @return {@code true} if the specific form is a derivation of a verb "have".
	 */
	static public boolean isHave(String form)
	{
		return RE_HAVE.matcher(form.toLowerCase()).find();
	}
	
	/**
	 * Returns {@code true} if the specific word-form is a derivation of a verb "do".
	 * @param form the word-form to be compared.
	 * @return {@code true} if the specific form is a derivation of a verb "do".
	 */
	static public boolean isDo(String form)
	{
		return RE_DO.matcher(form.toLowerCase()).find();
	}
	
	/**
	 * Returns {@code true} if the specific POS tag is a noun.
	 * @param pos the POS tag to be compared.
	 * @return {@code true} if the specific POS tag is a noun.
	 */
	static public boolean isNoun(String pos)
	{
		return pos.startsWith(CTLibEn.POS_NN) || pos.equals(CTLibEn.POS_PRP) || pos.equals(CTLibEn.POS_WP);
	}
	
	/**
	 * Returns {@code true} if the specific POS tag is a verb.
	 * @param pos the POS tag to be compared.
	 * @return {@code true} if the specific POS tag is a verb.
	 */
	static public boolean isVerb(String pos)
	{
		return pos.startsWith(CTLibEn.POS_VB);
	}
	
	/**
	 * Returns {@code true} if the specific POS tag is a adjective.
	 * @param pos the POS tag to be compared.
	 * @return {@code true} if the specific POS tag is a adjective.
	 */
	static public boolean isAdjective(String pos)
	{
		return pos.startsWith(CTLibEn.POS_JJ);
	}
	
	/**
	 * Returns {@code true} if the specific POS tag is a adverb.
	 * @param pos the POS tag to be compared.
	 * @return {@code true} if the specific POS tag is a adverb.
	 */
	static public boolean isAdverb(String pos)
	{
		return pos.startsWith(CTLibEn.POS_RB) || pos.equals(CTLibEn.POS_WRB);
	}
}
