package taskSolver.comparisonFunctions;

import java.util.List;

import matrices.MatrixEntry;

import org.apache.commons.math3.distribution.NormalDistribution;

import taskSolver.comparisonFunctions.CheatingComparator;

public class NoisyCheatingComparator extends CheatingComparator {
	
	private NormalDistribution noise = null;

	public NoisyCheatingComparator(String property, double stddev) {
		super(property);
		
		if(stddev > 0.0)
			noise = new NormalDistribution(0, stddev);
	}

	public NoisyCheatingComparator(String property, List<String> values, double stddev) {
		super(property, values);
		
		if(stddev > 0.0)
			noise = new NormalDistribution(0, stddev);
	}
	
	@Override
	public double compare(MatrixEntry obj1, MatrixEntry obj2) {
		if(noise == null)
			return super.compare(obj1, obj2);
		else
		{
			double ret = super.compare(obj1, obj2) + noise.sample();
			if(ret < 0.0)
				ret = 0.0;
			else if(ret > 1.0)
				ret = 1.0;
			return ret;
		}
	}

}
