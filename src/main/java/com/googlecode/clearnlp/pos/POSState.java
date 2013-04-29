/**
* Copyright 2012-2013 University of Massachusetts Amherst
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
package com.googlecode.clearnlp.pos;

import com.googlecode.clearnlp.classification.prediction.StringPrediction;

public class POSState
{
	public int              input;	// index of input
	public double           score;	// sum of all previous scores
	public StringPrediction label;
	
	public POSState(int input, double score, StringPrediction label)
	{
		this.input = input;
		this.score = score;
		this.label = label;
	}
}
