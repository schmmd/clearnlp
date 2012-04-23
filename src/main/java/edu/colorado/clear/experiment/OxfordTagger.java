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
package edu.colorado.clear.experiment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;
import edu.colorado.clear.util.UTXml;

public class OxfordTagger
{
	static public String  QUERY = "http://www.oed.com/srupage?operation=searchRetrieve&query=cql.serverChoice+=+%s&maximumRecords=100&startRecord=1";
	static public Pattern RE_POS = Pattern.compile("^(v|n|adj|adv)$");
	
	private Set<String> s_wordnet;
	
	public OxfordTagger(String wordnetFile) throws Exception
	{
		s_wordnet = new HashSet<String>();
		
		BufferedReader fin = UTInput.createBufferedFileReader(wordnetFile);
		String line;
		
		while ((line = fin.readLine()) != null)
		{
			for (String word : line.split(" "))
				s_wordnet.add(word);
		}
	}
	
	public boolean isWordnet(String form)
	{
		return s_wordnet.contains(form);
	}
	
	public Set<String> tag(String form) throws Exception
	{
		String query = String.format(QUERY, form);
		URL url = new URL(query);

		Element eRespose = UTXml.getDocumentElement(new BufferedInputStream(url.openStream()));
		NodeList list = eRespose.getElementsByTagName("dc:title");
		int i = 0, size = list.getLength();
		Element eTitle;
		String  sTitle, lemma, pos;
		String[] tmp;
		Set<String> set = new HashSet<String>();
		
		for (i=0; i<size; i++)
		{
			eTitle = (Element)list.item(i);
			sTitle = UTXml.getTrimmedTextContent(eTitle);
			
			tmp = sTitle.split(",");
			
			if (tmp.length > 1)
			{
				pos = tmp[1].trim();
				if (pos.split(" ").length > 1)
					continue;
				
				pos = pos.split("\\.")[0];
				if (!RE_POS.matcher(pos).find())
					continue;
				
				tmp = tmp[0].trim().split(" ");
				
				if (tmp.length == 3 && tmp[1].equals("in"))
					lemma = "_"+tmp[2];
				else
					lemma = "";
				
				set.add(pos+lemma);
			}
		}
		
		return set;
	}
	
	static public void main(String[] args) 
	{
		String wordnetFile = args[0];
		String inputFile   = args[1];
		String outputFile  = args[2];
		
		try
		{
			OxfordTagger ot = new OxfordTagger(wordnetFile);
			BufferedReader fin = UTInput.createBufferedFileReader(inputFile);
			PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
			Set<String> set;
			String form;
			StringBuilder build;
			int i;
			
			for (i=1; (form = fin.readLine()) != null; i++)
			{
				if (i%100 == 0)
				{
					System.out.println(i);
					fout.flush();
				}
				
				form = form.trim();
				if (ot.isWordnet(form))
					continue;
				set = ot.tag(form);
				
				if (!set.isEmpty())
				{
					build = new StringBuilder();
					build.append(form);
					
					for (String tag : set)
					{
						build.append(" ");
						build.append(tag);
					}
					
					fout.println(build.toString());
				}
			}
			
			fout.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
