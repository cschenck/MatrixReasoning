package featureExtraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import matrices.MatrixEntry;
import utility.Context;
import utility.Modality;
import utility.Utility;

public abstract class FeatureExtractor {
	
	private String dataPath;
	private String featurePath;
	
	public FeatureExtractor(String dataPath, String featurePath)
	{
		this.dataPath = dataPath;
		this.featurePath = featurePath;
	}
	
	protected String getDataPath()
	{
		return dataPath;
	}
	
	protected abstract Modality getModality();
	
	public void assignFeatures(List<MatrixEntry> objects, Set<Context> contexts) throws IOException
	{
		for(Context c : contexts)
		{
			//this is for convenience
			if(!c.modality.equals(this.getModality()))
				continue;
			
			Map<String, List<double[]>> features;
			if(new File(this.featurePath + "/" + c.toString() + ".txt").exists())
				features = loadFeatureFile(c);
			else
			{
				features = generateFeatures(c);
				saveFeaturesFile(c, features);
			}
			
			for(MatrixEntry object : objects)
			{
				if(!features.containsKey(object.getName()))
					throw new IllegalArgumentException("No feature entry for " + object.getName() + " for context " + c.toString());
				
				object.setFeatures(c, features.get(object.getName()));
			}
		}
	}

	private void saveFeaturesFile(Context c, Map<String, List<double[]>> features) throws IOException 
	{
		FileWriter fw = new FileWriter(this.featurePath + "/" + c.toString() + ".txt");
		for(Entry<String, List<double[]>> e : features.entrySet())
		{
			for(int i = 0; i < e.getValue().size(); i++)
			{
				double[] d = e.getValue().get(i);
				fw.write(e.getKey() + "," + i);
				for(double dd : d)
					fw.write("," + dd);
				fw.write("\n");
				fw.flush();
			}
		}
		
		fw.close();
		
	}

	protected abstract Map<String, List<double[]>> generateFeatures(Context c) throws IOException;

	private Map<String, List<double[]>> loadFeatureFile(Context c) throws FileNotFoundException {
		Map<String, List<double[]>> ret = new HashMap<String, List<double[]>>();
		
		Scanner file = new Scanner(new File(this.featurePath + "/" + c.toString() + ".txt"));
		while(file.hasNextLine())
		{
			Scanner line = new Scanner(file.nextLine());
			line.useDelimiter(",");
			//first scan the name
			String name = line.next();
			//next scan the execution number
			int exec = line.nextInt();
			
			//now scan doubles until there aren't anymore
			List<Double> fs = new ArrayList<Double>();
			while(line.hasNextDouble())
				fs.add(line.nextDouble());
			
			if(ret.get(name) == null)
				ret.put(name, new ArrayList<double[]>());
			while(ret.get(name).size() <= exec)
				ret.get(name).add(null);
			double[] d = new double[fs.size()];
			for(int i = 0; i < d.length; i++)
				d[i] = fs.get(i);
			ret.get(name).set(exec, d);
		}
		
		//now check to make sure each object has the correct number of executions
		for(Entry<String, List<double[]>> e : ret.entrySet())
		{
			//first remove any null values
			for(int i = 0; i < e.getValue().size();)
			{
				if(e.getValue().get(i) == null)
					e.getValue().remove(i);
				else
					i++;
			}
			if(e.getValue().size() != FeatureExtractionManager.NUM_EXECUTIONS)
				Utility.debugPrintln("Expected " + FeatureExtractionManager.NUM_EXECUTIONS + " executions for " 
						+ e.getKey() + " in context " + c.toString() + " but found " + e.getValue().size());
		}
		
		return ret;
	}

}








