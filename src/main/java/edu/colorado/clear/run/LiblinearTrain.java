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
package edu.colorado.clear.run;

import java.io.PrintStream;

import org.kohsuke.args4j.Option;

import edu.colorado.clear.classification.algorithm.AbstractAlgorithm;
import edu.colorado.clear.classification.algorithm.LiblinearL2LR;
import edu.colorado.clear.classification.algorithm.LiblinearL2SV;
import edu.colorado.clear.classification.model.AbstractModel;
import edu.colorado.clear.classification.train.AbstractTrainSpace;
import edu.colorado.clear.classification.train.SparseTrainSpace;
import edu.colorado.clear.classification.train.StringTrainSpace;
import edu.colorado.clear.classification.train.Trainer;
import edu.colorado.clear.util.UTInput;
import edu.colorado.clear.util.UTOutput;

/**
 * Trains a liblinear model.
 * @since v0.1
 * @author Jinho D. Choi ({@code choijd@colorado.edu})
 */
public class LiblinearTrain extends AbstractRun
{
	@Option(name="-i", usage="the trainning file (input; required)", required=true, metaVar="<filename>")
	private String s_trainFile;
	
	@Option(name="-m", usage="the model file (output; required)", required=true, metaVar="<filename>")
	private String s_modelFile;
	
	@Option(name="-nl", usage="label frequency cutoff (default: 0)\n"+"exclusive, string vector space only", required=false, metaVar="<integer>")
	private int i_labelCutoff = 0; 
	
	@Option(name="-nf", usage="feature frequency cutoff (default: 0)\n"+"exclusive, string vector space only", required=false, metaVar="<integer>")
	private int i_featureCutoff = 0;
	
	@Option(name="-nt", usage="the number of threads to be used (default: 1)", required=false, metaVar="<integer>")
	private int i_numThreads = 1;
	
	@Option(name="-v", usage="the type of vector space (default: "+AbstractTrainSpace.VECTOR_STRING+")\n"+
							AbstractTrainSpace.VECTOR_SPARSE+": sparse vector space\n"+
            				AbstractTrainSpace.VECTOR_STRING+": string vector space\n",
            required=false, metaVar="<byte>")
	private byte i_vectorType = AbstractTrainSpace.VECTOR_STRING;
	
	@Option(name="-s", usage="the type of solver (default: "+AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L1_SV+")\n"+
							AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L1_SV+": L2-regularized L1-loss support vector classification (dual)\n"+
							AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L2_SV+": L2-regularized L2-loss support vector classification (dual)\n"+
							AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_LR   +": L2-regularized logistic regression (dual)",
			required=false, metaVar="<byte>")
	private byte i_solver = AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L1_SV;
	
	@Option(name="-c", usage="the cost (default: 0.1)", required=false, metaVar="<double>")
	private double d_cost = 0.1;
	
	@Option(name="-e", usage="the tolerance of termination criterion (default: 0.1)", required=false, metaVar="<double>")
	private double d_eps = 0.1;
	
	@Option(name="-b", usage="the bias (default: 0)", required=false, metaVar="<double>")
	private double d_bias = 0.0;
	
	public LiblinearTrain() {}
	
	public LiblinearTrain(String[] args)
	{
		initArgs(args);
		
		try
		{
			train(s_trainFile, s_modelFile, i_vectorType, i_labelCutoff, i_featureCutoff, i_numThreads, i_solver, d_cost, d_eps, d_bias);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void train(String trainFile, String modelFile, byte vectorType, int labelCutoff, int featureCutoff, int numThreads, byte solver, double cost, double eps, double bias) throws Exception
	{
		AbstractTrainSpace space = null;
		boolean hasWeight = AbstractTrainSpace.hasWeight(vectorType, trainFile);
		
		switch (vectorType)
		{
		case AbstractTrainSpace.VECTOR_SPARSE:
			space = new SparseTrainSpace(hasWeight); break;
		case AbstractTrainSpace.VECTOR_STRING:
			space = new StringTrainSpace(hasWeight, labelCutoff, featureCutoff); break;
		}
		
		space.readInstances(UTInput.createBufferedFileReader(trainFile));
		space.build();
		
		AbstractModel model = getModel(space, numThreads, solver, cost, eps, bias);
		PrintStream   fout  = UTOutput.createPrintBufferedGzipFileStream(modelFile);
		
		model.setSolver(solver);
		model.save(fout);
		fout.close();
	}
	
	static public AbstractModel getModel(AbstractTrainSpace space, int numThreads, byte solver, double cost, double eps, double bias)
	{
		AbstractAlgorithm algorithm = null;
		
		switch (solver)
		{
		case AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L1_SV:
			algorithm = new LiblinearL2SV((byte)1, cost, eps, bias); break;			
		case AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_L2_SV:
			algorithm = new LiblinearL2SV((byte)2, cost, eps, bias); break;
		case AbstractAlgorithm.SOLVER_LIBLINEAR_LR2_LR:
			algorithm = new LiblinearL2LR(cost, eps, bias); break;
		}

		new Trainer(space, algorithm, numThreads);
		return space.getModel();
	}
	
	static public void main(String[] args)
	{
		new LiblinearTrain(args);
	}
}
