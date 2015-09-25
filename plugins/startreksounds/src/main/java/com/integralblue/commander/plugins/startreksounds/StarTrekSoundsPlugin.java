package com.integralblue.commander.plugins.startreksounds;

import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.integralblue.commander.api.AbstractPlugin;

import lombok.NonNull;
import lombok.SneakyThrows;

public class StarTrekSoundsPlugin extends AbstractPlugin {

	@Override
	public void initialize() throws Exception {
		super.initialize();

		final URL mainMenuSound = this.getClass().getResource("mainMenu.wav");
		final URL unknownSound = this.getClass().getResource("unknown.wav");

		manager.onAnyDictate((dc) -> {
			return dc.toBuilder().prompt(() -> {
				playUrl(mainMenuSound);
				dc.getPrompt().run();
			}).onUnknown((unknownMenuController, result) -> {
				playUrl(unknownSound);
				dc.getOnUnknown().accept(unknownMenuController, result);
			}).build();
		});

		manager.onAnyMenu((mc) -> {
			return mc.toBuilder().onUnknown((unknownMenuController, result) -> {
				playUrl(unknownSound);
				mc.getOnUnknown().accept(unknownMenuController, result);
			}).build();
		});

		manager.onKeyword(() -> {
			playUrl(mainMenuSound);
		});

	}

	@SneakyThrows
	public void playUrl(@NonNull URL url) {
		try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url)) {
			manager.getSpeaker().playAsync(audioInputStream).toCompletableFuture().get();
		}
	}

}
