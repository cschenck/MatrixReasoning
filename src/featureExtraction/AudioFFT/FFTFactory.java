package featureExtraction.AudioFFT;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


public class FFTFactory {
	/**
	 * Pass this function a filename and a number of fft points you want, and 
	 * it will return a DiscreteFourierTransform object that will give you the
	 * spectrums (numPoints must be a power of 2 plus 1.
	 * 
	 * The transform gives you data in 10ms intervals, over a window of 26.625ms.
	 * 
	 * Note:  For this to work, the audio must be PCM, mono, 16bit, bigEndian, signed
	 * 
	 * This can be changed by altering the initialization of the audio input stream
	 * stuff.
	 * 
	 * @param filename
	 * @param numFftPoints
	 * @return
	 */
	public static  DiscreteFourierTransform getFFT(String filename, int numFftPoints){
		File inputFile = new File(filename);
		AudioInputStream inStream = null;
		try{
			inStream = AudioSystem.getAudioInputStream(inputFile);
		}
		catch(Exception e){
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
