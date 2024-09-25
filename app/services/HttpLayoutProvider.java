package services;

import com.github.jknack.handlebars.io.URLTemplateLoader;
import com.google.inject.Singleton;
import play.Logger;

import java.io.IOException;
import java.net.URL;

@Singleton
public class HttpLayoutProvider implements LayoutProvider {

  public URLTemplateLoader getTemplateLoader() {
    URLTemplateLoader urlTemplateLoader = new HttpTemplateLoader();
    urlTemplateLoader.setSuffix("");
    return urlTemplateLoader;
  }

  public class HttpTemplateLoader extends URLTemplateLoader {
    protected URL getResource(final String location) throws IOException {
      Logger.debug("Fetching " + location);
      // TODO: cache templates, see e.g. https://stackoverflow.com/a/45439170
      return new URL(location);
    }
  }

}
