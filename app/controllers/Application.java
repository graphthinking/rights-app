package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.inject.Inject;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;
import play.api.Configuration;
import play.Logger;
import play.api.Environment;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.LayoutProvider;
import services.VocabProvider;

//import javax.activation.MimeType;
//import javax.activation.MimeTypeParseException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

/**
 * Created by fo on 16.11.15.
 */
public class Application extends Controller {

  private static Map<String, ConfigValue> mimeTypeParserMap = getMap(ConfigFactory.load().getConfig("parser").entrySet());

  private static Map<String, ConfigValue> mimeTypeExtMap =  getMap(ConfigFactory.load().getConfig("extension").entrySet());

  private static Map<String, ConfigValue> defaults =getMap( ConfigFactory.load().getConfig("default").entrySet());

  private static Map<String, ConfigValue> validParameters = getMap(ConfigFactory.load().getConfig("params").entrySet());

  private static Map<String, ConfigValue> sparqlQueries = getMap(ConfigFactory.load().getConfig("queries").entrySet());

  private static Map<String, ConfigValue> languages = getMap(ConfigFactory.load().getConfig("languages").entrySet());

  private final VocabProvider vocabProvider;

  private final LayoutProvider layoutProvider;

  private final Configuration configuration;

  private Environment env;

  @Inject
  public Application(VocabProvider vocabProvider, LayoutProvider layoutProvider, Configuration configuration,Environment env) {
    this.vocabProvider = vocabProvider;
    this.layoutProvider = layoutProvider;
    this.configuration = configuration;
    this.env = env;
  }

  public Result getVocab(String version, Http.Request request) {
    Logger.info("Getting vocab for version: "+version);
    if (request.accepts("text/html")) {
      Locale locale = getLocale(request, null);
      Logger.info(locale.getLanguage());
      return redirect(routes.Application.getVocabPage(version, locale.getLanguage()).url());
    } else {
      Logger.info(routes.Application.getVocabData(version, null).url());
      return redirect(routes.Application.getVocabData(version, null).url());
    }

  }

  public Result getVocabData(String version, String extension, Http.Request request) {

    Model vocab = getVocabModel(version);

    if (vocab.isEmpty()) {
      return notFoundPage(request);
    }

    //MimeType mimeType = getMimeType(request, extension);
    String mimeType ="application/json";
//    response().setHeader("Content-Location", routes.Application.getVocabData(version,
//        mimeTypeExtMap.getOrDefault(mimeType.toString(), defaults.get("mime").toString()).toString())
//        .url());
//    response().setHeader("Link", "<".concat(routes.Application.getVocabData(version, null)
//        .url()).concat(">; rel=derivedfrom"));

    return getData(vocab, mimeType);

  }

  public Result getVocabPage(String version, String language,Http.Request request) throws IOException {

    Model vocab = getVocabModel(version);
    Locale locale = getLocale(request, language);

    if (vocab.isEmpty()) {
      return notFoundPage(request);
    }

//    response().setHeader("Link", "<".concat(routes.Application.getVocabPage(version, null)
//        .url()).concat(">; rel=derivedfrom"));
//    response().setHeader("Content-Language", locale.getLanguage());

    return getPage(vocab, "/".concat(locale.toLanguageTag()).concat("/statements/vocab.html"), locale.getLanguage(), null,request);

  }

  public Result getStatement(String id, String version,Http.Request req) {

    if (!req.queryString().isEmpty()) {
      setAlternates(req, id, version, true);
      return notAcceptablePage(req);
    } else  if (req.accepts("text/html")) {
      Locale locale = getLocale(req, null);
      return redirect(routes.Application.getStatementPage(id, version, locale.getLanguage()).url());
    } else {
      return redirect(routes.Application.getStatementData(id, version, null).url());
    }

  }

  public Result getStatementData(String id, String version, String extension, Http.Request req) {

    if (!req.queryString().isEmpty()) {
      setAlternates(req, id, version, false);
      return notAcceptablePage(req);
    }

    Model rightsStatement = getStatementModel(id, version);

    if (rightsStatement.isEmpty()) {
      return notFoundPage(req);
    }

    //MimeType mimeType = getMimeType(req, extension);
    String mimeType ="application/json";
//    response().setHeader("Content-Location", routes.Application.getStatementData(id, version,
//        mimeTypeExtMap.getOrDefault(mimeType.toString(), defaults.get("mime").toString()).toString())
//        .url());
//    response().setHeader("Link", "<".concat(routes.Application.getStatementData(id, version, null)
//        .url()).concat(">; rel=derivedfrom"));

    return getData(rightsStatement, mimeType);

  }

  public Result getStatementPage(String id, String version, String language,Http.Request req) throws IOException {

    Model rightsStatement = getStatementModel(id, version);
    Locale locale = getLocale(req, language);

    if (rightsStatement.isEmpty()) {
      return notFoundPage(req);
    }

//    response().setHeader("Link", "<".concat(routes.Application.getStatementPage(id, version, null)
//        .url()).concat(">; rel=derivedfrom"));
//    response().setHeader("Content-Language", locale.getLanguage());

    return getPage(rightsStatement, "/en/statement.hbs", locale.getLanguage(), getParameters(req, id),req);

  }

  public Result getCollection(String id, String version,Http.Request req) {

    if (req.accepts("text/html")) {
      Locale locale = getLocale(req, null);
      return redirect(routes.Application.getCollectionPage(id, version, locale.getLanguage()).url());
    } else {
      return redirect(routes.Application.getCollectionData(id, version, null).url());
    }

  }

  public Result getCollectionData(String id, String version, String extension,Http.Request req) {

    Model collection = getCollectionModel(id, version);

    if (collection.isEmpty()) {
      return notFoundPage(req);
    }

   // MimeType mimeType = getMimeType(req, extension);

    String mimeType ="application/json";
//    response().setHeader("Content-Location", routes.Application.getCollectionData(id, version,
//        mimeTypeExtMap.getOrDefault(mimeType.toString(), defaults.get("mime").toString()).toString())
//        .url());
//    response().setHeader("Link", "<".concat(routes.Application.getCollectionData(id, version, null)
//        .url()).concat(">; rel=derivedfrom"));

    return getData(collection, mimeType);

  }

  public Result getCollectionPage(String id, String version, String language,Http.Request req) throws IOException {

    Model collection = getVocabModel(version);
    Locale locale = getLocale(req, language);

    if (collection.isEmpty()) {
      return notFoundPage(req);
    }

//    response().setHeader("Link", "<".concat(routes.Application.getCollectionPage(id, version, null)
//        .url()).concat(">; rel=derivedfrom"));
//    response().setHeader("Content-Language", locale.getLanguage());

    return getPage(collection, locale.toLanguageTag().concat("/statements/collection-").concat(id).concat(".html"),
        locale.getLanguage(), null,req);

  }

  private Result notFoundPage(Request request) {
    TemplateLoader loader = layoutProvider.getTemplateLoader();
    loader.setPrefix(getDeployUrl(request));
    try {
      return notFound(loader.sourceAt("/en/404.html").content()).as("text/html");
    } catch (IOException e) {
      Logger.error(e.toString());
      return notFound("Not Found");
    }

  }

  private Result notAcceptablePage(Request req) {
    TemplateLoader loader = layoutProvider.getTemplateLoader();
    loader.setPrefix(getDeployUrl(req));
    try {
      return status(406, loader.sourceAt("/en/406.html").content()).as("text/html");
    } catch (IOException e) {
      Logger.error(e.toString());
      return status(406, "Not Acceptable");
    }

  }

  private Result getData(Model model, String mimeType) {

    OutputStream result = new ByteArrayOutputStream();
    model.write(result, mimeTypeParserMap.getOrDefault(mimeType, defaults.get("parser"))
        .toString());
    return ok(result.toString()).as(
        mimeType.equals("*/*") ? defaults.get("mime").toString() : mimeType);

  }

  private Result getPage(Model model, String templateFile, String language, HashMap<String, String> parameters,Http.Request req)
      throws IOException {

    Model localized = ModelFactory.createDefaultModel();
    QueryExecutionFactory.create(QueryFactory.create(String.format(sparqlQueries.get("localize").toString(), language)),
        model).execConstruct(localized);

    Map<String, Object> scope = new HashMap<>();
    scope.put("parameters", parameters);
    scope.put("language", language);

    OutputStream boas = new ByteArrayOutputStream();
    localized.write(boas, "JSON-LD");
    scope.put("data", new ObjectMapper().readValue(boas.toString(), HashMap.class));

    TemplateLoader loader = layoutProvider.getTemplateLoader();
    loader.setPrefix(getDeployUrl(req));
    Handlebars handlebars = new Handlebars(loader);

    try {
      handlebars.registerHelpers("helpers.js",env.classLoader()
          .getResourceAsStream("public/js/helpers.js"));
    } catch (Exception e) {
      Logger.error(e.toString());
    }

    return ok(handlebars.compile(templateFile).apply(scope)).as("text/html");

  }

  private Model getVocabModel(String version) {

    Model vocab = ModelFactory.createDefaultModel();
    QueryExecutionFactory.create(QueryFactory.create(String.format(sparqlQueries.get("vocab").toString(), version)),
        vocabProvider.getVocab()).execConstruct(vocab);

    return vocab;

  }

  private Model getStatementModel(String id, String version) {

    Model statement = ModelFactory.createDefaultModel();
    QueryExecutionFactory.create(QueryFactory.create(String.format(sparqlQueries.get("statement").toString(), version,
        id)), vocabProvider.getVocab()).execConstruct(statement);

    return statement;

  }

  private Model getCollectionModel(String id, String version) {

    Model collection = ModelFactory.createDefaultModel();
    QueryExecutionFactory.create(QueryFactory.create(String.format(sparqlQueries.get("collection").toString(), id,
        version)), vocabProvider.getVocab()).execConstruct(collection);

    return collection;

  }

//  private MimeType getMimeType(Http.Request request, String extension) {
//
//    if (extension != null) {
//      return getMimeTypeByExtension(extension);
//    } else {
//      return getMimeTypeFromRequest(request);
//    }
//
//  }

//  private static MimeType getMimeTypeFromRequest(Http.Request request) {
//
//    MimeType mimeType;
//    List<MediaRange> acceptedTypes = request.acceptedTypes();
//
//    try {
//      if (! acceptedTypes.isEmpty()) {
//        mimeType = new MimeType(request.acceptedTypes().get(0).toString());
//      } else {
//        mimeType = new MimeType("*/*");
//      }
//    } catch (MimeTypeParseException e) {
//      Logger.error(e.toString());
//      mimeType = new MimeType();
//    }
//
//    return mimeType;
//
//  }

//  private static MimeType getMimeTypeByExtension(@Nonnull String extension) {
//
//    for (Map.Entry<String, Object> entry : mimeTypeExtMap.entrySet()) {
//      if (entry.getValue().equals(extension)) {
//        try {
//          return new MimeType(entry.getKey());
//        } catch (MimeTypeParseException e) {
//          Logger.error(e.toString());
//        }
//      }
//    }
//
//    return new MimeType();
//
//  }

  private Locale getLocale(Http.Request request, String language) {

    Locale[] requestedLocales;

    if (language != null) {
      requestedLocales = getLocalesByCode(language);
    } else {
      requestedLocales = getLocalesFromRequest(request);
    }

    Locale[] availableLocales = Arrays.stream(languages.get("available").toString().split(" +"))
        .map(code -> Locale.forLanguageTag(code)).toArray(Locale[]::new);

    if (requestedLocales != null) {
      for (Locale requestedLocale : requestedLocales) {
        if (Arrays.asList(availableLocales).contains(requestedLocale)) {
          return requestedLocale;
        }
      }
    }

    return availableLocales[0];

  }

  private Locale[] getLocalesFromRequest(Http.Request request) {

    if (!request.acceptLanguages().isEmpty()) {
      return request.acceptLanguages().stream().map(lang -> lang.toLocale()).toArray(Locale[]::new);
    }

    return null;

  }

  private Locale[] getLocalesByCode(String code) {

    return new Locale[]{Locale.forLanguageTag(code)};

  }

  private void setAlternates(Http.Request request, String id, String version, boolean includeVocab) {

    Map<String, String[]> parameters = request.queryString();

    if (parameters.size() > 0) {

      List<String> recoveryParameters = new ArrayList<>();

      for (Map.Entry<String, String> parameter : getParameters(request, id).entrySet()) {
        recoveryParameters.add(parameter.getKey().concat("=").concat(parameter.getValue()));
      }

      if (!recoveryParameters.isEmpty()) {

        String vocabUrl = routes.Application.getStatement(id, version).url();
        String pageUrl = routes.Application.getStatementPage(id, version, null).url().concat("?")
            .concat(String.join("&", recoveryParameters));
        String dataUrl = routes.Application.getStatementData(id, version, null).url();

        List<String> alternates = new ArrayList<>();

        if (includeVocab) {
          alternates.add(String.format("{\"%s\" 0.9}", vocabUrl));
        }

        alternates.add(String.format("{\"%s\" 0.9 {text/html}}", pageUrl));

        for ( Entry<String, ConfigValue> entry : mimeTypeExtMap.entrySet()) {
          if (entry.getKey().equals("*/*")) {
            continue;
          }
          alternates.add(String.format("{\"%s\" 0.9 {".concat(entry.getKey()).concat("}}"), dataUrl));
        }
        //response().setHeader("Alternates", String.join(",", alternates));

      }

    }

  }

  private HashMap<String, String> getParameters(Http.Request request, String id) {

    HashMap<String, String> parameters = new HashMap<>();
    String validParameters =   Application.validParameters.get(id).toString();

    if (validParameters != null) {
      for (String validParameter : validParameters.split(" ")) {
        String suppliedParameter = request.getQueryString(validParameter);
        if (suppliedParameter != null) {
          parameters.put(validParameter, StringEscapeUtils.escapeHtml4(request.getQueryString(validParameter)));
        }
      }
    }

    return parameters;

  }

  private String getDeployUrl(Http.Request req) {
    if (configuration.underlying().getString("source.site.http") != null) {
      return configuration.underlying().getString("source.site.http");
    }
    return req.hasHeader("X-Deploy-Url")
      ? req.headers().get("X-Deploy-Url").get()
      : "/";
  }




private static Map<String, ConfigValue> getMap(Set<Entry<String, ConfigValue>> inputSet){
  return inputSet.stream()
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

}



  public Result index() {
    return ok(views.html.index.render());
  }
}