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
package edu.colorado.clear.constituent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntOpenHashSet;

import edu.colorado.clear.conversion.C2DInfo;
import edu.colorado.clear.propbank.PBLoc;
import edu.colorado.clear.util.UTRegex;
import edu.colorado.clear.util.pair.StringIntPair;

/**
 * Constituent node.
 * See <a target="_blank" href="http://code.google.com/p/clearnlp/source/browse/trunk/src/edu/colorado/clear/test/constituent/CTNodeTest.java">CTNodeTest</a> for the use of this class.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class CTNode implements Comparable<CTNode>
{
	/** The phrase or pos tag of this node. */
	public String      pTag;
	/** The function tags of this node (default: empty). */
	protected Set<String> s_fTags;
	/** The co-index of this node (default: {@code -1}). */
	public int coIndex  = -1;
	/** The gap-index of this node (default: {@code -1}). */
	public int gapIndex = -1;
	/** The word-form of this node (default: {@code null}). */
	public String form  = null;
	
	/** The parent of this node (default: {@code null}). */
	protected CTNode parent     = null;
	/** The antecedent of this node (default: {@code null}). */
	protected CTNode antecedent = null;
	/** The children of this node. */
	protected List<CTNode> ls_children;
	/** The ID (starting at 0) of this node among other terminal nodes within the tree (default: {@code -1}). */
	protected int i_terminalId = -1;
	/** The ID (starting at 0) of this node among other terminal nodes (disregarding empty categories) within the tree (default: {@code -1}). */
	protected int i_tokenId    = -1;
	/** The ID (starting at 0) of this node among its siblings (default: {@code -1}). */
	protected int i_siblingId  = -1;
	/** The PropBank location of this node (default: {@code null}). */
	protected PBLoc  pb_loc    = null;
	
	/** The information of constituent-to-dependency conversion. */
	public C2DInfo c2d = null;
	public List<StringIntPair> pbArgs = null;
	
	/**
	 * Constructs a constituent node.
	 * @param tags {@link CTNode#pTag}{@code (-}{@link CTNode#s_fTags}{@code )*(-}{@link CTNode#coIndex}{@code ){0,1}(=}{@link CTNode#gapIndex}{@code ){0,1}}.
	 */
	public CTNode(String tags)
	{
		setTags(tags);
		ls_children = new ArrayList<CTNode>();
	}
	
	/**
	 * Constructs a constituent node with the specific word-form.
	 * @param tags see the {@code tags} parameter in {@link CTNode#CTNode(String, CTNode)}.
	 * @param form the word-form of this node.
	 */
	public CTNode(String tags, String form)
	{
		this(tags);
		this.form = form;
	}
	
//	======================== Getters ========================

	/**
	 * Returns all tags of this node in the Penn Treebank format.
	 * See the {@code tags} parameter in {@link CTNode#CTNode(String, CTNode)}.
	 * @return all tags of this node in the Penn Treebank format.
	 */
	public String getTags()
	{
		StringBuilder build = new StringBuilder();
		
		build.append(pTag);
		
		for (String fTag : s_fTags)
		{
			build.append("-");
			build.append(fTag);
		}
		
		if (coIndex != -1)
		{
			build.append("-");
			build.append(coIndex);
		}
		
		if (gapIndex != -1)
		{
			build.append("=");
			build.append(gapIndex);
		}
		
		return build.toString();
	}
	
	/**
	 * Returns the set of function tags of this node.
	 * @return the set of function tags of this node.
	 */
	public Set<String> getFTags()
	{
		return s_fTags;
	}
	
	/**
	 * Returns the ID (starting at 0) of this node among other terminal nodes within the tree (default: {@code -1}).
	 * @return the ID (starting at 0) of this node among other terminal nodes within the tree (default: {@code -1}).
	 */
	public int getTerminalId()
	{
		return i_terminalId;
	}
	
	/**
	 * Returns the ID (starting at 0) of this node among other terminal nodes (disregarding empty categories) within the tree (default: {@code -1}).
	 * @return the ID (starting at 0) of this node among other terminal nodes (disregarding empty categories) within the tree (default: {@code -1}).
	 */
	public int getTokenId()
	{
		return i_tokenId;
	}
	
	/**
	 * Returns the ID (starting at 0) of this node among its siblings (default: {@code -1}).
	 * @return the ID (starting at 0) of this node among its siblings (default: {@code -1}).
	 */
	public int getSiblingId()
	{
		return i_siblingId;
	}
	
	/**
	 * Returns the PropBank location of this node (default: {@code null}).
	 * @return the PropBank location of this node (default: {@code null}).
	 */
	public PBLoc getPBLoc()
	{
		return pb_loc;
	}
	
	/**
	 * Returns the parent of this node.
	 * @return the parent of this node.
	 */
	public CTNode getParent()
	{
		return parent;
	}
	
	/**
	 * Returns the antecedent of this node (default: {@code null}).
	 * @return the antecedent of this node (default: {@code null}).
	 */
	public CTNode getAntecedent()
	{
		return antecedent;
	}
	
	/**
	 * Returns a list of all children of this node.
	 * @return a list of all children of this node.
	 */
	public List<CTNode> getChildren()
	{
		return ls_children;
	}
	
	/**
	 * Returns an immutable list of sub-children of this node.
	 * The sublist begins at the specific position and extends to the end.
	 * @param fstId the ID of the first child (inclusive).
	 * @return an immutable list of sub-children of this node.
	 * @throws IndexOutOfBoundsException for an illegal ID.
	 */
	public List<CTNode> getChildren(int fstId)
	{
		return ls_children.subList(fstId, ls_children.size());
	}
	
	/**
	 * Returns an immutable list of sub-children of this node.
	 * The sublist begins and ends at the specific positions.
	 * @param fstId the ID of the first child (inclusive).
	 * @param lstId the ID of the last child (exclusive)
	 * @return an immutable list of sub-children of this node.
	 * @throws IndexOutOfBoundsException for an illegal ID.
	 */
	public List<CTNode> getChildren(int fstId, int lstId)
	{
		return ls_children.subList(fstId, lstId);
	}
	
	/**
	 * Returns the child of this node at the specific position.
	 * If such a child does not exists, returns {@code null}.
	 * @param childId the ID (starting at 0) of the child to be returned.
	 * @return the child of this node at the specific position.
	 */
	public CTNode getChild(int childId)
	{
		return (0 <= childId && childId < ls_children.size()) ? ls_children.get(childId) : null;
	}
	
	/**
	 * Returns the first child with the specific tags, or {@code null} if there is no such node.
	 * @param tags see the {@code tags} parameter in {@link CTNode#isTag(String...)}. 
	 * @return the first child with the specific tags, or {@code null} if there is no such node.
	 */
	public CTNode getFirstChild(String... tags)
	{
		for (CTNode child : ls_children)
		{
			if (child.isTag(tags))
				return child;
		}
		
		return null;
	}
	
	/**
	 * Returns the last child with the specific tags, or {@code null} if there is no such node.
	 * @param tags see the {@code tags} parameter in {@link CTNode#isTag(String...)}. 
	 * @return the last child with the specific tags, or {@code null} if there is no such node.
	 */
	public CTNode getLastChild(String... tags)
	{
		CTNode child;	int i;
		
		for (i=ls_children.size()-1; i>=0; i--)
		{
			child = ls_children.get(i);
			
			if (child.isTag(tags))
				return child;
		}
		
		return null;
	}
	
	/**
	 * Returns the first descendant with the specific tags, or {@code null} if there is no such node.
	 * @param tags see the {@code tags} parameter in {@link CTNode#isTag(String...)}.
	 * @return the first descendant with the specific tags, or {@code null} if there is no such node.
	 */
	public CTNode getFirstDescendant(String... tags)
	{
		return getFirstDescendantAux(ls_children, tags);
	}
	
	/** Called by {@link CTNode#getFirstDescendant(String...)}. */
	private CTNode getFirstDescendantAux(List<CTNode> nodes, String... tags)
	{
		CTNode desc;
		
		for (CTNode node : nodes)
		{
			if (node.isTag(tags))	return node;
			
			desc = getFirstDescendantAux(node.ls_children, tags);
			if (desc != null)	return desc;
		}
		
		return null;
	}
	
	/**
	 * Returns the nearest sibling with the specific tags prior to this node , or {@code null} if there is no such node.
	 * @param tags see the {@code tags} parameter in {@link CTNode#isTag(String...)}.
	 * @return the nearest sibling with the specific tags prior to this node , or {@code null} if there is no such node.
	 */
	public CTNode getPrevSibling(String... tags)
	{
		if (parent == null) return null;
		List<CTNode> siblings = parent.ls_children;
		CTNode node; int i;
		
		for (i=i_siblingId-1; i>=0; i--)
		{
			node = siblings.get(i);
			
			if (node.isTag(tags))
				return node;
		}
		
		return null;
	}
	
	/**
	 * Returns the list of all siblings prior to this node.
	 * @return the list of all siblings prior to this node.
	 */
	public List<CTNode> getPrevSiblings()
	{
		return (parent != null) ? parent.getChildren(0, this.i_siblingId) : new ArrayList<CTNode>(0);
	}
	
	/**
	 * Returns the nearest sibling with the specific tags following this node, or {@code null} if there is no such node.
	 * @param tags see the {@code tags} parameter in {@link CTNode#isTag(String...)}.
	 * @return the nearest sibling with the specific tags following this node, or {@code null} if there is no such node.
	 */
	public CTNode getNextSibling(String... tags)
	{
		if (parent == null) return null;
		List<CTNode> siblings = parent.ls_children;
		CTNode node; int i, size = siblings.size();
		
		for (i=i_siblingId+1; i<size; i++)
		{
			node = siblings.get(i);
			
			if (node.isTag(tags))
				return node;
		}
		
		return null;
	}
	
	/**
	 * Returns the nearest ancestor with the specific tags, or {@code null} if there is no such node.
	 * @param tags the {@code tags} parameter in {@link CTNode#isTag(String...)}.
	 * @return the nearest ancestor with the specific tags, or {@code null} if there is no such node.
	 */
	public CTNode getNearestAncestor(String... tags)
	{
		CTNode curr = this;
		
		while (curr.parent != null)
		{
			curr = curr.parent;
			if (curr.isTag(tags)) return curr;
		}
		
		return null;
	}
	
	public CTNode getFirstLowestDescendant(String... tags)
	{
		CTNode desc = this;
		
		while (desc.containsTags(tags))
			desc = desc.getFirstChild(tags);
		
		return (desc != this) ? desc : null;
	}
	
	/**
	 * Returns the top ancestor with the specific tags, or {@code null} if there is no such node.
	 * All ancestors in between must have the same tags.
	 * @param tags the {@code tags} parameter in {@link CTNode#isTag(String...)}.
	 * @return Returns the top ancestor with the specific tags, or {@code null} if there is no such node.
	 */
	public CTNode getTopChainedAncestor(String... tags)
	{
		CTNode curr = this;
		
		while (curr.parent != null && curr.parent.isTag(tags))
			curr = curr.parent;
	
		return (curr == this) ? null : curr;
	}
	
	/**
	 * Returns the list of empty categories matching the specific regular expression under this node.
	 * @param regex the regular expression to be matched.
	 * @return the list of empty categories matching the specific regular expression under this node.
	 */
	public List<CTNode> getIncludedEmptyCategory(String regex)
	{
		List<CTNode> list = new ArrayList<CTNode>();
		
		getIncludedEmptyCategoriesAux(this, list, regex);
		return list;
	}
	
	/** Called by {@link CTNode#getIncludedEmptyCategory(String)}. */
	private void getIncludedEmptyCategoriesAux(CTNode curr, List<CTNode> list, String regex)
	{
		if (curr.isEmptyCategory() && curr.form.matches(regex))
			list.add(curr);
		
		for (CTNode child : curr.ls_children)
			getIncludedEmptyCategoriesAux(child, list, regex);
	}
	
	/**
	 * Returns a list of all sub-terminal nodes under this node.
	 * @return a list of all sub-terminal nodes under this node.
	 */
	public List<CTNode> getSubTerminals()
	{
		List<CTNode> terminals = new ArrayList<CTNode>();
		
		getSubTerminals(this, terminals);
		return terminals;
	}
	
	/** Called by {@link CTNode#getSubTerminals()}. */
	private void getSubTerminals(CTNode curr, List<CTNode> terminals)
	{
		if (curr.isPhrase())
		{
			for (CTNode child : curr.ls_children)
				getSubTerminals(child, terminals);
		}
		else
			terminals.add(curr);
	}
	
	/**
	 * Returns a list of sub-terminal IDs of this node.
	 * @return a list of sub-terminal IDs of this node.
	 */
	public IntArrayList getSubTerminalIdList()
	{
		IntArrayList list = new IntArrayList();
		
		for (CTNode node : getSubTerminals())
			list.add(node.getTerminalId());
		
		return list;
	}
	
	/**
	 * Returns a set of sub-terminal IDs of this node.
	 * @return a set of sub-terminal IDs of this node.
	 */
	public IntOpenHashSet getSubTerminalIdSet()
	{
		IntOpenHashSet set = new IntOpenHashSet();
		
		for (CTNode node : getSubTerminals())
			set.add(node.getTerminalId());
		
		return set;
	}
	
	/**
	 * Returns the first terminal node under this node.
	 * @return the first terminal node under this node.
	 */
	public CTNode getFirstTerminal()
	{
		return ls_children.isEmpty() ? this : getSubTerminals().get(0);
	//	return ls_children.isEmpty() ? null : getSubTerminals().get(0);
	}
	
	/**
	 * Returns the number of children of this node.
	 * @return the number of children of this node.
	 */
	public int getChildrenSize()
	{
		return ls_children.size();
	}
	
	/**
	 * Returns the lowest common ancestor between this node and the specific node.
	 * @param node the node to get the LCA of.
	 * @return the lowest common ancestor between this node and the specific node.
	 */
	public CTNode getLowestCommonAncestor(CTNode node)
	{
		if (this.isDescendentOf(node))	return node;
		if (node.isDescendentOf(this))	return this;
		
		CTNode parent = getParent();
		
		while (parent != null)
		{
			if (node.isDescendentOf(parent))
				return parent;
			
			parent = parent.getParent();
		}
		
		return null;
	}
	
//	======================== Setters ========================
	
	/**
	 * Assigns {@link CTNode#pTag}, {@link CTNode#s_fTags}, {@link CTNode#coIndex}, and {@link CTNode#gapIndex}.
	 * @param tags see the {@code tags} parameter in {@link CTNode#CTNode(String, CTNode)}.
	 */
	public void setTags(String tags)
	{
		s_fTags = new TreeSet<String>();
		
		if (tags.charAt(0) == '-')
		{
			pTag = tags;
			return;
		}
		
		StringTokenizer tok = new StringTokenizer(tags, "-=", true);
		String delim, tag;
		
		pTag = tok.nextToken();
		
		while (tok.hasMoreTokens())
		{
			delim = tok.nextToken();
			if (!tok.hasMoreTokens())
			{
				System.err.println("Error: illegal tag \""+tags+"\"");
				break;
			}
			tag = tok.nextToken();
			
			if (delim.equals("-"))
			{
				if (UTRegex.isDigit(tag))
				{
					if (coIndex == -1)
						coIndex = Integer.parseInt(tag);
					else
						gapIndex = Integer.parseInt(tag);
				}
				else
					s_fTags.add(tag);
			}
			else // if (delim.equals("="))
				gapIndex = Integer.parseInt(tag);
		}
	}
	
	/**
	 * Adds the specific function tag to the node.
	 * @param fTag the function tag to be added.
	 */
	public void addFTag(String fTag)
	{
		s_fTags.add(fTag);
	}
	
	public void addFTags(Collection<String> fTags)
	{
		s_fTags.addAll(fTags);
	}
	
	/**
	 * Removes the specific function tag from this node if exists. 
	 * @param fTag the function tag to be removed.
	 */
	public void removeFTag(String fTag)
	{
		s_fTags.remove(fTag);
	}
	
	public void removeFTagAll()
	{
		s_fTags.clear();
	}
	
	/**
	 * Sets the antecedent of this node.
	 * @param ante the antecedent to be set.
	 */
	public void setAntecedent(CTNode ante)
	{
		antecedent = ante;
	}
	
	/**
	 * Adds the specific node as the last child of this node.
	 * @param child the node to be added.
	 */
	public void addChild(CTNode child)
	{
		child.parent = this;
		child.i_siblingId = ls_children.size();
		
		ls_children.add(child);
	}
	
	/**
	 * Adds a child to the specific location.
	 * Returns {@code false} if the specific location is out of range.  
	 * @param index the index of the specific location
	 * @param child the child to be added.
	 * @return {@code false} if the specific location is out of range.
	 */
	public void addChild(int index, CTNode child)
	{
		ls_children.add(index, child);
		
		child.parent = this;
		resetSiblingIDs(index);
	}
	
	/**
	 * Sets the specific node as the index'th child of this node.
	 * @param index the index of the child to be set.
	 * @param child the node to be set.
	 */
	public void setChild(int index, CTNode child)
	{
		ls_children.set(index, child).parent = null;
		
		child.parent = this;
		child.i_siblingId = index;
	}
	
	/**
	 * Removes the index'th child of this node.
	 * @throws IndexOutOfBoundsException
	 * @param index the index of the child to be removed.
	 */
	public void removeChild(int index)
	{
		if (!isChildrenRange(index))
			throw new IndexOutOfBoundsException(Integer.toString(index));
		
		ls_children.remove(index).parent = null;
		resetSiblingIDs(index);
	}
	
	/**
	 * Removes the specific child from this node.
	 * @param child the child to be removed.
	 */
	public void removeChild(CTNode child)
	{
		removeChild(ls_children.indexOf(child));
	}
	
	public void resetChildren(Collection<CTNode> children)
	{
		ls_children.clear();
		
		for (CTNode child : children)
			addChild(child);
	}
	
	private boolean isChildrenRange(int index)
	{
		return 0 <= index && index < ls_children.size();
	}
	
	private void resetSiblingIDs(int index)
	{
		int i, size = ls_children.size();
		
		for (i=index; i<size; i++)
			ls_children.get(i).i_siblingId = i;
	}
	
//	======================== Booleans ========================
	
	/**
	 * Returns {@code true} if this node's pTag equals to the specific tag.
	 * @see CTNode#pTag
	 * @param pTag the phrase or pos tag to be compared.
	 * @return {@code true} if this node's pTag equals to the specific tag.
	 */
	public boolean isPTag(String pTag)
	{
		return this.pTag.equals(pTag);
	}
	
	/**
	 * Returns {@code true} if this node's pTag equals to any of the specific tags.
	 * @see CTNode#pTag 
	 * @param pTags the phrase or pos tag to be compared.
	 * @return {@code true} if this node's pTag equals to any of the specific tags.
	 */
	public boolean isPTagAny(String... pTags)
	{
		for (String pTag : pTags)
		{
			if (isPTag(pTag))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Returns {@code true} if this node's pTag matches to the specific regular expression.
	 * Note that {@code regex} gets internally wrapped as {@code "^"+regex+"$"}.
	 * @see CTNode#pTag
	 * @param regex the regular expression to be compared.
	 * @return {@code true} if this node's pTag matches to the specific regular expression.
	 */
	public boolean matchesPTag(String regex)
	{
		return pTag.matches("^"+regex+"$");
	}
	
	/**
	 * Returns {@code true} if this node contains only the specific function tag.
	 * Returns {@code false} if this node contains no function tag.
	 * @param fTag the function tag to be compared.
	 * @return {@code true} if this node contains only the specific function tag.
	 */
	public boolean isFTag(String fTag)
	{
		return (s_fTags.size() == 1) && (s_fTags.contains(fTag)); 
	}
	
	/**
	 * Returns {@code true} if the specific tag is one of this node's function tags.
	 * @see CTNode#s_fTags
	 * @param fTag the function tag to be compared.
	 * @return {@code true} if the specific tag is one of this node's function tags.
	 */
	public boolean hasFTag(String fTag)
	{
		return s_fTags.contains(fTag);
	}
	
	/**
	 * Returns {@code true} if the collection of specific tags is a subset of this node's function tags.
	 * @see CTNode#s_fTags
	 * @param fTags the collection of function tags to be compared.
	 * @return {@code true} if the set of specific tags is a subset of this node's function tags.
	 */
	public boolean hasFTagAll(Collection<String> fTags)
	{
		return this.s_fTags.containsAll(fTags);
	}
	
	/**
	 * Returns {@code true} if any of specific tags in the collection is a function tag of this node. 
	 * @param fTags the collection of function tags to be compared.
	 * @return {@code true} if any of specific tags in the collection is a function tag of this node.
	 */
	public boolean hasFTagAny(Collection<String> fTags)
	{
		for (String fTag : fTags)
		{
			if (this.s_fTags.contains(fTag))
				return true;
		}
		
		return false;
	}
	
	public boolean hasFTagAny(String... fTags)
	{
		for (String fTag : fTags)
		{
			if (this.s_fTags.contains(fTag))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Returns {@code true} if this node satisfies all of the specific tags.
	 * @param tags {@code pTag} | {@code '+'+pRegex} | {@code '-'+fTag}.<br><br>
	 * {@code pTag}: a phrase or pos tag (unique)<br>
	 * {@code pRegex}: a regular expression of {@code pTag} (unique)<br>
	 * {@code fTag}: function tag (multiple)<br><br>
	 * e.g., {@code isTag("NP","-PRD")}, {@code isTag("+N.*","-PRD","-LOC")}.
	 * @see CTNode#isPTag(String)
	 * @see CTNode#matchesPTag(String)
	 * @see CTNode#hasFTagAll(Collection)
	 * @return {@code true} if this node satisfies all of the specific tags.
	 */
	public boolean isTag(String... tags)
	{
		String      pTag  = null;
		String      pRex  = null;
		Set<String> fTags = new HashSet<String>();
		
		for (String tag : tags)
		{
			if (tag.equals(CTLib.POS_NONE) || tag.equals(CTLibEn.POS_LRB) || tag.equals(CTLibEn.POS_RRB))
				pTag = tag;
			else
			{
				switch (tag.charAt(0))
				{
				case '-': fTags.add(tag.substring(1));	break;
				case '+': pRex = tag.substring(1);		break;
				default : pTag = tag;
				}				
			}
		}
		
		return (pTag == null || isPTag (pTag)) &&
		       (pRex == null || matchesPTag(pRex)) &&
		       hasFTagAll(fTags);
	}
	
	/**
	 * Returns {@code true} if the word-form of this node equals to the specific form.
	 * @param form the form to be compared.
	 * @return {@code true} if the word-form of this node equals to the specific form.
	 */
	public boolean isForm(String form)
	{
		return this.form != null && this.form.equals(form);
	}
	
	/**
	 * Returns {@code true} if this node is a phrase.
	 * @return {@code true} if this node is a phrase.
	 */
	public boolean isPhrase()
	{
		return !ls_children.isEmpty();
	}
	
	/**
	 * Returns {@code true} if this node is an empty category.
	 * All empty categories are assumed to have the pos tag of {@link CTLib#POS_NONE}.
	 * @return {@code true} if this node is an empty category.
	 */
	public boolean isEmptyCategory()
	{
		return pTag.equals(CTLib.POS_NONE);
	}
	
	/**
	 * Returns {@code true} if this node contains only an empty category recursively.
	 * Each sub-phrase must have one and only one child such as {@code (NP (NP (-NONE- *)))}.
	 * @return {@code true} if this node contains only an empty category recursively.
	 */
	public boolean isEmptyCategoryRec()
	{
		CTNode curr = this;
		
		while (curr.isPhrase())
		{
			if (curr.ls_children.size() > 1)
				return false;
			else
				curr = curr.getChild(0);
		}
		
		return curr.isEmptyCategory();
	}
	
	/**
	 * Returns {@code true} if this node is a descendent of the specific node.
	 * @param node the node to be compared.
	 * @return {@code true} if this node is a descendent of the specific node.
	 */
	public boolean isDescendentOf(CTNode node)
	{
		CTNode parent = getParent();

		while (parent != null)
		{
			if (parent == node)
				return true;
			
			parent = parent.getParent();
		}
		
		return false;
	}
	
	/**
	 * Returns {@code true} if this node contains a child with the specific tags.
	 * @param tags the {@code tags} parameter in {@link CTNode#isTag(String...)}.
	 * @return {@code true} if this node contains a child with the specific tags.
	 */
	public boolean containsTags(String... tags)
	{
		for (CTNode child : ls_children)
		{
			if (child.isTag(tags))
				return true;
		}
		
		return false;
	}
	
//	======================== Strings ========================

	/**
	 * Returns ordered word-forms of this node's subtree.
	 * @param includeNulls if {@code true}, include forms of empty categories.
	 * @param delim the delimiter between forms.
	 * @return ordered word-forms of this node's subtree.
	 */
	public String toForms(boolean includeNulls, String delim)
	{
		StringBuilder build = new StringBuilder();
		
		for (CTNode node : getSubTerminals())
		{
			if (includeNulls || !node.isEmptyCategory())
			{
				build.append(delim);
				build.append(node.form);
			}
		}
		
		return build.length() == 0 ? "" : build.substring(delim.length());
	}
	
	/**
	 * Returns {@link CTNode#toString(boolean...)}, where {@code args = {false, false}}.
	 * @return {@link CTNode#toString(boolean...)}, where {@code args = {false, false}}.
	 */
	public String toString()
	{
		return toString(false, false);
	}
	
	/**
	 * Returns the Penn Treebank style constituent node.
	 * @param args up to two arguments.<br><br>
	 * if {@code args[0] == true}, include line numbers.<br>
	 * if {@code args[1] == true}, include antecedent pointers.<br>
	 * @return the Penn Treebank style constituent node.
	 */
	public String toString(boolean... args)
	{
		boolean includeLineNumbers  = (args.length > 0) ? args[0] : false;
		boolean includeAntePointers = (args.length > 1) ? args[1] : false;
		
		ArrayList<String> lTree = new ArrayList<String>();
		toStringAux(this, lTree, "", includeAntePointers);
		
		StringBuilder build = new StringBuilder();
		
		for (int i=0; i<lTree.size(); i++)
		{
			if (includeLineNumbers)
				build.append(String.format("%3d: %s\n", i, lTree.get(i)));
			else
				build.append(lTree.get(i)+"\n");
		}
			
		build.deleteCharAt(build.length()-1);	// remove the last '\n'
		return build.toString();
	}
	
	/**
	 * Returns the string representation of this node in one line.
	 * @return the string representation of this node in one line.
	 */
	public String toStringLine()
	{
		ArrayList<String> lTree = new ArrayList<String>();
		toStringAux(this, lTree, "", false);
		
		StringBuilder build = new StringBuilder();
		
		for (int i=0; i<lTree.size(); i++)
			build.append(lTree.get(i).trim()+" ");
			
		build.deleteCharAt(build.length()-1);	// remove the last ' '
		return build.toString();
	}
	
	/** Called by {@link CTNode#toString(boolean)}. */
	private void toStringAux(CTNode curr, ArrayList<String> lTree, String sTags, boolean includeAntePointers)
	{
		if (curr.isPhrase())
		{
			sTags += "("+curr.getTags()+" ";
		//	sTags += "("+curr.getTags()+"-"+curr.pbLoc+" ";
			
			for (CTNode child : curr.ls_children)
			{
				toStringAux(child, lTree, sTags, includeAntePointers);
				
				if (child.i_siblingId == 0)
					sTags = sTags.replaceAll(".", " ");		// indent
			}

			int last = lTree.size() - 1;
			lTree.set(last, lTree.get(last)+")");
		}
		else
		{
			String tag = sTags+"("+curr.getTags()+" "+curr.form+")";
			if (includeAntePointers && curr.antecedent != null)
				tag += "["+curr.antecedent.getTags()+"]"; 
			lTree.add(tag);
		}
	}
	
	@Override
	public int compareTo(CTNode node)
	{
		return i_terminalId - node.i_terminalId;
	}
}
