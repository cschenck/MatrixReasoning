package utility;

public class UnorderedTuple<A> extends Tuple<A, A> {
	
	public UnorderedTuple(A a, A b)
	{
		super(a, b);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof UnorderedTuple<?>))
			return false;
		UnorderedTuple<A> ut = (UnorderedTuple<A>)o;
		if((ut.a.equals(this.a) && ut.b.equals(this.b)) 
				|| (ut.a.equals(this.b) && ut.b.equals(this.a)))
			return true;
		else
			return false;
	}
	
	@Override
	public int hashCode()
	{
		return this.a.hashCode()*this.b.hashCode();
	}

}
