/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2002-2004 Sun Microsystems, Inc.  
 * Portions Copyright 2002-2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package featureExtraction.AudioFFT;




/**
 * Implements a high-pass filter that compensates for attenuation in the audio
 * data. Speech signals have an attenuation (a decrease in intensity of a
 * signal) of 20 dB/dec. It increases the relative magnitude of the higher
 * frequencies with respect to the lower frequencies.
 * 
 * The Preemphasizer takes a {@link Data}object that usually represents audio
 * data as input, and outputs the same {@link Data}object, but with
 * preemphasis applied. For each value X[i] in the input Data object X, the
 * following formula is applied to obtain the output Data object Y:
 * <p>
 * <code>
 * Y[i] = X[i] - (X[i-1] * preemphasisFactor)
 * </code>
 * <p>
 * where 'i' denotes time.
 * <p>
 * The preemphasis factor has a value defined by the field {@link
 * #PROP_PREEMPHASIS_FACTOR}, with default defined by {@link
 * #PROP_PREEMPHASIS_FACTOR_DEFAULT}. A common value for this factor is
 * something around 0.97.
 * <p>
 * Other {@link Data}objects are passed along unchanged through this
 * Preemphasizer.
 * <p>
 * The Preemphasizer emphasizes the high frequency components, because they
 * usually contain much less energy than lower frequency components, even
 * though they are still important for speech recognition. It is a high-pass
 * filter because it allows the high frequency components to "pass through",
 * while weakening or filtering out the low frequency components.
 */
public class Preemphasizer{
    /**
     * The name of the SphinxProperty for preemphasis factor/alpha.
     */
    public static final String PROP_PREEMPHASIS_FACTOR = "factor";
    /**
     * The default value of PROP_PREEMPHASIS_FACTOR.
     */
    public static final double PROP_PREEMPHASIS_FACTOR_DEFAULT = 0.97;
    private double preemphasisFactor;
    private double prior;
    
    
    
    private String name;
    private StreamDataSource predecessor;
    private Timer timer;

    public void setPredecessor(StreamDataSource predecessor) {
        this.predecessor = predecessor;
    }
    
    public void initialize(){
    	preemphasisFactor = 
                PROP_PREEMPHASIS_FACTOR_DEFAULT;
    	this.timer = Timer.getTimer(name);
    }

    /**
     * Returns the next Data object being processed by this Preemphasizer, or
     * if it is a Signal, it is returned without modification.
     * 
     * @return the next available Data object, returns null if no Data object
     *         is available
     * 
     * @throws DataProcessingException
     *                 if there is a processing error
     * 
     * @see Data
     */
    public Data getData() throws DataProcessingException {
        Data input = predecessor.getData();
        timer.start();
        if (input != null) {
            if (input instanceof DoubleData) {
                applyPreemphasis(((DoubleData) input).getValues());
            } else if (input instanceof DataEndSignal) {
                prior = 0;
            }
        }
        timer.stop();
        return input;
    }

    /**
     * Applies pre-emphasis filter to the given Audio. The preemphasis is
     * applied in place.
     * 
     * @param in
     *                audio data
     */
    private void applyPreemphasis(double[] in) {
        // set the prior value for the next Audio
        double nextPrior = prior;
        if (in.length > 0) {
            nextPrior = in[in.length - 1];
        }
        if (in.length > 1 && preemphasisFactor != 0.0) {
            // do preemphasis
            double current;
            double previous = in[0];
            in[0] = previous - preemphasisFactor * prior;
            for (int i = 1; i < in.length; i++) {
                current = in[i];
                in[i] = current - preemphasisFactor * previous;
                previous = current;
            }
        }
        prior = nextPrior;
    }
}
