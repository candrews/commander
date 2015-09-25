package com.integralblue.commander.api;

import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
public class GrammarResult implements RecognitionResult {
	private static final long serialVersionUID = 8197274845498360838L;

	/**
	 * The text recognized. Must be all lowercase.
	 */
	@NonNull
	String text;

	@NonNull
	@Singular
	List<String> tags;

	/**
	 * The audio data used to get this result. This field is optional.
	 */
	AudioData audioData;

	/**
	 * The dictation result used to get this grammar result (if that's how the
	 * grammar result was derived). This field is optional - if the grammar
	 * result didn't come from a dictation result, this field will be null.
	 */
	DictationResult dictationResult;

	@Builder(toBuilder = true)
	public GrammarResult(String text, List<String> tags, AudioData audioData, DictationResult dictationResult) {
		this.text = text;
		this.tags = tags;
		this.dictationResult = dictationResult;
		if (audioData == null && dictationResult != null && dictationResult.getAudioData() != null) {
			this.audioData = dictationResult.getAudioData();
		} else {
			this.audioData = audioData;
		}
	}

	@Override
	public boolean isUnknown() {
		return UNKNOWN_TEXT.equals(text);
	}
}
