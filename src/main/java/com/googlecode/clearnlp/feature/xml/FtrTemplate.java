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
package com.googlecode.clearnlp.feature.xml;

/**
 * Feature template.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class FtrTemplate
{
	static private final String TYPE_SET	 = "s";
	static private final String TYPE_BOOLEAN = "b";
	
	public String     type;
	public FtrToken[] tokens;
	public boolean    visible;
	public String     note;
	
	public FtrTemplate(String type, int n, boolean visible, String note)
	{
		this.type    = type;
		this.visible = visible;
		this.note    = note;
		this.tokens  = new FtrToken[n];
	}
	
	public void setFtrToken(int index, FtrToken token)
	{
		tokens[index] = token;
	}
	
	public boolean isSetFeature()
	{
		return type.startsWith(TYPE_SET); 
	}
	
	public boolean isBooleanFeature()
	{
		return type.startsWith(TYPE_BOOLEAN);
	}
	
	public String toString()
	{
		StringBuilder build = new StringBuilder();
		
		if (isSetFeature())
		{
			toStringAux(build, AbstractFtrXml.XML_FEATURE_T, TYPE_SET);
			build.append(" ");
		}
		else if (isBooleanFeature())
		{
			toStringAux(build, AbstractFtrXml.XML_FEATURE_T, TYPE_BOOLEAN);
			build.append(" ");
		}
		
		toStringAux(build, AbstractFtrXml.XML_FEATURE_N, Integer.toString(tokens.length));
		
		for (int i=0; i<tokens.length; i++)
		{
			build.append(" ");
			toStringAux(build, AbstractFtrXml.XML_FEATURE_F + i, tokens[i].toString());
		}
		
		if (!visible)
		{
			build.append(" ");
			toStringAux(build, AbstractFtrXml.XML_FEATURE_VISIBLE, "false");
		}
		
		if (!note.isEmpty())
		{
			build.append(" ");
			toStringAux(build, AbstractFtrXml.XML_FEATURE_NOTE, note);
		}
		
		return build.toString();
	}
	
	private void toStringAux(StringBuilder build, String field, String value)
	{
		build.append(field);
		build.append("=\"");
		build.append(value);
		build.append("\"");
	}
}
