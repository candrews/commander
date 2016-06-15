package com.integralblue.commander.plugins.watson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import com.ibm.watson.developer_cloud.speech_to_text.v1.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
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
		final String model = audioData.getFormat().getSampleRate() >= 16000 ? "en-US_BroadbandModel"
				: "en-US_NarrowbandModel";
		String contentType = null;
		InputStream is = null;
		for (Type type : AudioSystem.getAudioFileTypes(audioData.getAudioInputStream())) {
			if ("flac".equals(type.getExtension())) {
				log.info("Found FLAC support, sending FLAC data");
				contentType = "audio/flac";
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					AudioSystem.write(audioData.getAudioInputStream(), type, baos);
				} catch (IOException e) {
					// this really should never happen
					throw new RuntimeException(e);
				}
				is = new ByteArrayInputStream(baos.toByteArray());
				break;
			}
		}
		if (contentType == null) {
			log.info("FLAC support not found, sending PCM data");
			AudioFormat audioFormat = audioData.getFormat();
			contentType = "audio/l" + audioFormat.getSampleSizeInBits() + "; rate=" + (int) audioFormat.getSampleRate()
					+ "; channels=" + audioFormat.getChannels();
			is = audioData.getAudioInputStream();
		}
		final CompletableFuture<DictationResult> recognitionCompletableFuture = new CompletableFuture<>();
		speechToText.recognizeUsingWebSocket(is,
				new RecognizeOptions.Builder().contentType(contentType).continuous(false).model(model).build(),
				new BaseRecognizeCallback() {

					@Override
					public void onDisconnected() {
						if (!recognitionCompletableFuture.isDone()) {
							recognitionCompletableFuture.completeExceptionally(
									new Exception("Disconnected without error or result - this should not happen"));
						}
					}

					@Override
					public void onError(Exception e) {
						recognitionCompletableFuture.completeExceptionally(e);
					}

					@Override
					public void onTranscription(SpeechResults speechResults) {
						if (recognitionCompletableFuture.isDone()) {
							recognitionCompletableFuture.completeExceptionally(
									new Exception("onTranscription called more than once - this should not happen"));
						}
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
						recognitionCompletableFuture.complete(dictationResultBuilder.build());
					}
				});

		return recognitionCompletableFuture;
	}

}
