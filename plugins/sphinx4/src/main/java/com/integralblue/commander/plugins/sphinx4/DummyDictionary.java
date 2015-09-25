package com.integralblue.commander.plugins.sphinx4;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

class DummyDictionary implements Dictionary {

	private static final Set<String> NOT_WORDS = Collections.unmodifiableSet(new HashSet<>(
			Arrays.asList(new String[] { SENTENCE_END_SPELLING, SENTENCE_START_SPELLING, SILENCE_SPELLING })));

	public static final Dictionary INSTANCE = new DummyDictionary();

	private DummyDictionary() {

	}

	@Override
	public void allocate() throws IOException {
		// do nothing
	}

	@Override
	public void deallocate() {
		// do nothing
	}

	@Override
	public Word[] getFillerWords() {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public Word getSentenceEndWord() {
		return getWord(SENTENCE_END_SPELLING);
	}

	@Override
	public Word getSentenceStartWord() {
		return getWord(SENTENCE_START_SPELLING);
	}

	@Override
	public Word getSilenceWord() {
		return getWord(SILENCE_SPELLING);
	}

	@Override
	public Word getWord(String text) {
		if (NOT_WORDS.contains(text)) {
			return null;
		} else {
			return new Word(text, null, false);
		}
	}

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		throw new IllegalStateException("Not implemented");
	}

}
