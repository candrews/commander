package com.integralblue.commander.plugins.watson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.DictationEngine;
import com.integralblue.commander.api.DictationResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WatsonDictationPlugin extends AbstractWatsonPlugin implements DictationEngine {
	private static final AudioFormat WATSON_AUDIO_FORMAT = new AudioFormat(16000, 16, 1, true, false);

	private SpeechToText speechToText;

	@Override
	public void initialize() throws Exception {
		super.initialize();
		speechToText = new SpeechToText();
		speechToText.setUsernameAndPassword(username, password);
	}

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync() {
		return manager.getMicrophone().getVoiceAsync(WATSON_AUDIO_FORMAT).thenCompose(this::listenForDictationAsync);
	}

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync(AudioData audioData) {
		// TODO use some kind of async IO instead of blocking IO on a different
		// thread
		return CompletableFuture.supplyAsync(() -> {
			// TODO instead of using a temporary file, send the audio data
			// directly
			// when the API is updated to support that. See
			// https://github.com/watson-developer-cloud/text-to-speech-java/issues/2
			try {
				File tempFile = null;
				try {
					tempFile = File.createTempFile("snd", null);
					Map<String, Object> params = new HashMap<String, Object>();
					params.put(SpeechToText.MODEL, audioData.getFormat().getSampleRate() >= 16000
							? "en-US_BroadbandModel" : "en-US_NarrowbandModel");
					boolean wroteFile = false;
					for (Type type : AudioSystem.getAudioFileTypes(audioData.getAudioInputStream())) {
						if ("flac".equals(type.getExtension())) {
							log.info("Found FLAC support, sending FLAC data");
							params.put(SpeechToText.CONTENT_TYPE, "audio/flac");
							AudioSystem.write(audioData.getAudioInputStream(), type, new FileOutputStream(tempFile));
							wroteFile = true;
							break;
						}
					}
					if (!wroteFile) {
						log.info("FLAC support not found, sending PCM data");
						AudioFormat audioFormat = audioData.getFormat();
						params.put(SpeechToText.CONTENT_TYPE, "audio/l" + audioFormat.getSampleSizeInBits() + "; rate="
								+ (int) audioFormat.getSampleRate() + "; channels=" + audioFormat.getChannels());
						Files.copy(audioData.getAudioInputStream(), tempFile.toPath(),
								StandardCopyOption.REPLACE_EXISTING);
					}
					params.put(SpeechToText.AUDIO, tempFile);
					SpeechResults speechResults = speechToText.recognize(params);
					DictationResult.DictationResultBuilder dictationResultBuilder = DictationResult.builder()
							.audioData(audioData);
					if (speechResults.getResults().isEmpty()
							|| speechResults.getResults().get(0).getAlternatives().isEmpty()
							|| speechResults.getResults().get(0).getAlternatives().get(0).getTranscript() == null
							|| speechResults.getResults().get(0).getAlternatives().get(0).getTranscript().trim()
									.isEmpty()) {
						dictationResultBuilder.text(DictationResult.UNKNOWN_TEXT);
					} else {
						dictationResultBuilder.text(
								speechResults.getResults().get(0).getAlternatives().get(0).getTranscript().trim());
					}
					return dictationResultBuilder.build();
				} finally {
					if (tempFile != null) {
						tempFile.delete();
					}
				}
			} catch (IOException e) {
				throw new CompletionException(e);
			}
		});
	}

}
