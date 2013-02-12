package featureExtraction.AudioFFT;

import java.io.File;
import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


public class SoundFFT {

	/*
	 * fK should be 2^n + 1
	 * usually 33, or 65
	 */
	public double [][] getMatrixFFT(String soundFileName, int fK){

		DiscreteFourierTransform fft = this.getFFT(soundFileName, fK);
		ArrayList<double[]> columns = new ArrayList<double[]>();
		
		int counter = 0;

		double maxIntensity = Double.MIN_VALUE;
		try {
			Data spectrum = fft.getData();
	
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
						//fileOut.print(intensities[i] + " ");
					}
					
					columns.add(intensities);
					
					//fftList.add(intensities);
					counter++;
					//fileOut.println("" + counter);
				}
				
				spectrum = fft.getData();
				
			}
		} catch (Exception e) {
			System.exit(-1);
		}
		
		
		double [][] matrixFFT = new double[fK][columns.size()];
		
		for (int i = 0; i < columns.size(); i ++){
			double [] c = columns.get(i);
			
			for (int j = 0; j < c.length; j ++){
				matrixFFT[j][i] = c[j];
			}
		}
		return matrixFFT;
	}
	
	public DiscreteFourierTransform getFFT(String filename, int numFftPoints){
		File inputFile = new File(filename);
		AudioInputStream inStream = null;
		try{
			inStream = AudioSystem.getAudioInputStream(inputFile);
			
		}
		catch(Exception e){
			System.out.println(e.toString());
			System.out.println("Error initializing audio stream");
			System.exit(-1);
		}
		DiscreteFourierTransform fft = new DiscreteFourierTransform();
		fft.initialize(numFftPoints);
		
		StreamDataSource file = new StreamDataSource();
		file.setInputStream(inStream, "sound");
		file.initialize();
		Preemphasizer pre = new Preemphasizer();
		pre.initialize();
		pre.setPredecessor(file);
		RaisedCosineWindower windower = new RaisedCosineWindower();
	
		windower.initialize();
		windower.setPredecessor(file);
		fft.setPredecessor(windower);
		return fft;
	}
	
}
