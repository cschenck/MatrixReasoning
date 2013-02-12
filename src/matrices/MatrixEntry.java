package matrices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import utility.Context;
import utility.Utility;


public class MatrixEntry {
	
	//property name -> property value
	private Map<String, String> properties;
	
	private String name;
	
	private Map<Context, List<double[]>> features = new HashMap<Context, List<double[]>>();
	
	public MatrixEntry(String name)
	{
		this.name = name;
		properties = new HashMap<String, String>();
	}
	
	public void setPropertyValue(String property, String value)
	{
		this.properties.put(property, value);
	}
	
	public String getPropertyValue(String property)
	{
		return this.properties.get(property);
	}
	
	public String getName()
	{
		return name;
	}
	
	public Set<String> getDefinedProperties()
	{
		return this.properties.keySet();
	}
	
	public void removeProperty(String property)
	{
		this.properties.remove(property);
	}
	
	public void setFeatures(Context c, List<double[]> fs)
	{
		this.features.put(c, fs);
	}
	
	public List<double[]> getFeatures(Context c)
	{
		return this.features.get(c);
	}
	
	@Override
	public String toString()
	{
		return this.getName();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof MatrixEntry))
			return false;
		
		MatrixEntry me = (MatrixEntry) obj;
		
		if(me.getName().equals(this.getName()) && me != this)
			throw new IllegalStateException("Two different objects are named " + this.getName());
		
		if(me.properties.equals(this.properties) && me != this)
			System.err.println("Warning: Objects " + me.getName() + " and " + this.getName() + " have the same properties.");
		
		return me == this;
	}
	
	@Override
	public int hashCode()
	{
		return this.getName().hashCode();
	}
	
	public static void generateMatrixEntryFile(List<String> orderedProps, Map<String, List<String>> properties, String filepath) throws IOException
	{
		FileWriter fw = new FileWriter(filepath);
		fw.write("object");
		
		for(String prop : orderedProps)
			fw.write("," + prop);
		fw.write("\n");
		fw.flush();
		
		int maxNumValues = 0;
		for(String prop : orderedProps)
		{
			if(properties.get(prop).size() > maxNumValues)
				maxNumValues = properties.get(prop).size();
		}
		
		List<Integer> alphabet = new ArrayList<Integer>();
		for(int i = 0; i < maxNumValues; i++)
			alphabet.add((Integer)i);
		
		List<List<Integer>> objects = Utility.createAllStringsOfSize(alphabet, orderedProps.size());
		for(List<Integer> obj : objects)
		{
			String name = "";
			String props = "";
			
			boolean invalid = false;
			for(int i = 0; i < orderedProps.size(); i++)
			{
				//if the value for this property is not actually a valid value
				if(obj.get(i) >= properties.get(orderedProps.get(i)).size())
				{
					invalid = true;
					break;
				}
				else
				{
					props += "," + properties.get(orderedProps.get(i)).get(obj.get(i));
					name += properties.get(orderedProps.get(i)).get(obj.get(i)) + "_";
				}
			}
			
			//if this object is invalid, then skip it
			if(invalid)
				continue;
			
			//remove the trailing '_'
			name = name.substring(0, name.length() - 1);
			fw.write(name + props + "\n");
			fw.flush();
		}
		
		fw.close();
	}
	
	public static List<MatrixEntry> loadMatrixEntryFile(String filepath) throws FileNotFoundException
	{
		Scanner file = new Scanner(new File(filepath));
		
		Scanner line = new Scanner(file.nextLine());
		line.useDelimiter(",");
		List<String> orderedProps = new ArrayList<String>();
		line.next();
		while(line.hasNext())
			orderedProps.add(line.next());
		
		List<MatrixEntry> ret = new ArrayList<MatrixEntry>();
		while(file.hasNext())
		{
			line = new Scanner(file.nextLine());
			line.useDelimiter(",");
			
			String name = line.next();
			MatrixEntry entry = new MatrixEntry(name);
			for(int i = 0; i < orderedProps.size(); i++)
				entry.setPropertyValue(orderedProps.get(i), line.next());
			ret.add(entry);
		}
		
		return ret;
	}
	
	public static MatrixEntry getObjectWithProperties(Map<String, String> properties, Set<MatrixEntry> objects)
	{
		for(MatrixEntry object : objects)
		{
			if(object.properties.equals(properties))
				return object;
		}
		
		return null;
	}

}













