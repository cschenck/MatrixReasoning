package taskSolver.comparisonFunctions;

import java.util.List;

import matrices.MatrixEntry;

public class CheatingComparator implements ComparisonFunction {
	
	private String property;
	private List<String> values = null;
	
	public CheatingComparator(String property)
	{
		this.property = property;
	}
	
	public CheatingComparator(String property, List<String> values)
	{
		this.property = property;
	}

	@Override
	public double compare(MatrixEntry obj1, MatrixEntry obj2) {
		if(values == null)
		{
			if(obj1.getPropertyValue(property).equals(obj2.getPropertyValue(property)))
				return 0;
			else
				return 1;
		}
		else
		{
			int index1 = values.indexOf(obj1.getPropertyValue(property));
			if(index1 < 0)
				throw new IllegalStateException(obj1.getPropertyValue(property) + " is not in the list " + values.toString());
			
			int index2 = values.indexOf(obj2.getPropertyValue(property));
			if(index2 < 0)
				throw new IllegalStateException(obj2.getPropertyValue(property) + " is not in the list " + values.toString());
			
			//we want the range to be [0,1], so first get it to be [-1,1] then shift up by 1 and divide by 2
			return (1.0 + 1.0*(index2 - index1)/(values.size() - 1))/2.0;
		}
	}

}
