package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.inject.Inject;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import play.Configuration;
import play.Logger;
import play.Play;
import play.api.http.MediaRange;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.LayoutProvider;
import services.VocabProvider;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.annotation.Nonnull;
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

  private static Map<String, Object> mimeTypeParserMap = Play.application().configuration().getConfig("parser").asMap();

  private static Map<String, Object> mimeTypeExtMap = Play.application().configuration().getConfig("extension").asMap();

  private static Map<String, Object> defaults = Play.application().configuration().getConfig("default").asMap();

  private static Map<String, Object> validParameters = Play.application().configuration().getConfig("params").asMap();

  private static Map<String, Object> sparqlQueries = Play.application().configuration().getConfig("queries").asMap();

  private static Map<String, Object> languages = Play.application().configuration().getConfig("languages").asMap();

  private final VocabProvider vocabProvider;

  private final LayoutProvider layoutProvider;

  private final Configuration configuration;

  @Inject
  public Application(VocabProvider vocabProvider, LayoutProvider layoutProvider, Configuration configuration) {
    this.vocabProvider = vocabProvider;
    this.layoutProvider = layoutProvider;
    this.configuration = configuration;
  }

  public Result getVocab(String version) {
    Logger.info("Getting vocab for version: "+version);
    if (request().accepts("text/html")) {
      Locale locale = getLocale(request(), null);
      Logger.info(locale.getLanguage());
      return redirect(routes.Application.getVocabPage(version, locale.getLanguage()).url());
    } else {
      Logger.info(routes.Application.getVocabData(version, null).url());
      return redirect(routes.Application.getVocabData(version, null).url());
    }

  }

  public Result getVocabData(String version, String extension) {

    Model vocab = getVocabModel(version);

    if (vocab.isEmpty()) {
      return notFoundPage();
    }

    MimeType mimeType = getMimeType(request(), extension);
    response().setHeader("Content-Location", routes.Application.getVocabData(version,
        mimeTypeExtMap.getOrDefault(mimeType.toString(), defaults.get("mime").toString()).toString())
        .url());
    response().setHeader("Link", "<".concat(routes.Application.getVocabData(version, null)
        .url()).concat(">; rel=derivedfrom"));

    return getData(vocab, mimeType);

  }

  public Result getVocabPage(String version, String language) throws IOException {

    Model vocab = getVocabModel(version);
    Locale locale = getLocale(request(), language);

    if (vocab.isEmpty()) {
      return notFoundPage();
    }

    response().setHeader("Link", "<".concat(routes.Application.getVocabPage(version, null)
        .url()).concat(">; rel=derivedfrom"));
    response().setHeader("Content-Language", locale.getLanguage());

    return getPage(vocab, "/".concat(locale.toLanguageTag()).concat("/statements/vocab.html"), locale.getLanguage(), null);

  }

  public Result getStatement(String id, String version) {

    if (!request().queryString().isEmpty()) {
      setAlternates(request(), id, version, true);
      return notAcceptablePage();
    } else  if (request().accepts("text/html")) {
      Locale locale = getLocale(request(), null);
      return redirect(routes.Application.getStatementPage(id, version, locale.getLanguage()).url());
    } else {
      return redirect(routes.Application.getStatementData(id, version, null).url());
    }

  }

  public Result getStatementData(String id, String version, String extension) {

    if (!request().queryString().isEmpty()) {
      setAlternates(request(), id, version, false);
      return notAcceptablePage();
    }

    Model rightsStatement = getStatementModel(id, version);

    if (rightsStatement.isEmpty()) {
      return notFoundPage();
    }

    MimeType mimeType = getMimeType(request(), extension);
    response().setHeader("Content-Location", routes.Application.getStatementData(id, version,
        mimeTypeExtMap.getOrDefault(mimeType.toString(), defaults.get("mime").toString()).toString())
        .url());
    response().setHeader("Link", "<".concat(routes.Application.getStatementData(id, version, null)
        .url()).concat(">; rel=derivedfrom"));

    return getData(rightsStatement, mimeType);

  }

  public Result getStatementPage(String id, String version, String language) throws IOException {

    Model rightsStatement = getStatementModel(id, version);
    Locale locale = getLocale(request(), language);

    if (rightsStatement.isEmpty()) {
      return notFoundPage();
    }

    response().setHeader("Link", "<".concat(routes.Application.getStatementPage(id, version, null)
        .url()).concat(">; rel=derivedfrom"));
    response().setHeader("Content-Language", locale.getLanguage());

    return getPage(rightsStatement, "/en/statement.hbs", locale.getLanguage(), getParameters(request(), id));

  }

  public Result getCollection(String id, String version) {

    if (request().accepts("text/html")) {
      Locale locale = getLocale(request(), null);
      return redirect(routes.Application.getCollectionPage(id, version, locale.getLanguage()).url());
    } else {
      return redirect(routes.Application.getCollectionData(id, version, null).url());
    }

  }

  public Result getCollectionData(String id, String version, String extension) {

    Model collection = getCollectionModel(id, version);

    if (collection.isEmpty()) {
      return notFoundPage();
    }

    MimeType mimeType = getMimeType(request(), extension);
    response().setHeader("Content-Location", routes.Application.getCollectionData(id, version,
        mimeTypeExtMap.getOrDefault(mimeType.toString(), defaults.get("mime").toString()).toString())
        .url());
    response().setHeader("Link", "<".concat(routes.Application.getCollectionData(id, version, null)
        .url()).concat(">; rel=derivedfrom"));

    return getData(collection, mimeType);

  }

  public Result getCollectionPage(String id, String version, String language) throws IOException {

    Model collection = getVocabModel(version);
    Locale locale = getLocale(request(), language);

    if (collection.isEmpty()) {
      return notFoundPage();
    }

    response().setHeader("Link", "<".concat(routes.Application.getCollectionPage(id, version, null)
        .url()).concat(">; rel=derivedfrom"));
    response().setHeader("Content-Language", locale.getLanguage());

    return getPage(collection, locale.toLanguageTag().concat("/statements/collection-").concat(id).concat(".html"),
        locale.getLanguage(), null);

  }

  private Result notFoundPage() {
    TemplateLoader loader = layoutProvider.getTemplateLoader();
    loader.setPrefix(getDeployUrl());
    try {
      return notFound(loader.sourceAt("/en/404.html").content()).as("text/html");
    } catch (IOException e) {
      Logger.error(e.toString());
      return notFound("Not Found");
    }

  }

  private Result notAcceptablePage() {
    TemplateLoader loader = layoutProvider.getTemplateLoader();
    loader.setPrefix(getDeployUrl());
    try {
      return status(406, loader.sourceAt("/en/406.html").content()).as("text/html");
    } catch (IOException e) {
      Logger.error(e.toString());
      return status(406, "Not Acceptable");
    }

  }

  private Result getData(Model model, MimeType mimeType) {

    OutputStream result = new ByteArrayOutputStream();
    model.write(result, mimeTypeParserMap.getOrDefault(mimeType.toString(), defaults.get("parser").toString())
        .toString());
    return ok(result.toString()).as(
        mimeType.toString().equals("*/*") ? defaults.get("mime").toString() : mimeType.toString());

  }

  private Result getPage(Model model, String templateFile, String language, HashMap<String, String> parameters)
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
    loader.setPrefix(getDeployUrl());
    Handlebars handlebars = new Handlebars(loader);

    try {
      handlebars.registerHelpers("helpers.js", Play.application().classloader()
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

  private MimeType getMimeType(Http.Request request, String extension) {

    if (extension != null) {
      return getMimeTypeByExtension(extension);
    } else {
      return getMimeTypeFromRequest(request);
    }

  }

  private static MimeType getMimeTypeFromRequest(Http.Request request) {

    MimeType mimeType;
    List<MediaRange> acceptedTypes = request.acceptedTypes();

    try {
      if (! acceptedTypes.isEmpty()) {
        mimeType = new MimeType(request.acceptedTypes().get(0).toString());
      } else {
        mimeType = new MimeType("*/*");
      }
    } catch (MimeTypeParseException e) {
      Logger.error(e.toString());
      mimeType = new MimeType();
    }

    return mimeType;

  }

  private static MimeType getMimeTypeByExtension(@Nonnull String extension) {

    for (Map.Entry<String, Object> entry : mimeTypeExtMap.entrySet()) {
      if (entry.getValue().equals(extension)) {
        try {
          return new MimeType(entry.getKey());
        } catch (MimeTypeParseException e) {
          Logger.error(e.toString());
        }
      }
    }

    return new MimeType();

  }

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

        for (Map.Entry<String, Object> entry : mimeTypeExtMap.entrySet()) {
          if (entry.getKey().equals("*/*")) {
            continue;
          }
          alternates.add(String.format("{\"%s\" 0.9 {".concat(entry.getKey()).concat("}}"), dataUrl));
        }

        response().setHeader("Alternates", String.join(",", alternates));

      }

    }

  }

  private HashMap<String, String> getParameters(Http.Request request, String id) {

    HashMap<String, String> parameters = new HashMap<>();
    String validParameters = (String) Application.validParameters.get(id);

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

  private String getDeployUrl() {
    if (configuration.getString("source.site.http") != null) {
      return configuration.getString("source.site.http");
    }
    return request().hasHeader("X-Deploy-Url")
      ? request().getHeader("X-Deploy-Url")
      : "/";
  }

} 
