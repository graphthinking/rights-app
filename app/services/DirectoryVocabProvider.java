package services;

import com.google.inject.Inject;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import java.util.List;
import org.apache.jena.riot.Lang;
import play.api.Configuration;
import play.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import scala.Option;

/**
 * Created by fo on 10.03.16.
 */
public class DirectoryVocabProvider implements VocabProvider {

  private Model vocab = ModelFactory.createDefaultModel();

  @Inject
  public DirectoryVocabProvider(Configuration configuration) {

    Config source = configuration.underlying().getConfig("source.data");
   //Configuration source = source;
    String sourceDir = source.getString("dir");
    ConfigList formats = source.getList("formats");
    Path sourcePath = new File(sourceDir).isAbsolute()
        ? Paths.get(new File(sourceDir).getPath())
        : Paths.get(ClassLoader.getSystemResource(sourceDir).getPath());

    for (ConfigValue format : formats) {
      String ext ="";// formats.getConfig(format.subKeys()).get().getString("ext");
      String lang = "";//formats.getConfig(format.toString()).get().getString("lang");
      try (DirectoryStream<Path> files = Files.newDirectoryStream(sourcePath, "*".concat(ext))) {
        for (Path file : files) {
          vocab.read(Files.newInputStream(file), null, lang);
        }
      } catch (IOException e) {
        Logger.error(e.toString());
      }
    }

  }

  @Override
  public Model getVocab() {
    return vocab;
  }

}
