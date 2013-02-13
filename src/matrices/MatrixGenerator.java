package matrices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import matrices.patterns.OneSameOneDifferentPattern;
import matrices.patterns.Pattern;
import utility.Utility;

public class MatrixGenerator {
	
	public final static int NUM_ROWS = 3;
	public final static int NUM_COLS = 3;  
	
	private static class PatternAligner
	{
		private class TempMatrix {
			int[][] values = new int[NUM_ROWS][NUM_COLS];
			String property;
			public int hashCode()
			{
				int ret = property.hashCode();
				for(int i = 0; i < values.length; i++)
				{
					for(int j = 0; j < values[i].length; j++)
						ret *= values[i][j];
				}
				return ret;
			}
			public boolean equals(Object obj)
			{
				if(!(obj instanceof TempMatrix))
					return false;
				TempMatrix tm = (TempMatrix) obj;
				if(!tm.property.equals(property))
					return false;
				for(int i = 0; i < values.length; i++)
				{
					for(int j = 0; j < values[i].length; j++)
					{
						if(tm.values[i][j] != values[i][j])
							return false;
					}
				}
				return true;
			}
		}
		//a map of properties to a list of all configurations of those values that are valid
		private Map<String, Set<TempMatrix>> validMatrices;
		//a map from row/col patterns to a list of matrix configurations that are valid and contain that pattern
		private Map<Pattern, Set<TempMatrix>> patternPresentRowMatrices;
		private Map<Pattern, Set<TempMatrix>> patternPresentColMatrices;
		
		private Map<String, List<String>> propertyValues;
		
		public void generateValidMatrices(Map<String, List<String>> propertyValues, List<MatrixEntry> objects, 
				Set<Pattern> rowPatterns, Set<Pattern> colPatterns, Map<Pattern, Boolean> validPatterns)
		{
			Utility.debugPrintln("Beginning generation of valid matrices");
			
			this.propertyValues = propertyValues;
			
			validMatrices = new HashMap<String, Set<TempMatrix>>();
			patternPresentRowMatrices = new HashMap<Pattern, Set<TempMatrix>>();
			patternPresentColMatrices = new HashMap<Pattern, Set<TempMatrix>>();
			
			//iterate over each property and find all the valid matrices for that property
			for(String property : propertyValues.keySet())
			{
				Utility.debugPrintln("starting " + property);
				List<Integer> alphabet = new ArrayList<Integer>();
				for(int i = 0; i < propertyValues.get(property).size(); i++)
					alphabet.add(i);
				
				//iterate over all possible matrices for this property
				for(List<Integer> list : Utility.createAllStringsOfSize(alphabet, NUM_COLS*NUM_ROWS))
				{
					TempMatrix m = new TempMatrix();
					m.property = property;
					for(int i = 0; i < NUM_ROWS; i++)
					{
						for(int j = 0; j < NUM_COLS; j++)
							m.values[i][j] = list.get(i*NUM_COLS + j);
					}
					
					//create an actual matrix so that we can call the patterns' detect method
					Matrix matrix = generateMatrix(m, objects);
					
					Set<Pattern> rowPresent = new HashSet<Pattern>();
					Set<Pattern> colPresent = new HashSet<Pattern>();
					//iterate over each pattern and see if they are all in a valid state
					boolean foundInvalid = false;
					for(Pattern p : rowPatterns)
					{
						//we don't care about patterns that don't deal with this property
						if(!p.getRelavantProperties().iterator().next().equals(property))
							continue;
						
						boolean predicate = true;
						for(int i = 0; i < matrix.getNumRows() - 1; i++)
						{
							if(!p.detectPattern(matrix.getRow(i)))
								predicate = false;
						}
						boolean antecedent = p.detectPattern(matrix.getRow(matrix.getNumRows() - 1));
						boolean validPattern = validPatterns.get(p);
						
						//a pattern is in a valid state iff (validPattern && !(predicate && !antecident)) || (!validPattern && !predicate)
						if(validPattern)
						{
							if(predicate && !antecedent)
								foundInvalid = true;
							else if(predicate && antecedent)
								rowPresent.add(p);
						}
						else
						{
							if(predicate)
								foundInvalid = true;
						}
							
//						if((!validPattern && predicate) || (validPattern && predicate && !antecedent))
//							foundInvalid = true;
//						else if(predicate && antecedent)
//							rowPresent.add(p);
					}
					for(Pattern p : colPatterns)
					{
						//we don't care about patterns that don't deal with this property
						if(!p.getRelavantProperties().iterator().next().equals(property))
							continue;
						
						
						boolean predicate = true;
						for(int i = 0; i < matrix.getNumCols() - 1; i++)
						{
							if(!p.detectPattern(matrix.getCol(i)))
								predicate = false;
						}
						boolean antecedent = p.detectPattern(matrix.getCol(matrix.getNumCols() - 1));
						boolean validPattern = validPatterns.get(p);
						
						//a pattern is in a valid state iff (validPattern && !(predicate && !antecident)) || (!validPattern && !predicate)
						if(validPattern)
						{
							if(predicate && !antecedent)
								foundInvalid = true;
							else if(predicate && antecedent)
								colPresent.add(p);
						}
						else
						{
							if(predicate)
								foundInvalid = true;
						}
						
//						if((!validPattern && predicate) || (validPattern && predicate && !antecedent))
//							foundInvalid = true;
//						else if(predicate && antecedent)
//							colPresent.add(p);
					}
					
					//alright, so if this matrix is in a valid state for all patterns, let's keep it
					if(!foundInvalid)
					{
						if(validMatrices.get(property) == null)
							validMatrices.put(property, new HashSet<TempMatrix>());
						validMatrices.get(property).add(m);
						for(Pattern p : rowPresent)
						{
							if(this.patternPresentRowMatrices.get(p) == null)
								this.patternPresentRowMatrices.put(p, new HashSet<TempMatrix>());
							this.patternPresentRowMatrices.get(p).add(m);
						}
						for(Pattern p : colPresent)
						{
							if(this.patternPresentColMatrices.get(p) == null)
								this.patternPresentColMatrices.put(p, new HashSet<TempMatrix>());
							this.patternPresentColMatrices.get(p).add(m);
						}
					}
				}
				Utility.debugPrintln("finished " + property);
			}
		}

		private Matrix generateMatrix(TempMatrix m, List<MatrixEntry> objects) {
			Matrix ret = new Matrix(NUM_ROWS, NUM_COLS);
			for(int i = 0; i < NUM_ROWS; i++)
			{
				for(int j = 0; j < NUM_COLS; j++)
				{
					Map<String, String> props = new HashMap<String, String>();
					for(String property : this.propertyValues.keySet())
						props.put(property, propertyValues.get(property).get(0));
					props.put(m.property, propertyValues.get(m.property).get(m.values[i][j]));
					ret.setEntry(i, j, MatrixEntry.getObjectWithProperties(props, new HashSet<MatrixEntry>(objects)));
				}
			}
			
			return ret;
		}
		
		@SuppressWarnings("unchecked")
		public Matrix generateMatrix(List<MatrixEntry> objects, Pattern rowPattern, Pattern colPattern, Random rand)
		{
			Map<String, TempMatrix> matrices = new HashMap<String, TempMatrix>();
			do
			{
				//select one matrix for each property from the list of valid matrices
				String rowProp = rowPattern.getRelavantProperties().iterator().next();
				String colProp = colPattern.getRelavantProperties().iterator().next();
				for(String prop : this.propertyValues.keySet())
				{
					Set<TempMatrix> choices;
					if(prop.equals(colProp) || prop.equals(rowProp))
					{
						if(prop.equals(colProp) && prop.equals(rowProp))
							choices = Utility.intersection(this.patternPresentRowMatrices.get(rowPattern), 
									this.patternPresentColMatrices.get(colPattern));
						else if(prop.equals(colProp))
							choices = this.patternPresentColMatrices.get(colPattern);
						else
							choices = this.patternPresentRowMatrices.get(rowPattern);
					}
					else
						choices = this.validMatrices.get(prop);
					
					//if choices has nothing in it, then this isn't possible
					if(choices.size() == 0)
						return null;
					
					//randomly select a matrix
					matrices.put(prop, choices.toArray(new TempMatrix[choices.size()])[rand.nextInt(choices.size())]);
				}
			} while(hasRepeats(matrices));
			
			return generateMatrix(objects, matrices);
		}

		private boolean hasRepeats(Map<String, TempMatrix> matrices) {
			Set<List<Integer>> objects = new HashSet<List<Integer>>();
			for(int i = 0; i < NUM_ROWS; i++)
			{
				for(int j = 0; j < NUM_COLS; j++)
				{
					List<Integer> obj = new ArrayList<Integer>();
					
					for(String prop : matrices.keySet())
						obj.add(matrices.get(prop).values[i][j]);
					
					if(objects.contains(obj))
						return true;
					else
						objects.add(obj);
				}
			}
			
			return false;
		}

		private Matrix generateMatrix(List<MatrixEntry> objects, Map<String, TempMatrix> matrices) {
			Matrix ret = new Matrix(NUM_ROWS, NUM_COLS);
			for(int i = 0; i < NUM_ROWS; i++)
			{
				for(int j = 0; j < NUM_COLS; j++)
				{
					Map<String, String> props = new HashMap<String, String>();
					for(String property : this.propertyValues.keySet())
						props.put(property, propertyValues.get(property).get(matrices.get(property).values[i][j]));
					
					ret.setEntry(i, j, MatrixEntry.getObjectWithProperties(props, new HashSet<MatrixEntry>(objects)));
				}
			}
			
			return ret;			
		}
	}
	
	public static Set<Matrix> generateMatrix(List<MatrixEntry> objects, Set<Pattern> rowPatterns, Set<Pattern> colPatterns,
			Map<Pattern, Boolean> validPatterns, int num, Random rand)
	{
		//build a list of all property values
		Map<String, List<String>> propertyValues = new HashMap<String, List<String>>();
		for(MatrixEntry obj : objects)
		{
			for(String prop : obj.getDefinedProperties())
			{
				if(propertyValues.get(prop) == null)
					propertyValues.put(prop, new ArrayList<String>());
				if(!propertyValues.get(prop).contains(obj.getPropertyValue(prop))
						)//&& propertyValues.get(prop).size() < 3) //TODO debug
					propertyValues.get(prop).add(obj.getPropertyValue(prop));
			}
		}
		
		//generate all valid matrices
		PatternAligner aligner = new PatternAligner();
		aligner.generateValidMatrices(propertyValues, objects, rowPatterns, colPatterns, validPatterns);
		
		Set<Matrix> ret = new HashSet<Matrix>();
		while(ret.size() < num)
		{
			//we need to ensure that at least one pattern is present for both the rows and columns
			//and to ensure that the patterns all appear equally, we're just going to randomly select
			//one from each to enforce
			
			Pattern rowPattern = null;
			do
			{
				rowPattern = rowPatterns.toArray(new Pattern[rowPatterns.size()])[rand.nextInt(rowPatterns.size())];
			} while(!validPatterns.get(rowPattern));
			
			Pattern colPattern = null;
			do
			{
				colPattern = colPatterns.toArray(new Pattern[colPatterns.size()])[rand.nextInt(colPatterns.size())];
			} while(!validPatterns.get(colPattern));
			
			Matrix m = aligner.generateMatrix(objects, rowPattern, colPattern, rand);
			if(m == null)
				continue;
			if(!m.isValidMatrix(rowPatterns, colPatterns, validPatterns))
				continue;
			if(!ret.contains(m))
				ret.add(m);				
		}
		
		return ret;
		
	}
	
	


}

