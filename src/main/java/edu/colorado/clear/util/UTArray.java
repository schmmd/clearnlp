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
package edu.colorado.clear.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Array utilities.
 * @author Jinho D. Choi (jdchoi77@gmail.com)
 */
public class UTArray
{
	static public String join(String delim, Object... arr)
	{
		return join(arr, delim);
	}
	
	static public String join(int[] arr, String delim)
	{
		StringBuilder builder = new StringBuilder();
		
		for (int item : arr)
		{
			builder.append(delim);
			builder.append(item);
		}
		
		return builder.substring(delim.length());
	}
	
	static public String join(List<Object> list, String delim)
	{
		StringBuilder builder = new StringBuilder();
		
		for (Object item : list)
		{
			builder.append(delim);
			builder.append(item.toString());
		}
		
		return builder.substring(delim.length());
	}
	
	static public String join(Object[] arr, String delim)
	{
		StringBuilder builder = new StringBuilder();
		
		for (Object item : arr)
		{
			builder.append(delim);
			builder.append(item.toString());
		}
		
		return builder.substring(delim.length());
	}
	
	static public int[] toIntArray(String[] sArr)
	{
		int i, size = sArr.length;
		int[] iArr = new int[size];
		
		for (i=0; i<size; i++)
			iArr[i] = Integer.parseInt(sArr[i]);
		
		return iArr;
	}
	
	static public int[] toIntArray(String[] sArr, int beginIdx)
	{
		int i, j, size = sArr.length;
		int[] iArr = new int[size - beginIdx];
		
		for (i=beginIdx,j=0; i<size; i++,j++)
			iArr[j] = Integer.parseInt(sArr[i]);
		
		return iArr;
	}
	
	static public Set<String> toSet(String... sArr)
	{
		Set<String> set = new HashSet<String>();
		
		for (String item : sArr)
			set.add(item);
				
		return set;
	}
	
	static public int max(int[] arr)
	{
		int i, size = arr.length, m = arr[0];
		
		for (i=1; i<size; i++)
			m = Math.max(m, arr[i]);
		
		return m;
	}
	
	static public int min(int[] arr)
	{
		int i, size = arr.length, m = arr[0];
		
		for (i=1; i<size; i++)
			m = Math.min(m, arr[i]);
		
		return m;
	}
	
	/**
	 * Swaps two items in the specific array.
	 * @param array the array to perform swap.
	 * @param idx0 the index of the first item.
	 * @param idx1 the index of the second item.
	 */
	static public void swap(int[] array, int idx0, int idx1)
	{
		int temp    = array[idx0];
		array[idx0] = array[idx1];
		array[idx1] = temp;
	}
}
