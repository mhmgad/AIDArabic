package mpi.experiment.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mpi.aida.data.Context;
import mpi.aida.data.Entities;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.data.PreparedInput;
import mpi.tools.javatools.util.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CollectionReader implements Iterable<PreparedInput> {
  private static final Logger logger = 
      LoggerFactory.getLogger(CollectionReader.class);

  protected String filePath = null;
  
  protected String collectionPath;

  private int[] docNums;

  protected Set<Integer> allDocNumbers = new HashSet<>();
  
  protected List<PreparedInput> preparedInputs;
  
  protected CollectionReaderSettings settings;

  public static enum DataSource {
    CONLL, WIKIPEDIA_YAGO2, AIDA, NEWSSTREAMS, GIGAWORD5, AIDA_SINGLE, DNB, MICROPOSTS, NONE 
  }

  public static final String CONLL = "CONLL";

  public static final String WIKIPEDIA_YAGO2 = "WIKIPEDIA_YAGO2";
  
  public static final String AIDA = "AIDA";
  
  public static final String GIGAWORD5 = "GIGAWORD5";
  
  public static final String NEWSSTREAMS = "NEWSSTREAMS";
  
  public static final String AIDA_SINGLE = "AIDA_SINGLE";
  
  public static final String DNB = "DNB";
  
  public static final String NONE = "NONE";

  public static enum CollectionPart {
    TRAIN, DEV, DEV_SMALL, TEST, ALL
  }

  public static final String TRAIN = "TRAIN";

  public static final String DEV = "DEV";
  
  public static final String DEV_SMALL = "DEV_SMALL";

  public static final String TEST = "TEST";
  
  public static final String ALL = "ALL";

  public CollectionReader(String collectionPath, CollectionReaderSettings settings) {
    this(collectionPath, CollectionPart.ALL, settings);
  }

  public CollectionReader(String collectionPath, CollectionPart cp, CollectionReaderSettings settings) {
    int[] ft = getCollectionPartFromTo(cp);
    if (ft != null) {
      init(collectionPath, ft[0], ft[1], settings);
    } else {
      // Take all documents.
      init(collectionPath, null, settings);
    }
  }

  public CollectionReader(String collectionPath, int from, int to, CollectionReaderSettings settings) {
    init(collectionPath, from, to, settings);
  }

  /**
   *
   * @param collectionPath
   * @param docNums If set to null, all documents will be loaded.
   * @param settings
   */
  public CollectionReader(String collectionPath, int[] docNums, CollectionReaderSettings settings) {
    init(collectionPath, docNums, settings);
  }

  public void init(String collectionPath, int from, int to, CollectionReaderSettings settings) {
    int[] docNums = new int[to - from + 1];
    for (int i = from; i <= to; ++i) {
      docNums[i - from] = i;
    }
    init(collectionPath, docNums, settings);
  }

  public void init(String collectionPath, int[] docNums,  CollectionReaderSettings settings) {
    this.collectionPath = collectionPath;
    this.settings = settings;
    this.docNums = docNums;

    if (docNums != null) {
      for (int i : docNums) {
        allDocNumbers.add(i);
      }
    }
  }

  public int[] getDocNums() {
    return docNums;
  }

  public String getFilePath() {
    return filePath;
  }

  public String getCollectionPath() {
    return collectionPath;
  }

  public abstract Mentions getDocumentMentions(String docId);
  
  public abstract Context getDocumentContext(String docId);
  
  public abstract int collectionSize();

  public abstract String getText(String docId) ;
  
  protected abstract int[] getCollectionPartFromTo(CollectionPart cp);

  public static CollectionPart getCollectionPart(String collectionPart) {
    if (collectionPart == null) {
      return null;
    }

    if (collectionPart.equals(TRAIN)) {
      return CollectionPart.TRAIN;
    } else if (collectionPart.equals(DEV)) {
      return CollectionPart.DEV;
    } else if (collectionPart.equals(DEV_SMALL)) {
      return CollectionPart.DEV_SMALL;
    } else if (collectionPart.equals(TEST)) {
      return CollectionPart.TEST;
    } else {
      return CollectionPart.ALL;
    }
  }
  
  public Map<String, String> getAllDocuments() {
    Map<String, String> docsWithText = new HashMap<String, String>();
    
    for (PreparedInput inputDoc : this) {
      docsWithText.put(inputDoc.getDocId(), getText(inputDoc.getDocId()));
    }
    
    return docsWithText;
  }

  public String readStringFromFile(File f) throws IOException {
	  BufferedReader reader = FileUtils.getBufferedUTF8Reader(f);

    StringBuilder sb = new StringBuilder();

    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      sb.append(line + "\n");
    }

    return sb.toString();
  }

  public CollectionReaderSettings getSettings() {
    return settings;
  }

  public String getCollectionStatistics() {
    int mentionCount = 0;
    int ookbeCount = 0;
    for (PreparedInput p : this) {
      mentionCount += p.getMentions().getMentions().size();
      for (Mention m : p.getMentions().getMentions()) {
        if (Entities.isOokbEntity(m.getGroundTruthResult())) {
          ++ookbeCount;
        }
      }
    }
    StringBuilder sb = new StringBuilder();
    if (!allDocNumbers.isEmpty()) {
      int min = Collections.min(allDocNumbers);
      int max = Collections.max(allDocNumbers);
      sb.append(collectionPath).append("[").append(min).append(":")
      .append(max).append("]:").append(collectionSize());
      if ((max - min + 1) != allDocNumbers.size()) {
        sb.append(" non-contiguous");
      }
    }
    sb.append(" documents, ").append(mentionCount).append(" mentions (").append(ookbeCount).append(" out of knowledge base).");
    return sb.toString();
  }
  
  public PreparedInput getPreparedInputForDocId(String docId) {
    PreparedInput prep = null;
    for (PreparedInput p : this) {
      if (p.getDocId().equals(docId)) {
        prep = p;
      }
    }
    return prep;
  }
}
