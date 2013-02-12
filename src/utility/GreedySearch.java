package utility;

import java.util.HashSet;
import java.util.Set;

public class GreedySearch<T> {

	public interface SearchCallback<T> {
		/**
		 * Takes a state and returns all states that can be reached
		 * in one hop from this state.
		 * 
		 * @param state	
		 * @return
		 */
		public Set<T> getConnectedStates(T state);
		
		/**
		 * Returns an estimate of the number of hops from the given
		 * state to a goal state. If the return value is 0, then
		 * the given state is assumed to be a goal state.
		 * 
		 * @param state
		 * @return
		 */
		public double distanceToGoal(T state);
	}
	
	private SearchCallback<T> callback;
	
	public GreedySearch(SearchCallback<T> callback)
	{
		this.callback = callback;
	}
	
	public T findGoal(T start)
	{
		Set<T> exploredStates = new HashSet<T>();
		MinHeap<StateDistancePair> frontier = new MinHeap<StateDistancePair>();
		
		frontier.add(new StateDistancePair(callback.distanceToGoal(start), start));
		
		int steps = 0;
		while(!frontier.isEmpty())
		{
			StateDistancePair sdp = frontier.removeMin();
			
			//if we're at a goal state, return it
			if(sdp.estimatedDistance == 0)
			{
				Utility.debugPrintln("found solution in " + steps + " steps.");
				return sdp.state;
			}
			
			//check to see if we've been here before
			if(exploredStates.contains(sdp.state))
				continue;
			
			//now grab all the adjacent states
			Set<T> states = callback.getConnectedStates(sdp.state);
			for(T state : states)
			{
				//if we've never been to this state before, then add it to the frontier
				if(!exploredStates.contains(state))
					frontier.add(new StateDistancePair(callback.distanceToGoal(state), state));
			}
			
			exploredStates.add(sdp.state);
			steps++;
			
			//TODO debug
			if(steps % 100000 == 0)
				System.out.println(steps + ":estimated distance to goal: " + sdp.estimatedDistance);
		}
		
		return null;
	}
	
	private class StateDistancePair implements Comparable<StateDistancePair>
	{
		public final double estimatedDistance;
		public final T state;
		
		public StateDistancePair(double est, T state)
		{
			this.estimatedDistance = est;
			this.state = state;
		}

		@Override
		public int compareTo(StateDistancePair o) {
			if(this.estimatedDistance < o.estimatedDistance)
				return -1;
			else if (this.estimatedDistance > o.estimatedDistance)
				return 1;
			else
				return 0;
		}
	}
	
}
