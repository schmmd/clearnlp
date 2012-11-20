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
package com.googlecode.clearnlp.dependency;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.googlecode.clearnlp.dependency.DEPFeat;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.reader.DEPReader;


public class DEPNodeTest
{
	@Test
	public void testDEPNode()
	{
		// initialization
		DEPNode root = new DEPNode();

	//	assertEquals(DEPLib.NULL_ID , root.id);
		assertEquals(AbstractReader.DUMMY_TAG, root.form);
		assertEquals(AbstractReader.DUMMY_TAG, root.lemma);
		assertEquals(AbstractReader.DUMMY_TAG, root.pos);

		root.initRoot();
		
		assertEquals(DEPLib.ROOT_ID , root.id);
		assertEquals(DEPLib.ROOT_TAG, root.form);
		assertEquals(DEPLib.ROOT_TAG, root.lemma);
		assertEquals(DEPLib.ROOT_TAG, root.pos);
		assertEquals(null, root.getLabel());
		assertEquals(null, root.getHead());
		
		DEPNode sbj  = new DEPNode(1, "Jinho", "jinho", "NNP", new DEPFeat("fst=jinho|lst=choi"));
		DEPNode verb = new DEPNode(2, "is", "be", "VBZ", new DEPFeat(DEPReader.BLANK_COLUMN));
		DEPNode obj  = new DEPNode(3, "awesome", "awesome", "JJ", new DEPFeat("_"));
		
		assertEquals(1      , sbj.id);
		assertEquals("Jinho", sbj.form);
		assertEquals("jinho", sbj.lemma);
		assertEquals("NNP"  , sbj.pos);
		assertEquals("choi" , sbj.getFeat("lst"));
		assertEquals(null   , sbj.getFeat("mid"));
		
		// getters and setters
		assertEquals(null, verb.getHead());
		
		verb.setHead(root, "ROOT");
		sbj .setHead(verb, "SBJ");
		obj .setHead(verb, "OBJ");
		
		assertEquals(root  , verb.getHead());
		assertEquals("ROOT", verb.getLabel());
		
		obj.setHead(verb, "PRD");
		
		assertEquals(verb , obj.getHead());
		assertEquals("PRD", obj.getLabel());
		
		obj.setHead(root, "OBJ");
		
		assertEquals(root , obj.getHead());
		assertEquals("OBJ", obj.getLabel());
		
		// booleans
		assertEquals(false, root.hasHead());
		assertEquals(false, root.isDependentOf(verb));
		assertEquals(true , verb.isDependentOf(root));
		assertEquals(false, sbj .isDependentOf(root));
		assertEquals(true , sbj .isDescendentOf(root));
		assertEquals(true , sbj .isDescendentOf(verb));
	}
}
