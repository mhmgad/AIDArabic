package mpi.aida.util;

import java.io.BufferedWriter;
import java.io.Reader;

import mpi.tools.basics2.Normalize;
import mpi.tools.javatools.filehandlers.FileLines;
import mpi.tools.javatools.util.FileUtils;

/**
 * Extracts all article ids from a Wikipedia pages-articles dump.
 * Output format is:
 * article_title<TAB>id
 * 
 *
 */
public class WikipediaDumpArticleIdExtractor {

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      printUsage();
      System.exit(1);
    }
    
    final Reader reader = FileUtils.getBufferedUTF8Reader(args[0]);
    String page = FileLines.readBetween(reader, "<page>", "</page>");
    
    BufferedWriter writer = FileUtils.getBufferedUTF8Writer(args[1]);
    
    int pagesDone = 0;
    
    while (page != null) {
      if (++pagesDone % 100000 == 0) {
        System.err.println(pagesDone + " pages done.");
      }
      
      String title = FileLines.readBetween(page, "<title>", "</title>");
      String id = FileLines.readBetween(page, "<id>", "</id>");
      writer.write(Normalize.entity(title) + "\t" + id);
      writer.newLine();
      
      page = FileLines.readBetween(reader, "<page>", "</page>");
    }
    
    writer.flush();
    writer.close();
  }

  public static void printUsage() {
    System.out.println("Usage:");
    System.out.println("\tWikipediaDumpArticleIdExtractor <wikipedia-pages-articles.xml> <outfile>");
  }
}