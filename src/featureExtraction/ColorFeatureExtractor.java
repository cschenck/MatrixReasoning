package featureExtraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import utility.Context;
import utility.Modality;
import utility.RunningMean;
import utility.Tuple;
import utility.Utility;
import weka.attributeSelection.PrincipalComponents;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ColorFeatureExtractor extends FeatureExtractor {
	
	private static final int NUM_HUE_BINS = 8;
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
		
		Map<String, List<Set<Pixel>>> results = new HashMap<String, List<Set<Pixel>>>();
		
		for(File object : new File(this.getDataPath()).listFiles())
		{
			//skip files
			if(!object.isDirectory())
				continue;
			
			results.put(object.getName(), new ArrayList<Set<Pixel>>());
			
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
			
			for(Set<Pixel> fs : results.get(object.getName()))
			{
				if(fs == null)
				{
					results.remove(object.getName());
					Utility.debugPrintln("WARNING! Removing " + object.getName() + " from consideration because it was missing a feature set.");
					break;
				}
			}
		}
		
		Map<String, List<double[]>> ret = computeFeatures(results);
		//ret = removeRedundantFeatures(ret);
		
		//now do PCA
		//reduceDimensionality(ret);
		
		Utility.debugPrintln("Done generating color features for " + c.toString());
		
		return ret;
	}
	
	private Map<String, List<double[]>> removeRedundantFeatures(Map<String, List<double[]>> features)
	{
		Utility.debugPrintln("Removing identical features");
		int numRemoved = 0;
		//let's do some post-processing, if there are any features that are identical for all objects
		//across all trials, let's remove that feature. This is to get rid of image patches over all
		//background (all black)
		Map<String, List<List<Double>>> tempResults = new HashMap<String, List<List<Double>>>();
		int featureLength = features.values().iterator().next().get(0).length;
		for(int i = 0; i < featureLength; i++)
		{
			//first check to see if the i'th feature is identical over all objects/exeuctions
			double value = features.values().iterator().next().get(0)[i];
			boolean foundDiff = false;
			for(List<double[]> list : features.values())
			{
				for(double[] dd : list)
				{
					if(dd[i] != value)
					{
						foundDiff = true;
						break;
					}
				}
				if(foundDiff)
					break;
			}

			//if a difference was found, keep the feature
			if(foundDiff)
			{
				for(Entry<String, List<double[]>> e : features.entrySet())
				{
					if(tempResults.get(e.getKey()) == null)
						tempResults.put(e.getKey(), new ArrayList<List<Double>>());
					for(int j = 0; j < e.getValue().size(); j++)
					{
						while(tempResults.get(e.getKey()).size() <= j)
							tempResults.get(e.getKey()).add(new ArrayList<Double>());
						double d = e.getValue().get(j)[i];
						tempResults.get(e.getKey()).get(j).add(d);
					}
				}
			}
			else
				numRemoved++;
		}

		//now put the features back in the results map
		Map<String, List<double[]>> results = new HashMap<String, List<double[]>>();
		results.clear();
		for(Entry<String, List<List<Double>>> e : tempResults.entrySet())
		{
			List<double[]> list = new ArrayList<double[]>();
			for(List<Double> dList : e.getValue())
			{
				double[] dd = new double[dList.size()];
				for(int i = 0; i < dd.length; i++)
					dd[i] = dList.get(i);
				list.add(dd);
			}
			results.put(e.getKey(), list);
		}
		Utility.debugPrintln("removed " + numRemoved + " redundant features.");
		return results;

	}
	
	private Map<String, List<double[]>> computeFeatures(Map<String, List<Set<Pixel>>> pixels)
	{
		double minx = Double.MAX_VALUE;
		double maxx = -Double.MAX_VALUE;
		double miny = Double.MAX_VALUE;
		double maxy = -Double.MAX_VALUE;
		double minz = Double.MAX_VALUE;
		double maxz = -Double.MAX_VALUE;
		
		for(List<Set<Pixel>> list : pixels.values())
		{
			for(Set<Pixel> set : list)
			{
				for(Pixel p : set)
				{
					if(p.x < minx)
						minx = p.x;
					if(p.x > maxx)
						maxx = p.x;
					if(p.y < miny)
						miny = p.y;
					if(p.y > maxy)
						maxy = p.y;
					if(p.z < minz)
						minz = p.z;
					if(p.z > maxz)
						maxz = p.z;
				}
			}
		}
		
		Pixel max = new Pixel(maxx, maxy, maxz, 0, 0, 0);
		Pixel min = new Pixel(minx, miny, minz, 0, 0, 0);
		
		Map<String, List<double[]>> ret = new HashMap<String, List<double[]>>();
		for(Entry<String, List<Set<Pixel>>> e : pixels.entrySet())
		{
			ret.put(e.getKey(), new ArrayList<double[]>());
			for(Set<Pixel> set : e.getValue())
			{
				ret.get(e.getKey()).add(computeFeatures(set, max, min));
			}
		}
		
		return ret;
	}
	
	private double[] computeFeatures(Set<Pixel> set, Pixel max, Pixel min) {
		RunningMean[][][][] bins = new RunningMean[NUM_HUE_BINS][NUM_SAT_BINS][NUM_VAL_BINS][3];
		
		RunningMean avgx = new RunningMean();
		RunningMean avgy = new RunningMean();
		RunningMean avgz = new RunningMean();
		for(Pixel p : set)
		{
			avgx.addValue(p.x);
			avgy.addValue(p.y);
			avgz.addValue(p.z);
		}
		
		max = new Pixel(avgx.getMean()+3*avgx.getStandardDeviation(), 
				avgy.getMean()+3*avgy.getStandardDeviation(), 
				avgz.getMean()+3*avgz.getStandardDeviation(), 0, 0, 0);
		min = new Pixel(avgx.getMean()-3*avgx.getStandardDeviation(), 
				avgy.getMean()-3*avgy.getStandardDeviation(), 
				avgz.getMean()-3*avgz.getStandardDeviation(), 0, 0, 0);
		
		for(Pixel p : set)
		{
			int i = Math.max(0,Math.min((int) ((p.x - min.x)/(max.x - min.x)*NUM_HUE_BINS), NUM_HUE_BINS - 1));
			int j = Math.max(0,Math.min((int) ((p.y - min.y)/(max.y - min.y)*NUM_SAT_BINS), NUM_SAT_BINS - 1));
			int k = Math.max(0,Math.min((int) ((p.z - min.z)/(max.z - min.z)*NUM_VAL_BINS), NUM_VAL_BINS - 1));
			
			for(int n = 0; n < 3; n++)
			{
				if(bins[i][j][k][n] == null)
					bins[i][j][k][n] = new RunningMean();
			}
			
			bins[i][j][k][0].addValue(p.h);
			bins[i][j][k][1].addValue(p.s);
			bins[i][j][k][2].addValue(p.v);
		}
		
		double[] ret = new double[NUM_HUE_BINS*NUM_SAT_BINS*NUM_VAL_BINS*3];

		int n = 0;
		for(int m = 0; m < 3; m++)
		{
			for(int i = 0; i < NUM_HUE_BINS; i++)
			{
				for(int j = 0; j < NUM_SAT_BINS; j++)
				{
					for(int k = 0; k < NUM_VAL_BINS; k++)
					{
							if(bins[i][j][k][m] == null)
								ret[n] = 0;
							else
								ret[n] = bins[i][j][k][m].getMean();
							n++;
					}
				}
			}
		}
		
		return ret;
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
	
	private class Pixel {
		public final double x, y, z, h, s, v;
		public Pixel(double x, double y, double z, double h, double s, double v)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.h = h;
			this.s = s;
			this.v = v;
		}
	}
	
	private Set<Pixel> extractHistogramFeatures(File input)
	{
		Set<Pixel> ret = new HashSet<Pixel>();
		
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
			double x = line.nextDouble();
			double y = line.nextDouble();
			double z = line.nextDouble();
			double[] hsv = convertRGBToHSV(new double[]{line.nextDouble()/255, line.nextDouble()/255, line.nextDouble()/255});
			ret.add(new Pixel(x,y,z,hsv[0],hsv[1],hsv[2]));
			count++;
		}
		System.out.println(count + " = " + input.getAbsolutePath());
		
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
