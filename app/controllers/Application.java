package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.inject.Inject;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import play.Logger.ALogger;
import play.api.Configuration;
import play.Logger;
import play.api.Environment;
import play.api.http.MediaRange;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.LayoutProvider;
import services.VocabProvider;

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

  private static Map<String, Object> mimeTypeParserMap = generateParserMap();// generateValueMap(ConfigFactory.load().getConfig("parser"));
  private ALogger logger =Logger.of(this.getClass());

  private static Map<String, Object> generateParserMap() {
    Map<String, Object> mimeTypeParserMap =  new HashMap<>();
    mimeTypeParserMap.put("text/turtle","TURTLE");
    mimeTypeParserMap.put("application/json","JSON-LD");
    mimeTypeParserMap.put("application/ld+json","JSON-LD");
    mimeTypeParserMap.put("*/*","JSON-LD");
    return mimeTypeParserMap;
  }

  private static Map<String, Object> mimeTypeExtMap =  generateExtentionsMap();//generateValueMap(ConfigFactory.load().getConfig("extension"));

  private static Map<String, Object> generateExtentionsMap() {
    Map<String, Object> extensionsMap =  new HashMap<>();
    extensionsMap.put("text/turtle" ,"ttl");
    extensionsMap.put("application/json" ,"json");
    extensionsMap.put("application/ld+json" ,"jsonld");
    extensionsMap.put("*/*" ,"json");
    return extensionsMap;
  }

  private static Map<String, Object> defaults =generateValueMap( ConfigFactory.load().getConfig("default"));

  private static Map<String, Object> validParameters = generateValueMap(ConfigFactory.load().getConfig("params"));

  private static Map<String, Object> sparqlQueries = generateValueMap(ConfigFactory.load().getConfig("queries"));

  private static Map<String, Object> languages = generateValueMap(ConfigFactory.load().getConfig("languages"));

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
    String mimeType =getMimeType(request, extension);
    String mime = routes.Application.getVocabData(version,mimeTypeExtMap.getOrDefault(mimeType.toString(),
        defaults.get("mime").toString()).toString()).url();
    String link = "<".concat(routes.Application.getVocabData(version, null)
        .url()).concat(">; rel=derivedfrom");
    return getData(vocab, mimeType).withHeaders("Content-Location", mime,"Link",    link);

  }

  public Result getVocabPage(String version, String language,Http.Request request) throws IOException {
    Model vocab = getVocabModel(version);
    Locale locale = getLocale(request, language);
    if (vocab.isEmpty()) {
      return notFoundPage(request);
    }
    String linkValue = "<".concat(routes.Application.getVocabPage(version, null).url()).concat(">; rel=derivedfrom");
    return getPage(vocab, "/".concat(locale.toLanguageTag()).concat("/statements/vocab.html"), locale.getLanguage(), null,request)
        .withHeaders("Content-Language", locale.getLanguage(),"Link",linkValue);
  }

  public Result getStatement(String id, String version,Http.Request req) {
    if (!req.queryString().isEmpty()) {
      return notAcceptablePage(req).withHeaders("Alternates", setAlternates(req, id, version, true));
    } else  if (req.accepts("text/html")) {
      Locale locale = getLocale(req, null);
      return redirect(routes.Application.getStatementPage(id, version, locale.getLanguage()).url());
    } else {
      return redirect(routes.Application.getStatementData(id, version, null).url());
    }
  }

  public Result getStatementData(String id, String version, String extension, Http.Request req) {
    if (!req.queryString().isEmpty()) {
      return notAcceptablePage(req).withHeaders("Alternates",setAlternates(req, id, version, false));
    }
    Model rightsStatement = getStatementModel(id, version);
    if (rightsStatement.isEmpty()) {
      return notFoundPage(req);
    }
    String  mimeType = getMimeType(req, extension);
    String location = routes.Application.getStatementData(id, version,
            mimeTypeExtMap.getOrDefault(mimeType.toString(), defaults.get("mime").toString())
                .toString()).url();
    String link = "<".concat(routes.Application.getStatementData(id, version, null)
        .url()).concat(">; rel=derivedfrom");
    return getData(rightsStatement, mimeType).withHeaders("Content-Location", location,"Link", link);
  }

  public Result getStatementPage(String id, String version, String language,Http.Request req) throws IOException {

    Model rightsStatement = getStatementModel(id, version);
    Locale locale = getLocale(req, language);

    if (rightsStatement.isEmpty()) {
      return notFoundPage(req);
    }
    return getPage(rightsStatement, "/en/statement.hbs", locale.getLanguage(),
        getParameters(req, id), req).withHeaders("Content-Language", locale.getLanguage(),"Link", "<".concat(routes.Application.getStatementPage(id, version, null)
        .url()).concat(">; rel=derivedfrom"));
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
    String mimeType = getMimeType(req, extension);
    String mime = routes.Application.getCollectionData(id, version,
            mimeTypeExtMap.getOrDefault(mimeType.toString(), defaults.get("mime").toString()).toString()).url();
    String link = "<".concat(routes.Application.getCollectionData(id, version, null)
        .url()).concat(">; rel=derivedfrom");
    return getData(collection, mimeType).withHeaders("Content-Location", mime,"Link", link);
  }

  public Result getCollectionPage(String id, String version, String language,Http.Request req) throws IOException {
    Model collection = getVocabModel(version);
    Locale locale = getLocale(req, language);

    if (collection.isEmpty()) {
      return notFoundPage(req);
    }
    String concat = "<".concat(routes.Application.getCollectionPage(id, version, null)
        .url()).concat(">; rel=derivedfrom");
    Result result = getPage(collection,
        locale.toLanguageTag().concat("/statements/collection-").concat(id).concat(".html"),
        locale.getLanguage(), null, req);
    return  result.withHeaders("Link", concat,"Content-Language", locale.getLanguage());
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
    String contentTypeValue = mimeType.equals("*/*") ? defaults.get("mime").toString() : mimeType;
    if(StringUtils.isNotEmpty(contentTypeValue) )
      return ok(result.toString()).as(contentTypeValue + ";charset=utf-8");
    else
       return ok(result.toString());
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

    String output = boas.toString();
    scope.put("data", new ObjectMapper().readValue(output, HashMap.class));
    TemplateLoader loader = layoutProvider.getTemplateLoader();
    loader.setPrefix(getDeployUrl(req));
    Handlebars handlebars = new Handlebars(loader);

    try {
      handlebars.registerHelpers("helpers.js",env.classLoader()
          .getResourceAsStream("public/js/helpers.js"));
    } catch (Exception e) {
      Logger.error(e.toString());
    }
    String apply = handlebars.compile(templateFile).apply(scope);
    return ok(apply).as("text/html");
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

  private String getMimeType(Http.Request request, String extension) {
    if (extension != null) {
      return getMimeTypeByExtension(extension);
    } else {
      return getMimeTypeFromRequest(request);
    }
  }

  private static String getMimeTypeFromRequest(Http.Request request) {
    String mimeType = "*/*";
    List<MediaRange> acceptedTypes = request.acceptedTypes();
    if (!acceptedTypes.isEmpty()) {
      mimeType = request.acceptedTypes().get(0).toString();
    }
    return mimeType;
  }

  private static String getMimeTypeByExtension(String extension) {
    for (Map.Entry<String, Object> entry : mimeTypeExtMap.entrySet()) {
      if (entry.getValue().equals(extension)) {
          return  entry.getKey();
      }
    }
    return "*/*";
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

  private String setAlternates(Request request, String id, String version, boolean includeVocab) {

    List<String> alternates = new ArrayList<>();
    if (request.queryString().size() > 0) {
      List<String> recoveryParameters = new ArrayList<>();
      for (Map.Entry<String, String> parameter : getParameters(request, id).entrySet()) {
        recoveryParameters.add(parameter.getKey().concat("=").concat(parameter.getValue()));
      }
      if (!recoveryParameters.isEmpty()) {
        String vocabUrl = routes.Application.getStatement(id, version).url();
        String pageUrl = routes.Application.getStatementPage(id, version, null).url().concat("?")
            .concat(String.join("&", recoveryParameters));
        String dataUrl = routes.Application.getStatementData(id, version, null).url();
        if (includeVocab) {
          alternates.add(String.format("{\"%s\" 0.9}", vocabUrl));
        }
        alternates.add(String.format("{\"%s\" 0.9 {text/html}}", pageUrl));
        for ( Map.Entry<String, Object> entry : mimeTypeExtMap.entrySet()) {
          if (entry.getKey().equals("*/*")) {
            continue;
          }
          alternates.add(String.format("{\"%s\" 0.9 {".concat(entry.getKey()).concat("}}"), dataUrl));
        }
      }
    }
    return String.join(",", alternates);
  }

  private HashMap<String, String> getParameters(Http.Request request, String id) {

    HashMap<String, String> parameters = new HashMap<>();
    String validParameters =  Application.validParameters.get(id)!=null? Application.validParameters.get(id).toString():null;

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

  private static Map<String, Object> generateValueMap(Config queries)  {
    Map<String, Object> result = new HashMap<>();
    for(String key : queries.root().keySet()){
      result.put(key,queries.getAnyRef(key));
    }
    return result;
  }

  public Result index() {
    return ok(views.html.index.render());
  }
}