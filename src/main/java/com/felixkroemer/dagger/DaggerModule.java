package com.felixkroemer.dagger;

import com.felixkroemer.config.ConfigurationManager;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import dagger.Module;
import dagger.Provides;

import static com.felixkroemer.config.ConfigurationManager.OAI_KEY;

@Module
public class DaggerModule {

    @Provides
    OpenAIClient providesOpenAIClient(ConfigurationManager configurationManager) {
        return OpenAIOkHttpClient.builder().apiKey(configurationManager.getString(OAI_KEY)).build();
    }

}
