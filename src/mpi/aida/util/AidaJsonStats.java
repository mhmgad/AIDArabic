package mpi.aida.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tools.javatools.util.FileUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Get stats from a directory of AIDA JSON files.
 *
 */
public class AidaJsonStats {
  
  public static void main(String[] args) throws Exception {
    new AidaJsonStats().run(args);
  }

  public void run(String args[]) throws Exception {
    Options commandLineOptions = buildCommandLineOptions();
    CommandLineParser parser = new PosixParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(commandLineOptions, args);
    } catch (MissingOptionException e) {
      System.out.println("\n\n" + e + "\n\n");
      printHelp(commandLineOptions);
    }
    if (cmd.hasOption("h")) {
      printHelp(commandLineOptions);
    }

    String input = cmd.getOptionValue("i");
    File inputFile = new File(input);
    List<File> files = new ArrayList<File>();
    if (!inputFile.isDirectory()) {
      System.out.println("\n\nError: expected " + input
          + " to be a directory.");
      printHelp(commandLineOptions);
    }
    for (File f : FileUtils.getAllFiles(inputFile)) {
      if (f.getName().endsWith(".json")) {
        files.add(f);
      }
    } 
    
    Map<String, Integer> tokenCounts = new HashMap<String, Integer>();
    Map<String, Integer> entityCounts = new HashMap<String, Integer>();
    int docs = 0;
    for (File f : files) {
      System.out.println("Processed " + ++docs + " docs.");
      String jsonStr = FileUtils.getFileContent(f);
      JSONObject json = (JSONObject) JSONValue.parse(jsonStr);
      String originalText = (String) json.get("originalText");
      for (Token token : TokenizerManager.tokenize(originalText)) {
        String word = token.getOriginal();
        if (!StopWord.isStopwordOrSymbol(word) && word.length() > 2) {
          Integer count = tokenCounts.get(word);
          if (count == null) {
            count = 0;
          }
          count++;
          tokenCounts.put(word, count);
        }   
      }
      JSONArray mentions = (JSONArray) json.get("mentions");
      for (Object o : mentions) {
        JSONObject m = (JSONObject) o;
        JSONObject bestEntity = (JSONObject) m.get("bestEntity");
        if (bestEntity != null) {
          String id = (String) bestEntity.get("kbIdentifier");
          Integer count = tokenCounts.get(id);
          if (count == null) {
            count = 0;
          }
          count++;
          entityCounts.put(id, count);
        }
      }
    }
    System.out.println("Most frequent non-stopword Tokens:\n-----\n\n");
    for (String top : CollectionUtils.getTopKeys(tokenCounts, 200)) {
      System.out.println(top + "\t" + tokenCounts.get(top));
    }
    System.out.println("\n\nMost frequent Entities:\n-----\n\n");
    for (String top : CollectionUtils.getTopKeys(entityCounts, 200)) {
      System.out.println(top + "\t" + entityCounts.get(top));
    }
  }

  @SuppressWarnings("static-access")
  private Options buildCommandLineOptions() throws ParseException {
    Options options = new Options();
    options.addOption(OptionBuilder
        .withLongOpt("input")
        .withDescription(
            "Input, assumed to be directory containing .json files. "
                + "Will process all directories recursively.").hasArg()
        .withArgName("FILE").isRequired().create("i"));
    options.addOption(OptionBuilder.withLongOpt("help").create('h'));
    return options;
  }

  private void printHelp(Options commandLineOptions) {
    String header = "\n\nGet Stats for AIDA JSON files.:\n\n";
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("AidaJsonStats", header, commandLineOptions,
        "", true);
    System.exit(0);
  }
}
