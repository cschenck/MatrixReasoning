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
 * This pattern takes in a property and then checks
 * to see if all the given objects have a different
 * value for that property. If so, it detects the
 * pattern, if not then not.
 * 
 * @author cschenck
 *
 */
public class DifferentPattern implements Pattern {

	private String property;
	private Set<String> allValues;
	private Random rand;
	
	public DifferentPattern(String property, Set<MatrixEntry> objects, Random rand)
	{
		this.property = property;
		this.allValues = new HashSet<String>();
		this.rand = rand;
		for(MatrixEntry object : objects)
		{
			if(!this.allValues.contains(object.getPropertyValue(property)))
				this.allValues.add(object.getPropertyValue(property));
		}
	}

	@Override
	public boolean detectPattern(List<MatrixEntry> objects) {
		Set<String> values = new HashSet<String>();
		for(MatrixEntry object : objects)
		{
			if(values.contains(object.getPropertyValue(property)))
				return false;
			else
				values.add(object.getPropertyValue(property));
		}
		
		return true;
	}

	@Override
	public List<MatrixEntry> align(List<MatrixEntry> toAlign, Set<MatrixEntry> objectPool) {
		
		//if there are more object to align than there are values for this property, throw an exception
		if(allValues.size() < toAlign.size())
			throw new IllegalArgumentException("There are only " + allValues.size() 
					+ " possible values to assign for property " + property + " but asked to assign different values across "
					+ toAlign.size() + " objects");
		
		List<String> valuesToUse = new ArrayList<String>(allValues); 
		
		List<MatrixEntry> ret = new ArrayList<MatrixEntry>(toAlign);
		
		for(int i = 0; i < toAlign.size(); i++)
		{
			Map<String, String> properties = new HashMap<String, String>();
			for(String property : toAlign.get(i).getDefinedProperties())
				properties.put(property, toAlign.get(i).getPropertyValue(property));
			
			String value = valuesToUse.get(rand.nextInt(valuesToUse.size()));
			
			valuesToUse.remove(value);
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
		return "Different:" + property;
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
