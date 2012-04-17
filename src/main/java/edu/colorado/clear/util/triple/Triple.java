package edu.colorado.clear.util.triple;

public class Triple<T1, T2, T3>
{
	public T1 o1;
	public T2 o2;
	public T3 o3;
	
	public Triple(T1 o1, T2 o2, T3 o3)
	{
		set(o1, o2, o3);
	}
	
	public void set(T1 o1, T2 o2, T3 o3)
	{
		this.o1 = o1;
		this.o2 = o2;
		this.o3 = o3;
	}
}
