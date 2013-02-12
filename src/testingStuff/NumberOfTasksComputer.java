package testingStuff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utility.RunningMean;

public class NumberOfTasksComputer {

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
//		computeNumberOfTasks();
		outputExampleMatrices();
		
//		List<List<Integer>> mat = new ArrayList<List<Integer>>();
//		List<Integer> obj = new ArrayList<Integer>();
//		obj.add(1);obj.add(1);obj.add(1); mat.add(obj);
//		
//		obj = new ArrayList<Integer>();
//		obj.add(1);obj.add(1);obj.add(2); mat.add(obj);
//		
//		obj = new ArrayList<Integer>();
//		obj.add(1);obj.add(2);obj.add(1); mat.add(obj);
//		
//		obj = new ArrayList<Integer>();
//		obj.add(2);obj.add(1);obj.add(1); mat.add(obj);
//		
//		obj = new ArrayList<Integer>();
//		obj.add(2);obj.add(1);obj.add(2); mat.add(obj);
//		
//		obj = new ArrayList<Integer>();
//		obj.add(2);obj.add(2);obj.add(1); mat.add(obj);
//		
//		obj = new ArrayList<Integer>();
//		obj.add(3);obj.add(1);obj.add(1); mat.add(obj);
//		
//		obj = new ArrayList<Integer>();
//		obj.add(3);obj.add(1);obj.add(2); mat.add(obj);
//		
//		obj = new ArrayList<Integer>();
//		obj.add(3);obj.add(2);obj.add(1); mat.add(obj);
//		
//		System.out.println(intArrayToString(getDiffs(mat.get(0), mat.get(1), mat.get(2))));
//		System.out.println(intArrayToString(getDiffs(mat.get(3), mat.get(4), mat.get(5))));
//		System.out.println(intArrayToString(getDiffs(mat.get(6), mat.get(7), mat.get(8))));
		
	}
	
	private static String intArrayToString(int[] a)
	{
		String ret = "[";
		for(int i : a)
			ret += i + ",";
		return ret.substring(0, ret.length() - 1) + "]";
	}
	
	@SuppressWarnings("unchecked")
	private static void outputExampleMatrices()
	{
		List<List<String>> values = new ArrayList<List<String>>();
		
		List<String> list = new ArrayList<String>();
		list.add("Red");list.add("Green");list.add("Blue");
		values.add(list);
		
		list = new ArrayList<String>();
		list.add("Big");list.add("Medium");list.add("Small");
		values.add(list);
		
		list = new ArrayList<String>();
		list.add("Metal");list.add("Wood");list.add("Plastic");
		values.add(list);
		
		List<Integer> maxValueStringLength = new ArrayList<Integer>();
		for(int i = 0; i < values.size(); i++)
		{
			int max = 0;
			for(int j = 0; j < values.get(i).size(); j++)
			{
				if(max < values.get(i).get(j).length())
					max = values.get(i).get(j).length();
			}
			maxValueStringLength.add(max);
		}
		
		List<Integer> properties = new ArrayList<Integer>();
		for(int i = 0; i < 3; i++)
			properties.add(values.get(i).size());
		
		for(int num = 0; num < 100; num++)
		{
			List<List<Integer>> mat = generateMatrix(properties);
			
			for(int i = 0; i < mat.size(); i++)
			{
				System.out.print("[");
				for(int p = 0; p < properties.size(); p++)
				{
					System.out.format("%" + maxValueStringLength.get(p) + "s", values.get(p).get(mat.get(i).get(p)));
					if(p < properties.size() - 1)
						System.out.print(",");
				}
				if((i + 1) % 3 == 0)
					System.out.print("]\n");
				else
					System.out.print("],");
			}
			
			int[] r1 = getDiffs(mat.get(0), mat.get(1), mat.get(2));
			int[] r2 = getDiffs(mat.get(3), mat.get(4), mat.get(5));
			int[] r3 = getDiffs(mat.get(6), mat.get(7), mat.get(8));
			
			System.out.print("rows=");
			for(int i = 0; i < r1.length; i++)
			{
				if(r1[i] == r2[i] && r2[i] == r3[i])
					System.out.print(r1[i] + ",");
				else
					System.out.print("0,");
			}
			System.out.println();
			
			int[] c1 = getDiffs(mat.get(0), mat.get(3), mat.get(6));
			int[] c2 = getDiffs(mat.get(1), mat.get(4), mat.get(7));
			int[] c3 = getDiffs(mat.get(2), mat.get(5), mat.get(8));
			
			System.out.print("cols=");
			for(int i = 0; i < c1.length; i++)
			{
				if(c1[i] == c2[i] && c2[i] == c3[i])
					System.out.print(c1[i] + ",");
				else
					System.out.print("0,");
			}
			System.out.println();
			
			System.out.println("===================================================================");
		}
	}
	
	private static void computeNumberOfTasks()
	{
		List<Integer> properties = new ArrayList<Integer>();
		for(int i = 0; i < 3; i++)
			properties.add(3);
		
		System.out.println("Number of possible tasks: " + computeNumberOfTasksUntilRepeat(properties));
	}
	
	@SuppressWarnings("unchecked")
	private static int computeNumberOfTasksUntilRepeat(List<Integer> properties)
	{
		Set<List<List<Integer>>> mats = new HashSet<List<List<Integer>>>();
		RunningMean m = new RunningMean();
		int num = 0;
		while(true)
		{
			mats.clear();
			while(true)
			{
				List<List<Integer>> mat = generateMatrix(properties);
				
				if(mats.contains(mat))	
					break;
				mats.add(mat);
				
				
			}
			m.addValue(mats.size());
			num++;
			System.out.println("value=" + mats.size() + ",mean=" + m.getMean() + ",std.error=" 
					+ Math.sqrt(m.getVariance()/num) + ",n=" + num);
			
			//break if we are 95% confident that the mean is one particular value
			if(num > 30 && Math.sqrt(m.getVariance()/num)*1.96 < 0.5)
				break;
			
		}
		return (int) m.getMean();
	}
	
	@SuppressWarnings("unchecked")
	private static int computeNumberOfRepeatTasks(List<Integer> properties)
	{
		Map<List<List<Integer>>, Integer> mats = new HashMap<List<List<Integer>>, Integer>();
		RunningMean m = new RunningMean();
		int num = 0;
		while(true)
		{
			mats.clear();
			int repeats = 0;
			for(int reps = 0; reps < 1000000; reps++)
			{
				List<List<Integer>> mat = generateMatrix(properties);
				
				if(mats.containsKey(mat))
				{
					repeats += mats.get(mat);
					mats.put(mat, mats.get(mat) + 1);
				}
				else
					mats.put(mat, 1);
				
				
			}
			
			m.addValue(repeats);
			num++;
			
			System.out.println("value=" + repeats + ",mean=" + m.getMean() + ",std.error=" 
					+ Math.sqrt(m.getVariance()/num) + ",n=" + num);
			
			if(num > 30 && Math.sqrt(m.getVariance()/num)*1.96 < 0.0)
				break;
			
		}
		return (int) m.getMean();
	}

	@SuppressWarnings("unchecked")
	private static List<List<Integer>> generateMatrix(List<Integer> properties) {
		//let's uniformly at random generate a valid matrix
		List<List<Integer>> mat = new ArrayList<List<Integer>>();
		
		//first pick a row property
		int rowProp = (int) (Math.random()*properties.size());
		//next pick a column property that is different from rowProp
		int colProp = rowProp;
		while(colProp == rowProp)
			colProp = (int) (Math.random()*properties.size());
		
		//next decide if we want the row property to be the same or different
		boolean rowSame = (Math.random() > 0.5 ? true : false);
		//next decide if we want the col property to be the same or different
		boolean colSame = (Math.random() > 0.5 ? true : false);
		
		//next generate the objects for the first row
		for(int i = 0; i < 9; i++)
			mat.add(createObject(properties));
		
		//if the rowSame is true, then make sure the first elmts of each row have a diff value for rowProp
		if(rowSame)
			alignProperty(rowProp, !rowSame, properties, mat.get(0), mat.get(3), mat.get(6));
		
		//if the colSame is true, then make sure the first elmts of each col have a diff value for colProp
		if(colSame)
			alignProperty(colProp, !colSame, properties, mat.get(0), mat.get(1), mat.get(2));
			
		//now make sure the rows are aligned for the row property
		alignProperty(rowProp, rowSame, properties, mat.get(0), mat.get(1), mat.get(2));
		alignProperty(rowProp, rowSame, properties, mat.get(3), mat.get(4), mat.get(5));
		alignProperty(rowProp, rowSame, properties, mat.get(6), mat.get(7), mat.get(8));
		
		//finally make sure the cols are aligned
		alignProperty(colProp, colSame, properties, mat.get(0), mat.get(3), mat.get(6));
		alignProperty(colProp, colSame, properties, mat.get(1), mat.get(4), mat.get(7));
		alignProperty(colProp, colSame, properties, mat.get(2), mat.get(5), mat.get(8));
		
		//because of random chance, it is possible for there to be a pattern exhibited across
		//the first 2 rows and the first 2 objects of the last row, but not on the entirety
		//of the last row. This would then not be considered a valid matrix. Same goes for
		//the columns.
		int[] r1 = getDiffs(mat.get(0), mat.get(1), mat.get(2));
		int[] r2 = getDiffs(mat.get(3), mat.get(4), mat.get(5));
		int[] r3 = getDiffs(mat.get(6), mat.get(7), mat.get(8));
		int[] r3t = getDiffs(mat.get(6), mat.get(7));
		
		boolean invalid = false;
		for(int i = 0; i < r1.length; i++)
		{
			//if there is a pattern across the first two rows and
			//first two objects of the last row but not on the last
			//row entirely, it is invalid
			if(r1[i] == r2[i] && r2[i] == r3t[i] && r1[i] != r3[i])
				invalid = true;
		}
		
		if(invalid)
			return generateMatrix(properties);
		
		int[] c1 = getDiffs(mat.get(0), mat.get(3), mat.get(6));
		int[] c2 = getDiffs(mat.get(1), mat.get(4), mat.get(7));
		int[] c3 = getDiffs(mat.get(2), mat.get(5), mat.get(8));
		int[] c3t = getDiffs(mat.get(2), mat.get(5));
		
		for(int i = 0; i < c1.length; i++)
		{
			//if there is a pattern across the first two cols and
			//first two objects of the last col but not on the last
			//col entirely, it is invalid
			if(c1[i] == c2[i] && c2[i] == c3t[i] && c1[i] != c3[i])
				invalid = true;
		}
		
		if(invalid)
			return generateMatrix(properties);
		
		//finally check to make sure there are no repeat objects
		boolean repeat = false;
		for(int i = 0; i < mat.size() && !repeat; i++)
		{
			for(int j = i + 1; j < mat.size() && !repeat; j++)
			{
				if(mat.get(i).equals(mat.get(j)))
					repeat = true;
			}
		}
		if(repeat)
			return generateMatrix(properties);
		
		return mat;
	}
	
	private static void alignProperty(int prop, boolean same, List<Integer> properties, List<Integer> ... objects)
	{
		if(same)
		{
			for(int i = 1; i < objects.length; i++)
				objects[i].set(prop, objects[0].get(prop));
		}
		else
		{
			for(int i = 1; i < objects.length; i++)
			{
				while(true)
				{
					boolean overlap = false;
					for(int j = 0; j < i; j++)
					{
						if(objects[i].get(prop).equals(objects[j].get(prop)))
						{
							overlap = true;
							break;
						}
					}
					if(!overlap)
						break;
					else //if there was overlap and we need all values to be different, pick a different one
						objects[i].set(prop, ((int)(Math.random()*properties.get(prop))));
				}
			}
		}
	}
	
	private static List<Integer> createObject(List<Integer> properties)
	{
		List<Integer> ret = new ArrayList<Integer>();
		for(int i = 0; i < properties.size(); i++)
			ret.add((Integer)(int)(Math.random()*properties.get(i)));
		return ret;
	}
	
//	private static int computeNumberOfTasks(List<Integer> properties)
//	{
//		System.out.println("Creating all objects...");
//		List<Integer> alphabet = new ArrayList<Integer>();
//		int max = 0;
//		for(Integer i : properties)
//		{
//			if(i > max)
//				max = i;
//		}
//		for(int i = 0; i < max; i++)
//			alphabet.add(i);
//		
//		List<List<Integer>> objects = Utility.createAllStringsOfSize(alphabet, max);
//		for(int i = 0; i < objects.size();)
//		{
//			boolean valid = true;
//			for(int j = 0; j < properties.size(); j++)
//			{
//				if(objects.get(i).get(j) >= properties.get(j))
//					valid = false;
//			}
//			if(!valid)
//				objects.remove(i);
//			else
//				i++;
//		}
//		
//		System.out.println("Computing...");
////		List<List<List<Integer>>> allMats = getValidMatrices(new ArrayList<List<Integer>>(), 
////				new int[properties.size()], objects);
//		List<List<List<Integer>>> allMats = getValidMatrices(objects);
//		return allMats.size();
//		
//	}
//	
//	@SuppressWarnings("unchecked")
//	private static List<List<List<Integer>>> getValidMatrices(List<List<Integer>> objects)
//	{
//		//first lets compute every possible row of 3 objects
//		System.out.println("Computing all sets of 3...");
//		Map<List<List<Integer>>, int[]> diffs = new HashMap<List<List<Integer>>, int[]>();
//		List<List<List<Integer>>> setsOf3 = Utility.createAllUniqueCombosOfSize(objects, 3); 
//		for(List<List<Integer>> set : setsOf3)
//			diffs.put(set, getDiffs(set.get(0), set.get(1), set.get(2)));
//		
//		//next iterate over every pair of rows and see if they are compatible
//		//if they are, then iterate over all other rows and see if there are 3 that
//		//are compatible
//		List<List<List<Integer>>> mats = new LinkedList<List<List<Integer>>>();
//		System.out.println("Computing all row-valid matrices...");
//		for(int i = 0; i < setsOf3.size(); i++)
//		{
//			System.out.println(i + "/" + setsOf3.size());
//			int[] r1 = diffs.get(setsOf3.get(i));
//			for(int j = i + 1; j < setsOf3.size(); j++)
//			{
//				int[] r2 = diffs.get(setsOf3.get(j));
//				if(compatibleDiffs(r1, r2))
//				{
//					for(int k = j + 1; k < setsOf3.size(); k++)
//					{
//						int[] r3 = diffs.get(setsOf3.get(k));
//						if(compatibleDiffs(r1, r2, r3))
//						{
//							List<List<Integer>> mat = new ArrayList<List<Integer>>();
//							mat.addAll(setsOf3.get(i));
//							mat.addAll(setsOf3.get(j));
//							mat.addAll(setsOf3.get(k));
//							mats.add(mat);
//						}
//					}
//				}
//			}
//		}
//		
//		return mats;
//		
//	}
//	
//	private static boolean compatibleDiffs(int[] ... d)
//	{
//		/*
//		 //this code return true if they all agree in at least one spot
//		for(int i = 0; i < d[0].length; i++)
//		{
//			boolean agreement = true;
//			for(int j = 0; j < d.length - 1; j++)
//			{
//				if(d[j][i] != d[j+1][i])
//				{
//					agreement = false;
//					break;
//				}
//			}
//			if(agreement)
//				return true;
//		}
//		return false;
//		/*/
//		//this code returns true only if they all agree in every spot
//		for(int i = 0; i < d[0].length; i++)
//		{
//			boolean disagreement = false;
//			for(int j = 0; j < d.length - 1; j++)
//			{
//				if(d[j][i] != d[j+1][i])
//				{
//					disagreement = true;
//					break;
//				}
//			}
//			if(disagreement)
//				return false;
//		}
//		return true;
//		//*/
//	}
	
//	@SuppressWarnings("unchecked")
//	private static List<List<List<Integer>>> getValidMatrices(List<List<Integer>> matrixSoFar, 
//			int[] diffs, List<List<Integer>> objects) {
//		
//		List<List<List<Integer>>> ret = new ArrayList<List<List<Integer>>>();
//		
//		if(matrixSoFar.size() == 9)
//		{
//			//this is a valid matrix, so let's return it
//			List<List<Integer>> mat = new ArrayList<List<Integer>>(matrixSoFar); //make a copy
//			ret.add(mat);
//			return ret;
//		}
//		
//		//otherwise we need to go to the recursive case
//		for(List<Integer> obj : objects)
//		{
//			//for diagnostic purposes, print out
//			if(matrixSoFar.size() == 0)
//				System.out.println(objects.indexOf(obj) + "/" + objects.size());
//			
//			//we don't want any repeats
//			if(matrixSoFar.contains(obj))
//				continue;
//			//each of the 9 cases needs to be treated seperately
//			if(matrixSoFar.size() < 3 ) //pick any three objects
//			{
//				matrixSoFar.add(obj);
//				ret.addAll(getValidMatrices(matrixSoFar, diffs, objects));
//				matrixSoFar.remove(obj);
//			}
//			else if(matrixSoFar.size() == 3)
//			{
//				diffs = getDiffs(matrixSoFar.get(0), matrixSoFar.get(1), matrixSoFar.get(2));
//				
//				//now recurse again, but this time with the computed diffs
//				matrixSoFar.add(obj);
//				ret.addAll(getValidMatrices(matrixSoFar, diffs, objects));
//				matrixSoFar.remove(obj);
//			}
//			else if(matrixSoFar.size() == 4)
//			{
//				//we need to make sure that the (2,2) object varies from (2,1) according to diffs
//				//special case for only 2 objects in the row so far
//				int[] d = getDiffs(matrixSoFar.get(3), obj);
//				boolean valid = true;
//				for(int i = 0; i < diffs.length; i++)
//				{
//					if(diffs[i] != 0 && diffs[i] != d[i])
//					{
//						valid = false;
//						break;
//					}
//				}
//				
//				if(!valid)
//					continue;
//				
//				//if we made it this far, then let's add obj to the matrix and recurse
//				matrixSoFar.add(obj);
//				ret.addAll(getValidMatrices(matrixSoFar, diffs, objects));
//				matrixSoFar.remove(obj);
//			}
//			else if(matrixSoFar.size() == 5)
//			{
//				//now we need to make sure that the entire second row follows diffs
//				if(intArrayEqual(diffs, getDiffs(matrixSoFar.get(3), matrixSoFar.get(4), obj)))
//					continue;
//				
//				//if we made it this far, then let's add obj to the matrix and recurse
//				matrixSoFar.add(obj);
//				ret.addAll(getValidMatrices(matrixSoFar, diffs, objects));
//				matrixSoFar.remove(obj);
//			}
//			else if(matrixSoFar.size() == 6) //we can pick w/e object for (3,1)
//			{
//				matrixSoFar.add(obj);
//				ret.addAll(getValidMatrices(matrixSoFar, diffs, objects));
//				matrixSoFar.remove(obj);
//			}
//			else if(matrixSoFar.size() == 7)
//			{
//				//we need to make sure that the (3,2) object varies from (3,1) according to diffs
//				//special case for only 2 objects in the row so far
//				int[] d = getDiffs(matrixSoFar.get(6), obj);
//				boolean valid = true;
//				for(int i = 0; i < diffs.length; i++)
//				{
//					if(diffs[i] != 0 && diffs[i] != d[i])
//					{
//						valid = false;
//						break;
//					}
//				}
//				
//				if(!valid)
//					continue;
//				
//				//if we made it this far, then let's add obj to the matrix and recurse
//				matrixSoFar.add(obj);
//				ret.addAll(getValidMatrices(matrixSoFar, diffs, objects));
//				matrixSoFar.remove(obj);
//			}
//			else if(matrixSoFar.size() == 8)
//			{
//				if(intArrayEqual(diffs, getDiffs(matrixSoFar.get(6), matrixSoFar.get(7), obj)))
//					continue;
//				
//				//if we made it this far, then let's add obj to the matrix and recurse
//				matrixSoFar.add(obj);
//				ret.addAll(getValidMatrices(matrixSoFar, diffs, objects));
//				matrixSoFar.remove(obj);
//			}
//		}
//		
//		return ret;
//	}
	
	private static int[] getDiffs(List<Integer> ... objects)
	{
		int[] ret = new int[objects[0].size()];
		for(int i = 0; i < objects[0].size(); i++)
		{
			ret[i] = 0;
			//first check for same-ness
			boolean diff = false;
			for(int j = 0; j < objects.length - 1; j++)
			{
				if(!objects[j].get(i).equals(objects[j+1].get(i)))
				{
					diff = true;
					break;
				}
			}
			if(!diff)
				ret[i] = 1;
			else
			{
				boolean same = false;
				//let's see if they are all different from each other
				for(int j = 0; j < objects.length && !same; j++)
				{
					for(int k = j + 1; k < objects.length && !same; k++)
					{
						if(objects[j].get(i).equals(objects[k].get(i)))
							same = true;
					}
				}
				if(!same)
					ret[i] = -1;
			}
		}
		return ret;
	}
	
	private static boolean intArrayEqual(int[] a, int[] b)
	{
		if(a.length != b.length)
			return false;
		for(int i = 0; i < a.length; i++)
		{
			if(a[i] != b[i])
				return false;
		}
		return true;
	}

}




















