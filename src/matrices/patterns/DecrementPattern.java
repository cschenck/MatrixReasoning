package matrices.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import matrices.MatrixEntry;

public class DecrementPattern implements Pattern {
	
	private IncrementPattern wrapped;
	private String property;
	
	public DecrementPattern(String property, List<String> values, Random rand)
	{
		//reverse the list
		List<String> reversed = new ArrayList<String>();
		for(int i = values.size() - 1; i >= 0; i--)
			reversed.add(values.get(i));
		
		this.property = property;
		
		wrapped = new IncrementPattern(property, reversed, rand);
	}

	@Override
	public boolean detectPattern(List<MatrixEntry> objects) {
		return wrapped.detectPattern(objects);
	}

	@Override
	public List<MatrixEntry> align(List<MatrixEntry> toAlign, Set<MatrixEntry> objectPool) {
		return wrapped.align(toAlign, objectPool);
	}

	@Override
	public List<MatrixEntry> disalign(List<MatrixEntry> toAlter, Set<MatrixEntry> objectPool) {
		return wrapped.disalign(toAlter, objectPool);
	}
	
	@Override
	public String toString()
	{
		return "DecrementPattern:" + this.property;
	}

}
