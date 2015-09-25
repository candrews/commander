package com.integralblue.commander.plugins.googlettsv1;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.SynthesisEngine;

import lombok.SneakyThrows;

public class GoogleTTSv1SynthesisPlugin extends AbstractPlugin implements SynthesisEngine {

	private static final String GOOGLE_SYNTHESIZER_URL_FORMAT = "https://translate.google.com/translate_tts?tl={0}&q={1}&ie=UTF-8&total=1&idx=0&client=t";

	/**
	 * This API only allows text up to this length (any text beyond this length
	 * results in an error)
	 */
	private static final int MAX_TEXT_LENGTH = 100;

	private String languageCode;

	/**
	 * Finds the last word in your String (before the index of 99) by searching
	 * for spaces and ending punctuation. Will preferably parse on punctuation
	 * to alleviate mid-sentence pausing
	 *
	 * @param input
	 *            The String you want to search through.
	 * @return The index of where the last word of the string ends before the
	 *         index of 99.
	 */
	private int findLastWord(String input) {
		if (input.length() < MAX_TEXT_LENGTH) {
			return input.length();
		}
		int space = -1;
		for (int i = MAX_TEXT_LENGTH - 1; i > 0; i--) {
			char tmp = input.charAt(i);
			if (isEndingPunctuation(tmp)) {
				return i + 1;
			}
			if (space == -1 && tmp == ' ') {
				space = i;
			}
		}
		if (space > 0) {
			return space;
		}
		return -1;
	}

	/**
	 * Gets an InputStream to MP3Data for the returned information from a
	 * request
	 *
	 * @param synthText
	 *            List of Strings you want to be synthesized into MP3 data
	 * @return Returns an input stream of all the MP3 data that is returned from
	 *         Google
	 * @throws IOException
	 *             Throws exception if it cannot complete the request
	 */
	@SneakyThrows
	public InputStream getMP3Data(List<String> synthText) {
		// by default, parallelStream() will use the common fork join pool,
		// which may be used for other things and has a size of the number of
		// processors - 1.
		// since these tasks aren't CPU bound, and we want a lot concurrency not
		// depending on what's going on in the common pool, use a new separate
		// pool.
		final ForkJoinPool forkJoinPool = new ForkJoinPool(10); // allow 10
																// concurrent
																// requests
		try {
			return forkJoinPool.submit(() -> new SequenceInputStream(Collections
					.enumeration(synthText.parallelStream().map(this::getMP3Data).collect(Collectors.toList())))).get();
		} finally {
			forkJoinPool.shutdown();
		}
	}

	/**
	 * Gets an input stream to MP3 data for the returned information from a
	 * request
	 *
	 * @param synthText
	 *            Text you want to be synthesized into MP3 data
	 * @return Returns an input stream of the MP3 data that is returned from
	 *         Google
	 * @throws IOException
	 *             Throws exception if it can not complete the request
	 */
	@SneakyThrows
	private InputStream getMP3Data(String text) {

		if (text.length() > MAX_TEXT_LENGTH) {
			List<String> fragments = parseString(text);// parses String if too
														// long
			return getMP3Data(fragments);
		}

		URL url = new URL(MessageFormat.format(GOOGLE_SYNTHESIZER_URL_FORMAT, URLEncoder.encode(languageCode, "UTF-8"),
				URLEncoder.encode(text, "UTF-8")));

		URLConnection urlConn = url.openConnection();

		urlConn.addRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:2.0) Gecko/20100101 Firefox/4.0");

		return urlConn.getInputStream();
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();
		languageCode = config.getString("languageCode");
	}

	/**
	 * Checks if char is an ending character Ending punctuation for all
	 * languages according to Wikipedia (Except for Sanskrit non-unicode)
	 *
	 * @param The
	 *            char you want check
	 * @return True if it is, false if not.
	 */
	private boolean isEndingPunctuation(char input) {
		return input == '.' || input == '!' || input == '?' || input == ';' || input == ':' || input == '|';
	}

	/**
	 * Separates a string into smaller parts so that Google will not reject the
	 * request.
	 *
	 * @param input
	 *            The string you want to separate
	 * @return A List<String> of the String fragments from your input..
	 */
	private List<String> parseString(String input) {
		return parseString(input, new ArrayList<String>());
	}

	/**
	 * Separates a string into smaller parts so that Google will not reject the
	 * request.
	 *
	 * @param input
	 *            The string you want to break up into smaller parts
	 * @param fragments
	 *            List<String> that you want to add stuff too. If you don't have
	 *            a List<String> already constructed "new ArrayList<String>()"
	 *            works well.
	 * @return A list of the fragments of the original String
	 */
	private List<String> parseString(String input, List<String> fragments) {
		if (input.length() <= MAX_TEXT_LENGTH) {// Base Case
			fragments.add(input);
			return fragments;
		} else {
			int lastWord = findLastWord(input);// Checks if a space exists
			if (lastWord <= 0) {
				fragments.add(input.substring(0, MAX_TEXT_LENGTH));// In case
																	// you sent
				// gibberish to Google.
				return parseString(input.substring(MAX_TEXT_LENGTH), fragments);
			} else {
				fragments.add(input.substring(0, lastWord));// Otherwise, adds
															// the last word to
															// the list for
															// recursion.
				return parseString(input.substring(lastWord), fragments);
			}
		}
	}

	@Override
	public CompletionStage<Void> sayAsync(String text) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return AudioSystem.getAudioInputStream(new BufferedInputStream(getMP3Data(text)));
			} catch (UnsupportedAudioFileException | IOException e) {
				throw new CompletionException(e);
			}
		}).thenCompose(manager.getSpeaker()::playAsync);
	}

}
