package edu.colorado.clear.util;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class UTXml
{
	static public Element getFirstElementByTagName(Element element, String name)
	{
		NodeList list = element.getElementsByTagName(name);
		return list.getLength() > 0 ? (Element)list.item(0) : null;		
	}
	
	static public String getTrimmedAttribute(Element element, String name)
	{
		return element.getAttribute(name).trim();
	}
	
	static public String getTrimmedTextContent(Element element)
	{
		return element.getTextContent().trim();
	}
	
	static public Element getDocumentElement(InputStream fin)
	{
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		
		try
		{
			DocumentBuilder builder = dFactory.newDocumentBuilder();
			Document        doc     = builder.parse(fin);
			
			return doc.getDocumentElement();
		}
		catch (Exception e) {System.exit(1);}
		
		return null;
	}
	
	static public String startsElement(boolean isClosed, String element, String... attributes)
	{
		StringBuilder build = new StringBuilder();
		int i, size = attributes.length;
		String key, val;
		
		build.append("<");
		build.append(element);
		
		for (i=0; i<size; i+=2)
		{
			key = attributes[i];
			val = attributes[i+1];
			
			build.append(" ");
			build.append(key);
			build.append("=\"");
			build.append(val);
			build.append("\"");
		}
		
		if (isClosed)	build.append("/>");
		else			build.append(">");
		
		return build.toString();
	}
	
	static public String endsElement(String element)
	{
		StringBuilder build = new StringBuilder();
		
		build.append("</");
		build.append(element);
		build.append(">");
		
		return build.toString();
	}	
}
