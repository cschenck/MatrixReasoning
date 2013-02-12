package featureExtraction;

public class BinningFeatureExtractor {
	
	private int numCols;
	private int numRows;
	
	public BinningFeatureExtractor(int numCols, int numRows)
	{
		this.numCols = numCols;
		this.numRows = numRows;
	}
	
	public double[] bin(double[][] data)
	{
		double[][] binnedData = new double[numCols][numRows];
		
		for(int i = 0; i < data.length; i++)
		{
			for(int j = 0; j < data[0].length; j++)
			{
				int col = (int) ((double)i/data.length*numCols);
				int row = (int) ((double)j/data[0].length*numRows);
				binnedData[col][row] += data[i][j];
			}
		}
		
		double[] ret = new double[numCols*numRows];
		for(int i = 0;  i < numCols; i++)
		{
			for(int j = 0; j < numRows; j++)
				ret[i*numRows + j] = binnedData[i][j];
		}
		
		return ret;
	}

}
