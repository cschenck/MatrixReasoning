package matrices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import matrices.patterns.Pattern;

public class Matrix implements Iterable<MatrixEntry> {
	
	private MatrixEntry[][] entries;
	private int numCols, numRows;
	
	public Matrix(int numRows, int numCols)
	{
		this.numRows = numRows;
		this.numCols = numCols;
		
		entries = new MatrixEntry[numRows][numCols];
	}
	
	public Matrix(Matrix m)
	{
		numCols = m.numCols;
		numRows = m.numRows;
		
		entries = new MatrixEntry[numRows][numCols];
		for(int i = 0; i < this.getNumRows(); i++)
		{
			for(int j = 0; j < this.getNumCols(); j++)
				this.setEntry(i, j, m.getEntry(i, j));
		}
	}
	
	public void setMatrixByRows(List<List<MatrixEntry>> objects)
	{
		if(objects.size() != this.getNumRows())
			throw new IllegalArgumentException();
		
		for(int row = 0; row < objects.size(); row++)
		{
			if(objects.get(row).size() != this.getNumCols())
				throw new IllegalArgumentException();
			
			for(int col = 0; col < objects.get(row).size(); col++)
				this.setEntry(row, col, objects.get(row).get(col));
		}
	}
	
	public void setMatrixByCols(List<List<MatrixEntry>> objects)
	{
		if(objects.size() != this.getNumCols())
			throw new IllegalArgumentException();
		
		for(int col = 0; col < objects.size(); col++)
		{
			if(objects.get(col).size() != this.getNumRows())
				throw new IllegalArgumentException();
			
			for(int row = 0; row < objects.get(col).size(); row++)
				this.setEntry(row, col, objects.get(col).get(row));
		}
	}
	
	public void setEntry(int row, int col, MatrixEntry entry)
	{
		validateCoordiantes(row, col);
		
		entries[row][col] = entry;
	}
	
	public MatrixEntry getEntry(int row, int col)
	{
		validateCoordiantes(row, col);
		
		return entries[row][col];
	}
	
	public int getNumRows()
	{
		return numRows;
	}
	
	public int getNumCols()
	{
		return numCols;
	}
	
	public List<MatrixEntry> getRow(int row)
	{
		List<MatrixEntry> ret = new ArrayList<MatrixEntry>();
		for(int col = 0; col < numCols; col++)
		{
			validateCoordiantes(row, col);
			ret.add(this.getEntry(row, col));
		}
		
		return ret;
	}
	
	public List<List<MatrixEntry>> getAllRows()
	{
		List<List<MatrixEntry>> ret = new ArrayList<List<MatrixEntry>>();
		for(int i = 0; i < this.getNumRows(); i++)
			ret.add(this.getRow(i));
		
		return ret;
	}
	
	public List<MatrixEntry> getCol(int col)
	{
		List<MatrixEntry> ret = new ArrayList<MatrixEntry>();
		for(int row = 0; row < numRows; row++)
		{
			validateCoordiantes(row, col);
			ret.add(this.getEntry(row, col));
		}
		
		return ret;
	}
	
	public List<List<MatrixEntry>> getAllCols()
	{
		List<List<MatrixEntry>> ret = new ArrayList<List<MatrixEntry>>();
		for(int i = 0; i < this.getNumCols(); i++)
			ret.add(this.getCol(i));
		
		return ret;
	}
	
	public int rowIndex(MatrixEntry object)
	{
		for(int i = 0; i < this.getNumRows(); i++)
		{
			if(this.getRow(i).contains(object))
				return i;
		}
		
		return -1;
	}
	
	public int colIndex(MatrixEntry object)
	{
		for(int i = 0; i < this.getNumCols(); i++)
		{
			if(this.getCol(i).contains(object))
				return i;
		}
		
		return -1;
	}
	
	/**
	 * Determines whether this is considered a valid matrix based on the properties of the
	 * matrix entries contained in the matrix. A valid matrix is one in which all the rows
	 * have the same patterns present and all the columns have the same patterns present.
	 * A pattern across a property for a set of entries is defined as being either all the
	 * same value, all different values, or neither.
	 * 
	 * @return	Returns true if and only if all entries are filled in the matrix AND if all
	 * 			rows have exactly the same patterns across each property AND if all columns
	 * 			have exactly the same patterns across each property. Returns false otherwise.
	 */
//	public boolean isValidMatrix()
//	{
//		//first lets make sure the matrix is entirely filled in
//		for( int i = 0; i < this.getNumRows(); i++)
//		{
//			for(int j = 0; j < this.getNumCols(); j++)
//			{
//				if(this.getEntry(i, j) == null)
//					return false;
//			}
//		}
//		
//		//next lets check the rows to make sure they have the same patterns
//		Map<String, String> firstPattern = this.findPatterns(this.getRow(0));
//		for(int i = 1; i < this.getNumRows(); i++)
//		{
//			if(!firstPattern.equals(this.findPatterns(this.getRow(i))))
//				return false;
//		}
//		
//		//do the same for the columns
//		firstPattern = this.findPatterns(this.getCol(0));
//		for(int i = 1; i < this.getNumCols(); i++)
//		{
//			if(!firstPattern.equals(this.findPatterns(this.getCol(i))))
//				return false;
//		}
//		
//		//if we made it here, then the matrix is valid
//		return true;
//	}
	
//	/*
//	 * This version is modified from the above version such that if there is not a
//	 * pattern for a certain property across a row or column, then the training rows/
//	 * cols (i.e., all but the last) will have different patterns for that
//	 * property in each.
//	 */
//	public boolean isValidMatrix()
//	{
//		//first lets make sure the matrix is entirely filled in
//		for( int i = 0; i < this.getNumRows(); i++)
//		{
//			for(int j = 0; j < this.getNumCols(); j++)
//			{
//				if(this.getEntry(i, j) == null)
//					return false;
//			}
//		}
//		
//		//next lets check which properties are supposed to have patterns and which aren't
//		//based on the training rows
//		Map<String, String> patterns = new HashMap<String, String>();
//		for(int i = 0; i < this.getNumRows() - 1; i++)
//		{
//			for(Entry<String, String> e : this.findPatterns(this.getRow(i)).entrySet())
//			{
//				if(e.getValue().equals("neither") || 
//						(patterns.get(e.getKey()) != null && !patterns.get(e.getKey()).equals(e.getValue())))
//					patterns.put(e.getKey(), "none");
//				else
//					patterns.put(e.getKey(), e.getValue());
//			}
//		}
//		
//		//now verify that this set of patterns holds for the test row
//		for(Entry<String, String> e : this.findPatterns(this.getRow(this.getNumRows() - 1)).entrySet())
//		{
//			if(!patterns.get(e.getKey()).equals("none") && !patterns.get(e.getKey()).equals(e.getValue()))
//				return false;
//		}
//		
//		//properties that map to non-null values have patterns, the others don't
//		//enforce that at least one property has a pattern
//		boolean found = false;
//		for(Entry<String, String> e : patterns.entrySet())
//		{
//			if(!e.getValue().equals("none"))
//			{
//				found = true;
//				break;
//			}
//		}
//		if(!found)
//			return false;
//		
//		//next enforce that all non-pattern properties have different patterns in the training rows
//		for(int i = 0; i < this.getNumRows() - 1; i++)
//		{
//			for(int j = i + 1; j < this.getNumRows() - 1; j++)
//			{
//				Map<String, String> rowi = this.findPatterns(this.getRow(i));
//				Map<String, String> rowj = this.findPatterns(this.getRow(j));
//				for(Entry<String, String> e : rowi.entrySet())
//				{
//					if(!patterns.get(e.getKey()).equals("none")) //we don't need to look at properties that have patterns
//						continue;
//					if(e.getValue().equals(rowj.get(e.getKey())))
//						return false;
//				}
//			}
//		}
//		
//		//do the same for the columns
//		//first lets check which properties are supposed to have patterns and which aren't
//		//based on the training cols
//		patterns = new HashMap<String, String>();
//		for(int i = 0; i < this.getNumCols() - 1; i++)
//		{
//			for(Entry<String, String> e : this.findPatterns(this.getCol(i)).entrySet())
//			{
//				if(e.getValue().equals("neither") || 
//						(patterns.get(e.getKey()) != null && !patterns.get(e.getKey()).equals(e.getValue())))
//					patterns.put(e.getKey(), "none");
//				else
//					patterns.put(e.getKey(), e.getValue());
//			}
//		}
//		
//		//now verify that this set of patterns holds for the test col
//		for(Entry<String, String> e : this.findPatterns(this.getCol(this.getNumCols() - 1)).entrySet())
//		{
//			if(!patterns.get(e.getKey()).equals("none") && !patterns.get(e.getKey()).equals(e.getValue()))
//				return false;
//		}
//		
//		//properties that map to non-null values have patterns, the others don't
//		//enforce that at least one property has a pattern
//		found = false;
//		for(Entry<String, String> e : patterns.entrySet())
//		{
//			if(!e.getValue().equals("none"))
//			{
//				found = true;
//				break;
//			}
//		}
//		if(!found)
//			return false;
//		
//		//next enforce that all non-pattern properties have different patterns in the training cols
//		for(int i = 0; i < this.getNumCols() - 1; i++)
//		{
//			for(int j = i + 1; j < this.getNumCols() - 1; j++)
//			{
//				Map<String, String> coli = this.findPatterns(this.getCol(i));
//				Map<String, String> colj = this.findPatterns(this.getCol(j));
//				for(Entry<String, String> e : coli.entrySet())
//				{
//					if(!patterns.get(e.getKey()).equals("none")) //we don't need to look at properties that have patterns
//						continue;
//					if(e.getValue().equals(colj.get(e.getKey())))
//						return false;
//				}
//			}
//		}
//		
//		//if we made it here, then the matrix is valid
//		return true;
//	}
	
	/**
	 * Takes in a set of patterns to verify on the rows and a set to verify
	 * on the columns. If a row pattern is detected on all but the last row
	 * of the column, then to be valid it must also be detected on the last
	 * row. The same goes for column patterns. Any pattern that is considered
	 * 'invalid' by the mapping in validPatterns (i.e.,maps to false) will
	 * cause the return value to be false if it is detected in the first two
	 * rows or columns. This is to allow some patterns to not be used in
	 * determining how to complete the matrix that would otherwise be too
	 * complex to detect but to also enforce that they don't create ambiguity
	 * in the detectable patterns in the matrix. Also, there must exist at
	 * least one detected pattern across the rows and one across the columns
	 * in order to return true;
	 * 
	 * @param rowPatterns	Patterns to verify on the rows.
	 * @param colPatterns	Patterns to verify on the columns.
	 * @param validPatterns Maps patterns to boolean values. Any pattern that
	 * 						maps to false is not considered 'valid,' meaning
	 * 						that it may not be present in the first 2 rows
	 * 						or columns. If it is, then this method returns
	 * 						false.
	 * @return				True if all row and column patterns detected in 
	 * 						all but the last row/column implies that that
	 * 						pattern is present in the last row/column. False
	 * 						if that is not the case or if the matrix is not
	 * 						completely filled in or if an 'invalid' pattern
	 * 						is found in the first two rows or columns.
	 */
	public boolean isValidMatrix(Set<Pattern> rowPatterns, Set<Pattern> colPatterns, Map<Pattern, Boolean> validPatterns)
	{
		//first lets make sure the matrix is entirely filled in
		for( int i = 0; i < this.getNumRows(); i++)
		{
			for(int j = 0; j < this.getNumCols(); j++)
			{
				if(this.getEntry(i, j) == null)
					return false;
			}
		}
		
		boolean foundRowPattern = false;
		//next check row patterns
		for(Pattern p : rowPatterns)
		{
			boolean found = true;
			for(int i = 0; i < this.getNumRows() - 1; i++)
				found = found && p.detectPattern(this.getRow(i));
			
			if((found && !validPatterns.get(p)) || (found && !p.detectPattern(this.getRow(this.getNumRows() - 1))))
				return false;
			else if(found)
				foundRowPattern = true;
		}
		
		boolean foundColPattern = false;
		//check col patterns
		for(Pattern p : colPatterns)
		{
			boolean found = true;
			for(int i = 0; i < this.getNumCols() - 1; i++)
				found = found && p.detectPattern(this.getCol(i));
			
			if((found && !validPatterns.get(p)) || (found && !p.detectPattern(this.getCol(this.getNumCols() - 1))))
				return false;
			else if(found)
				foundColPattern = true;
		}
		
		//next let's check to make sure there are no duplicate objects		
		for(int i1 = 0; i1 < this.getNumRows(); i1++)
		{
			for(int j1 = 0; j1 < this.getNumRows(); j1++)
			{
				for(int i2 = 0; i2 < this.getNumRows(); i2++)
				{
					for(int j2 = 0; j2 < this.getNumRows(); j2++)
					{
						if(this.getEntry(i1, j1).equals(this.getEntry(i2, j2)) &&
								(i1 != i2 || j1 != j2))
							return false;
					}
				}
			}
		}
		
		//if either the rows or the columns don't have a valid pattern return false
		if(!foundRowPattern || !foundColPattern)
			return false;
		else //if we made it here, return true
			return true;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof Matrix))
			return false;
		
		Matrix m = (Matrix) obj;
		
		if(m.getNumRows() != this.getNumRows() || m.getNumCols() != this.getNumCols())
			return false;
		
		for(int i = 0; i < this.getNumRows(); i++)
		{
			for(int j = 0; j < this.getNumCols(); j++)
			{
				if(!this.getEntry(i, j).equals(m.getEntry(i, j)))
					return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int hashCode()
	{
		return entries.hashCode();
	}
	
	@Override
	public String toString()
	{
		//map of column number to maximum object name width
		Map<Integer, Integer> widths = new HashMap<Integer, Integer>();
		for(int i = 0; i < this.getNumCols(); i++)
		{
			int max = 0;
			for(MatrixEntry entry : this.getCol(i))
			{
				if(entry.toString().length() > max)
					max = entry.toString().length();
			}
			widths.put(i, max);
		}
		
		String ret = "";
		for(int i = 0; i < this.getNumRows(); i++)
		{
			if(i == 0)
				ret += "[";
			else
				ret += " ";
			
			List<MatrixEntry> row = this.getRow(i);
			for(int j = 0; j < row.size(); j++)
				ret += (j == 0 ? "[" : " ") + String.format("%" + widths.get(j) + "s", row.get(j)) + (j < row.size() - 1 ? "," : "]");
			
			if(i == this.getNumRows() - 1)
				ret += "]";
			else
				ret += " \n";
		}
		
		return ret;
	}
	
	public Set<Pattern> findPatterns(List<MatrixEntry> entries, Set<Pattern> patterns)
	{
		Set<Pattern> ret = new HashSet<Pattern>();
		for(Pattern p : patterns)
		{
			if(p.detectPattern(entries))
				ret.add(p);
		}
		
		return ret;
	}
	
//	/**
//	 * 
//	 * @param entries a list of matrix entries to find patterns over
//	 * @return A map from property values to either "same" if they are all the same,
//	 * 			"different" if the are all different from eachother, or "neither". 
//	 */
//	public Map<String, String> findPatterns(List<MatrixEntry> entries)
//	{
//		if(entries.size() < 2)
//			throw new IllegalArgumentException("Must have at least two entries to find patterns");
//		
//		//first check to make sure each object has the same set of properties defined
//		for(int i = 0; i < entries.size() - 1; i++)
//		{
//			if(!entries.get(i).getDefinedProperties().equals(entries.get(i+1).getDefinedProperties()))
//			{
//				String error = "Not all entries have the same properties defined:";
//				for(MatrixEntry e : entries)
//					error += "\n" + e.getName() + ":" + e.getDefinedProperties();
//				throw new IllegalArgumentException(error);
//			}
//		}
//		
//		//next for each property, check every pair of matrix entries in the list for similarities or differences
//		Map<String, String> ret = new HashMap<String, String>();
//		for(String property : entries.get(0).getDefinedProperties())
//		{
//			boolean foundDiff = false;
//			boolean foundSame = false;
//			for(MatrixEntry e1 : entries)
//			{
//				for(MatrixEntry e2 : entries)
//				{
//					if(e1 == e2)
//						continue;
//					if(e1.getPropertyValue(property).equals(e2.getPropertyValue(property)))
//						foundSame = true;
//					else
//						foundDiff = true;
//				}
//			}
//			
//			//if we found differences for this property AND none were the same, then mark as different
//			if(foundDiff && !foundSame)
//				ret.put(property, "different");
//			//if we found no differences for this property AND found some the same, then mark as same
//			else if(!foundDiff && foundSame)
//				ret.put(property, "same");
//			//if we found some were different and some were the same, then mark as neither
//			else
//				ret.put(property, "neither");
//		}
//		
//		return ret;
//	}
	
	public boolean contains(MatrixEntry object)
	{
		for(int i = 0; i < this.getNumRows(); i++)
		{
			for(int j = 0; j < this.getNumCols(); j++)
			{
				if(this.getEntry(i, j) != null && this.getEntry(i, j).equals(object))
					return true;
			}
		}
		return false;
	}
	
	private void validateCoordiantes(int i, int j)
	{
		if(i >= numRows || j >= numCols || i < 0 || j < 0)
			throw new IllegalArgumentException("(" + i + ", " + j + ") is outside of the valid range for this " 
					+ numRows + "x" + numCols + " matrix.");
	}

	@Override
	public Iterator<MatrixEntry> iterator() {
		return new Iterator<MatrixEntry>() {
			private int i = 0;
			private int j = 0;
			@Override
			public boolean hasNext() {
				if(i < Matrix.this.getNumRows())
					return true;
				else
					return false;
			}

			@Override
			public MatrixEntry next() {
				if(!this.hasNext())
					throw new NoSuchElementException();
				
				MatrixEntry ret = Matrix.this.getEntry(i, j);
				j++;
				if(j >= Matrix.this.getNumCols())
				{
					j = 0;
					i++;
				}
				
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
