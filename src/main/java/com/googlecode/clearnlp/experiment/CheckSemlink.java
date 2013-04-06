package com.googlecode.clearnlp.experiment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.googlecode.clearnlp.propbank.verbnet.PVMap;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;

public class CheckSemlink
{
	final Pattern DELIM = Pattern.compile(" ");
	
	public CheckSemlink(String pbvnFile, String semlinkFile, String propFile) throws Exception
	{
		PVMap pvMap = new PVMap(new BufferedInputStream(new FileInputStream(pbvnFile)));
		Set<String> semKeys = getSemlinkKeys(semlinkFile);
		countMoreAnnotations(pvMap, semKeys, propFile);
	}
	
	Set<String> getSemlinkKeys(String semlinkFile) throws Exception
	{
		BufferedReader fin = UTInput.createBufferedFileReader(semlinkFile);
		Set<String> set = new HashSet<String>();
		String line;
		String[] t;

		while ((line = fin.readLine()) != null)
		{
			t = DELIM.split(line);
			set.add(getKey(t));
		}
		
		return set;
	}
	
	void countMoreAnnotations(PVMap pvMap, Set<String> semKeys, String propFile) throws Exception
	{
		BufferedReader fin = UTInput.createBufferedFileReader(propFile);
		int c0 = 0, c1 = 0, c2 = 0, tc = 0, vc = 0, sc = 0, z;
		String line, v;
		String[] t;
		
		while ((line = fin.readLine()) != null)
		{
			t = DELIM.split(line);
			v = t[4];
			v = v.substring(0, v.length()-2);
			
			tc++;
			
			if (pvMap.containsKey(v))
			{
				vc++;
			
				if (!semKeys.contains(getKey(t)))
				{
					sc++;
					z = pvMap.getVNSet(t[5]).size();
					
					if      (z == 0)	c0++;
					else if (z == 1)	c1++;
					else 				c2++;
					
				}
			}
		}
		
		System.out.println(semKeys.size());
		System.out.println(c0+" "+c1+" "+c2+" "+tc+" "+vc+" "+sc);
	}
	
	String getKey(String[] t)
	{
		return t[0]+" "+t[1]+" "+t[2];
	}
	
	public void check(String pbvnFile, String semlinkFile, String errorFile) throws Exception
	{
		PVMap pvMap = new PVMap(new BufferedInputStream(new FileInputStream(pbvnFile)));
		BufferedReader fin = UTInput.createBufferedFileReader(semlinkFile);
		Map<String,ObjectIntOpenHashMap<String>> miss = new HashMap<String,ObjectIntOpenHashMap<String>>();
		Pattern delim = Pattern.compile(" ");
		ObjectIntOpenHashMap<String> m;
		String line, verbnet, roleset;
		Set<String> vnset;
		String[] t;
		
		while ((line = fin.readLine()) != null)
		{
			t = delim.split(line);
			verbnet = t[5];
			roleset = t[7];
			vnset = pvMap.getVNSet(roleset);
			
			if (vnset.isEmpty() || !vnset.contains(verbnet))
			{
				m = miss.get(roleset);
				
				if (m == null)
				{
					m = new ObjectIntOpenHashMap<String>();
					miss.put(roleset, m);
				}
				
				m.put(verbnet, m.get(verbnet)+1);
			}
		}
		
		printErrors(miss, errorFile);
	}
	
	void printErrors(Map<String,ObjectIntOpenHashMap<String>> map, String outputFile)
	{
		PrintStream fout = UTOutput.createPrintBufferedFileStream(outputFile);
		List<String> keys = new ArrayList<String>(map.keySet());
		Collections.sort(keys);
		
		for (String key: keys)
		{
			fout.println(key+" "+map.get(key).toString());
		}
		
		fout.close();
	}
	
	static public void main(String[] args)
	{
		try
		{
			new CheckSemlink(args[0], args[1], args[2]);
		}
		catch (Exception e) {e.printStackTrace();}
	}
}

