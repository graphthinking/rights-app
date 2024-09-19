package modules;

import com.google.inject.AbstractModule;
import play.api.Configuration;
import play.api.Environment;
import play.api.Play;
import services.LayoutProvider;

/**
 * Created by fo on 19.04.16.
 */
public class LayoutProviderModule extends AbstractModule {

  private final Environment environment;

  private final Configuration configuration;

  public LayoutProviderModule(Environment environment, Configuration configuration) {
    this.environment = environment;
    this.configuration = configuration;
  }

  protected void configure() {

    String vocabVersion = configuration.underlying().getString("layout.provider");

    try {
      Class<? extends LayoutProvider> bindingClass = environment.classLoader().loadClass(vocabVersion)
          .asSubclass(LayoutProvider.class);
      bind(LayoutProvider.class).to(bindingClass);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

  }

}
