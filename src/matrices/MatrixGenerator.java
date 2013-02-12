package matrices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import matrices.patterns.DecrementPattern;
import matrices.patterns.IncrementPattern;
import matrices.patterns.Pattern;

import utility.GreedySearch;
import utility.Utility;
import utility.GreedySearch.SearchCallback;

public class MatrixGenerator {
	
	public final static int NUM_ROWS = 3;
	public final static int NUM_COLS = 3;  
	
//	private final static int MAX_RECURSE_DEPTH = 3;
	
	public static Matrix generateMatrix(List<MatrixEntry> objects, Set<Pattern> rowPatterns, Set<Pattern> colPatterns,
			Map<Pattern, Boolean> validPatterns, Random rand)
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
		
		do
		{
			//let's first make a random matrix
			Matrix m = new Matrix(NUM_ROWS, NUM_COLS);
			for(int i = 0; i < m.getNumRows(); i++)
			{
				for(int j = 0; j < m.getNumCols(); j++)
				{
					MatrixEntry obj = null;
					do
					{
						obj = objects.get(rand.nextInt(objects.size()));
					} while(m.contains(obj));
					m.setEntry(i, j, obj);
				}
			}
			
			//now force it to be aligned along the row pattern
			List<List<MatrixEntry>> matrix = m.getAllRows();
			if(!align(matrix, new HashSet<MatrixEntry>(objects), rowPattern))
				continue;
			else
				m.setMatrixByRows(matrix);
			
			//next force it to align on the col pattern
			matrix = m.getAllCols();
			if(!align(matrix, new HashSet<MatrixEntry>(objects), colPattern))
				continue;
			else
				m.setMatrixByCols(matrix);
			
			//next let's check for validity
			if(m.isValidMatrix(rowPatterns, colPatterns, validPatterns))
				return m; //if so, we're done!
			
			//otherwise we're going to have to try some stuff
			MatrixSearchCallback callback = new MatrixSearchCallback(rowPattern, colPattern, 
					new HashSet<MatrixEntry>(objects), rowPatterns, colPatterns, validPatterns);
			GreedySearch<Matrix> search = new GreedySearch<Matrix>(callback);
			Matrix ret = search.findGoal(m);
			
			if(ret == null)
				throw new IllegalStateException("Greedy search returned null, this should not happen.");
			
			
		} while(true);
	}
	
	private static class MatrixSearchCallback implements SearchCallback<Matrix>
	{

		private Pattern rowPattern;
		private Pattern colPattern;
		private Set<MatrixEntry> objects;
		private Set<Pattern> rowPatterns;
		private Set<Pattern> colPatterns;
		private Map<Pattern, Boolean> validPatterns;
		
		public MatrixSearchCallback(Pattern rowPattern, Pattern colPattern, Set<MatrixEntry> objects,
				Set<Pattern> rowPatterns, Set<Pattern> colPatterns, Map<Pattern, Boolean> validPatterns)
		{
			this.rowPattern = rowPattern;
			this.colPattern = colPattern;
			this.objects = objects;
			this.rowPatterns = rowPatterns;
			this.colPatterns = colPatterns;
			this.validPatterns = validPatterns;
		}
		
		@Override
		public Set<Matrix> getConnectedStates(Matrix state) {
			
			Set<Matrix> ret = new HashSet<Matrix>();
			for(MatrixEntry object : objects)
			{
				//don't allow duplicate objects
				if(state.contains(object))
					continue;
				
				//now put the object in each spot
				for(int i = 0; i < state.getNumRows(); i++)
				{
					for(int j = 0; j < state.getNumCols(); j++)
					{
						Matrix m = new Matrix(state);
						m.setEntry(i, j, object);
						//only add it if it doesn't break the rowPattern and colPattern
						if(containsPattern(m.getAllRows(), rowPattern) && containsPattern(m.getAllCols(), colPattern))
							ret.add(m);
					}
				}
			}
			
			return ret;
		}

		@Override
		public double distanceToGoal(Matrix state) {
			//first check to see if we are at the goal
			if(state.isValidMatrix(rowPatterns, colPatterns, validPatterns)
					&& containsPattern(state.getAllRows(), rowPattern)
					&& containsPattern(state.getAllCols(), colPattern))
				return 0.0;
			
			//if not estimate how far off we are from the goal
			double ret = 0;
			//one step for each row/col that doesn't adhere to the rowPattern/colPattern
			for(List<MatrixEntry> row : state.getAllRows())
			{
				if(!rowPattern.detectPattern(row))
					ret += 1.0;
			}
			for(List<MatrixEntry> col : state.getAllCols())
			{
				if(!colPattern.detectPattern(col))
					ret += 1.0;
			}
			
			//add one step for each pattern in rowPatterns/colPatterns that is in an invalid state
			for(Pattern p : rowPatterns)
			{
				if(!isValid(state.getAllRows(), p, validPatterns.get(p)))
					ret += 1.0;
			}
			for(Pattern p : colPatterns)
			{
				if(!isValid(state.getAllCols(), p, validPatterns.get(p)))
					ret += 1.0;
			}
			
			if(ret == 0)
				throw new IllegalStateException("Somehow the estimated distance is 0 but this is not a valid matrix.");
			
			//TODO debug
//			if(ret == 1.0)
//				printErrors(state);
			
			return ret;
		}
		
		private void printErrors(Matrix state) {
			
			
			//one step for each row/col that doesn't adhere to the rowPattern/colPattern
			for(List<MatrixEntry> row : state.getAllRows())
			{
				if(!rowPattern.detectPattern(row))
					System.out.println("violates rowPattern + " + rowPattern.toString() + " on row " + row.toString());
			}
			for(List<MatrixEntry> col : state.getAllCols())
			{
				if(!colPattern.detectPattern(col))
					System.out.println("violates colPattern + " + colPattern.toString() + " on col " + col.toString());
			}
			
			//add one step for each pattern in rowPatterns/colPatterns that is in an invalid state
			for(Pattern p : rowPatterns)
			{
				if(!isValid(state.getAllRows(), p, validPatterns.get(p)))
					System.out.println("violoates " + p.toString() + " on the rows");
			}
			for(Pattern p : colPatterns)
			{
				if(!isValid(state.getAllCols(), p, validPatterns.get(p)))
					System.out.println("violates " + p.toString() + " on the cols");
			}
		}
		
	}
	
	
//	public static Matrix generateMatrix(List<MatrixEntry> objects, Set<Pattern> rowPatterns, Set<Pattern> colPatterns,
//			Map<Pattern, Boolean> validPatterns, Random rand)
//	{
//		//we need to ensure that at least one pattern is present for both the rows and columns
//		//and to ensure that the patterns all appear equally, we're just going to randomly select
//		//one from each to enforce
//		
//		Pattern rowPattern = null;
//		do
//		{
//			rowPattern = rowPatterns.toArray(new Pattern[rowPatterns.size()])[rand.nextInt(rowPatterns.size())];
//		} while(!validPatterns.get(rowPattern));
//		
//		Pattern colPattern = null;
//		do
//		{
//			colPattern = colPatterns.toArray(new Pattern[colPatterns.size()])[rand.nextInt(colPatterns.size())];
//		} while(!validPatterns.get(colPattern));
//		
//		int count = 0;
//		do
//		{
//			//let's first make a random matrix
//			Matrix m = new Matrix(NUM_ROWS, NUM_COLS);
//			for(int i = 0; i < m.getNumRows(); i++)
//			{
//				for(int j = 0; j < m.getNumCols(); j++)
//				{
//					MatrixEntry obj = null;
//					do
//					{
//						obj = objects.get(rand.nextInt(objects.size()));
//					} while(m.contains(obj));
//					m.setEntry(i, j, obj);
//				}
//			}
//			
//			//now force it to be aligned along the row pattern
//			List<List<MatrixEntry>> matrix = m.getAllRows();
//			if(!align(matrix, new HashSet<MatrixEntry>(objects), rowPattern))
//				continue;
//			else
//				m.setMatrixByRows(matrix);
//			
//			//next force it to align on the col pattern
//			matrix = m.getAllCols();
//			if(!align(matrix, new HashSet<MatrixEntry>(objects), colPattern))
//				continue;
//			else
//				m.setMatrixByCols(matrix);
//			
//			//next let's check for validity
//			if(m.isValidMatrix(rowPatterns, colPatterns, validPatterns))
//				return m; //if so, we're done!
//			
//			//otherwise we're going to have to try some stuff
//			List<Pattern> toCheckRowPatterns = new ArrayList<Pattern>(rowPatterns);
//			toCheckRowPatterns.remove(rowPattern);
//			List<Pattern> toCheckColPatterns = new ArrayList<Pattern>(colPatterns);
//			toCheckColPatterns.remove(colPattern);
//			boolean ret = fixMatrix(m, toCheckRowPatterns, toCheckColPatterns, 
//					rowPattern, colPattern, rowPatterns, colPatterns, validPatterns, 
//					new HashSet<MatrixEntry>(objects), rand, 0);
//			
//			if(ret) //it's fixed!
//			{
//				if(!containsPattern(m.getAllRows(), rowPattern) || !containsPattern(m.getAllCols(), colPattern))
//					System.out.print("");
//				return m;
//			}
//			
//			//if not, then try again
//			
//			if(count > 100)
//				System.out.print("");
//			count++;
//			
//		} while(true);
//	}
//	
//	//this is a lot of arguments, I feel like it needs an explanation
//	/**
//	 * This method takes in a matrix and a set of patterns to check, and iterates over
//	 * them in random order trying to fix the matrix. It first tries, for each pattern,
//	 * to align the matrix to that pattern, then if that doesn't work, it attempts to
//	 * disalign the matrix to that pattern. If that doesn't work after iterating over
//	 * all patterns, then it attempts to do that again, but after each align/disalign,
//	 * it recurses on itself.
//	 * 
//	 * @param m						The matrix to fix
//	 * @param toCheckRowPatterns	the row patterns that need to be checked
//	 * @param toCheckColPatterns	the col patterns that need to be checked
//	 * @param permenantRowPattern	a row pattern that must always be present (cannot return true if it gets broken)
//	 * @param permenantColPattern	a col pattern that must always be present (cannot return true if it gets broken)
//	 * @param rowPatterns			the overall set of row patterns that m must be valid with respect to
//	 * @param colPatterns			the overall set of col patterns that m must be valid with respect to
//	 * @param validPatterns			the overall set of valid and non-valid patterns that m must be valid with respect to
//	 * @param objects				the set of objects to pick from when aligning/disaligning
//	 * @param rand					a rand object to help with random decision making
//	 * @param recurseDepth			the recurse depth, to keep track of how deep it has gone so as to not recurse to far
//	 * @return						True if m has been fixed to be a valid matrix, false otherwise
//	 */
//	private static boolean fixMatrix(Matrix m, List<Pattern> toCheckRowPatterns, List<Pattern> toCheckColPatterns, 
//			Pattern permenantRowPattern, Pattern permenantColPattern,
//			Set<Pattern> rowPatterns, Set<Pattern> colPatterns, Map<Pattern, Boolean> validPatterns, 
//			Set<MatrixEntry> objects, Random rand, int recurseDepth)
//	{
//		//base case
//		if(recurseDepth > MAX_RECURSE_DEPTH)
//			return false;
//		
//		List<Pattern> toDoRows = new ArrayList<Pattern>(toCheckRowPatterns);
//		List<Pattern> toDoCols = new ArrayList<Pattern>(toCheckColPatterns);
//		
//		while(!toDoRows.isEmpty() && !toDoCols.isEmpty())
//		{
//			//first randomly select a pattern to test
//			boolean isRow;
//			Pattern selected;
//			if(toDoRows.isEmpty())
//			{
//				isRow = false;
//				selected = toDoCols.remove(rand.nextInt(toDoCols.size()));
//			}
//			else if(toDoCols.isEmpty())
//			{
//				isRow = true;
//				selected = toDoRows.remove(rand.nextInt(toDoRows.size()));
//			}
//			else if(rand.nextBoolean())
//			{
//				isRow = true;
//				selected = toDoRows.remove(rand.nextInt(toDoRows.size()));
//			}
//			else
//			{
//				isRow = false;
//				selected = toDoCols.remove(rand.nextInt(toDoCols.size()));
//			}
//			
//			//next attempt to disalign the pattern
//			List<List<MatrixEntry>> matrix;
//			if(isRow)
//				matrix = m.getAllRows();
//			else
//				matrix = m.getAllCols();
//			
//			if(!disalign(matrix, objects, selected, rand))
//				return false;
//			
//			Matrix testDisaligned = new Matrix(m.getNumRows(), m.getNumCols());
//			if(isRow)
//				testDisaligned.setMatrixByRows(matrix);
//			else
//				testDisaligned.setMatrixByCols(matrix);
//			
//			if(testDisaligned.isValidMatrix(rowPatterns, colPatterns, validPatterns))
//			{
//				if(isRow)
//					testDisaligned.setMatrixByRows(matrix);
//				else
//					testDisaligned.setMatrixByCols(matrix);
//				
//				//make sure we didn't break the patterns we wanted to keep
//				if(containsPattern(testDisaligned.getAllRows(), permenantRowPattern) 
//						&& containsPattern(testDisaligned.getAllCols(), permenantColPattern))
//				{
//					m.setMatrixByRows(testDisaligned.getAllRows());
//					return true;
//				}
//			}
//			
//			//if that didn't work, attempt to align it
//			if(!validPatterns.get(selected)) //don't try to align a non-valid pattern
//				continue;
//			if(!align(matrix, objects, selected))
//				return false;
//			Matrix testAligned = new Matrix(m.getNumRows(), m.getNumCols());
//			if(isRow)
//				testAligned.setMatrixByRows(matrix);
//			else
//				testAligned.setMatrixByCols(matrix);
//			if(testAligned.isValidMatrix(rowPatterns, colPatterns, validPatterns))
//			{
//				if(isRow)
//					testAligned.setMatrixByRows(matrix);
//				else
//					testAligned.setMatrixByCols(matrix);
//				
//				//make sure we didn't break the patterns we wanted to keep
//				if(containsPattern(testAligned.getAllRows(), permenantRowPattern) 
//						&& containsPattern(testAligned.getAllCols(), permenantColPattern))
//				{
//					m.setMatrixByRows(testAligned.getAllRows());
//					return true;
//				}
//			}
//			
//			//if NONE of this worked, then move onto the next pattern
//		}
//		
//		//if we can't align or disalign any single pattern and get it to work, lets try recursing
//		toDoRows = new ArrayList<Pattern>(toCheckRowPatterns);
//		toDoCols = new ArrayList<Pattern>(toCheckColPatterns);
//		
//		while(!toDoRows.isEmpty() && !toDoCols.isEmpty())
//		{
//			//first randomly select a pattern to test
//			boolean isRow;
//			Pattern selected;
//			if(toDoRows.isEmpty())
//			{
//				isRow = false;
//				selected = toDoCols.remove(rand.nextInt(toDoCols.size()));
//			}
//			else if(toDoCols.isEmpty())
//			{
//				isRow = true;
//				selected = toDoRows.remove(rand.nextInt(toDoRows.size()));
//			}
//			else if(rand.nextBoolean())
//			{
//				isRow = true;
//				selected = toDoRows.remove(rand.nextInt(toDoRows.size()));
//			}
//			else
//			{
//				isRow = false;
//				selected = toDoCols.remove(rand.nextInt(toDoCols.size()));
//			}
//			
//			//setup the patterns to check on the way down
//			List<Pattern> toCheckRowsPassDown = new ArrayList<Pattern>(toCheckRowPatterns);
//			List<Pattern> toCheckColsPassDown = new ArrayList<Pattern>(toCheckColPatterns);
//			if(isRow)
//				toCheckRowsPassDown.remove(selected);
//			else
//				toCheckColsPassDown.remove(selected);
//			
//			//make the disalign matrix
//			List<List<MatrixEntry>> matrix;
//			if(isRow)
//				matrix = m.getAllRows();
//			else
//				matrix = m.getAllCols();
//			if(!disalign(matrix, objects, selected, rand))
//				return false;
//			Matrix testDisaligned = new Matrix(m.getNumRows(), m.getNumCols());
//			if(isRow)
//				testDisaligned.setMatrixByRows(matrix);
//			else
//				testDisaligned.setMatrixByCols(matrix);
//			
//			//let's try it
//			if(fixMatrix(testDisaligned, toCheckRowsPassDown, toCheckColsPassDown, permenantRowPattern, permenantColPattern,
//					rowPatterns, colPatterns, validPatterns, objects, rand, recurseDepth + 1))
//			{
//				//if it worked, lets return it
//				m.setMatrixByRows(testDisaligned.getAllRows());
//				return true;
//			}
//			
//			
//			//if that didn't work, attempt to align it
//			if(!validPatterns.get(selected)) //don't try to align a non-valid pattern
//				continue;
//			if(!align(matrix, objects, selected))
//				return false;
//			Matrix testAligned = new Matrix(m.getNumRows(), m.getNumCols());
//			if(isRow)
//				testAligned.setMatrixByRows(matrix);
//			else
//				testAligned.setMatrixByCols(matrix);			
//			
//			//now try it
//			if(fixMatrix(testAligned, toCheckRowsPassDown, toCheckColsPassDown, permenantRowPattern, permenantColPattern, 
//					rowPatterns, colPatterns, validPatterns, objects, rand, recurseDepth + 1))
//			{
//				//if it worked, lets return it
//				m.setMatrixByRows(testAligned.getAllRows());
//				return true;
//			}
//			
//			//if NONE of this worked, then move onto the next pattern
//		}
//		
//		//if we've gone over every pattern and can't get it to work, then return false
//		return false;
//	}
//	
	private static boolean align(List<List<MatrixEntry>> matrix, Set<MatrixEntry> objects, Pattern pattern)
	{
		for(int i = 0; i < matrix.size(); i++)
		{
			List<MatrixEntry> list = matrix.get(i);
			list = pattern.align(list, objects);
			if(list == null) //if the list could not be aligned
			{
				return false;
			}
			
			matrix.set(i, list);
		}
		
		return true;
	}
//	
//	private static boolean disalign(List<List<MatrixEntry>> matrix, Set<MatrixEntry> objects, Pattern pattern, Random rand)
//	{
//		
//		while(!isValid(matrix, pattern))
//		{
//			int index = rand.nextInt(matrix.size());
//			List<MatrixEntry> list = matrix.get(index);
//			if(!pattern.detectPattern(list)) //can't disalign a list if there's already no pattern to begin with
//				continue;
//				
//			list = pattern.disalign(list, objects);
//			if(list == null) //if the list could not be disaligned
//			{
//				return false;
//			}
//			
//			matrix.set(index, list);
//		}
//		
//		return true;
//	}
//	
	private static boolean isValid(List<List<MatrixEntry>>  matrix, Pattern pattern, boolean isValidPattern)
	{
		//prediate = the pattern is found in the first n-1 lists
		boolean predicate = true;
		for(int i = 0; i < matrix.size() - 1; i++)
		{
			if(!pattern.detectPattern(matrix.get(i)))
				predicate = false;
		}
		
		if(predicate && !isValidPattern) //invalid patterns may never have a true predicate
			return false;
		else if(predicate && !pattern.detectPattern(matrix.get(matrix.size() - 1)))
			return false;
		else
			return true;
	}
	
	private static boolean containsPattern(List<List<MatrixEntry>> matrix, Pattern pattern)
	{
		boolean predicate = true;
		for(int i = 0; i < matrix.size(); i++)
		{
			if(!pattern.detectPattern(matrix.get(i)))
				predicate = false;
		}
		
		return predicate;
	}


}
