package matrices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import utility.Utility;

public class MatrixCompletionTask {
	
	private Matrix matrix;
	private MatrixEntry correctAnswer;
	private List<MatrixEntry> possibleAnswers;
	
	public MatrixCompletionTask(Matrix matrix, MatrixEntry correctAnswer, List<MatrixEntry> possibleAnswers, Random rand)
	{
		this.matrix = matrix;
		this.correctAnswer = correctAnswer;
		this.possibleAnswers = new ArrayList<MatrixEntry>(possibleAnswers);
		
		if(!this.possibleAnswers.contains(correctAnswer))
			this.possibleAnswers.add(correctAnswer);
		this.possibleAnswers = Utility.randomizeOrder(this.possibleAnswers, rand);
	}
	
	public int getNumRows()
	{
		return matrix.getNumRows();
	}
	
	public int getNumCols()
	{
		return matrix.getNumCols();
	}
	
	public List<MatrixEntry> getRow(int i)
	{
		List<MatrixEntry> ret = matrix.getRow(i);
		if(i == matrix.getNumRows() - 1) //we need to remove that last object from being returned
			ret.remove(ret.size() - 1);
		return ret;
	}
	
	public List<MatrixEntry> getCol(int i)
	{
		List<MatrixEntry> ret = matrix.getCol(i);
		if(i == matrix.getNumCols() - 1) //we need to remove that last object from being returned
			ret.remove(ret.size() - 1);
		return ret;
	}
	
	public List<MatrixEntry> getChoices()
	{
		List<MatrixEntry> ret = new ArrayList<MatrixEntry>(possibleAnswers);
		if(!ret.contains(correctAnswer))
			ret.add(correctAnswer);
		
		return ret;
	}
	
	public boolean isCorrect(MatrixEntry object)
	{
		return object.equals(correctAnswer);
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
			for(int j = 0; j < this.getNumCols(); j++)
			{
				if(j < row.size())
					ret += (j == 0 ? "[" : " ") + String.format("%" + widths.get(j) + "s", row.get(j)) + (j < this.getNumCols() - 1 ? "," : "]");
				else
					ret += (j == 0 ? "[" : " ") + String.format("%" + widths.get(j) + "s", "?") + (j < this.getNumCols() - 1 ? "," : "]");
			}
			
			if(i == this.getNumRows() - 1)
				ret += "]";
			else
				ret += " \n";
		}
		
		return ret;
	}
	
	public String toSerialString()
	{
		String ret = "";
		ret += matrix.getNumRows() + "," + matrix.getNumCols();
		ret += "," + (this.possibleAnswers.size());
		
		for(int i = 0; i < matrix.getNumRows(); i++)
		{
			for(int j = 0; j < matrix.getNumCols(); j++)
				ret += "," + matrix.getEntry(i, j).getName();
		}
		
		for(MatrixEntry obj : possibleAnswers)
			ret += "," + obj.getName();
		
		return ret;
	}
	
	public static MatrixCompletionTask constructTaskFromSerialString(String s, List<MatrixEntry> objects, Random rand)
	{
		Map<String, MatrixEntry> names = new HashMap<String, MatrixEntry>();
		for(MatrixEntry obj : objects)
			names.put(obj.getName(), obj);
		
		Scanner scan = new Scanner(s);
		scan.useDelimiter(",");
		int rows = scan.nextInt();
		int cols = scan.nextInt();
		int choices = scan.nextInt();
		Matrix m = new Matrix(rows, cols);
		for(int i = 0; i < rows; i++)
		{
			for(int j = 0; j < cols; j++)
			{
				m.setEntry(i, j, names.get(scan.next()));
			}
		}
		
		List<MatrixEntry> possibleChoices = new ArrayList<MatrixEntry>();
		for(int i = 0; i < choices; i++)
			possibleChoices.add(names.get(scan.next()));
		
		return new MatrixCompletionTask(m, m.getEntry(m.getNumRows() - 1, m.getNumCols() - 1), possibleChoices, rand);
	}

}
