package testingStuff.reasonerInterface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SequenceCompletionTask<T, E> {
	
	private Sequence<T,E> givenSequence;
	private Set<E> possibleSolutions;
	private E correctSolution;
	
	public SequenceCompletionTask(Sequence<T,E> givenSequence, Set<E> possibleSolutions, E correctSolution)
	{
		this.givenSequence = givenSequence;
		this.possibleSolutions = new HashSet<E>(possibleSolutions);
		this.correctSolution = correctSolution;
	}
	
	

}
