package com.integralblue.commander.plugins.sphinx4;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.RecognitionResult;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.Context;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class AbstractSphinx4RecognitionPlugin extends AbstractPlugin {
	@Value
	protected static class SphinxResult {
		String text;
		AudioData audioData;
	}

	protected AudioFormat audioFormat;
	protected Recognizer recognizer;

	protected Context context;

	@Override
	public final void close() throws Exception {
		recognizer.deallocate();
	}

	protected Configuration getSphinxConfiguration() {

		Configuration configuration = new Configuration();

		// Set path to acoustic model.
		configuration.setAcousticModelPath(config.getString("acousticModelPath"));
		// Set path to dictionary.
		configuration.setDictionaryPath(config.getString("dictionaryPath"));
		// Set language model.
		configuration.setLanguageModelPath(config.getString("languageModelPath"));

		return configuration;
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();
		audioFormat = new AudioFormat((float) config.getDouble("audioFormat.sampleRate"),
				config.getInt("audioFormat.sampleSizeInBits"), config.getInt("audioFormat.channels"),
				config.getBoolean("audioFormat.signed"), config.getBoolean("audioFormat.bigEndian"));

		context = new Context(getSphinxConfiguration());
		recognizer = context.getInstance(Recognizer.class);
		recognizer.allocate();
	}

	/**
	 * Listens for a result from Sphinx, then returns the resulting text (or
	 * {@link RecognitionResult#UNKNOWN_TEXT} if the input sound wasn't
	 * recognized by Sphinx)
	 *
	 * @param audioData
	 *
	 * @return
	 */
	protected CompletionStage<SphinxResult> listenForText(final AudioData audioData) {
		return CompletableFuture.supplyAsync(() -> {
			final AudioInputStream audioInputStream;
			if (audioData.getFormat().equals(audioFormat)) {
				audioInputStream = audioData.getAudioInputStream();
			} else {
				audioInputStream = AudioSystem.getAudioInputStream(audioFormat, audioData.getAudioInputStream());
			}
			String text;
			Result result;
			context.setSpeechSource(audioInputStream);
			result = recognizer.recognize();
			if (result == null || Word.UNKNOWN.getSpelling().equals(result.getBestFinalResultNoFiller())
					|| "".equals(result.getBestFinalResultNoFiller())) {
				text = RecognitionResult.UNKNOWN_TEXT;
			} else {
				text = result.getBestFinalResultNoFiller();
			}
			log.debug("Heard text: {}", text);
			return new SphinxResult(text, audioData);
		});
	}
}
