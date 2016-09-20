package mpi.aida.datapreparation.gnd.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import mpi.keyphraseextraction.KeyphraseExtractor;
import mpi.keyphraseextraction.NounPhrase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.util.StringUtils;

public class ExtractKeyphraseFromTitleData {

  private static final Logger logger = LoggerFactory.getLogger(ExtractKeyphraseFromTitleData.class);
  
  //mamir: not all files are of proper utf8, the most reliable tool I found
  //is the linux command chardet.
  //I first run the script /local/var/tmp/aida/data/DNB/findFilesWithUTF8Encoding.sh
  //to produce list of utf files stored in /local/var/tmp/aida/data/DNB/TOC_files_with_UTF8.txt
  private String utf8FilesList = "/local/var/tmp/aida/data/DNB/TOC_files_with_UTF8.txt";

  private String tocRootDirectoryPath = "/local/var/tmp/aida/data/DNB/TOC/TOCs-20130521/";
  //private String tocRootDirectoryPath = "/local_san/var/tmp/mamir/gnd/TOC/TOCs-20130521/";

  private String outputPath = "/local/var/tmp/aida/data/DNB/TOC/";
  //private String outputPath = "/local_san/var/tmp/mamir/gnd/TOC/";

  private String tocOutputFileName = outputPath + "combined_TOCs-20130521.tsv";

  private String abstractOutputFileName = outputPath + "combined_Abstracts-20130521.tsv";

  private final String TOC_FILE_NAME = "toc.txt";

  private final String ABSTRACT_FILE_NAME = "abstract.html";

  private final int THREAD_MAX_COUNT = 16;

  private KeyphraseExtractor keyphraseExtractor;

  public ExtractKeyphraseFromTitleData() {
    keyphraseExtractor = new KeyphraseExtractor();
  }

  private void run() throws IOException, InterruptedException {
    Set<String> tocTitlesWhiteList = new HashSet<String>();
    Set<String> abstractTitlesWhiteList = new HashSet<String>();
    logger.info("Reading whilelist file ...");
    readWhiteLists(tocTitlesWhiteList, abstractTitlesWhiteList);
    logger.info("DONE");
    run(TOC_FILE_NAME, tocOutputFileName, false, tocTitlesWhiteList);
    run(ABSTRACT_FILE_NAME, abstractOutputFileName, true, abstractTitlesWhiteList);
  }

  private void run(String inputFilesName, String outputFileName, boolean isHTML, Set<String> titlesWhiteList) throws IOException, InterruptedException {
    FilenameFilter filenameFilter = new NameBasedFileNameFilter(inputFilesName);
    Writer writer = new Writer(outputFileName);
    logger.info("Start reading " + inputFilesName + " ...");
    ExecutorService es = Executors.newFixedThreadPool(THREAD_MAX_COUNT);

    File rootDirectory = new File(tocRootDirectoryPath);
    for (File level1 : rootDirectory.listFiles()) {
      for (File level2 : level1.listFiles()) {
        for (File level3 : level2.listFiles()) {
          String titleId = level3.getName() + level2.getName() + level1.getName();
          if (!titlesWhiteList.contains(titleId)) {
            continue;
          }
          File[] tocFiles = level3.listFiles(filenameFilter);
          if (tocFiles.length != 1) {
            continue;
          }
          File file = tocFiles[0];

          Extractor extractor = new Extractor(keyphraseExtractor, file, titleId, writer, isHTML);
          es.execute(extractor);

        }
      }
    }
    
    es.shutdown();
    es.awaitTermination(6, TimeUnit.DAYS);

    writer.close();

    logger.info("Done!");
  }
  
  private void readWhiteLists(Set<String> tocTitlesWhiteList, Set<String> abstractTitlesWhiteList) {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(utf8FilesList), Charset.forName("ASCII")));
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.split(":")[0];
        String[] lineParts = line.split("/");
        String titleId = lineParts[5] + lineParts[4] + lineParts[3];
        if(lineParts[6].equals("toc.txt")) {
          tocTitlesWhiteList.add(titleId);
        } else if(lineParts[6].equals("abstract.html")) {
          abstractTitlesWhiteList.add(titleId);
        } else {
          logger.error("UnEXPECTED FILE:" + line);
        }
      }
      reader.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  

  class Extractor implements Runnable {

    private KeyphraseExtractor keyphraseExtractor;

    private File sourceFile;

    private String titleId;

    private Writer writer;

    private boolean isHTML;

    public Extractor(KeyphraseExtractor keyphraseExtractor, File sourceFile, String titleId, Writer writer, boolean isHTML) {
      this.keyphraseExtractor = keyphraseExtractor;
      this.sourceFile = sourceFile;
      this.titleId = titleId;
      this.writer = writer;
      this.isHTML = isHTML;
    }

    @Override
    public void run() {
      try {
        String text = FileUtils.readFileToString(sourceFile, "UTF-8");

        if (isHTML) {
          // Clean text - remove html tags.
          Document doc = Jsoup.parse(text);
          StringBuilder sb = new StringBuilder();
          if (doc.title() != null) {
            sb.append(doc.title()).append("\n\n");
          }
          if (doc.body() != null) {
            sb.append(doc.body().text().replaceAll("\\s+", " "));
          }
          text = sb.toString();
          text =  StringEscapeUtils.unescapeHtml(text);
        }

        List<NounPhrase> keyphrases = keyphraseExtractor.findKeyphrases(text);
        Set<NounPhrase> nounPhrases = new HashSet<>(keyphrases);
        
        writer.writeNounPhrases(titleId, nounPhrases);

      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

  }

  class Writer {

    private BufferedWriter writer;
    int count = 0;

    public Writer(String outputFileName) throws FileNotFoundException {
      BufferedWriter writer = mpi.tools.javatools.util.FileUtils.getBufferedUTF8Writer(outputFileName);
      this.writer = writer;
    }

    public synchronized void writeNounPhrases(String titleId, Set<NounPhrase> nounPhrases) throws IOException {
      String allNounPhrases = StringUtils.join(nounPhrases, "\t");
      writer.write(titleId + "\t" + allNounPhrases);
      writer.newLine();
      

      if ((++count % 100000) == 0) {
        logger.info("Finished extraction for " + count );
      }
    }

    public void close() throws IOException {
      writer.flush();
      writer.close();
    }
  }
  
  /**
   * @param args
   * @throws IOException 
   * @throws InterruptedException 
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    new ExtractKeyphraseFromTitleData().run();
  }

  private class NameBasedFileNameFilter implements FilenameFilter {

    private String acceptedName;

    public NameBasedFileNameFilter(String acceptedName) {
      this.acceptedName = acceptedName;
    }

    @Override
    public boolean accept(File dir, String name) {
      if (name.equals(acceptedName)) {
        return true;
      } else {
        return false;
      }
    }
  }

}
