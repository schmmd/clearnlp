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

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.colorado.clear.morphology.EnglishMPAnalyzer;
import edu.colorado.clear.reader.AbstractColumnReader;
import edu.colorado.clear.run.AbstractRun;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;

public class Merge
{
	public Merge(String[] args) throws Exception
	{
		EnglishMPAnalyzer morph = AbstractRun.getMPAnalyzerEn(args[0]);
		BufferedReader fin0 = UTInput.createBufferedFileReader(args[1]);
		BufferedReader fin1 = UTInput.createBufferedFileReader(args[2]);
		PrintStream fout = UTOutput.createPrintBufferedFileStream(args[3]);
		String line;
		String[] tmp0, tmp1;
		List<String> list;
		StringBuilder build;
		
		while ((line = fin0.readLine()) != null)
		{
			tmp0 = line.split(AbstractColumnReader.DELIM_COLUMN);
			tmp1 = fin1.readLine().split(AbstractColumnReader.DELIM_COLUMN);
			
			if (line.trim().isEmpty())
			{
				fout.println();
				continue;
			}
			
			list = new ArrayList<String>();
			Collections.addAll(list, tmp1);
			
			list.add(3, morph.getLemma(tmp0[0], tmp0[1]));
			list.add(5, tmp0[1]);
			
			build = new StringBuilder();
			build.append(list.get(0));
			
			for (int i=1; i<list.size(); i++)
			{
				build.append(AbstractColumnReader.DELIM_COLUMN);
				build.append(list.get(i));
			}
			
			fout.println(build.toString());
		}
		
		fout.close();
	}
	
	static public void main(String[] args)
	{
		try {
			new Merge(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
