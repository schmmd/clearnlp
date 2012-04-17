package edu.colorado.clear.experiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;

import edu.colorado.clear.util.UTArray;
import edu.colorado.clear.util.UTOutput;
import edu.colorado.clear.util.UTXml;
import edu.colorado.clear.util.pair.Pair;

public class ExtractSteve
{
	public ExtractSteve(String xmlFile) throws Exception
	{
		Element eAnnotations = UTXml.getDocumentElement(new FileInputStream(xmlFile));
		NodeList files = eAnnotations.getElementsByTagName("file");
		int i, size = files.getLength();
		
		String dirPath = xmlFile.substring(0, xmlFile.lastIndexOf(File.separator));
		Pair<String[][],ObjectIntOpenHashMap<String>> pTokens;
		PrintStream fout;
		String filename;
		Element eFile;
		
		for (i=0; i<size; i++)
		{
			eFile = (Element)files.item(i);
			filename = dirPath+File.separator+UTXml.getTrimmedAttribute(eFile, "name");
			pTokens = getTokens(UTXml.getFirstElementByTagName(eFile, "tokens"), new RandomAccessFile(new File(filename), "r"));
			setAnnotators(UTXml.getFirstElementByTagName(eFile, "annotators"), pTokens);
			
			fout = UTOutput.createPrintBufferedFileStream(filename+".dep");
			
			for (String[] t : pTokens.o1)
				fout.println(UTArray.join(t, "\t"));
			
			fout.println();
			fout.close();
		}
	}
	
	private Pair<String[][],ObjectIntOpenHashMap<String>> getTokens(Element eTokens, RandomAccessFile fin) throws Exception
	{
		ObjectIntOpenHashMap<String> map = new ObjectIntOpenHashMap<String>();
		NodeList nTokens = eTokens.getElementsByTagName("token");
		int i, size = nTokens.getLength();
		String[][] tokens = new String[size][];
		String key, token;
		Element eToken;
		int[] indexes = new int[2];
		byte[] b;
		
		for (i=0; i<size; i++)
		{
			eToken = (Element)nTokens.item(i);
			key = getKey(eToken, indexes);
			fin.seek(indexes[0]);
			b = new byte[indexes[1]-indexes[0]];
			fin.read(b);
			token = new String(b);
			
			map.put(key, i);
			tokens[i] = new String[]{Integer.toString(i+1), token, "_", "_", "_", "_"};
		}
		
		return new Pair<String[][],ObjectIntOpenHashMap<String>>(tokens, map);
	}
	
	private String getKey(Element element, int[] indexes)
	{
		indexes[0] = Integer.parseInt(UTXml.getTrimmedAttribute(element, "begin"));
		indexes[1] = Integer.parseInt(UTXml.getTrimmedAttribute(element, "end"));
		
		return UTArray.join(indexes, "-");
	}
	
	private void setAnnotators(Element eAnnotators, Pair<String[][],ObjectIntOpenHashMap<String>> pTokens)
	{
		NodeList nAnnotators = eAnnotators.getElementsByTagName("annotator");
		int i, size = nAnnotators.getLength();
		
		for (i=0; i<size; i++)
			setAnnotator((Element)nAnnotators.item(i), (i+1)*2, pTokens);
	}
	
	private void setAnnotator(Element eAnnotator, int annotatorId, Pair<String[][],ObjectIntOpenHashMap<String>> pTokens)
	{
		Element eEvents = UTXml.getFirstElementByTagName(eAnnotator, "events");
		NodeList nEvents = eEvents.getElementsByTagName("event");
		Set<String> set = new HashSet<String>();
		int i, size = nEvents.getLength();
		int[] indexes = new int[2];
		
		for (i=0; i<size; i++)
			set.add(getKey((Element)nEvents.item(i), indexes));
		
		Element eTlinks = UTXml.getFirstElementByTagName(eAnnotator, "tlinks");
		NodeList nTlinks = eTlinks.getElementsByTagName("tlink");
		String deprel, source, target;
		Element eTlink;
		size = nTlinks.getLength();
		
		String[][] tokens = pTokens.o1;
		ObjectIntOpenHashMap<String> map = pTokens.o2;
		int targetId;
		
		for (i=0; i<size; i++)
		{
			eTlink = (Element)nTlinks.item(i);
			deprel = UTXml.getFirstElementByTagName(eTlink, "relation").getAttribute("type").trim();
			source = getKey(UTXml.getFirstElementByTagName(eTlink, "source"), indexes);
			target = getKey(UTXml.getFirstElementByTagName(eTlink, "target"), indexes);
			 
			set.remove(target);
			
			targetId = map.get(target);
			tokens[targetId][annotatorId]   = Integer.toString(map.get(source)+1);
			tokens[targetId][annotatorId+1] = deprel;
		}
		
		for (String root : set)
		{
			targetId = map.get(root);
			tokens[targetId][annotatorId]   = "0";
			tokens[targetId][annotatorId+1] = "root";
		}
	}

	public static void main(String[] args) throws Exception
	{
		new ExtractSteve(args[0]);
	}
}
