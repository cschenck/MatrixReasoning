package featureExtraction.backgroundSubtraction;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import utility.Utility;

import featureExtraction.backgroundSubtraction.BackgroundSubtraction.Pixel;

public class ROIBackgroundSubtraction extends BackgroundSubtraction {
	
	private static final int MAX_X = 299;
	private static final int MIN_X = 199;
	private static final int MAX_Y = 416;
	private static final int MIN_Y = 316;

	public ROIBackgroundSubtraction(int width, int height, int id) {
		super(width, height, id);
	}
	
	public void trainBackgroundModel(List<String> imagePaths) throws IOException
	{
		Utility.debugPrintln("ROIBackgroundSubtraction requires no training");
	}
	
	public BufferedImage subtractBackground(BufferedImage image)
	{
		workingImage = image;
		Set<Pixel> set = new HashSet<BackgroundSubtraction.Pixel>();
		for(int i = 0; i < pixels.length; i++)
		{
			for(int j = 0; j < pixels[0].length; j++)
			{
				if(i >= MIN_X && i <= MAX_X && j >= MIN_Y && j <= MAX_Y)
					set.add(pixels[i][j]);
			}
		}
		
		return applyMask(image, set);
	}
	
	public double getAverageRedValue(int i, int j)
	{
		throw new UnsupportedOperationException();
	}
	
	public double getAverageGreenValue(int i, int j)
	{
		throw new UnsupportedOperationException();
	}
	
	public double getAverageBlueValue(int i, int j)
	{
		throw new UnsupportedOperationException();
	}
	
	public double getRedStdDev(int i, int j)
	{
		throw new UnsupportedOperationException();
	}
	
	public double getBlueStdDev(int i, int j)
	{
		throw new UnsupportedOperationException();
	}
	
	public double getGreenStdDev(int i, int j)
	{
		throw new UnsupportedOperationException();
	}

}
