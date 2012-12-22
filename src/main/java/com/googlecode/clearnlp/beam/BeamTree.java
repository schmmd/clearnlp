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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import com.googlecode.clearnlp.classification.prediction.StringPrediction;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class BeamTree<T>
{
	private List<BeamNode<T>> b_nodes;
	private int n_size;
	
	public BeamTree(int beamSize)
	{
		b_nodes = new ArrayList<BeamNode<T>>(beamSize);
		n_size  = beamSize;
	}
	
	public BeamNode<T> getNode(int k)
	{
		return b_nodes.get(k);
	}
	
	public List<BeamNode<T>> getCurrNodes()
	{
		return b_nodes;
	}
	
	/**
	 * Sets the new beam given a sorted list of predictions.
	 * @param predictions the sorted list of predictions.
	 */
	public void setBeam(List<List<StringPrediction>> predictions)
	{
		List<BeamNode<T>> bNodes = new ArrayList<BeamNode<T>>();
		List<StringPrediction> cNodes;
		BeamNode<T> pNode;
		double score;
		int i, size;
		
		// add first predictions
		cNodes = predictions.get(0);
		pNode  = getPrevNode(0);
		size   = cNodes.size();
		if (size > n_size)	size = n_size;
		
		for (i=0; i<size; i++)
			bNodes.add(new BeamNode<T>(pNode, cNodes.get(i)));
		
		// add 1..k predictions
		size = predictions.size();
		
		for (i=1; i<size; i++)
		{
			cNodes = predictions.get(i);
			pNode  = getPrevNode(i);
			score  = bNodes.get(bNodes.size()-1).getScore();
				
			for (StringPrediction p : cNodes)
			{
				if (p.score < score)	break;
				bNodes.add(new BeamNode<T>(pNode, p));
			}
			
			Collections.sort(bNodes);
			
			if (bNodes.size() > n_size)
				bNodes.subList(n_size, bNodes.size()).clear();
		}
		
		b_nodes = bNodes;
	}
	
	public List<BeamNode<T>> getBestSequence()
	{
		List<BeamNode<T>> mSeq = b_nodes.get(0).getSequence(), tSeq;
		double mScore = getOverallScore(mSeq), tScore;
		int i, size = b_nodes.size();
		
		for (i=1; i<size; i++)
		{
			tSeq   = b_nodes.get(i).getSequence();
			tScore = getOverallScore(tSeq);
			
			if (tScore > mScore)
			{
				mSeq   = tSeq;
				mScore = tScore;
			}
		}
		
		return mSeq;
	}
	
	protected double getOverallScore(List<BeamNode<T>> nodes)
	{
		double d = 0;
		
		for (BeamNode<T> node : nodes)
			d += node.getScore();
		
		return d;
	}
	
	private BeamNode<T> getPrevNode(int k)
	{
		return b_nodes.isEmpty() ? null : b_nodes.get(k);
	}
	
	public String toString()
	{
		StringBuilder build = new StringBuilder();

		for (BeamNode<T> node : b_nodes)
		{
			build.append("\n");
			build.append(toString(node));
		}
		
		return build.substring(1);
	}
	
	public String toString(BeamNode<T> node)
	{
		Deque<BeamNode<T>> nodes = new ArrayDeque<BeamNode<T>>();

		while (node != null)
		{
			nodes.add(node);
			node = node.getPrevNode();
		}

		StringBuilder build = new StringBuilder();
		
		while (!nodes.isEmpty())
		{
			node = nodes.pollLast();
			build.append(" -> ");
			build.append(node.getLabel()+":"+node.getScore());
		}
		
		return build.substring(4);
	}
}
