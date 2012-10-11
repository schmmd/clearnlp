import java.io.PrintStream;
import java.util.Arrays;

import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.reader.DEPReader;
import com.googlecode.clearnlp.util.UTFile;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.UTOutput;


public class Tmp
{

	public void toRaw(String dirPath)
	{
		DEPReader reader = new DEPReader(0, 1, 2, 3, 4, 5, 6);
		String[] filenames = UTFile.getSortedFileList(dirPath);
		PrintStream fout;
		DEPTree tree;
		
		for (String filename : filenames)
		{
			reader.open(UTInput.createBufferedFileReader(filename));
			fout = UTOutput.createPrintBufferedFileStream(filename+".raw");
			System.out.println(filename);
			
			while ((tree = reader.next()) != null)
				fout.println(tree.toStringRaw());
			
			reader.close();
			fout.close();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		String s = "A  B C";
		System.out.println(Arrays.toString(s.split(" ")));
	}
}
