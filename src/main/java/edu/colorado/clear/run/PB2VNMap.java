package edu.colorado.clear.run;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.colorado.clear.io.FileExtFilter;
import edu.colorado.clear.util.UTOutput;
import edu.colorado.clear.util.UTXml;

public class PB2VNMap
{
	final String VERB_EXT	= "-v.xml";
	final String INDENT		= "  ";
	
	final String E_PBVNMAP			= "pbvnmap";
	final String E_FRAMESET			= "frameset";
	final String E_VERB				= "verb";
	final String A_VERB_LEMMA		= "lemma";
	final String E_ROLESET			= "roleset";
	final String A_ROLESET_ID		= "id";
	final String E_ROLES			= "roles";
	final String E_ROLE				= "role";
	final String A_ROLE_N			= "n";
	final String A_ROLE_F			= "f";
	final String E_VNROLE			= "vnrole";
	final String A_VNROLE_VNCLS		= "vncls";
	final String A_VNROLE_VNTHETA	= "vntheta";
	
	public PB2VNMap(String framesetDir, String outputFile) throws Exception
	{
		String[] filelist = new File(framesetDir).list(new FileExtFilter(VERB_EXT));
		PrintStream  fout = UTOutput.createPrintBufferedFileStream(outputFile);
		Arrays.sort(filelist);
		
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dFactory.newDocumentBuilder();
		Document doc;
		
		fout.println(UTXml.startsElement(false, E_PBVNMAP));
		Element eFrameset;
		String lemma, verb;
		int count = 0;
		
		for (String framesetFile : filelist)
		{
		//	System.out.println(framesetFile);
			
			doc       = builder.parse(new FileInputStream(framesetDir+File.separator+framesetFile));
			eFrameset = (Element)doc.getElementsByTagName(E_FRAMESET).item(0);
			lemma     = framesetFile.substring(0, framesetFile.length()-VERB_EXT.length());
			verb      = getVerb(eFrameset, lemma, 0);
			
			if (verb != null)
			{
				fout.println(verb);
				count++;
			}
		}
		
		fout.println(UTXml.endsElement(E_PBVNMAP));
		fout.close();
		
		System.out.println("# of verbs in VerbNet: "+count);
	}
	
	private String getVerb(Element eFrameset, String lemma, int indent)
	{
		NodeList nRolesets = eFrameset.getElementsByTagName(E_ROLESET);
		Map<String,String> map = new HashMap<String,String>();
		int i, size = nRolesets.getLength();
		String rolesetId, roleset;
		Element eRoleset;
		
		for (i=0; i<size; i++)
		{
			eRoleset  = (Element)nRolesets.item(i);
			rolesetId = UTXml.getTrimmedAttribute(eRoleset, A_ROLESET_ID);
			roleset   = getRoleset(eRoleset, rolesetId, indent+1);
			if (roleset != null)	map.put(rolesetId, roleset);
		}
		
		if (map.isEmpty())	return null;
		
		List<String> keys = new ArrayList<String>(map.keySet());
		Collections.sort(keys);
		
		StringBuilder build = new StringBuilder();
		
		for (String key : keys)
		{
			build.append("\n");
			build.append(map.get(key));
		}
		
		return getTemplate(E_VERB, build.substring(1), indent, A_VERB_LEMMA, lemma);
	}
	
	private String getRoleset(Element eRoleset, String rolesetId, int indent)
	{
		String[] vnclses = UTXml.getTrimmedAttribute(eRoleset, A_VNROLE_VNCLS).split(" ");
		NodeList nRoles  = eRoleset.getElementsByTagName(E_ROLE);
		Arrays.sort(vnclses);
		
		StringBuilder build = new StringBuilder();
		String roles;
		
		for (String vncls : vnclses)
		{
			if (vncls.isEmpty() || vncls.equals("-"))	continue;
			roles = getRoles(rolesetId, nRoles, vncls, indent+1);
			
			if (roles == null)
				System.err.println(rolesetId+" "+vncls);
			else
			{
				build.append("\n");
				build.append(roles);				
			}
		}
		
		if (build.length() > 0)
			return getTemplate(E_ROLESET, build.substring(1), indent, A_ROLESET_ID, rolesetId);
		else
			return null;
	}
	
	private String getRoles(String rolesetId, NodeList nRoles, String vncls, int indent)
	{
		StringBuilder build = new StringBuilder();
		int i, size = nRoles.getLength();
		String vntheta, n, f;
		Element eRole;
		
		for (i=0; i<size; i++)
		{
			eRole   = (Element)nRoles.item(i);
			vntheta = getVntheta(eRole.getElementsByTagName(E_VNROLE), vncls);
			
			if (!vntheta.isEmpty())
			{
				n = UTXml.getTrimmedAttribute(eRole, A_ROLE_N);
				f = UTXml.getTrimmedAttribute(eRole, A_ROLE_F);

				build.append("\n");
				build.append(getIndent(indent+1));
				build.append(UTXml.startsElement(true, E_ROLE, A_ROLE_N, n, A_ROLE_F, f, A_VNROLE_VNTHETA, vntheta));
			}
		}
		
		if (build.length() > 0)
			return getTemplate(E_ROLES, build.substring(1), indent, A_VNROLE_VNCLS, vncls);
		else
			return null;
	}
	
	private String getVntheta(NodeList nVnroles, String vncls)
	{
		int i, size = nVnroles.getLength();
		Element eVnrole;
		String cls;
		
		for (i=0; i<size; i++)
		{
			eVnrole = (Element)nVnroles.item(i);
			cls = UTXml.getTrimmedAttribute(eVnrole, A_VNROLE_VNCLS);
			
			if (cls.equals(vncls))
				return UTXml.getTrimmedAttribute(eVnrole, A_VNROLE_VNTHETA);
		}
		
		return "";
	}
	
	private String getTemplate(String element, String contents, int indent, String... attributes)
	{
		StringBuilder build = new StringBuilder();
		
		build.append(getIndent(indent));
		build.append(UTXml.startsElement(false, element, attributes));
		build.append("\n");
		
		build.append(contents);
		build.append("\n");
		
		build.append(getIndent(indent));
		build.append(UTXml.endsElement(element));
		
		return build.toString();
	}
	
	private String getIndent(int n)
	{
		StringBuilder build = new StringBuilder();
		
		for (int i=0; i<n; i++)
			build.append(INDENT);
		
		return build.toString();
	}

	public static void main(String[] args)
	{
		String framesetDir = args[0];
		String outputFile  = args[1];
		
		try
		{
			new PB2VNMap(framesetDir, outputFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}

}
