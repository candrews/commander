package com.integralblue.commander.plugins.watson;

import com.ibm.watson.developer_cloud.question_and_answer.v1.QuestionAndAnswer;
import com.ibm.watson.developer_cloud.question_and_answer.v1.model.QuestionAndAnswerDataset;
import com.ibm.watson.developer_cloud.question_and_answer.v1.model.WatsonAnswer;
import com.integralblue.commander.Manager.DictationConfiguration;

public class WatsonQuestionsPlugin extends AbstractWatsonPlugin {
	QuestionAndAnswer service;
	double confidenceThreshold;

	void dictateWatsonQuestion(QuestionAndAnswerDataset dataset) {
		final String prompt = "What is your question?";
		manager.dictate(DictationConfiguration.builder().prompt(() -> manager.say(prompt)).consumer((mc, r) -> {
			service.setDataset(dataset);
			WatsonAnswer watsonAnswer = service.ask(r.getText());
			if (watsonAnswer.getAnswers().isEmpty()
					|| watsonAnswer.getAnswers().get(0).getConfidence() < confidenceThreshold) {
				manager.say("Sorry, I do not know the answer to the question '" + r.getText() + "'");
			} else {
				manager.say(watsonAnswer.getAnswers().get(0).getText());
			}
		}).build());
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();

		confidenceThreshold = config.getDouble("confidenceThreshold");

		service = new QuestionAndAnswer();
		service.setUsernameAndPassword(username, password);

		manager.onMainMenuOption("watson health", (mainMenuController, mainMenuResult) -> {
			dictateWatsonQuestion(QuestionAndAnswerDataset.HEALTHCARE);
		});

		manager.onMainMenuOption("watson travel", (mainMenuController, mainMenuResult) -> {
			dictateWatsonQuestion(QuestionAndAnswerDataset.TRAVEL);
		});
	}

}
