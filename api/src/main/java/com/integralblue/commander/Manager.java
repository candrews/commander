package com.integralblue.commander;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.integralblue.commander.api.DictationResult;
import com.integralblue.commander.api.GrammarResult;
import com.integralblue.commander.api.Microphone;
import com.integralblue.commander.api.Plugin;
import com.integralblue.commander.api.Speaker;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Wither;

public abstract class Manager {
	@Value
	@Builder(toBuilder = true)
	@Wither
	public static class DictationConfiguration {
		public static class DictationConfigurationBuilder {
			// set the default value
			private BiConsumer<MenuController, DictationResult> onUnknown = (mc, r) -> {
			};
		}

		/**
		 * The prompt is run before recognition begins.
		 */
		@NonNull
		Runnable prompt;

		/**
		 * The consumer is called when a final result has been generated (in
		 * other words, when recognition is finished).
		 */
		@NonNull
		BiConsumer<MenuController, DictationResult> consumer;

		/**
		 * The onUnknown consumer is called when a final result has been
		 * generated (in other words, when recognition is finished) that isn't a
		 * recognized word.
		 */
		@NonNull
		BiConsumer<MenuController, DictationResult> onUnknown;
	}

	@Value
	@Wither
	@Builder(toBuilder = true)
	public static class MenuConfiguration {
		public static class MenuConfigurationBuilder {
			// set the default value
			private BiConsumer<MenuController, GrammarResult> onUnknown = (mc, r) -> {
			};
		}

		/**
		 * The prompt is run before recognition begins.
		 */
		@NonNull
		Runnable prompt;

		@NonNull
		@Singular
		Map<String, BiConsumer<MenuController, GrammarResult>> jsgfToConsumers;

		/**
		 * The onUnknown consumer is called when a final result has been
		 * generated (in other words, when recognition is finished) that isn't
		 * in a the grammar.
		 */
		@NonNull
		BiConsumer<MenuController, GrammarResult> onUnknown;
	}

	public static interface MenuController {
		/**
		 * Await the completion of the given {@link CompletionStage} before this
		 * menu is considered to be processed.
		 *
		 * @param completionStage
		 */
		void awaitCompletion(CompletionStage<?> completionStage);

		/**
		 * Repeat this menu.
		 */
		void repeatMenu();

		/**
		 * Repeat this menu asynchronously.
		 */
		CompletionStage<Void> repeatMenuAsync();
	}

	public static final String DICATION_ENGINE_PLUGIN_NAME = "dictationEngine";
	public static final String GRAMMAR_ENGINE_PLUGIN_NAME = "grammarEngine";

	public static final String JSGF_PARSER_ENGINE_PLUGIN_NAME = "jsgfParser";

	public static final String KEYWORD_ENGINE_PLUGIN_NAME = "keywordEngine";

	public static final String SYNTHESIS_ENGINE_PLUGIN_NAME = "synthesisEngine";

	public static final String SPEAKER_PLUGIN_NAME = "speaker";

	public static final String MICROPHONE_PLUGIN_NAME = "microphone";

	/**
	 * http://www.w3.org/TR/jsgf/ defines the JSGF special characters
	 */
	private static final Pattern JSGF_SPECIAL = Pattern.compile("([;=|*+<>()\\[\\]{}/\\\\])");

	/**
	 * Escape the given text so it is a JSGF literal having no special meaning
	 *
	 * @see <a href="http://www.w3.org/TR/jsgf/">JSpeech Grammar Format</a>
	 *
	 * @param text
	 * @return
	 */
	public static final String escapeJsgf(@NonNull String text) {
		return JSGF_SPECIAL.matcher(text).replaceAll("\\\\$1");
	}

	/**
	 * Add keyword. This text will be recognized as text, not as JSGF.
	 *
	 * @param keyword
	 */
	public abstract void addKeyword(String keyword);

	public abstract void dictate(DictationConfiguration dictationConfiguration);

	public abstract Microphone getMicrophone();

	/**
	 * Get a plugin by its name and class
	 *
	 * @param pluginName
	 *            the name of the plugin to get (ex, "grammarEngine")
	 * @param pluginClass
	 *            the class of the plugin to get (ex,
	 *            com.integralblue.commander.api.GrammarEngine)
	 * @return the plugin instance Never null.
	 * @throws IllegalArgumentException
	 *             If no plugin by the given pluginName is found
	 * @throws ClassCastException
	 *             If the plugin class can be cast to the given pluginClass
	 */
	public abstract <T extends Plugin> T getPlugin(String pluginName, Class<T> pluginClass)
			throws IllegalArgumentException, ClassCastException;

	public abstract Speaker getSpeaker();

	public abstract void menu(MenuConfiguration menuConfiguration);

	public abstract CompletionStage<Void> menuAsync(MenuConfiguration menuConfiguration);

	public abstract void onAnyDictate(Function<DictationConfiguration, DictationConfiguration> interceptor);

	/**
	 * Applies the given interceptor whenever any menu is setup (including the
	 * main menu)
	 *
	 * @param interceptor
	 */
	public abstract void onAnyMenu(Function<MenuConfiguration, MenuConfiguration> interceptor);

	/**
	 * Register a {@link Runnable} to be run whenever a keyword is recognized
	 * (the main menu will be run immediately after all of these
	 * {@link Runnable}s are executed)
	 *
	 * @param r
	 */
	public abstract void onKeyword(Runnable r);

	public abstract void onMainMenu(Function<MenuConfiguration, MenuConfiguration> interceptor);

	public abstract void onMainMenuOption(String jsgf, BiConsumer<MenuController, GrammarResult> consumer);

	public abstract void onSay(Function<Consumer<String>, Consumer<String>> interceptor);

	public abstract void quit();

	/**
	 * Use a synthesizer to speak the give text aloud. This method ensures that
	 * only one piece of text will be spoken at a time (speaking is performed
	 * sequentially).
	 *
	 * @param text
	 */
	public abstract void say(String text);

	/**
	 * Use a synthesizer to speak the give text aloud. This method ensures that
	 * only one piece of text will be spoken at a time (speaking is performed
	 * sequentially).
	 *
	 * @param text
	 */
	public abstract CompletionStage<Void> sayAsync(String text);
}
