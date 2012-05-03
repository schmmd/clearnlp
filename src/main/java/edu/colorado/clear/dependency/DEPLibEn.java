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
package edu.colorado.clear.dependency;

/**
 * Dependency library for English.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class DEPLibEn extends DEPLib
{
	static public final String DEP_PASS	= "pass";
	static public final String DEP_SUBJ	= "subj";

	/** The Stanford dependency label for adjectival complements. */
	static public final String DEP_ACOMP		= "acomp";
	/** The Stanford dependency label for adverbial clause modifiers. */
	static public final String DEP_ADVCL		= "advcl";
	/** The Stanford dependency label for adverbial modifiers. */
	static public final String DEP_ADVMOD		= "advmod";
	/** The Stanford dependency label for agents. */
	static public final String DEP_AGENT		= "agent";
	/** The Stanford dependency label for adjectival modifiers. */
	static public final String DEP_AMOD			= "amod";
	/** The Stanford dependency label for appositional modifiers. */
	static public final String DEP_APPOS		= "appos";
	/** The Stanford dependency label for attributes. */
	static public final String DEP_ATTR			= "attr";
	/** The Stanford dependency label for auxiliary verbs. */
	static public final String DEP_AUX			= "aux";
	/** The Stanford dependency label for passive auxiliary verbs. */
	static public final String DEP_AUXPASS		= DEP_AUX+DEP_PASS;
	/** The Stanford dependency label for coordinating conjunctions. */
	static public final String DEP_CC			= "cc";
	/** The Stanford dependency label for clausal complements. */
	static public final String DEP_CCOMP		= "ccomp";
	/** The Stanford dependency label for complementizers. */
	static public final String DEP_COMPLM		= "complm";
	/** The Stanford dependency label for conjuncts. */
	static public final String DEP_CONJ			= "conj";
	/** The Stanford dependency label for clausal subjects. */
	static public final String DEP_CSUBJ		= "c"+DEP_SUBJ;
	/** The Stanford dependency label for clausal passive subjects. */
	static public final String DEP_CSUBJPASS	= DEP_CSUBJ+DEP_PASS;
	/** The Stanford dependency label for unknown dependencies. */
	static public final String DEP_DEP  		= "dep";
	/** The Stanford dependency label for determiners. */
	static public final String DEP_DET			= "det";
	/** The Stanford dependency label for direct objects. */
	static public final String DEP_DOBJ 		= "dobj";
	/** The Stanford dependency label for expletives. */
	static public final String DEP_EXPL 		= "expl";
	/** The Stanford dependency label for indirect objects. */
	static public final String DEP_IOBJ 		= "iobj";
	/** The Stanford dependency label for interjections. */
	static public final String DEP_INTJ			= "intj";
	/** The Stanford dependency label for markers. */
	static public final String DEP_MARK			= "mark";
	/** The Stanford dependency label for meta modifiers. */
	static public final String DEP_META			= "meta";
	/** The Stanford dependency label for negation modifiers. */
	static public final String DEP_NEG			= "neg";
	/** The Stanford dependency label for infinitival modifiers. */
	static public final String DEP_INFMOD		= "infmod";
	/** The Stanford dependency label for noun phrase modifiers. */
	static public final String DEP_NMOD 		= "nmod";
	/** The Stanford dependency label for noun compound modifiers. */
	static public final String DEP_NN			= "nn";
	/** The Stanford dependency label for noun phrase as adverbial modifiers. */
	static public final String DEP_NPADVMOD		= "npadvmod";
	/** The Stanford dependency label for nominal subjects. */
	static public final String DEP_NSUBJ		= "n"+DEP_SUBJ;
	/** The Stanford dependency label for nominal passive subjects. */
	static public final String DEP_NSUBJPASS	= DEP_NSUBJ+DEP_PASS;
	/** The Stanford dependency label for numeric modifiers. */
	static public final String DEP_NUM			= "num";
	/** The Stanford dependency label for elements of compound numbers. */
	static public final String DEP_NUMBER		= "number";
	/** The Stanford dependency label for object predicates. */
	static public final String DEP_OPRD			= "oprd";
	/** The Stanford dependency label for parataxis. */
	static public final String DEP_PARATAXIS 	= "parataxis";
	/** The Stanford dependency label for participial modifiers. */
	static public final String DEP_PARTMOD		= "partmod";
	/** The Stanford dependency label for prepositional complements. */
	static public final String DEP_PCOMP 		= "pcomp";
	/** The Stanford dependency label for objects of prepositions. */
	static public final String DEP_POBJ 		= "pobj";
	/** The Stanford dependency label for possession modifiers. */
	static public final String DEP_POSS			= "poss";
	/** The Stanford dependency label for possessive modifiers. */
	static public final String DEP_POSSESSIVE 	= "possessive";
	/** The Stanford dependency label for pre-conjuncts. */
	static public final String DEP_PRECONJ		= "preconj";
	/** The Stanford dependency label for pre-determiners. */
	static public final String DEP_PREDET		= "predet";
	/** The Stanford dependency label for prepositional modifiers. */
	static public final String DEP_PREP			= "prep";
	/** The Stanford dependency label for particles. */
	static public final String DEP_PRT 			= "prt";
	/** The Stanford dependency label for punctuation. */
	static public final String DEP_PUNCT		= "punct";
	/** The Stanford dependency label for quantifier phrase modifiers. */
	static public final String DEP_QUANTMOD		= "quantmod";
	/** The Stanford dependency label for relative clause modifiers. */
	static public final String DEP_RCMOD		= "rcmod";
	/** The Stanford dependency label for roots. */
	static public final String DEP_ROOT 		= "root";
	/** The Stanford dependency label for open clausal modifiers. */
	static public final String DEP_XCOMP		= "xcomp";
	/** The Stanford dependency label for open clausal subjects. */
	static public final String DEP_XSUBJ		= "x"+DEP_SUBJ;
	/** The secondary dependency label for gapping relations. */
	static public final String DEP_GAP			= "gap";
	/** The secondary dependency label for referents. */
	static public final String DEP_REF			= "ref";
	/** The secondary dependency label for right node raising. */
	static public final String DEP_RNR			= "rnr";
	
	static public final String CONLL_ADV	= "ADV";
	static public final String CONLL_AMOD	= "AMOD";
	static public final String CONLL_APPO	= "APPO";
	static public final String CONLL_COORD	= "COORD";
	static public final String CONLL_CONJ	= "CONJ";
	static public final String CONLL_DEP	= "DEP";
	static public final String CONLL_DTV	= "DTV";
	static public final String CONLL_EXTR	= "EXTR";
	static public final String CONLL_IM		= "IM";
	static public final String CONLL_INTJ	= "INTJ";
	static public final String CONLL_LGS	= "LGS";
	static public final String CONLL_LOC	= "LOC";
	static public final String CONLL_META	= "META";
	static public final String CONLL_NMOD	= "NMOD";
	static public final String CONLL_OBJ	= "OBJ";
	static public final String CONLL_OPRD	= "OPRD";
	static public final String CONLL_P		= "P";
	static public final String CONLL_PMOD	= "PMOD";
	static public final String CONLL_PRD	= "PRD";
	static public final String CONLL_PRN	= "PRN";
	static public final String CONLL_PRT	= "PRT";
	static public final String CONLL_PUT	= "PUT";
	static public final String CONLL_QMOD	= "QMOD";
	static public final String CONLL_ROOT	= "ROOT";
	static public final String CONLL_SBJ	= "SBJ";
	static public final String CONLL_SUB	= "SUB";
	static public final String CONLL_VC		= "VC";
	static public final String CONLL_XCOMP	= "XCOMP";
}
