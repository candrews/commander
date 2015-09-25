package com.integralblue.commander.api;

import java.io.Serializable;

public interface RecognitionResult extends Serializable {

	public static final String UNKNOWN_TEXT = "<unk>";

	/**
	 * The audio data used to get this result. This field is optional.
	 */
	AudioData getAudioData();

	String getText();

	boolean isUnknown();
}
