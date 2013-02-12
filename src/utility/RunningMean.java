package utility;

import java.util.ArrayList;
import java.util.List;

public class RunningMean {
	
	private int n = 0;
	private double m = 0;
	private double s = 0;
	
	//because math is hard
	//private List<Double> data = new ArrayList<Double>();
	
	public void addValue(double x)
	{
		//special case for if this is the first thing added
		if(n == 0)
		{
			n++;
			m = x;
			s = 0;
		}
		else
		{
			n++;
			double oldm = m;
			m = oldm + (x - oldm)/n;
			s = s + (x - oldm)*(x - m);
		}
		
		//data.add(x);
	}
	
	public double getVariance()
	{
		if(n - 1 <= 0)
			return 0.0;
		else
			return s/(n - 1);
	}
	
	public double getMean()
	{
		return m;
	}
	
	public double getStandardDeviation()
	{
		return Math.sqrt(this.getVariance());
	}
	
	public int getN()
	{
		return n;
	}
	
//	public void combine(RunningMean other)
//	{
//		n = 0;
//		m = 0;
//		s = 0;
//		
//		List<Double> newData = new ArrayList<Double>(data);
//		newData.addAll(other.data);
//		data.clear();
//		
//		for(double x : newData)
//			this.addValue(x);
//	}
	
//	public List<Double> getDataPoints()
//	{
//		return data;
//	}
	
	@Override
	public String toString()
	{
		return this.getMean() + "";
	}

}
