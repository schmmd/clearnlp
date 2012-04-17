package edu.colorado.clear.dependency;


public class DEPLib
{
	static public final int ROOT_ID =  0;
	static public final int NULL_ID = -1;

	/** A dummy tag for the root node. */
	static public final String ROOT_TAG = "_R_";
	/** The dependency label representing roots. */
	static public final String DEP_ROOT = "root";
	/** The dependency label representing unknown dependencies. */
	static public final String DEP_DEP  = "dep";
	
	static public final String FEAT_SEM	= "sem";
	static public final String FEAT_SYN	= "syn";
	static public final String FEAT_PB	= "pb";
	static public final String FEAT_WS	= "ws";

	static public final String DELIM_HEADS     = ";";
	static public final String DELIM_HEADS_KEY = ":";
}
