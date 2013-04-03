package utility;

public class Tuple<A, B> {

	public final A a;
	public final B b;
	
	public Tuple(A a, B b)
	{
		this.a = a;
		this.b = b;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Tuple<?,?>))
			return false;
		Tuple<A,B> ut = (Tuple<A,B>)o;
		if(ut.a.equals(this.a) && ut.b.equals(this.b))
			return true;
		else
			return false;
	}
	
	@Override
	public int hashCode()
	{
		return this.a.hashCode()*this.b.hashCode() + this.b.hashCode();
	}
	
	@Override
	public String toString()
	{
		return "<" + a.toString() + "," + b.toString() + ">";
	}
	
}
