package utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

public class Utility {
	
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T cloneObject(T obj)
	{
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(bout);
			oos.writeObject(obj);
			
			ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bin);
			return (T) ois.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		throw new RuntimeException();
	}
	
	public static <T> List<T> randomizeOrder(List<T> list, Random rand)
	{
		List<T> ret = new ArrayList<T>();
		list = new ArrayList<T>(list); //so we don't make any modifications to the input list
		while(list.size() > 0)
			ret.add(list.remove(rand.nextInt(list.size())));
		return ret;
	}
	
	public static <T> List<T> reverseOrder(List<T> list)
	{
		List<T> ret = new ArrayList<T>();
		for(int i = list.size() - 1; i >= 0; i--)
			ret.add(list.get(i));
		
		return ret;
	}
	
	public static <T> Set<T> findKLessThan(T object, List<T> list, int k, Comparator<T> comparator, Random rand)
	{
		List<T> ret = new ArrayList<T>();
		for(T t : list)
		{
			if(comparator.compare(t, object) < 0)
				ret.add(t);
		}
		
		while(ret.size() > k)
			ret.remove(rand.nextInt(ret.size()));
		
		return new HashSet<T>(ret);
	}
	
	public static int getMax(int[] list)
	{
		int best = 0;
		for(int i = 0; i < list.length; i++)
		{
			if(list[i] > list[best])
				best = i;
		}
		
		return best;
	}
	
	public static <T extends Comparable<T>> T getMax(List<T> list, Random rand)
	{
		T best = null;
		for(T t : list)
		{
			if(best == null || t.compareTo(best) > 0)
				best = t;
		}
		
		//now we need to make sure that if there are multiple, equivalent maxes that a random one is picked
		List<T> bestList = new ArrayList<T>();
		for(T t : list)
		{
			if(t.compareTo(best) == 0)
				bestList.add(t);
		}
		
		return bestList.get(rand.nextInt(bestList.size()));
	}
	
	public static <T> T getMax(List<T> list, Comparator<T> comparator, Random rand)
	{
		T best = null;
		for(T t : list)
		{
			if(best == null || comparator.compare(t, best) > 0)
				best = t;
		}
		
		//now we need to make sure that if there are multiple, equivalent maxes that a random one is picked
		List<T> bestList = new ArrayList<T>();
		for(T t : list)
		{
			if(comparator.compare(t, best) == 0)
				bestList.add(t);
		}
		
		return bestList.get(rand.nextInt(bestList.size()));
	}
	
	public static <T> List<List<T>> createRandomListsOfSize(List<T> objects, int size, int numLists, boolean allowRepeats, Random rand)
	{
		//first we need to figure out how many possible lists there are of this size
		BigInteger maxLists = numberOfChoices(objects.size(), size);
		//if there are fewer possible lists than asked for and repeats aren't allowed then just return all possible lists
		if(!allowRepeats && maxLists.compareTo(new BigInteger(numLists + "")) < 0)
			return createAllUniqueCombosOfSize(objects, size);
		
		List<List<T>> ret = new ArrayList<List<T>>();
		for(int i = 0; i < numLists; i++)
		{
			List<T> list = new ArrayList<T>(objects);
			while(list.size() > size)
				list.remove(rand.nextInt(list.size()));
			
			//if we aren't allowing repeats and this list was already generated
			if(!allowRepeats && ret.contains(list))
				i--; //don't count this list
			else
				ret.add(list);
		}
		
		return ret;
	}
	
	public static BigInteger numberOfChoices(int n, int k)
	{
		return factorial(n).divide(factorial(k).multiply(factorial(n - k)));
	}
	
	public static BigInteger factorial(int n)
	{
		BigInteger ret = new BigInteger("1");
		for(int i = 1; i <= n; i++)
			ret = ret.multiply(new BigInteger(i + ""));
		return ret;
	}
	
	public static <T> List<List<T>> createAllUniqueCombosOfSize(List<T> objects, int size)
	{
		//this is intractable when (|objects| choose size) is large
		//but we want to make sure it works in every other case
		//so either create all combos to include, or create all reverse combos
		boolean reverse = false;
		if(size > objects.size() - size)
			reverse = true;
		
		List<List<T>> ret = createAllCombosOfSize(objects, (reverse ? objects.size() - size : size));
		//now remove all duplicates
		for(int i = 0; i < ret.size();)
		{
			//if we're reversing for efficiency, then flip this list
			if(reverse)
			{
				List<T> list = new ArrayList<T>(objects);
				list.removeAll(ret.get(i));
				ret.set(i, list);
			}
			
			//let's check to see if ret.get(i) is a duplicate
			//and we don't care about order
			boolean found = false;
			for(int j = i + 1; j < ret.size() && !found; j++)
			{
				boolean containsAll = true;
				for(int n = 0; n < size; n++)
				{
					if(!ret.get(j).contains(ret.get(i).get(n)))
						containsAll = false;
				}
				
				if(containsAll)
					found = true;
			}
			
			if(found)
				ret.remove(i);
			else
				i++;
		}
		
		return ret;
	}
	
	public static <T> List<List<T>> createAllCombosOfSize(List<T> objects, int size)
	{
		List<List<T>> ret = new ArrayList<List<T>>();
		
		//base case
		if(size == 0)
		{
			ret.add(new ArrayList<T>());
		}
		else //recursive case
		{
			List<List<T>> smallerCombos = createAllCombosOfSize(objects, size - 1);
			for(List<T> list : smallerCombos)
			{
				for(T object : objects)
				{
					//don't double include objects
					if(list.contains(object))
						continue;
					
					List<T> newList = new ArrayList<T>(list);
					newList.add(object);
					ret.add(newList);
				}
			}
		}
		
		return ret;
			
	}
	
	public static <T> List<T> convertToList(T[] list)
	{
		List<T> ret = new ArrayList<T>();
		for(T t : list)
			ret.add(t);
		return ret;
	}
	
	public static List<Integer> convertToList(int[] list)
	{
		List<Integer> ret = new ArrayList<Integer>();
		for(Integer t : list)
			ret.add(t);
		return ret;
	}
	
	public static <T> List<List<T>> createAllStringsOfSize(List<T> alphabet, int size)
	{
		List<List<T>> ret = new ArrayList<List<T>>();
		if(size <= 0)
			ret.add(new ArrayList<T>());
		else
		{
			List<List<T>> smaller = createAllStringsOfSize(alphabet, size - 1);
			for(List<T> list : smaller)
			{
				for(T letter : alphabet)
				{
					List<T> newList = new ArrayList<T>(list);
					newList.add(0, letter);
					ret.add(newList);
				}
			}
		}
		return ret;
	}
	
	public static void printTable(List<String> columnHeaders, List<String> rowHeaders, double[][] data, boolean headersOnRight)
	{
		String[][] stringData = new String[data.length][data[0].length];
		for(int i = 0; i < data.length; i++)
		{
			for(int j = 0; j < data[0].length; j++)
				stringData[i][j] = String.format("%.4g", data[i][j]);
		}
		
		printTable(columnHeaders, rowHeaders, stringData, headersOnRight);
	}
	
	public static void printTable(List<String> columnHeaders, List<String> rowHeaders, String[][] data, boolean headersOnRight)
	{
		int maxRowLength = 0;
		if(!headersOnRight)
		{
			for(String row : rowHeaders)
				if(row.length() > maxRowLength)
					maxRowLength = row.length();
		}
		
		int[] maxColumnLength = new int[columnHeaders.size()];
		for(int i = 0; i < columnHeaders.size(); i++)
		{
			String column = columnHeaders.get(i);
			if(column.length() > maxColumnLength[i])
				maxColumnLength[i] = column.length();
		}
		
		for(int i = 0; i < data.length; i++)
		{
			for(int j = 0; j < data[0].length; j++)
			{
				if(data[i][j].length() > maxColumnLength[j])
					maxColumnLength[j] = data[i][j].length();
			}
		}
		
		if(!headersOnRight)
			System.out.format("%" + maxRowLength + "." + maxRowLength + "s", " ");
		
		for(int i = 0; i < columnHeaders.size(); i++)
			System.out.format("%" + (maxColumnLength[i] + 2) + "." + (maxColumnLength[i] + 2) + "s", columnHeaders.get(i));
		System.out.println();
		
		for(int i = 0; i < data.length; i++)
		{
			if(!headersOnRight)
				System.out.format("%" + maxRowLength + "." + maxRowLength + "s", rowHeaders.get(i));
			for(int j = 0; j < data[i].length; j++)
			{
				String number = data[i][j];
				number = String.format("%" + (maxColumnLength[j] + 2 - number.length()) + "." + (maxColumnLength[j] + 2 - number.length()) + "s", " ") + number;
				System.out.print(number);
			}
			
			if(headersOnRight)
				System.out.print("  " + rowHeaders.get(i));
			
			System.out.println();
		}
	}
	
	public static void debugPrint(String s)
	{
		StackTraceElement[] eles = Thread.currentThread().getStackTrace();
		StackTraceElement ele = null;
		
		int i;
		for(i = 1; i < eles.length && eles[i].getClassName().endsWith(".Utility"); i++) {}
		ele = eles[i];
		
		System.err.print(ele.getClassName() + "." + ele.getMethodName() + ":" + ele.getLineNumber() + ":" + s);
	}
	
	public static void debugPrintln(String s)
	{
		debugPrint(s + "\n");
	}
	
	public static String doubleToStringWithCommas(double d)
	{
		String ds = Double.toString(d);
		String ret = "";
		int periodLocation = ds.lastIndexOf(".");
		if(periodLocation == -1)
			periodLocation = ds.length();
		
		for(int i = periodLocation - 1; i >= 0; i--)
		{
			if(i > 0 && (periodLocation - 1 - i) % 3 == 2)
				ret = "," + ds.charAt(i) + ret;
			else
				ret = ds.charAt(i) + ret;
		}
		
		for(int i = periodLocation; i < ds.length(); i++)
			ret += ds.charAt(i);
		
		return ret;
	}
	
	public static <T> Set<T> intersection(Set<T> ... sets)
	{
		Set<T> ret = new HashSet<T>();
		for(T t : sets[0])
		{
			ret.add(t);
			for(Set<T> set : sets)
			{
				if(!set.contains(t))
					ret.remove(t);
			}
		}
		
		return ret;
	}
	
	public static <T> Map<T, Double> normalize(Map<T, Double> map)
	{
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		
		for(Double d : map.values())
		{
			if(d > max)
				max = d;
			if(d < min)
				min = d;
		}
		
		Map<T, Double> ret = new HashMap<T, Double>();
		for(Entry<T, Double> e : map.entrySet())
			ret.put(e.getKey(), (e.getValue() - min)/(max - min));
		
		return ret;
	}
	
}
