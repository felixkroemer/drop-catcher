package com.felixkroemer.dagger;

import com.felixkroemer.config.ConfigurationManager;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import dagger.Module;
import dagger.Provides;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.sql.Connection;
import java.sql.DriverManager;

import static com.felixkroemer.config.ConfigurationManager.OAI_KEY;

@Module
@Slf4j
public class DaggerModule {

  @Provides
  OpenAIClient providesOpenAIClient(ConfigurationManager configurationManager) {
    return OpenAIOkHttpClient.builder().apiKey(configurationManager.getString(OAI_KEY)).build();
  }

  @Provides
  SessionFactory getSessionFactory(ConfigurationManager configurationManager) {
    SessionFactory sessionFactory;
    try {
      var configDir = configurationManager.getConfigDir();
      String jdbcUrl = "jdbc:sqlite:" + configDir + "/sqlite.db";

      runLiquibaseMigrations(jdbcUrl);

      Configuration configuration = new Configuration();

      configuration.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC");
      configuration.setProperty(
          "hibernate.connection.url", "jdbc:sqlite:" + configDir + "/sqlite.db");
      configuration.setProperty(
          "hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");

      // configuration.setProperty("hibernate.show_sql", "true");
      // configuration.setProperty("hibernate.format_sql", "true");

      ServiceRegistry serviceRegistry =
          new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();

      sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    } catch (Exception e) {
      log.error("Error creating SessionFactory", e);
      throw new RuntimeException(e);
    }
    return sessionFactory;
  }

  void runLiquibaseMigrations(String jdbcUrl) {
    try {
      Connection connection = DriverManager.getConnection(jdbcUrl);
      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      var liquibase =
          new Liquibase(
              "db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database);

      liquibase.update(new Contexts());
      log.info("Liquibase migrations completed successfully");
    } catch (Exception e) {
      throw new RuntimeException("Failed to run Liquibase migrations", e);
    }
  }
}
