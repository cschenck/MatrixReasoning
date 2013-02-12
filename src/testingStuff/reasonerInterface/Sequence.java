package testingStuff.reasonerInterface;

import java.util.ArrayList;
import java.util.List;

public abstract class Sequence<T, E> {
	
	private List<E> entries; //the entries in the sequence
	
	public Sequence(List<E> entries)
	{
		this.entries = new ArrayList<E>(entries);
	}
	
	/**
	 * Returns the description of the sequence that will act as the input
	 * to the recognition models.
	 * 
	 * @return
	 */
	public abstract T getDescription();
	
	/**
	 * returns the i'th entry in the sequence
	 * 
	 * @param i
	 * @return
	 */
	public E getEntry(int i)
	{
		return entries.get(i);
	}
	
	public int size()
	{
		return entries.size();
	}
	
	/**
	 * Create a new sequence of the same type as the instantiated sequence
	 * 
	 * @param entries
	 * @return
	 */
	public abstract Sequence constructSequence(List<E> entries);
	
	public Sequence append(E entry)
	{
		List<E> list = new ArrayList<E>(entries);
		list.add(entry);
		return this.constructSequence(list);
	}
	
}
