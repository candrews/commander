package com.integralblue.commander;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.integralblue.commander.Manager.DictationConfiguration;
import com.integralblue.commander.Manager.MenuConfiguration;
import com.integralblue.commander.api.DictationEngine;
import com.integralblue.commander.api.GrammarEngine;
import com.integralblue.commander.api.GrammarResult;
import com.integralblue.commander.api.JsgfParser;
import com.integralblue.commander.api.JsgfRule;
import com.integralblue.commander.api.KeywordEngine;
import com.integralblue.commander.api.Microphone;
import com.integralblue.commander.api.Plugin;
import com.integralblue.commander.api.PluginDefinition;
import com.integralblue.commander.api.Speaker;
import com.integralblue.commander.api.SynthesisEngine;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class Brain implements Runnable {
	private class ManagerImpl extends Manager {

		/**
		 * Lock used to ensure only 1 sample is being spoken at a time
		 */
		private final Semaphore SAY_LOCK = new Semaphore(1);

		@Override
		public void addKeyword(@NonNull String keyword) {
			keywords.add(keyword);
		}

		@Override
		public void dictate(@NonNull DictationConfiguration dictationConfiguration) {
			// because dictates may be nested, or dictate may call menu, dictate
			// execution has to be synchronous
			dictateAsync(dictationConfiguration).join();
		}

		public CompletableFuture<Void> dictateAsync(@NonNull DictationConfiguration dictationConfigurationParameter) {
			final DictationConfiguration dictationConfiguration = applyInterceptors(dictationConfigurationParameter,
					anyDictationConfigurationInterceptors);

			final List<CompletionStage<?>> awaitCompletions = new ArrayList<>();

			final MenuController menuController = new MenuController() {
				@Override
				public void awaitCompletion(CompletionStage<?> completionStage) {
					awaitCompletions.add(completionStage);
				}

				@Override
				public void repeatMenu() {
					dictate(dictationConfigurationParameter);
				}

				@Override
				public CompletionStage<Void> repeatMenuAsync() {
					return dictateAsync(dictationConfigurationParameter);
				}
			};
			return CompletableFuture.runAsync(dictationConfiguration.getPrompt())
					.thenCompose((unusedVoid) -> getPlugin(Manager.DICATION_ENGINE_PLUGIN_NAME, DictationEngine.class)
							.listenForDictationAsync())
					.thenAccept((result) -> {
						if (result.isUnknown()) {
							dictationConfiguration.getOnUnknown().accept(menuController, result);
						} else {
							dictationConfiguration.getConsumer().accept(menuController, result);
						}
					}).thenCompose((unusedVoid) -> {
						return CompletableFuture.allOf(awaitCompletions.stream().map(x -> x.toCompletableFuture())
								.toArray(CompletableFuture[]::new));
					});
		}

		@Override
		public Microphone getMicrophone() {
			return getPlugin(Manager.MICROPHONE_PLUGIN_NAME, Microphone.class);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Plugin> T getPlugin(@NonNull String pluginName, @NonNull Class<T> pluginClass)
				throws IllegalArgumentException, ClassCastException {
			Plugin plugin = pluginNameToPlugins.get(pluginName);
			if (plugin == null) {
				throw new IllegalArgumentException("There is no plugin with name: " + pluginName);
			}
			return (T) plugin;
		}

		@Override
		public Speaker getSpeaker() {
			return getPlugin(Manager.SPEAKER_PLUGIN_NAME, Speaker.class);
		}

		@Override
		public void menu(@NonNull final MenuConfiguration menuConfigurationParameter) {
			// because menus may be nested, or menus may call dictation, menu
			// execution has to be synchronous
			menuAsync(menuConfigurationParameter).join();
		}

		@Override
		public CompletableFuture<Void> menuAsync(@NonNull final MenuConfiguration menuConfigurationParameter) {
			final MenuConfiguration menuConfiguration = applyInterceptors(menuConfigurationParameter,
					anyMenuConfigurationInterceptors);
			if (menuConfiguration.getJsgfToConsumers().isEmpty()) {
				throw new IllegalArgumentException(
						"menu configuration doesn't specify anything to listen for. Please provide at least one jsgfToConsumer mapping.");
			}

			final List<CompletionStage<?>> awaitCompletions = new ArrayList<>();

			final MenuController menuController = new MenuController() {
				@Override
				public void awaitCompletion(CompletionStage<?> completionStage) {
					awaitCompletions.add(completionStage);
				}

				@Override
				public void repeatMenu() {
					menu(menuConfigurationParameter);
				}

				@Override
				public CompletionStage<Void> repeatMenuAsync() {
					return menuAsync(menuConfigurationParameter);
				}
			};
			final AtomicInteger tagCounter = new AtomicInteger(0);
			final Map<String, String> internalTagToJsgf = menuConfiguration.getJsgfToConsumers().keySet().stream()
					.collect(Collectors.toMap(s -> INTERNAL_TAG_PREFIX + tagCounter.getAndIncrement(),
							Function.identity()));
			final String combinedAndTaggedJsgf = internalTagToJsgf.entrySet().stream()
					.map(entry -> "(" + entry.getValue() + "){" + entry.getKey() + "}")
					.collect(Collectors.joining("|"));
			return CompletableFuture.runAsync(menuConfiguration.getPrompt())
					.thenCompose((unusedVoid) -> getPlugin(Manager.GRAMMAR_ENGINE_PLUGIN_NAME, GrammarEngine.class)
							.listenForGrammarAsync(Collections.singleton(JsgfRule.builder()
									.name("INTERNAL_NAME_COMMAND").jsgf(combinedAndTaggedJsgf).build())))
					.thenAccept((result) -> {
						if (result.isUnknown()) {
							menuConfiguration.getOnUnknown().accept(menuController, result);
						} else {
							String resultInternalTag = null;
							final List<String> resultExternalTags = new ArrayList<>();
							for (String tag : result.getTags()) {
								if (tag.startsWith(INTERNAL_TAG_PREFIX)) {
									if (resultInternalTag == null) {
										resultInternalTag = tag;
									} else {
										throw new IllegalStateException("Multiple internal tags found");
									}
								} else {
									resultExternalTags.add(tag);
								}
							}
							if (resultInternalTag == null) {
								throw new IllegalStateException("No internal tag found");
							}
							final String jsgf = internalTagToJsgf.get(resultInternalTag);
							if (jsgf == null) {
								throw new IllegalStateException("No jsgf found for internal tag");
							}
							final BiConsumer<MenuController, GrammarResult> consumer = menuConfiguration
									.getJsgfToConsumers().get(jsgf);
							if (consumer == null) {
								throw new IllegalStateException("No consumer found for jsgf");
							}

							consumer.accept(menuController,
									result.withTags(Collections.unmodifiableList(resultExternalTags)));
						}
					}).thenCompose((unusedVoid) -> {
						return CompletableFuture.allOf(awaitCompletions.stream().map(x -> x.toCompletableFuture())
								.toArray(CompletableFuture[]::new));
					});

		}

		@Override
		public void onAnyDictate(@NonNull Function<DictationConfiguration, DictationConfiguration> interceptor) {
			anyDictateConfigurationInterceptors.add(interceptor);
		}

		@Override
		public void onAnyMenu(@NonNull Function<MenuConfiguration, MenuConfiguration> interceptor) {
			anyMenuConfigurationInterceptors.add(interceptor);
		}

		@Override
		public void onKeyword(@NonNull Runnable r) {
			onKeywords.add(r);
		}

		@Override
		public void onMainMenu(@NonNull Function<MenuConfiguration, MenuConfiguration> interceptor) {
			mainMenuConfigurationInterceptors.add(interceptor);
		}

		@Override
		public void onMainMenuOption(@NonNull String jsgf,
				@NonNull BiConsumer<MenuController, GrammarResult> consumer) {
			onMainMenu(mc -> mc.toBuilder().jsgfToConsumer(jsgf, consumer).build());
		}

		@Override
		public void onSay(@NonNull Function<Consumer<String>, Consumer<String>> interceptor) {
			onSayInterceptors.add(interceptor);
		}

		@Override
		public void quit() {
			shutdown.set(true);
		}

		@Override
		@SneakyThrows
		public void say(@NonNull String text) {
			sayAsync(text).toCompletableFuture().get();
		}

		@Override
		public CompletionStage<Void> sayAsync(String text) {
			final SynthesisEngine synthesisEngine = getPlugin(Manager.SYNTHESIS_ENGINE_PLUGIN_NAME,
					SynthesisEngine.class);
			final AtomicReference<CompletionStage<Void>> retRef = new AtomicReference<CompletionStage<Void>>(
					CompletableFuture.completedFuture(null));
			Consumer<String> consumer = (whatToSay) -> {
				// SAY_LOCK ensures that only one "say" runs at a time
				SAY_LOCK.acquireUninterruptibly();
				final CompletionStage<Void> sayAsyncFuture = synthesisEngine.sayAsync(whatToSay);
				retRef.set(sayAsyncFuture);
				sayAsyncFuture.whenComplete((unusedVoid, throwable) -> {
					SAY_LOCK.release();
				});
			};
			consumer = applyInterceptors(consumer, onSayInterceptors);
			consumer.accept(text);
			return retRef.get();
		}

	}

	private static final String INTERNAL_TAG_PREFIX = "internal_tag_";

	private Map<String, Plugin> pluginNameToPlugins;

	private final Map<String, PluginDefinition> pluginNameToDefinitions;
	private final List<Function<DictationConfiguration, DictationConfiguration>> anyDictateConfigurationInterceptors = new ArrayList<>();
	private final Set<String> keywords = new HashSet<>();
	private final List<Function<MenuConfiguration, MenuConfiguration>> anyMenuConfigurationInterceptors = new ArrayList<>();
	private final List<Function<DictationConfiguration, DictationConfiguration>> anyDictationConfigurationInterceptors = new ArrayList<>();
	private final List<Function<MenuConfiguration, MenuConfiguration>> mainMenuConfigurationInterceptors = new ArrayList<>();

	private final List<Runnable> onKeywords = new ArrayList<>();

	private final List<Function<Consumer<String>, Consumer<String>>> onSayInterceptors = new ArrayList<>();

	private final AtomicBoolean shutdown = new AtomicBoolean(false);

	private final ManagerImpl manager = new ManagerImpl();

	private CompletableFuture<Void> completableFuture = null;

	@Builder
	private Brain(@Singular @NonNull Map<String, PluginDefinition> pluginNameToDefinitions) {
		this.pluginNameToDefinitions = pluginNameToDefinitions;
	}

	private <T> T applyInterceptors(@NonNull T initial, @NonNull Iterable<Function<T, T>> interceptors) {
		for (Function<T, T> function : interceptors) {
			initial = function.apply(initial);
			if (initial == null) {
				throw new IllegalStateException();
			}
		}
		return initial;
	}

	public CompletableFuture<Void> getCompletableFuture() {
		return completableFuture;
	}

	private void initializePlugins() {
		log.debug("Initializing plugins");

		pluginNameToPlugins.entrySet().stream().forEach((entry) -> {
			try {
				log.debug("Initializing plugin {}", entry.getKey());
				Plugin plugin = entry.getValue();
				runWithPluginContextClassLoader(plugin.getClass(), () -> {
					plugin.setManager(manager);
					plugin.initialize();
					return null;
				});
			} catch (Exception e) {
				throw new RuntimeException("exception initializing plugin " + entry.getKey(), e);
			}
		});

		log.debug("All plugins initialized");
	}

	private CompletionStage<Void> listenForKeywords() {
		if (shutdown.get()) {
			log.debug("Shutdown is set, so not listening for keywords");
			return CompletableFuture.completedFuture(null);
		} else {
			if (keywords.isEmpty()) {
				log.info("No keywords to listen for so running the main menu");
				return mainMenu().thenCompose((unusedVoid) -> {
					return listenForKeywords();
				});
			} else {
				return manager.getPlugin(Manager.KEYWORD_ENGINE_PLUGIN_NAME, KeywordEngine.class)
						.listenForKeywordsAsync(keywords).thenCompose((result) -> {
							onKeywords.stream().forEach(Runnable::run);
							return mainMenu();
						}).thenCompose((unusedVoid) -> {
							return listenForKeywords();
						});
			}
		}
	}

	private Plugin loadPluginFromDefinition(PluginDefinition pluginDefinition) {
		// run in runWithPluginContextClassLoader just in case the constructor
		// expects the contextClassLoader to be set the plugin class's
		// classLoader
		return runWithPluginContextClassLoader(pluginDefinition.getClazz(), () -> {
			Plugin plugin = pluginDefinition.getClazz().newInstance();
			plugin.setConfig(pluginDefinition.getConfig());
			return plugin;
		});
	}

	private void loadPlugins() {
		log.debug("Loading plugins");

		pluginNameToPlugins = this.pluginNameToDefinitions.entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(), entry -> loadPluginFromDefinition(entry.getValue())));

		Objects.requireNonNull(pluginNameToPlugins.get(Manager.DICATION_ENGINE_PLUGIN_NAME),
				Manager.DICATION_ENGINE_PLUGIN_NAME + " plugin must be set");
		if (!(pluginNameToPlugins.get(Manager.DICATION_ENGINE_PLUGIN_NAME) instanceof DictationEngine)) {
			throw new IllegalStateException(
					Manager.DICATION_ENGINE_PLUGIN_NAME + " must be of type " + DictationEngine.class.getName());
		}

		Objects.requireNonNull(pluginNameToPlugins.get(Manager.GRAMMAR_ENGINE_PLUGIN_NAME),
				Manager.GRAMMAR_ENGINE_PLUGIN_NAME + " plugin must be set");
		if (!(pluginNameToPlugins.get(Manager.GRAMMAR_ENGINE_PLUGIN_NAME) instanceof GrammarEngine)) {
			throw new IllegalStateException(
					Manager.GRAMMAR_ENGINE_PLUGIN_NAME + " must be of type " + GrammarEngine.class.getName());
		}

		Objects.requireNonNull(pluginNameToPlugins.get(Manager.JSGF_PARSER_ENGINE_PLUGIN_NAME),
				Manager.JSGF_PARSER_ENGINE_PLUGIN_NAME + " plugin must be set");
		if (!(pluginNameToPlugins.get(Manager.JSGF_PARSER_ENGINE_PLUGIN_NAME) instanceof JsgfParser)) {
			throw new IllegalStateException(
					Manager.JSGF_PARSER_ENGINE_PLUGIN_NAME + " must be of type " + JsgfParser.class.getName());
		}

		Objects.requireNonNull(pluginNameToPlugins.get(Manager.KEYWORD_ENGINE_PLUGIN_NAME),
				Manager.KEYWORD_ENGINE_PLUGIN_NAME + " plugin must be set");
		if (!(pluginNameToPlugins.get(Manager.KEYWORD_ENGINE_PLUGIN_NAME) instanceof KeywordEngine)) {
			throw new IllegalStateException(
					Manager.KEYWORD_ENGINE_PLUGIN_NAME + " must be of type " + KeywordEngine.class.getName());
		}

		Objects.requireNonNull(pluginNameToPlugins.get(Manager.SYNTHESIS_ENGINE_PLUGIN_NAME),
				Manager.SYNTHESIS_ENGINE_PLUGIN_NAME + " plugin must be set");
		if (!(pluginNameToPlugins.get(Manager.SYNTHESIS_ENGINE_PLUGIN_NAME) instanceof SynthesisEngine)) {
			throw new IllegalStateException(
					Manager.SYNTHESIS_ENGINE_PLUGIN_NAME + " must be of type " + SynthesisEngine.class.getName());
		}

		Objects.requireNonNull(pluginNameToPlugins.get(Manager.MICROPHONE_PLUGIN_NAME),
				Manager.MICROPHONE_PLUGIN_NAME + " plugin must be set");
		if (!(pluginNameToPlugins.get(Manager.MICROPHONE_PLUGIN_NAME) instanceof Microphone)) {
			throw new IllegalStateException(
					Manager.MICROPHONE_PLUGIN_NAME + " must be of type " + Microphone.class.getName());
		}

		Objects.requireNonNull(pluginNameToPlugins.get(Manager.SPEAKER_PLUGIN_NAME),
				Manager.SPEAKER_PLUGIN_NAME + " plugin must be set");
		if (!(pluginNameToPlugins.get(Manager.SPEAKER_PLUGIN_NAME) instanceof Speaker)) {
			throw new IllegalStateException(
					Manager.SPEAKER_PLUGIN_NAME + " must be of type " + Speaker.class.getName());
		}
		log.debug("Plugins loaded");
	}

	private CompletableFuture<Void> mainMenu() {
		final MenuConfiguration menuConfiguration = applyInterceptors(
				MenuConfiguration.builder().prompt(() -> log.debug("Main menu prompt")).build(),
				mainMenuConfigurationInterceptors);

		return manager.menuAsync(menuConfiguration);
	}

	@Override
	public void run() {
		runAsync();
		completableFuture.join();
	}

	public void runAsync() {
		if (completableFuture == null) {
			completableFuture = CompletableFuture.runAsync(this::loadPlugins).thenRun(this::initializePlugins)
					.thenCompose((notUsedVoid) -> {
						return listenForKeywords();
					}).thenRun(this::shutdownPlugins);
			completableFuture.whenComplete((unusedVoid, throwable) -> {
				if (throwable == null) {
					log.debug("Brain completed without error");
				} else {
					log.error("Brain completed with an exception", throwable);
				}
			});
		}
	}

	@SneakyThrows
	private <V> V runWithPluginContextClassLoader(Class<? extends Plugin> pluginClass, Callable<V> r) {
		// have to save then restore the context class loader for this
		// thread
		// plugin may use the contextClassLoader, and expect it to be able
		// to load their classes
		// for example, if a plugin uses java.util.ServiceLoader that won't
		// work unless the context class loader is set to the plugin's class
		// loader.
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(pluginClass.getClassLoader());
			return r.call();
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	private void shutdownPlugins() {
		log.debug("Shutting down plugins");
		pluginNameToPlugins.entrySet().stream().forEach((entry) -> {
			log.debug("Closing plugin {}", entry.getKey());
			Plugin plugin = entry.getValue();
			try {
				plugin.close();
			} catch (Exception e) {
				log.error("Error while closing plugin {}", entry.getKey(), e);
			}
		});
		log.debug("All plugins shut down");
	}

}
