package edu.colorado.clear.engine;

import edu.colorado.clear.classification.vector.StringFeatureVector;
import edu.colorado.clear.feature.xml.AbstractFtrXml;
import edu.colorado.clear.feature.xml.FtrTemplate;
import edu.colorado.clear.feature.xml.FtrToken;
import edu.colorado.clear.reader.AbstractColumnReader;

abstract public class AbstractTool
{
	static public final byte FLAG_LEXICA		= 0;
	static public final byte FLAG_TRAIN			= 1;
	static public final byte FLAG_PREDICT		= 2;
	static public final byte FLAG_BOOST			= 3;
	static public final byte FLAG_DEMO	= 4;
	
	protected StringFeatureVector getFeatureVector(AbstractFtrXml xml)
	{
		StringFeatureVector vector = new StringFeatureVector();
		
		for (FtrTemplate template : xml.getFtrTemplates())
			addFeatures(vector, template);
		
		return vector;
	}

	/** Called by {@link AbstractTool#getFeatureVector(AbstractFtrXml)}. */
	private void addFeatures(StringFeatureVector vector, FtrTemplate template)
	{
		FtrToken[] tokens = template.tokens;
		int i, size = tokens.length;
		
		if (template.isSetFeature())
		{
			String[][] fields = new String[size][];
			String[]   tmp;
			
			for (i=0; i<size; i++)
			{
				tmp = getFields(tokens[i]);
				if (tmp == null)	return;
				fields[i] = tmp;
			}
			
			addFeatures(vector, template.type, fields, 0, "");
		}
		else
		{
			StringBuilder build = new StringBuilder();
			String field;
			
			for (i=0; i<size; i++)
			{
				field = getField(tokens[i]);
				if (field == null)	return;
				
				if (i > 0)	build.append(AbstractColumnReader.BLANK_COLUMN);
				build.append(field);
			}
			
			vector.addFeature(template.type, build.toString());			
		}
    }
	
	private void addFeatures(StringFeatureVector vector, String type, String[][] fields, int index, String prev)
	{
		if (index < fields.length)
		{
			for (String field : fields[index])
			{
				if (prev.isEmpty())
					addFeatures(vector, type, fields, index+1, field);
				else
					addFeatures(vector, type, fields, index+1, prev + AbstractColumnReader.BLANK_COLUMN + field);
			}
		}
		else
			vector.addFeature(type, prev);
	}
	
	abstract protected String   getField (FtrToken token);
	abstract protected String[] getFields(FtrToken token);
}
