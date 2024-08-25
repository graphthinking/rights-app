package modules;

import com.google.inject.AbstractModule;
import play.api.Configuration;
import play.api.Environment;
import services.VocabProvider;

/**
 * Created by fo on 25.11.15.
 */
public class VocabProviderModule extends AbstractModule {

  private final Environment environment;

  private final Configuration configuration;

  public VocabProviderModule(Environment environment, Configuration configuration) {
    this.environment = environment;
    this.configuration = configuration;
  }

  protected void configure() {

    String vocabVersion = configuration.underlying().getString("vocab.provider");
    try {
      Class<? extends VocabProvider> bindingClass = environment.classLoader().loadClass(vocabVersion)
          .asSubclass(VocabProvider.class);
      bind(VocabProvider.class).to(bindingClass);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

  }

}
