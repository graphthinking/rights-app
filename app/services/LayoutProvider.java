package services;

import com.github.jknack.handlebars.io.URLTemplateLoader;

/**
 * Created by fo on 19.04.16.
 */
public interface LayoutProvider {
  URLTemplateLoader getTemplateLoader();
}
