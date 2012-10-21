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
package com.googlecode.clearnlp.dependency.srl;

import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.engine.AbstractEngine;

/**
 * @since 1.1.1
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
abstract public class AbstractSRLabeler extends AbstractEngine
{
	public AbstractSRLabeler(byte flag)
	{
		super(flag);
	}

	abstract public void label(DEPTree tree);
}
