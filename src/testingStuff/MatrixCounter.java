package testingStuff;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import matrices.Matrix;
import matrices.MatrixEntry;
import matrices.patterns.DecrementPattern;
import matrices.patterns.DifferentPattern;
import matrices.patterns.IncrementPattern;
import matrices.patterns.ORMetaPattern;
import matrices.patterns.OneSameOneDifferentPattern;
import matrices.patterns.Pattern;
import matrices.patterns.SamePattern;
import matrices.patterns.XORMetaPattern;
import utility.RunningMean;
import utility.Utility;

public class MatrixCounter {
	
	public static final String objectsFile = "objects.txt";
	
	private final static Map<String, List<String>> ORDERED_PROPERTIES = DEFINE_PROPERTIES(); 
	
	private static Map<String, List<String>> DEFINE_PROPERTIES()
	{
		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		
		//weight, with light, medium, heavy
		List<String> values = new ArrayList<String>();
		values.add("light"); values.add("medium"); values.add("heavy");
		ret.put("weight", values);
		
		return ret;
	}

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile(objectsFile);
		RunningMean ratio = new RunningMean();
		Random rand = new Random(1);
		
		Set<Pattern> rowPatterns = new HashSet<Pattern>();
		Set<Pattern> colPatterns = new HashSet<Pattern>();
		Map<Pattern, Boolean> validPatterns = new HashMap<Pattern, Boolean>();
		
		intializePatterns(objects, rand, rowPatterns, colPatterns, validPatterns);
		
		double numMats = Utility.numberOfChoices(36, 9).multiply(Utility.factorial(9)).doubleValue();
		System.out.println("Total number of matrices = " + Utility.doubleToStringWithCommas(numMats));
		
		while(true)
		{
			Matrix m = generateMatrix(objects, rand);
			if(m.isValidMatrix(rowPatterns, colPatterns, validPatterns))
				ratio.addValue(1.0);
			else
				ratio.addValue(0.0);
			
			if(ratio.getN() % 1000000 == 0)
			{
				double stderr = Math.sqrt((ratio.getMean()*(1 - ratio.getMean()))/(ratio.getN()));
				System.out.println(ratio.getN() + ", " + ratio.getMean() + ", " + stderr + ", [" + 
						Utility.doubleToStringWithCommas(Math.floor(numMats*(ratio.getMean() - 1.96*stderr))) 
						+ ", " + 
						Utility.doubleToStringWithCommas(Math.floor(numMats*(ratio.getMean() + 1.96*stderr)))
						+ "]");
			}
		}

	}
	
	private static Matrix generateMatrix(List<MatrixEntry> objects, Random rand)
	{
		Matrix ret = new Matrix(3, 3);
		for(int i = 0; i < ret.getNumRows(); i++)
		{
			for(int j = 0; j < ret.getNumCols(); j++)
			{
				MatrixEntry obj = null;
				do
				{
					obj = objects.get(rand.nextInt(objects.size()));
				} while(ret.contains(obj));
				ret.setEntry(i, j, obj);
			}
		}
		
		return ret;
	}
	
	private static void intializePatterns(List<MatrixEntry> objects, Random rand, Set<Pattern> rowPatterns,
			Set<Pattern> colPatterns, Map<Pattern, Boolean> validPatterns) 
	{		
		for(String property: objects.get(0).getDefinedProperties())
		{
			Pattern sp = new SamePattern(property, rand);
			Pattern osod = new OneSameOneDifferentPattern(property, rand);
			rowPatterns.add(sp); rowPatterns.add(osod);
			colPatterns.add(sp); colPatterns.add(osod);
			validPatterns.put(sp, true); validPatterns.put(osod, false);
			
			if(ORDERED_PROPERTIES.keySet().contains(property))
			{
				Pattern inc = new IncrementPattern(property, ORDERED_PROPERTIES.get(property), rand);
				Pattern dec = new DecrementPattern(property, ORDERED_PROPERTIES.get(property), rand);
				Pattern xor = new XORMetaPattern(new DifferentPattern(property, new HashSet<MatrixEntry>(objects), rand), 
						new ORMetaPattern(dec, inc));
				
				rowPatterns.add(inc); rowPatterns.add(dec); rowPatterns.add(xor);
				colPatterns.add(inc); colPatterns.add(dec); colPatterns.add(xor);
				validPatterns.put(inc, true); validPatterns.put(dec, true); validPatterns.put(xor, false); 
			}
			else
			{
				Pattern dp = new DifferentPattern(property, new HashSet<MatrixEntry>(objects), rand);
				rowPatterns.add(dp); 
				colPatterns.add(dp); 
				validPatterns.put(dp, true); 
			}
			
		}
	}

}
