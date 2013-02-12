package matrices.patterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import matrices.MatrixEntry;

/**
 * 
 * This class takes a property to look at and checks to see if that
 * property is the same across all the objects.
 * 
 * @author cschenck
 *
 */
public class SamePattern implements Pattern {
	
	private String property;
	private Random rand;
	
	public SamePattern(String property, Random rand)
	{
		this.property = property;
		this.rand = rand;
	}

	@Override
	public boolean detectPattern(List<MatrixEntry> objects) {
		String value = objects.get(0).getPropertyValue(property);
		for(MatrixEntry object : objects)
		{
			if(!object.getPropertyValue(property).equals(value))
				return false;
		}
		
		return true;
	}

	@Override
	public List<MatrixEntry> align(List<MatrixEntry> toAlign, Set<MatrixEntry> objectPool) {
		String value = toAlign.get(rand.nextInt(toAlign.size())).getPropertyValue(property);
		
		List<MatrixEntry> ret = new ArrayList<MatrixEntry>(toAlign);
		
		for(int i = 0; i < toAlign.size(); i++)
		{
			Map<String, String> properties = new HashMap<String, String>();
			for(String property : toAlign.get(i).getDefinedProperties())
				properties.put(property, toAlign.get(i).getPropertyValue(property));
			properties.put(property, value);
			
			MatrixEntry newObject = MatrixEntry.getObjectWithProperties(properties, objectPool);
			if(newObject == null)
				return null;
			else
				ret.set(i, newObject);
		}
		
		return ret;
	}
	
	@Override
	public String toString()
	{
		return "Same:" + property;
	}

	@Override
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

}
