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
package com.googlecode.clearnlp.classification.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.log4j.Logger;

import com.googlecode.clearnlp.util.UTArray;


/**
 * Sparse vector model.
 * @since 1.0.0
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class SparseModel extends AbstractModel
{
	private final Logger LOG = Logger.getLogger(this.getClass());
	
	/** Constructs a sparse model for training. */
	public SparseModel()
	{
		super();
	}
	
	/**
	 * Constructs a sparse model for decoding.
	 * @param reader the reader to load the model from.
	 */
	public SparseModel(BufferedReader reader)
	{
		super(reader);
	}
	
	/* (non-Javadoc)
	 * @see edu.colorado.clear.classification.model.AbstractModel#load(java.io.Reader)
	 */
	public void load(BufferedReader reader)
	{
		LOG.info("Loading model:\n");
		
		try
		{
			i_solver = Byte.parseByte(reader.readLine());
			loadLabels(reader);
			loadFeatures(reader);
			loadWeightVector(reader);			
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	/* (non-Javadoc)
	 * @see edu.colorado.clear.classification.model.AbstractModel#save(java.io.PrintStream)
	 */
	public void save(PrintStream fout)
	{
		LOG.info("Saving model:\n");
		
		try
		{
			fout.println(i_solver);
			saveLabels(fout);
			saveFeatures(fout);
			saveWeightVector(fout);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void loadFeatures(BufferedReader fin) throws IOException
	{
		n_features = Integer.parseInt(fin.readLine());
	}
	
	private void saveFeatures(PrintStream fout)
	{
		fout.println(n_features);
	}
	
	/**
	 * Adds the specific feature indices to this model.
	 * @param indices the feature indices.
	 */
	public void addFeatures(int[] indices)
	{
		n_features = Math.max(n_features, UTArray.max(indices)+1);
	}
}
