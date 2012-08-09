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
package com.googlecode.clearnlp.morphology;

import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.pos.POSNode;

/**
 * Abstract morphological analyzer.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
abstract public class AbstractMPAnalyzer
{
	/**
	 * Returns the lemma of the specific word-form given its POS tag.
	 * @param form the word-form.
	 * @param pos the POS tag of the word.
	 */
	abstract public String getLemma(String form, String pos);
	
	/**
	 * Returns a normalized form of the specific word-form.
	 * @see MPLib#normalizeDigits(String)
	 * @see MPLib#normalizePunctuation(String)
	 * @param form the word-form.
	 * @param toLower if {@code true}, returns a lowercased form.
	 * @return a normalized form of the specific word-form.
	 */
	public String getNormalizedForm(String form, boolean toLower)
	{
		form = MPLib.normalizeDigits(form);
		form = MPLib.normalizePunctuation(form);
		if (toLower)	form = form.toLowerCase();
		
		return form;
	}
	
	/**
	 * Adds lemmas of all word-forms given their POS tags.
	 * @param nodes the array of POS nodes.
	 */
	public void lemmatize(POSNode[] nodes)
	{
		for (POSNode node : nodes)
			node.lemma = getLemma(node.form, node.pos);
	}
	
	/**
	 * Adds lemmas of all word-forms given their POS tags in the dependency tree.
	 * @param tree the dependency tree.
	 */
	public void lemmatize(DEPTree tree)
	{
		int i, size = tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			node.lemma = getLemma(node.form, node.pos);
		}
	}
}
