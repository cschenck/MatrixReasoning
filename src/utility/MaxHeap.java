package utility;


public class MaxHeap<E extends Comparable<? super E>>
{
	private class WrapperComparor implements Comparable<WrapperComparor>
	{
		public final E obj;
		
		public WrapperComparor(E obj)
		{
			this.obj = obj;
		}

		@Override
		public int compareTo(WrapperComparor o) {
			return -1*obj.compareTo(o.obj);
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if(!(obj instanceof MaxHeap.WrapperComparor))
				return false;
			WrapperComparor wc = (WrapperComparor) obj;
			return this.obj.equals(wc.obj);
		}
		
		@Override
		public int hashCode()
		{
			return this.obj.hashCode();
		}
	}

	private MinHeap<WrapperComparor> wrapped;
	
	public MaxHeap()
	{
		wrapped = new MinHeap<WrapperComparor>();
	}
	
	public MaxHeap(int aSize)
    {
		wrapped = new MinHeap<WrapperComparor>(aSize);
    }
	
	public int size()
	{
		return wrapped.size();
	}
	
	public boolean isEmpty()
	{
		return wrapped.isEmpty();
	}
	
	public void add(E element)
	{
		wrapped.add(new WrapperComparor(element));
	}
	
	public E getMax()
	{
		return wrapped.getMin().obj;
	}
	
	public E removeMax()
	{
		return wrapped.removeMin().obj;
	}
	
	public boolean contains(E item)
	{
		return wrapped.contains(new WrapperComparor(item));
	}
	
}
