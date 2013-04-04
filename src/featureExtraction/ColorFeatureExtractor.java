package featureExtraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;

import utility.Context;
import utility.Modality;
import utility.Tuple;
import utility.Utility;
import weka.attributeSelection.PrincipalComponents;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ColorFeatureExtractor extends FeatureExtractor {
	
	private static final int NUM_HUE_BINS = 32;
	private static final int NUM_SAT_BINS = 8;
	private static final int NUM_VAL_BINS = 1;
	private static double PCA_VARIANCE_COVERED = 0.95;

	public ColorFeatureExtractor(String dataPath, String featurePath, Random rand) {
		super(dataPath, featurePath);
	}
	

	@Override
	protected Modality getModality() {
		return Modality.color;
	}

	@Override
	protected Map<String, List<double[]>> generateFeatures(Context c) throws IOException {
		
		Utility.debugPrintln("generating features for " + c.toString());
		
		Map<String, List<double[]>> results = new HashMap<String, List<double[]>>();
		
		for(File object : new File(this.getDataPath()).listFiles())
		{
			//skip files
			if(!object.isDirectory())
				continue;
			
			results.put(object.getName(), new ArrayList<double[]>());
			
			for(File execution : new File(object.getAbsolutePath() + "/trial_1").listFiles())
			{
				//read the execution number off the execution file name, and subtract 1 to make it 0-based
				int execNum = Integer.parseInt(execution.getName().substring(execution.getName().lastIndexOf("_") + 1)) - 1;
				
				//make a spot in the results for this data
				while(results.get(object.getName()).size() <= execNum)
					results.get(object.getName()).add(null);
				
				File dataFile = new File(execution.getAbsolutePath() + "/" + c.behavior.toString() + "/clouds/object_cloud.txt");
				if(!dataFile.exists())
				{
					Utility.debugPrintln("WARNING! Cannot find file " + dataFile.getAbsolutePath());
					continue;
				}
				
				results.get(object.getName()).set(execNum, extractHistogramFeatures(dataFile));
			}
			
			for(double[] fs : results.get(object.getName()))
			{
				if(fs == null)
				{
					results.remove(object.getName());
					Utility.debugPrintln("WARNING! Removing " + object.getName() + " from consideration because it was missing a feature set.");
					break;
				}
			}
		}
		
		//now do PCA
		//reduceDimensionality(results);
		
		Utility.debugPrintln("Done generating color features for " + c.toString());
		
		return results;
	}
	
	private void reduceDimensionality(Map<String, List<double[]>> features)
	{
		Utility.debugPrintln("computing Principal Components");
		
		Tuple<Map<String, List<Instance>>,Instances> temp = createInstances(features);
		Map<String, List<Instance>> map = temp.a;
		Instances data = temp.b;
		
		PrincipalComponents pca = new PrincipalComponents();
		pca.setVarianceCovered(PCA_VARIANCE_COVERED);
		try {
			pca.buildEvaluator(data);
			for(String obj : features.keySet())
			{
				for(int i = 0; i < features.get(obj).size(); i++)
				{
					features.get(obj).set(i, pca.convertInstance(map.get(obj).get(i)).toDoubleArray());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Utility.debugPrintln("done computing Principal Components");
	}
	
	private Tuple<Map<String, List<Instance>>,Instances> createInstances(Map<String, List<double[]>> features)
	{
		int numFeatures = features.values().iterator().next().get(0).length;
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(int i = 0; i < numFeatures; i++)
		{
			Attribute attribute = new Attribute("" + i);
			attributes.add(attribute);
		}
		
		Instances data = new Instances("data", attributes, 1);
		Map<String, List<Instance>> map = new HashMap<String, List<Instance>>();
		for(Entry<String, List<double[]>> e : features.entrySet())
		{
			map.put(e.getKey(), new ArrayList<Instance>());
			for(double[] fs : e.getValue())
			{				
				Instance dataPoint = new DenseInstance(attributes.size());
				for(int i = 0; i < fs.length; i++)
					dataPoint.setValue(attributes.get(i), fs[i]);
				
				dataPoint.setDataset(data);
				data.add(dataPoint);
				map.get(e.getKey()).add(dataPoint);
			}
		}
		
		return new Tuple<Map<String,List<Instance>>, Instances>(map, data);
	}
	
	private double[] extractHistogramFeatures(File input)
	{
		double[][][] hist = new double[NUM_HUE_BINS][NUM_SAT_BINS][NUM_VAL_BINS];
		
		Scanner scan = null;
		try {
			scan = new Scanner(input);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		int count = 0;
		while(scan.hasNextLine())
		{
			Scanner line = new Scanner(scan.nextLine().replace(",", ""));
			//scan off xyz
			line.next(); line.next(); line.next();
			
			//scan out rgb
			double[] rgb = new double[]{line.nextDouble()/255.0, line.nextDouble()/255.0, line.nextDouble()/255.0};
			double[] hsv = convertRGBToHSV(rgb);
			hist[(int) (hsv[0]*NUM_HUE_BINS)][(int) (hsv[1]*NUM_SAT_BINS)][(int) (hsv[2]*NUM_VAL_BINS)] += 1.0;
			count++;
		}
		System.out.println(count + " = " + input.getAbsolutePath());
		
		double[] ret = new double[NUM_HUE_BINS*NUM_SAT_BINS*NUM_VAL_BINS];
		
		int n = 0;
		for(int i = 0; i < NUM_HUE_BINS; i++)
		{
			for(int j = 0; j < NUM_SAT_BINS; j++)
			{
				for(int k = 0; k < NUM_VAL_BINS; k++)
				{
					ret[n] = hist[i][j][k];
					n++;
				}
			}
		}
		
		//first normalize this thing
		double sum = 0;
		for(double d : ret)
			sum += d;
		for(int i = 0; i < ret.length; i++)
			ret[i] /= sum;
		
		return ret;
	}
	
	
	
	private double[] convertRGBToHSV(double[] rgb)
	{
		//let's try HSV
		double r =  rgb[0];
		double g =  rgb[1];
		double b =  rgb[2];
		double max = Math.max(Math.max(r, g), b);
		double min = Math.min(Math.min(r, g), b);
		double h, s, v;
		
		if(max == 0)
			h = 0;
		else if(max - min == 0)
			h = 0;
		else if(r == max)
			h = (g - b)/(max - min);
		else if(g == max)
			h = 2 + (b - r)/(max - min);
		else
			h = 4 + (r - g)/(max - min);
		h *= 60;
		if(h < 0)
			h += 360;
		h /= 360;
			
		s = (max == 0 ? 0 : (max -  min)/max);
		
		v = max;
		
		return new double[]{h, s, v};

	}
	
	

}
