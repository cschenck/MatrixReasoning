package testingStuff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import matrices.MatrixCompletionTask;
import matrices.MatrixEntry;
import matrices.patterns.DecrementPattern;
import matrices.patterns.DifferentPattern;
import matrices.patterns.IncrementPattern;
import matrices.patterns.ORMetaPattern;
import matrices.patterns.OneSameOneDifferentPattern;
import matrices.patterns.Pattern;
import matrices.patterns.SamePattern;
import matrices.patterns.XORMetaPattern;
import featureExtraction.FeatureExtractionManager;

public class PatternCounter {

	public static final String objectsFile = "objects.txt";
	private final static String TASK_CACHE_FILE = "cachedTasks.txt";
	
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
	 */
	public static void main(String[] args) {
		Random rand = new Random(1);
		List<MatrixEntry> objects = initializeObjects(objectsFile, rand);
		List<MatrixCompletionTask> tasks = loadTasksFromFile(objects, rand);

		List<Pattern> rowPatterns = new ArrayList<Pattern>();
		List<Pattern> colPatterns = new ArrayList<Pattern>();
		Map<Pattern, Boolean> validPatterns = new HashMap<Pattern, Boolean>();
		
		intializePatterns(objects, rand, rowPatterns, colPatterns, validPatterns);
		
		Map<Pattern, Integer> patternCounts = new HashMap<Pattern, Integer>();
		Map<Integer, Integer> numPatterns = new HashMap<Integer, Integer>();
		for(MatrixCompletionTask task : tasks)
		{
			//put the matrix into row format
			//we don't actually need to use the last row since if the pattern is present
			//in all the preceeding rows, it'll be present in the last
			List<List<MatrixEntry>> rows = new ArrayList<List<MatrixEntry>>();
			for(int i = 0; i < task.getNumRows() - 1; i++)
				rows.add(task.getRow(i));
			
			int num = 0;
			for(Pattern p : rowPatterns)
			{
				boolean predicate = true;
				for(List<MatrixEntry> row : rows)
				{
					if(!p.detectPattern(row))
						predicate = false;
				}
				if(predicate)
				{
					num++;
					if(patternCounts.get(p) == null)
						patternCounts.put(p, 1);
					else
						patternCounts.put(p, patternCounts.get(p) + 1);
				}
			}
			
			//put the matrix into col format
			List<List<MatrixEntry>> cols = new ArrayList<List<MatrixEntry>>();
			for(int i = 0; i < task.getNumCols() - 1; i++)
				cols.add(task.getCol(i));
			
			for(Pattern p : colPatterns)
			{
				boolean predicate = true;
				for(List<MatrixEntry> col : cols)
				{
					if(!p.detectPattern(col))
						predicate = false;
				}
				if(predicate)
				{
					num++;
					if(patternCounts.get(p) == null)
						patternCounts.put(p, 1);
					else
						patternCounts.put(p, patternCounts.get(p) + 1);
				}
			}
			
			if(numPatterns.get(num) == null)
				numPatterns.put(num, 1);
			else
				numPatterns.put(num, numPatterns.get(num) + 1);
		}
		
		System.out.println(patternCounts.toString());
		System.out.println(numPatterns);
	}
	
	private static void intializePatterns(List<MatrixEntry> objects, Random rand, List<Pattern> rowPatterns,
			List<Pattern> colPatterns, Map<Pattern, Boolean> validPatterns) 
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
	
	private static List<MatrixCompletionTask> loadTasksFromFile(List<MatrixEntry> objects, Random rand)
	{
		List<MatrixCompletionTask> ret = new ArrayList<MatrixCompletionTask>();
		Scanner lines;
		try {
			lines = new Scanner(new File(TASK_CACHE_FILE));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		while(lines.hasNextLine())
		{
			ret.add(MatrixCompletionTask.constructTaskFromSerialString(lines.nextLine(), objects, rand));
		}
		
		return ret;
	}
	
	private static List<MatrixEntry> initializeObjects(String objectFilepath, Random rand)
	{
		try {
			List<MatrixEntry> objects = MatrixEntry.loadMatrixEntryFile(objectFilepath);
			return objects;
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("The objects file was not found at " + objectFilepath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
