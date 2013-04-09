package matrices.patterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import matrices.MatrixEntry;

/**
 * This class detects the pattern over a given property that is:
 * -at least two objects in a given set of objects have the same value for that property
 * -at least two objects in a given set of objects have different values for that property
 * 
 * @author cschenck
 *
 */
public class OneSameOneDifferentPattern implements Pattern {
	
	private String property;
	private Random rand;
	
	public OneSameOneDifferentPattern(String property, Random rand)
	{
		this.property = property;
		this.rand = rand;
	}

	@Override
	public boolean detectPattern(List<MatrixEntry> objects) {
		boolean same = false;
		boolean different = false;
		
		for(int i = 0; i < objects.size(); i++)
		{
			MatrixEntry obj1 = objects.get(i);
			for(int j = i + 1; j < objects.size(); j++)
			{
				MatrixEntry obj2 = objects.get(j);
				
				if(obj1.getPropertyValue(property).equals(obj2.getPropertyValue(property)))
					same = true;
				else
					different = true;
			}
		}
		
		return same && different;
	}

	public List<MatrixEntry> align(List<MatrixEntry> toAlign, Set<MatrixEntry> objectPool) {
		throw new UnsupportedOperationException("OneSameOneDifferent pattern does not align objects");
	}
	
	@Override
	public String toString()
	{
		return "OneSameOneDifferent:" + property;
	}
	
	public List<MatrixEntry> disalign(List<MatrixEntry> toAlter, Set<MatrixEntry> objectPool) {
		
		List<MatrixEntry> ret = new ArrayList<MatrixEntry>(toAlter);
		
		while(this.detectPattern(ret))
		{
			int index = rand.nextInt(ret.size());
			String value = "";
			do
			{
				//because fuck good coding practices
				value = objectPool.toArray(new MatrixEntry[objectPool.size()])[rand.nextInt(objectPool.size())].getPropertyValue(property);
			} while(value.equals(ret.get(index).getPropertyValue(this.property)));
			
			Map<String, String> properties = new HashMap<String, String>();
			for(String property : ret.get(index).getDefinedProperties())
				properties.put(property, ret.get(index).getPropertyValue(property));
			properties.put(property, value);
			
			MatrixEntry newObject = MatrixEntry.getObjectWithProperties(properties, objectPool);
			if(newObject == null)
				return null;
			else
				ret.set(index, newObject);
		}
		
		return ret;
		
	}
	
	@Override
	public Set<String> getRelavantProperties() {
		Set<String> ret = new HashSet<String>();
		ret.add(this.property);
		return ret;
	}

	@Override
	public String getPatternName() {
		return "OneSameOneDifferent";
	}

}
