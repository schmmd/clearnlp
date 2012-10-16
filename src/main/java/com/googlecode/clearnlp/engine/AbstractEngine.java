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
package com.googlecode.clearnlp.engine;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.feature.xml.AbstractFtrXml;
import com.googlecode.clearnlp.feature.xml.FtrTemplate;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.reader.AbstractColumnReader;

abstract public class AbstractEngine
{
	static public final byte FLAG_LEXICA	= 0;
	static public final byte FLAG_TRAIN		= 1;
	static public final byte FLAG_PREDICT	= 2;
	static public final byte FLAG_BOOST		= 3;
	static public final byte FLAG_DEMO		= 4;
	
	protected byte i_flag;
	
	/**
	 * Initializes the flag of this engine.
	 * @param flag {@code FLAG_*}.
	 */
	public AbstractEngine(byte flag)
	{
		i_flag = flag;
	}
	
	public Set<String> getStringSet(BufferedReader fin) throws Exception
	{
		Set<String> set = new HashSet<String>();
		int i, size = Integer.parseInt(fin.readLine());
		
		for (i=0; i<size; i++)
			set.add(fin.readLine());
		
		return set;
	}
	
	protected void printSet(PrintStream fout, Set<String> set)
	{
		fout.println(set.size());
		for (String key : set)	fout.println(key);
	}
	
	protected Map<String,String> getStringMap(BufferedReader fin, String delim) throws Exception
	{
		Map<String,String> map = new HashMap<String, String>();
		int i, size = Integer.parseInt(fin.readLine());
		String[] tmp;
		
		for (i=0; i<size; i++)
		{
			tmp = fin.readLine().split(delim);
			map.put(tmp[0], tmp[1]);
		}
		
		return map;
	}
	
	protected void printMap(PrintStream fout, Map<String,String> map, String delim)
	{
		StringBuilder build;
		fout.println(map.size());
		
		for (String key : map.keySet())
		{
			build = new StringBuilder();
			
			build.append(key);
			build.append(delim);
			build.append(map.get(key));

			fout.println(build.toString());
		}
	}
	
	protected StringFeatureVector getFeatureVector(AbstractFtrXml xml)
	{
		StringFeatureVector vector = new StringFeatureVector();
		
		for (FtrTemplate template : xml.getFtrTemplates())
			addFeatures(vector, template);
		
		return vector;
	}

	/** Called by {@link AbstractEngine#getFeatureVector(AbstractFtrXml)}. */
	private void addFeatures(StringFeatureVector vector, FtrTemplate template)
	{
		FtrToken[] tokens = template.tokens;
		int i, size = tokens.length;
		
		if (template.isSetFeature())
		{
			String[][] fields = new String[size][];
			String[]   tmp;
			
			for (i=0; i<size; i++)
			{
				tmp = getFields(tokens[i]);
				if (tmp == null)	return;
				fields[i] = tmp;
			}
			
			addFeatures(vector, template.type, fields, 0, "");
		}
		else
		{
			StringBuilder build = new StringBuilder();
			String field;
			
			for (i=0; i<size; i++)
			{
				field = getField(tokens[i]);
				if (field == null)	return;
				
				if (i > 0)	build.append(AbstractColumnReader.BLANK_COLUMN);
				build.append(field);
			}
			
			vector.addFeature(template.type, build.toString());			
		}
    }
	
	private void addFeatures(StringFeatureVector vector, String type, String[][] fields, int index, String prev)
	{
		if (index < fields.length)
		{
			for (String field : fields[index])
			{
				if (prev.isEmpty())
					addFeatures(vector, type, fields, index+1, field);
				else
					addFeatures(vector, type, fields, index+1, prev + AbstractColumnReader.BLANK_COLUMN + field);
			}
		}
		else
			vector.addFeature(type, prev);
	}
	
	abstract protected String   getField (FtrToken token);
	abstract protected String[] getFields(FtrToken token);
	
	abstract public void saveModel(PrintStream fout);
}
