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
package com.googlecode.clearnlp.engine;


/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public interface EngineLib
{
	final String ENTRY_CONFIGURATION	= "CONFIGURATION";
	final String ENTRY_FEATURE			= "FEATURE";
	final String ENTRY_MODEL			= "MODEL";

	final String ENTRY_THRESHOLD		= "THRESHOLD";	// for POSTagger
	final String ENTRY_SET_PUNCT		= "SET_PUNCT";	// for DEPParser
	final String ENTRY_SET_DOWN 		= "SET_DOWN";	// for SRLabeler
	final String ENTRY_SET_UP   		= "SET_UP";		// for SRLabeler
}
