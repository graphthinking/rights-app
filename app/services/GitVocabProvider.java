package services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import play.api.Configuration;
import play.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import scala.Option;


/**
 * Created by fo on 25.11.15.
 */

@Singleton
public class GitVocabProvider implements VocabProvider {

  private Model vocab = ModelFactory.createDefaultModel();

  @Inject
  public GitVocabProvider(Configuration configuration) {

    Config source = configuration.underlying().getConfig("source.data");
    Config gitSource = source.getConfig("git");
    Config formats = source.getConfig("formats");
    try {

      String remoteURL = gitSource.getString("remote");
      File localPath = new File(String.valueOf(gitSource.getString("local")));
      FileUtils.deleteDirectory(localPath);

      try (Git git = Git.cloneRepository().setURI(remoteURL).setDirectory(localPath).call()) {

        Repository repository = git.getRepository();
        ObjectId lastCommitId = repository.resolve(String.valueOf(gitSource.getString("rev")));

        try (RevWalk revWalk = new RevWalk(repository)) {

          RevCommit commit = revWalk.parseCommit(lastCommitId);
          RevTree tree = commit.getTree();

          for (String formatName : formats.root().keySet()) {
            String ext = formats.getConfig(formatName).getString("ext");
            String lang =formats.getConfig(formatName).getString("lang");

            try (TreeWalk treeWalk = new TreeWalk(repository)) {
              treeWalk.addTree(tree);
              treeWalk.setRecursive(true);
              treeWalk.setFilter(PathSuffixFilter.create(ext));
              while (treeWalk.next()) {
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                loader.copyTo(baos);
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                vocab.read(bais, null, lang);
              }
            }
          }
          revWalk.dispose();
        }
      }
    } catch (IOException | GitAPIException e) {
      Logger.error(e.toString());
    }

  }

  @Override
  public Model getVocab() {

    return vocab;

  }

}
