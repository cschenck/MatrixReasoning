package featureExtraction.AudioFFT;

import java.io.File;
import java.io.PrintStream;

public class ExampleFFTProgram {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String filename = "/home/users/jsinapov/weldspace/toolSimJava/sound/data/RobotGraspAudio/Book_01/Book_01_01.wav";//PCM, mono, 16bit, bigEndian, signed
		DiscreteFourierTransform fft = FFTFactory.getFFT(filename, 33);
		int counter = 0;
		
		//ArrayList<double[]> fftList = new ArrayList<double[]>();
		//ArrayList<Long> samplesList = new ArrayList<Long>();
		double maxIntensity = Double.MIN_VALUE;
		try {
			Data spectrum = fft.getData();
			File outfile = new File("FFTData33.vec");
			outfile.createNewFile();
			PrintStream fileOut = new PrintStream(outfile);
			fileOut.println("$TYPE vec");
			fileOut.println("$XDIM 394401");
			fileOut.println("$YDIM 1");
			fileOut.println("$VEC_DIM 33");
			

			while (!(spectrum instanceof DataEndSignal)) {
				if (spectrum instanceof DoubleData) {
					double[] spectrumData = ((DoubleData) spectrum).getValues();
					//System.out.println(((DoubleData)spectrum).getFirstSampleNumber());
					double[] intensities = new double[spectrumData.length];
					for (int i = 0; i < intensities.length; i++) {
						/*
						 * A very small intensity is, for all intents and
						 * purposes, the same as 0.
						 */
						intensities[i] = Math.max(Math.log(spectrumData[i]),
								0.0);
						if (intensities[i] > maxIntensity) {
							maxIntensity = intensities[i];
						}
						fileOut.print(intensities[i] + " ");
					}
					//fftList.add(intensities);
					counter++;
					fileOut.println("" + counter);
				}
				
				spectrum = fft.getData();
				
			}
		} catch (Exception e) {
			System.exit(-1);
		}
		System.out.println("NumVecs = " + counter);

	}

}
