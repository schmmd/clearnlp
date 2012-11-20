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
package com.googlecode.clearnlp.beam;

import com.googlecode.clearnlp.classification.prediction.StringPrediction;


/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class BeamNode<T> implements Comparable<BeamNode<T>>
{
	private BeamNode<T>			g_prevNode = null;
	private StringPrediction	g_prediction = null;
	private T					g_configuration = null;
	
	public BeamNode(BeamNode<T> prevNode, StringPrediction prediction)
	{
		g_prevNode   = prevNode;
		g_prediction = prediction;
	}
	
	public BeamNode<T> getPrevNode()
	{
		return g_prevNode;
	}
	
	public void setPrevNode(BeamNode<T> prevNode)
	{
		g_prevNode = prevNode;
	}
	
	public String getLabel()
	{
		return g_prediction.label;
	}
	
	public double getScore()
	{
		return g_prediction.score;
	}
	
	public void setPrediction(StringPrediction prediction)
	{
		g_prediction = prediction;
	}
	
	public T getConfiguration()
	{
		return g_configuration;
	}

	@Override
	public int compareTo(BeamNode<T> node)
	{
		double d = getScore() - node.getScore();
		
		if      (d > 0)	return -1;
		else if (d < 0)	return  1;
		else			return  0;
	}
	
	public String toString()
	{
		return getLabel()+":"+getScore();
	}
}
