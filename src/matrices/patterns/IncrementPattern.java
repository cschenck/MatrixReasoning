package matrices.patterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import matrices.MatrixEntry;

public class IncrementPattern implements Pattern {
	
	private String property;
	private List<String> values;
	private Random rand;
	
	public IncrementPattern(String property, List<String> values, Random rand)
	{
		this.property = property;
		this.values = values;
		this.rand = rand;
	}

	@Override
	public boolean detectPattern(List<MatrixEntry> objects) {
		for(int i = 0; i < objects.size() - 1; i++)
		{
			int index1 = values.indexOf(objects.get(i).getPropertyValue(property));
			if(index1 < 0)
				throw new IllegalStateException(objects.get(i).getPropertyValue(property) + " is not in the list " + values.toString());
			
			int index2 = values.indexOf(objects.get(i + 1).getPropertyValue(property));
			if(index2 < 0)
				throw new IllegalStateException(objects.get(i + 1).getPropertyValue(property) + " is not in the list " + values.toString());
			
			if(index1 >= index2) //if the value of the i'th object does not come before the value of the (i+!)'th object
				return false;
		}
		
		//if we made it here, we're good
		return true;
	}

	public List<MatrixEntry> align(List<MatrixEntry> toAlign, Set<MatrixEntry> objectPool) {
		
		List<MatrixEntry> ret = new ArrayList<MatrixEntry>(toAlign);
		
		int index;
		index = rand.nextInt(values.size() - toAlign.size() + 1);
		
		for(int i = 0; i < toAlign.size(); i++)
		{			
			Map<String, String> properties = new HashMap<String, String>();
			for(String property : toAlign.get(i).getDefinedProperties())
				properties.put(property, toAlign.get(i).getPropertyValue(property));
			properties.put(property, values.get(index + i));
			
			MatrixEntry newObject = MatrixEntry.getObjectWithProperties(properties, objectPool);
			if(newObject == null)
				return null;
			else
				ret.set(i, newObject);
		}
		
		return ret;
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
	public String toString()
	{
		return "IncrementPattern:" + this.property;
	}
	
	@Override
	public Set<String> getRelavantProperties() {
		Set<String> ret = new HashSet<String>();
		ret.add(this.property);
		return ret;
	}

}
