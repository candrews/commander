package com.integralblue.commander.plugins.freetts;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.sun.speech.freetts.audio.AudioPlayer;

class CompletableFutureAudioPlayer implements AudioPlayer {

	private AudioFormat currentFormat;
	private byte[] outputData;
	private int curIndex = 0;

	private final CompletableFuture<AudioInputStream> completableFuture;

	public CompletableFutureAudioPlayer(CompletableFuture<AudioInputStream> completableFuture) {
		this.completableFuture = completableFuture;
	}

	@Override
	public void begin(int size) {
		outputData = new byte[size];
		curIndex = 0;
	}

	@Override
	public void cancel() {
	}

	@Override
	public void close() {
	}

	@Override
	public boolean drain() {
		return true;
	}

	@Override
	public boolean end() {
		assert(curIndex == outputData.length);
		AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(outputData), currentFormat,
				curIndex / currentFormat.getFrameSize());
		completableFuture.complete(ais);
		return false;
	}

	@Override
	public AudioFormat getAudioFormat() {
		return currentFormat;
	}

	@Override
	public long getTime() {
		return -1L;
	}

	@Override
	public float getVolume() {
		return 1.0f;
	}

	@Override
	public void pause() {
	}

	@Override
	public void reset() {
	}

	@Override
	public void resetTime() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void setAudioFormat(AudioFormat audioFormat) {
		this.currentFormat = audioFormat;
	}

	@Override
	public void setVolume(float arg0) {
	}

	@Override
	public void showMetrics() {
	}

	@Override
	public void startFirstSampleTimer() {
	}

	@Override
	public boolean write(byte[] audioData) {
		return write(audioData, 0, audioData.length);
	}

	@Override
	public boolean write(byte[] bytes, int offset, int size) {
		System.arraycopy(bytes, offset, outputData, curIndex, size);
		curIndex += size;
		return true;
	}

}
