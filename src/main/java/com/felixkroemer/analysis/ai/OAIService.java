package com.felixkroemer.analysis.ai;

import static com.felixkroemer.config.ConfigurationManager.LLM_MODEL;

import com.felixkroemer.config.ConfigurationManager;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OAIService {

  private final OpenAIClient openAIClient;
  private final ConfigurationManager configurationManager;

  @Inject
  public OAIService(OpenAIClient oaiClient, ConfigurationManager configurationManager) {
    this.openAIClient = oaiClient;
    this.configurationManager = configurationManager;
  }

  public String analyzeFileName(String analyzableContent) {
    var result =
        this.openAIClient
            .chat()
            .completions()
            .create(
                ChatCompletionCreateParams.builder()
                    .addUserMessage(getFileNameAnalysisSystemPrompt())
                    .addUserMessage(analyzableContent)
                    .model(configurationManager.getString(LLM_MODEL))
                    .build());
    return result
        .choices()
        .getFirst()
        .message()
        .content()
        .orElseThrow(() -> new RuntimeException("Could not analyze file name"));
  }

  private String getFileNameAnalysisSystemPrompt() {
    return """
                Based on the following content, generate a Linux-compatible filename under 40 characters that clearly describes the content.
                Use underscores instead of spaces, avoid special characters, and make it descriptive enough to identify the content at a glance.
                Make sure to include all relevant information, such as the date, if available.
                Do not add a file extension.
                Return only the filename, nothing else.
                """;
  }
}
