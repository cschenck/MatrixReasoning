package taskSolver.comparisonFunctions;

import matrices.MatrixEntry;

public class WeightedComparisonFunction implements ComparisonFunction {
	
	private ComparisonFunction wrapped;
	private double weight;
	
	public WeightedComparisonFunction(ComparisonFunction wrapped, double weight)
	{
		this.wrapped = wrapped;
		this.weight = weight;
	}

	@Override
	public double compare(MatrixEntry obj1, MatrixEntry obj2) {
		return this.wrapped.compare(obj1, obj2)*this.weight;
	}

}
