package edu.colorado.clear.morphology;

import java.util.regex.Pattern;

import edu.colorado.clear.constituent.CTLibEn;

public class MPLibEn extends MPLib
{
	static private Pattern RE_BE		= Pattern.compile("be|been|being|am|is|was|are|were|'m|'s|'re");
	static private Pattern RE_BECOME	= Pattern.compile("become|becomes|became|becoming");
	static private Pattern RE_GET		= Pattern.compile("get|gets|got|gotten|getting");
	static private Pattern RE_HAVE		= Pattern.compile("have|has|had|having|'ve|'d");
	static private Pattern RE_DO		= Pattern.compile("do|does|did|done|doing");
	
	static public boolean isBe(String form)
	{
		return RE_BE.matcher(form.toLowerCase()).find();
	}
	
	static public boolean isGet(String form)
	{
		return RE_GET.matcher(form.toLowerCase()).find();
	}
	
	static public boolean isBecome(String form)
	{
		return RE_BECOME.matcher(form.toLowerCase()).find();
	}
	
	static public boolean isHave(String form)
	{
		return RE_HAVE.matcher(form.toLowerCase()).find();
	}
	
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
	
	static public String revertBracket(String form)
	{
		if      (form.equals("-LRB-"))	return "(";
		else if (form.equals("-RRB-"))	return ")";
		if      (form.equals("-LSB-"))	return "[";
		else if (form.equals("-RSB-"))	return "]";
		if      (form.equals("-LCB-"))	return "{";
		else if (form.equals("-RCB-"))	return "}";
		
		return form;
	}
}
