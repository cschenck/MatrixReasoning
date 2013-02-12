package utility;

import java.util.*;

public class MinHeap<E extends Comparable<? super E>>
{
	private static final int INIT_CAP = 10;
	private ArrayList<E> list;
	
	public MinHeap()
	{
		list = new ArrayList<E>(INIT_CAP);
	}
	
	public MinHeap(int aSize)
    {
		if ( aSize < 1 )
				throw new IllegalStateException();
		list = new ArrayList<E>(aSize);
    }
	
	public int size()
	{
		return list.size();
	}
	
	public boolean isEmpty()
	{
		return list.isEmpty();
	}
	
	public void add(E element)
	{
		if ( element == null )
				throw new NullPointerException("add");
		list.add(element); // append it to the end of the list
		percolateUp(); // move it up to the proper place
	}
	
	// move the last element up to the proper place.
	private void percolateUp()
	{
		int child = list.size() - 1; // last element in the list
		int parent;
		while ( child > 0 )
		{
			parent = (child - 1) / 2; // use the (j-1)/2 formula
			if ( list.get(child).compareTo(list.get(parent)) >= 0 )
				break;
			swap(parent, child);
			child = parent;
		}
	}
	
	private void swap(int parent, int child)
	{
		E tmp = list.get(parent);
		list.set( parent, list.get(child) );
		list.set(child, tmp);
	}
	
	public E getMin()
	{
		if ( list.isEmpty() )
			throw new NoSuchElementException();
		return list.get(0);
	}
	
	public E removeMin()
	{
		if ( list.isEmpty() )
			throw new NoSuchElementException();
		E minElem = list.get(0); // get the min element at the root
		list.set(0, list.get(list.size() - 1) ); // copy the last element to the root
		list.remove( list.size() - 1 ); // remove the last element from the list
		if ( ! list.isEmpty() )
			percolateDown(0); // move the element at the root down to the proper place
		return minElem;
	}
	
	// Move the element at index start down to the proper place.
	private void percolateDown(int start)
	{
		if ( start < 0 || start >= list.size() )
			throw new RuntimeException("start < 0 or >= n");
		int parent = start;
		int child = 2 * parent + 1; // use the 2*i+1 formula
		while ( child < list.size() )
		{
			if ( child + 1 < list.size() &&
					list.get(child).compareTo(list.get(child + 1)) > 0 )
				child++; // select the smaller child
			if ( list.get(child).compareTo(list.get(parent)) >= 0 )
				break; // reach the proper place
			swap(parent, child);
			parent = child;
			child = 2 * parent + 1;
		}
	}
	
	public boolean contains(E item)
	{
		return list.contains(item);
	}

} // Heap
