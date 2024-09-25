package services;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Created by fo on 25.11.15.
 */

public interface VocabProvider {
  public Model getVocab();
}
