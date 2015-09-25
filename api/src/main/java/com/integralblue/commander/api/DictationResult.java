package com.integralblue.commander.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder(toBuilder = true)
@Wither
public class DictationResult implements RecognitionResult {
	private static final long serialVersionUID = -1190326821095276165L;

	/**
	 * The audio input stream used to get this result. This field is optional.
	 */
	AudioData audioData;

	@NonNull
	String text;

	@Override
	public boolean isUnknown() {
		return UNKNOWN_TEXT.equals(text);
	}
}
