package featureExtraction.backgroundSubtraction;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.imageio.ImageIO;

import utility.RunningMean;
import utility.Utility;

public class BackgroundSubtraction {
	
	protected BufferedImage workingImage;
	
	protected class Pixel {
		public final int i;
		public final int j;
		
		public Pixel(int i, int j)
		{
			this.i = i;
			this.j = j;
		}
		
		public int getRed() {
			return new Color(workingImage.getRGB(i, j)).getRed();
		}
		
		public int getBlue() {
			return new Color(workingImage.getRGB(i, j)).getBlue();
		}
		
		public int getGreen() {
			return new Color(workingImage.getRGB(i, j)).getGreen();
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if(!(obj instanceof Pixel))
				return false;
			
			Pixel p = (Pixel) obj;
			return p.i == this.i && p.j == this.j;
		}
		
		@Override
		public int hashCode()
		{
			return this.i*this.j*this.i;
		}
		
	}
	
	private class BackgroundModel
	{
		
		private static final double MAX_DEVIATION = 1.5;
		
		private RunningMean[][] modelRed;
		private RunningMean[][] modelBlue;
		private RunningMean[][] modelGreen;
		
		public BackgroundModel(int width, int height)
		{
			modelRed = new RunningMean[width][height];
			modelBlue = new RunningMean[width][height];
			modelGreen = new RunningMean[width][height];
			for(int i = 0; i < width; i++)
			{
				for(int j = 0; j < height; j++)
				{
					modelRed[i][j] = new RunningMean();
					modelBlue[i][j] = new RunningMean();
					modelGreen[i][j] = new RunningMean();
				}
			}
		}
		
		public void addTrainingImage(Pixel[][] image)
		{
			if(image.length != modelRed.length || image[0].length != modelRed[0].length)
				throw new IllegalArgumentException("Given image with dimensions (" + image.length + "x" 
						+ image[0].length + "), expected (" + modelRed.length + "x" + modelRed[0].length + ")");
			
			for(int i = 0; i < image.length; i++)
			{
				for(int j = 0; j < image[0].length; j++)
				{
					modelRed[i][j].addValue(image[i][j].getRed());
					modelBlue[i][j].addValue(image[i][j].getBlue());
					modelGreen[i][j].addValue(image[i][j].getGreen());
				}
			}
		}
		
		public boolean isBackground(Pixel p)
		{
			if(Math.abs(modelRed[p.i][p.j].getMean() - p.getRed())/Math.sqrt(modelRed[p.i][p.j].getVariance()) > MAX_DEVIATION)
				return false;
			
			if(Math.abs(modelBlue[p.i][p.j].getMean() - p.getBlue())/Math.sqrt(modelBlue[p.i][p.j].getVariance()) > MAX_DEVIATION)
				return false;
			
			if(Math.abs(modelGreen[p.i][p.j].getMean() - p.getGreen())/Math.sqrt(modelGreen[p.i][p.j].getVariance()) > MAX_DEVIATION)
				return false;
			
			return true;
		}
	}
	
	protected Pixel[][] pixels;
	private BackgroundModel model;
	private int id; //TODO debug
	
	public BackgroundSubtraction(int width, int height, int id)
	{
		this.id = id;
		model = new BackgroundModel(width, height);
		pixels = new Pixel[width][height];
		for(int i = 0; i < pixels.length; i++)
		{
			for(int j = 0; j < pixels[0].length; j++)
				pixels[i][j] = new Pixel(i, j);
		}
	}
	
	/**
	 * Trains the background model for background subtraction.
	 * 
	 * @param imagePaths	A list of file paths to images, rather than the images 
	 * 						themselves so that a ton of memory is not used.
	 * @throws IOException 
	 */
	public void trainBackgroundModel(List<String> imagePaths) throws IOException
	{
		int count = 0;
		for(String imagePath : imagePaths)
		{
			System.out.println("training on (" + count++ + "/" + imagePaths.size() + ") - " + imagePath);
			workingImage = ImageIO.read(new File(imagePath));
			if(workingImage.getWidth() != pixels.length || workingImage.getHeight() != pixels[0].length)
				throw new IllegalArgumentException(imagePath + " has incorrect dimensions");
			model.addTrainingImage(pixels);
		}
		
		//TODO debug now output a mean image
		BufferedImage img = ImageIO.read(new File(imagePaths.get(0)));
		BufferedImage mean = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
		for(int i = 0; i < mean.getWidth(); i++)
		{
			for(int j = 0; j < mean.getHeight(); j++)
			{
				mean.setRGB(i, j, new Color((int)this.getAverageRedValue(i, j), (int)this.getAverageGreenValue(i, j), 
						(int)this.getAverageBlueValue(i, j)).getRGB());
			}
		}
		if(!ImageIO.write(mean, "jpg", new File("backgroundSubtractedImages/mean_image_" + id + ".jpg")))
			System.err.println("Error writing mean image");
	}
	
	public BufferedImage subtractBackground(BufferedImage image)
	{
		workingImage = image;
		Set<Pixel> set = new HashSet<BackgroundSubtraction.Pixel>();
		for(int i = 0; i < pixels.length; i++)
		{
			for(int j = 0; j < pixels[0].length; j++)
			{
				if(!model.isBackground(pixels[i][j]))
					set.add(pixels[i][j]);
			}
		}
		
		for(int i = 0; i < 0; i++)
			set = erode(set);
		
		for(int i = 0; i < 0; i++)
			set = dialte(set);
		
		for(int i = 0; i < 0; i++)
			set = erode(set);
		
//		Set<Pixel> largest = new HashSet<BackgroundSubtraction.Pixel>();
//		while(!set.isEmpty() && largest.size() < set.size()) //if there are more pixels in the largest connected region than remain, we are done
//		{
//			
//			Pixel start = set.iterator().next();
//			if(model.isBackground(start))
//			{
//				set.remove(start);
//			}
//			else
//			{
//				Set<Pixel> component = computeConnectedComponent(start, set);
//				if(component.size() > largest.size())
//					largest = component;
//				set.removeAll(component);
//			}
//		}
//		set = largest;
		
		return applyMask(image, set);
	}
	
//	public BufferedImage subtractBackground(BufferedImage image)
//	{
//		workingImage = image;
//		Set<Pixel> set = new HashSet<BackgroundSubtraction.Pixel>();
//		for(int i = 0; i < pixels.length; i++)
//		{
//			for(int j = 0; j < pixels[0].length; j++)
//			{
//				if(!model.isBackground(pixels[i][j]))
//					set.add(pixels[i][j]);
//			}
//		}
//		
//		for(int i = 0; i < 3; i++)
//			set = erode(set);
//		
//		for(int i = 0; i < 3; i++)
//			set = dialte(set);
//		
//		return applyMask(image, set);
//	}
	
	public double getAverageRedValue(int i, int j)
	{
		return this.model.modelRed[i][j].getMean();
	}
	
	public double getAverageGreenValue(int i, int j)
	{
		return this.model.modelGreen[i][j].getMean();
	}
	
	public double getAverageBlueValue(int i, int j)
	{
		return this.model.modelBlue[i][j].getMean();
	}
	
	public double getRedStdDev(int i, int j)
	{
		return Math.sqrt(this.model.modelRed[i][j].getVariance());
	}
	
	public double getBlueStdDev(int i, int j)
	{
		return Math.sqrt(this.model.modelBlue[i][j].getVariance());
	}
	
	public double getGreenStdDev(int i, int j)
	{
		return Math.sqrt(this.model.modelGreen[i][j].getVariance());
	}
	
	private Set<Pixel> erode(Set<Pixel> set)
	{
		Set<Pixel> ret = new HashSet<BackgroundSubtraction.Pixel>();
		for(Pixel p : set)
		{
			boolean found = false; //test if there is an off pixel in the area around p
			for(Pixel neighbor : this.getNeighbors(p, false))
			{
				if(!set.contains(neighbor))
				{
					found = true;
					break;
				}
			}
			if(!found)
				ret.add(p);
		}
		
		return ret;
	}
	
	private Set<Pixel> dialte(Set<Pixel> set)
	{
		Set<Pixel> ret = new HashSet<BackgroundSubtraction.Pixel>(set);
		for(int x = 0; x < pixels.length; x++)
		{
			for(int y = 0; y < pixels[0].length; y++)
			{
				Pixel p = pixels[x][y];
				for(Pixel neighbor : this.getNeighbors(p, false))
				{
					if(set.contains(neighbor))
					{
						ret.add(p);
						break;
					}
				}
			}
		}
		
		return ret;
	}

	protected BufferedImage applyMask(BufferedImage image, Set<Pixel> mask) {
		BufferedImage ret = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

		for(Pixel p : mask)
			ret.setRGB(p.i, p.j, image.getRGB(p.i, p.j));
		
		return ret;
	}

	private Set<Pixel> computeConnectedComponent(Pixel start, Set<Pixel> foreground) {
		Set<Pixel> ret = new HashSet<BackgroundSubtraction.Pixel>();
		
		Set<Pixel> toProcess = new HashSet<BackgroundSubtraction.Pixel>();
		toProcess.add(start);
		while(!toProcess.isEmpty())
		{
			Pixel p = toProcess.iterator().next();
			toProcess.remove(p);
			ret.add(p);
			
			for(Pixel neighbor : this.getNeighbors(p, true))
			{
				if(!ret.contains(neighbor) && !toProcess.contains(neighbor) && foreground.contains(neighbor))
					toProcess.add(neighbor);
			}
		}
		
		return ret;
	}
	
	private int recurseCount = 0;
	
	private void addConnectedNeighbors(Pixel p, Set<Pixel> component)
	{
		recurseCount++;
		if(recurseCount > 1000)
			Utility.debugPrint("");
		component.add(p);
		for(Pixel neighbor : this.getNeighbors(p, true))
		{
			if(!component.contains(neighbor) && !model.isBackground(neighbor))
				addConnectedNeighbors(neighbor, component);
		}
		recurseCount--;
	}
	
	private List<Pixel> getNeighbors(Pixel p, boolean fourConnectedness)
	{
		List<Pixel> ret = new ArrayList<BackgroundSubtraction.Pixel>();
		//neighbors for 8-connectedness
		int[] i = {p.i - 1, p.i - 1, p.i - 1, p.i,     p.i,     p.i + 1, p.i + 1, p.i + 1};
		int[] j = {p.j - 1, p.j,     p.j + 1, p.j - 1, p.j + 1, p.j - 1, p.j,     p.j + 1};
		
		//neighbors for 4-connectedness
		if(fourConnectedness)
		{
			int[] i2 = {p.i - 1, p.i,     p.i,     p.i + 1};
			int[] j2 = {p.j,     p.j - 1, p.j + 1, p.j    };
			i = i2;
			j = j2;
		}
		
		for(int n = 0; n < i.length; n++)
		{
			if(i[n] >= 0 && i[n] < pixels.length && j[n] >= 0 && j[n] < pixels[0].length)
				ret.add(pixels[i[n]][j[n]]);
		}
		
		return ret;
	}

}












