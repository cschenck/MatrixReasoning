package testingStuff;

import java.util.HashSet;
import java.util.Set;

import utility.GreedySearch;
import utility.GreedySearch.SearchCallback;
import utility.Utility;

public class testGreedySearch {

	private static class Callback implements SearchCallback<double[]>
	{
		private double[] goal;
		
		public Callback(double[] goal)
		{
			this.goal = goal;
		}

		@Override
		public Set<double[]> getConnectedStates(double[] state) {
			Set<double[]> ret = new HashSet<double[]>();
			
			for(int i = 0; i < state.length; i++)
			{
				double[] d1 = new double[state.length];
				double[] d2 = new double[state.length];
				for(int j = 0; j < state.length; j++)
				{
					d1[j] = state[j];
					d2[j] = state[j];
				}
				
				d1[i] = state[i] - 1;
				d2[i] = state[i] + 1;
				ret.add(d1);
				ret.add(d2);
			}
			
			return ret;
		}

		@Override
		public double distanceToGoal(double[] state) {
			double dis = 0;
			for(int i = 0; i < state.length; i++)
				dis += Math.abs(state[i] - goal[i]);
			
			return dis;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double[] goal = {0,0,0};
		double[] start = {-98368,987,-100};
		
		Callback cb = new Callback(goal);
		GreedySearch<double[]> gs = new GreedySearch<double[]>(cb);
		
		double[] solution = gs.findGoal(start);
		for(double d : solution)
			System.out.print(d + ",");
		System.out.println();
	}

}
