package testingStuff;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import utility.RunningMean;
import utility.Tuple;
import utility.Utility;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ResultsProcessor {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
//		aggregateResults("results/results.txt");
		processResults("results/aggregateResults.txt");
	}
	
	private static void writeOutTable(PrintStream out, Instances data, Attribute xAxis, int startX, int endX, 
			Attribute zAxis, int startZ, int endZ, Map<Attribute, Integer> values, boolean printStds, boolean kappa)
	{
		Attribute accuracy = null;
		Attribute stddev = null;
		Attribute candidates = null;
		for(int i = 0; i < data.numAttributes(); i++)
		{
			Attribute a = data.attribute(i);
			if(a.name().equalsIgnoreCase("ACCURACY"))
				accuracy = a;
			else if(a.name().equalsIgnoreCase("STD_DEV"))
				stddev = a;
			else if(a.name().equalsIgnoreCase("NUM_CANDIDATES"))
				candidates = a;
		}
		
		int xDim = endX - startX + 1;
		int zDim = (endZ - startZ + 1)*(printStds ? 3 : 1);
		
		double[][] table = new double[xDim][zDim];
		for(Instance inst : data)
		{
			//first determine whether or not this instance belongs in the table
			boolean check = true;
			for(Entry<Attribute, Integer> e : values.entrySet())
			{
				if(e.getValue().intValue() != ((int)inst.value(e.getKey())))
					check = false;
			}
			if(((int)inst.value(xAxis)) < startX || ((int)inst.value(xAxis)) > endX)
				check = false;
			if(((int)inst.value(zAxis)) < startZ || ((int)inst.value(zAxis)) > endZ)
				check = false;
			if(!check) //this datapoint is not part of the table
				continue;
			
			//ok, now that we know this datapoint belongs in the table, lets find its coords
			int xPos = ((int)inst.value(xAxis)) - startX;
			int zPos = ((int)inst.value(zAxis)) - startZ;
			if(printStds)
				zPos *= 3;
			
			double acc = inst.value(accuracy);
			double std = inst.value(stddev);
			if(kappa)
			{
				int numCandidates = ((int)inst.value(candidates));
				acc = (acc - 1.0/numCandidates)/(1.0 - 1.0/numCandidates);
				std = std/(1.0 - 1.0/numCandidates);
				table[xPos][zPos] = acc;
				if(printStds)
				{
					table[xPos][zPos+1] = Math.max(acc - std, -1.0);
					table[xPos][zPos+2] = Math.min(acc + std, 1.0);
				}
			}
			else
			{
				acc *= 100;
				std *= 100;
				table[xPos][zPos] = acc;
				if(printStds)
				{
					table[xPos][zPos+1] = Math.max(acc - std, 0.0);
					table[xPos][zPos+2] = Math.min(acc + std, 100.0);
				}
			}
		}
		
		List<String> rowHeaders = new ArrayList<String>();
		for(int i = startX; i <= endX; i++)
		{
			rowHeaders.add(i + "");
		}
		
		List<String> colHeaders = new ArrayList<String>();
		for(int i = startZ; i <= endZ; i++)
		{
			String s;
			if(zAxis.isNumeric())
				s = i + "";
			else
				s = zAxis.value(i);
			colHeaders.add(s);
			if(printStds)
			{
				colHeaders.add(s + "-std");
				colHeaders.add(s + "+std");
			}
		}
		
		PrintStream temp = System.out;
		System.setOut(out);
		Utility.printTable(colHeaders, rowHeaders, table, false);
		System.setOut(temp);
	}
	
	private static void processResults(String filename) throws FileNotFoundException, IOException
	{
		Instances data = new Instances(new FileReader(filename));
		Enumeration iter = data.enumerateAttributes();
		while(iter.hasMoreElements())
			System.out.println(iter.nextElement().toString());
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(int i = 0; i < data.numAttributes() - 2; i++) //exclude standard dev and accuracy from consideration
			attributes.add(data.attribute(i));
		
		Tuple<Attribute, Tuple<Integer, Integer>> xAxis = selectAxisAndRange("x", attributes, data);
		attributes.remove(xAxis.a);
		Tuple<Attribute, Tuple<Integer, Integer>> zAxis = selectAxisAndRange("z", attributes, data);
		attributes.remove(zAxis.a);
		
		Map<Attribute, Integer> values = new HashMap<Attribute, Integer>();
		for(Attribute att : attributes)
		{
			List<String> choices = new ArrayList<String>();
			if(!att.isNumeric())
			{
				for(int i = 0; i < att.numValues(); i++)
					choices.add(att.value(i));
				int choice = choices.indexOf(presentOptions("Please select a value for " + att.name() + ":", choices));
				values.put(att, choice);
			}
			else
			{
				double max = -Double.MAX_VALUE;
				double min = Double.MAX_VALUE;
				for(Instance inst : data)
				{
					double value = inst.value(att);
					if(value > max)
						max = value;
					if(value < min)
						min = value;
				}
				for(int i = (int) min; i <= max; i++)
					choices.add(i + "");
				int choice = (int) (choices.indexOf(presentOptions("Please select a value for " + att.name() + ":", choices)) + min);
				values.put(att, choice);
			}
			
		}
		
		boolean printStds = presentOptions("Print standard deviations?", 
				Utility.convertToList(new String[]{"No","Yes"})).equals("Yes");
		boolean kappa = presentOptions("Print accuracy or kappa?", 
				Utility.convertToList(new String[]{"Accuracy","Kappa"})).equals("Kappa");
		
		writeOutTable(System.out, data, xAxis.a, xAxis.b.a, xAxis.b.b, zAxis.a, zAxis.b.a, zAxis.b.b, values, printStds, kappa);
	}
	
	private static Tuple<Attribute, Tuple<Integer, Integer>> selectAxisAndRange(String axis, 
			List<Attribute> attributes, Instances data)
	{
		Attribute xAxis = presentOptions("Please select an attribute for the " + axis + "-axis:", attributes);
		attributes.remove(xAxis);
		int start, end;
		if(!xAxis.isNumeric())
		{
			List<String> values = new ArrayList<String>();
			for(int i = 0; i < xAxis.numValues(); i++)
				values.add(xAxis.value(i));
			String startValue = presentOptions("Which value for " + xAxis.name() + " would you like to start at?", 
					values.subList(0, values.size() - 1));
			String endValue = presentOptions("Which value would you like to end at?", 
					values.subList(values.indexOf(startValue) + 1, values.size()));
			start = values.indexOf(startValue);
			end = values.indexOf(endValue);
		}
		else
		{
			double max = -Double.MAX_VALUE;
			double min = Double.MAX_VALUE;
			for(Instance inst : data)
			{
				double value = inst.value(xAxis);
				if(value > max)
					max = value;
				if(value < min)
					min = value;
			}
			start = (int) min;
			end = (int) max;
		}
		
		return new Tuple<Attribute, Tuple<Integer,Integer>>(xAxis, new Tuple<Integer, Integer>(start, end));
	}
	
	private static <T> T presentOptions(String prompt, List<T> options)
	{
		System.out.println(prompt);
		
		for(int i = 1; i <= options.size(); i++)
			System.out.println("(" + i + ") " + options.get(i - 1).toString());
		System.out.print(">");
		
		Scanner scan = new Scanner(System.in);
		int choice;
		do
		{
			choice = scan.nextInt();
		} while(choice <= 0 && choice > options.size());
		return options.get(choice - 1);
	}
	
	private static void aggregateResults(String filename) throws FileNotFoundException, IOException
	{
		Instances allData = new Instances(new FileReader(filename));
		Map<HashArray, RunningMean> means = new HashMap<ResultsProcessor.HashArray, RunningMean>();
		int count = 0;
		System.out.println("Beginning aggregation");
		for(Instance inst : allData)
		{
			double[] dd = inst.toDoubleArray();
			HashArray ha = new HashArray(dd, dd.length - 2); //don't use the last two values
			if(means.get(ha) == null)
				means.put(ha, new RunningMean());
			means.get(ha).addValue(dd[dd.length - 2]);
			count++;
			if(count % 100000 == 0)
				System.out.println(count);
		}
		
		System.out.println("Aggregating results");
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(int i = 0; i < allData.numAttributes(); i++)
			attributes.add(allData.attribute(i));
		Instances aggregate = new Instances("aggregateResults", attributes, means.size());
		
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		int maxCount = 0;
		for(Entry<HashArray, RunningMean> e : means.entrySet())
		{
			Instance inst = new DenseInstance(attributes.size());
			inst.setDataset(aggregate);
			for(int i = 0; i < e.getKey().array.length; i++)
			{
				if(attributes.get(i).isNumeric())
					inst.setValue(attributes.get(i), e.getKey().array[i]);
				else
					inst.setValue(attributes.get(i), attributes.get(i).value((int) e.getKey().array[i]));
			}
			inst.setValue(attributes.get(attributes.size() - 2), e.getValue().getMean());
			inst.setValue(attributes.get(attributes.size() - 1), e.getValue().getStandardDeviation());
			aggregate.add(inst);
			if(counts.get(e.getValue().getN()) == null)
				counts.put(e.getValue().getN(), 1);
			else
				counts.put(e.getValue().getN(), counts.get(e.getValue().getN()) + 1);
			if(e.getValue().getN() > maxCount)
				maxCount = e.getValue().getN();
		}
		
		FileWriter fw = new FileWriter("results/aggregateResults.txt");
		fw.write(aggregate.toString());
		fw.flush();
		fw.close();
		System.out.println("Done");
		for(int i = 1; i <= maxCount; i++)
		{
			if(counts.get(i) != null)
				System.out.println(i + " => " + counts.get(i));
		}
	}
	
	private static class HashArray
	{
		public final double[] array;
		
		public HashArray(double[] d, int length)
		{
			double[] temp = new double[length];
			System.arraycopy(d, 0, temp, 0, length);
			array = temp;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if(!(obj instanceof HashArray))
				return false;
			HashArray ha = (HashArray) obj;
			
			if(ha.array.length != this.array.length)
				return false;
			
			for(int i = 0; i < this.array.length; i++)
			{
				if(ha.array[i] != this.array[i])
					return false;
			}
			
			return true;
		}
		
		@Override
		public int hashCode()
		{
			double ret = 1;
			for(double d : array)
				ret += d;
			return (int) ret;
		}
	}

}









