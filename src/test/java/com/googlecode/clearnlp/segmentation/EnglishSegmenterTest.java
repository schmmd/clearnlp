/**
 * Copyright 2012 University of Massachusetts Amherst
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.googlecode.clearnlp.segmentation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.List;

import com.googlecode.clearnlp.tokenization.EnglishTokenizer;
import com.googlecode.clearnlp.util.UTArray;
import com.googlecode.clearnlp.util.UTInput;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class EnglishSegmenterTest
{
//	@Test
	public void testSamples1() throws FileNotFoundException
	{
		BufferedReader fin = UTInput.createBufferedFileReader("/Users/jdchoi/Desktop/sample1.txt");
		EnglishSegmenter tok = new EnglishSegmenter(new EnglishTokenizer(UTInput.createZipFileInputStream("src/main/resources/model/dict-121004.jar")));
		
		for (List<String> sentence : tok.getSentences(fin))
			System.out.println(UTArray.join(sentence, " "));
	}
}
