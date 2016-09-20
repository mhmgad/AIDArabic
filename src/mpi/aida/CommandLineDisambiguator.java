package mpi.aida;

import mpi.aida.access.DataAccess;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.JsonSettings.JSONTYPE;
import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.PreparationSettings.DOCUMENT_INPUT_FORMAT;
import mpi.aida.config.settings.disambiguation.*;
import mpi.aida.config.settings.preparation.ManualPreparationSettings;
import mpi.aida.config.settings.preparation.StanfordHybridPreparationSettings;
import mpi.aida.data.*;
import mpi.aida.preparator.Preparator;
import mpi.aida.util.Counter;
import mpi.aida.util.htmloutput.HtmlGenerator;
import mpi.aida.util.splitter.DelimBasedTextSplitter;
import mpi.aida.util.timing.RunningTimer;
import mpi.tools.javatools.util.FileUtils;
import org.apache.commons.cli.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Disambiguates a document from the command line.
 *
 */
public class CommandLineDisambiguator {

  private Options commandLineOptions;

  private Set<OutputFormat> outputFormats;

  enum OutputFormat {
    HTML,JSON,TSV
  }

  public static void main(String[] args) throws Exception {
    new CommandLineDisambiguator().run(args);
  }

  public void run (String args[]) throws Exception {
    commandLineOptions = buildCommandLineOptions();
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

    String disambiguationTechniqueSetting = "GRAPH";
    if (cmd.hasOption("t")) {
      disambiguationTechniqueSetting = cmd.getOptionValue("t");
    }

    // Default to HTML.
    outputFormats = new HashSet<>();
    if (cmd.hasOption("o")) {
      String outputFormatString = cmd.getOptionValue("o");
      String[] parts = outputFormatString.split(",");
      for (String p : parts) {
        if (p.equals("ALL")) {
          for (OutputFormat of : OutputFormat.values()) {
            if (of != OutputFormat.TSV) {
              outputFormats.add(of);
            }
          }
          break;
        }
        try {
          OutputFormat of = OutputFormat.valueOf(p);
          outputFormats.add(of);
        } catch (IllegalArgumentException e) {
          printHelp(commandLineOptions);
          System.out.println("\n\nIllegal output format: '" + p + "' does not exist.");
        }
      }
    } else {
      outputFormats.add(OutputFormat.HTML);
    }

    String input = cmd.getOptionValue("i");
    File inputFile = new File(input);
    List<File> files = new ArrayList<File>();
    if (cmd.hasOption("d")) {
      if (!inputFile.isDirectory()) {
        System.out.println("\n\nError: expected " + input + " to be a directory.");
        printHelp(commandLineOptions);
      } else {
        for (File f : FileUtils.getAllFiles(inputFile)) {
          // Ignore output files.
          boolean add = true;
          for (OutputFormat of : outputFormats) {
            if (f.getName().toLowerCase().endsWith("." + of.toString().toLowerCase())) {
              add = false;
            }
          }
          if (add) {
            files.add(f);
          }
        }
      }
    } else if (cmd.hasOption("s")) {
      String text = cmd.getOptionValue("i");
      int end = Math.min(text.length(), 8);
      File tmpFile = File.createTempFile(text.substring(0, end), ".txt", new File(System.getProperty("user.dir")));
      FileUtils.writeFileContent(tmpFile, text);
      tmpFile.deleteOnExit();
      files.add(tmpFile);
    } else {
      if (inputFile.isDirectory()) {
        printHelp(commandLineOptions);
        System.out.println("\n\nError: expected " + input + " to be a file.");
      }
      files.add(inputFile);
    }

    PreparationSettings prepSettings;
    if (cmd.hasOption("manual")) {
      prepSettings = new ManualPreparationSettings();
    } else {
      prepSettings = new StanfordHybridPreparationSettings();
    }

    String inputFormat = "PLAIN";
    if (cmd.hasOption("f")) {
      inputFormat = cmd.getOptionValue("f");
    }    
    String jsonFormat = cmd.getOptionValue("j", "EXTENDED");

    DOCUMENT_INPUT_FORMAT[] docInpFormats = DOCUMENT_INPUT_FORMAT.values();
    boolean valid = false;
    for(DOCUMENT_INPUT_FORMAT docInpFmt : docInpFormats) {
      if(docInpFmt.name().equals(inputFormat)) {
        valid = true;
        break;
      }
    }

    if(!valid) {
      StringBuilder msg = new StringBuilder("Invalid Input Format provided. Currently Supports: ");
      for(int i=0; i < docInpFormats.length; ++i) {
        msg.append("'" + docInpFormats[i].name() + "'");
        if(i != docInpFormats.length-1) {
          msg.append(", ");
        }
      }

      System.err
      .println(msg.toString());
      System.exit(2);
    }

    // set the document input format
    prepSettings.setDocumentInputFormat(DOCUMENT_INPUT_FORMAT.valueOf(inputFormat));
    
    if(cmd.hasOption("dfield")){
      String documentField = cmd.getOptionValue("dfield");
      prepSettings.setDocumentField(documentField);
    }
    
    if(cmd.hasOption("did")){
      String documentId = cmd.getOptionValue("did");
      prepSettings.setDocumentId(documentId);
    }
    
    if(cmd.hasOption("dtitle")){
      String documentTitle = cmd.getOptionValue("dtitle");
      prepSettings.setDocumentTitle(documentTitle);
    }
    
    if (cmd.hasOption('m')) {
      int minCount = Integer.parseInt(cmd.getOptionValue('m'));
      prepSettings.setMinMentionOccurrenceCount(minCount);
      System.out.println("Dropping mentions with less than " + minCount + " occurrences in the text.");
    }

    int threadCount = 1;
    if (cmd.hasOption("c")) {
      threadCount = Integer.parseInt(cmd.getOptionValue("c"));
    }

    int chunkThreadCount = 1;
    if (cmd.hasOption("x")) {
      chunkThreadCount = Integer.parseInt(cmd.getOptionValue("x"));
    }

    int resultCount = 10;
    if (cmd.hasOption("e")) {
      resultCount = Integer.parseInt(cmd.getOptionValue("e"));
    }
    
    Double threshold = 0.0;
    if (cmd.hasOption("r")) {
      threshold = Double.parseDouble(cmd.getOptionValue("r"));
    }
    
    String encoding = "UTF-8";
    if (cmd.hasOption("n")) {
      encoding = cmd.getOptionValue("n");
    }
    prepSettings.setEncoding(encoding);

    boolean isTimed = cmd.hasOption('z');
    boolean isVerbose = cmd.hasOption('v');
    boolean runDummyDoc = cmd.hasOption('g');
    boolean writeTimingInfo = false;

    String timingDir = "";
    if (cmd.hasOption('w')) {
      writeTimingInfo = true;
      timingDir = cmd.getOptionValue("w");
      File f = new File(timingDir);
      if (!f.exists() || !f.isDirectory()) {
        System.out.println("Timing directory doesnt exists or not a directory!");
        printHelp(commandLineOptions);
      }
    }

    boolean multiDoc = false;
    String docDelim = "";
    if(cmd.hasOption("multidoc")) {
      multiDoc = true;
      docDelim = cmd.getOptionValue("multidoc");
    }

    ExecutorService es = Executors.newFixedThreadPool(threadCount);
    Preparator p = new Preparator();

    if (runDummyDoc) {
      // To eliminate the cache loading time effect on overall running timer results.      
      System.out.println("Executing disambiguation on dummy document..");

      // execute dummy disambiguation
      runDummyDisambiguation("A dummy text to disambigute.",
          disambiguationTechniqueSetting, p, prepSettings, outputFormats, resultCount,
          false, false);

      RunningTimer.clear();
    }

    System.out.println("Processing " + files.size() + " documents with " +
        threadCount + " threads, ignoring existing .html and .json files.");

    for (File f : files) {
      Processor proc = new Processor(f.getAbsolutePath(),
          disambiguationTechniqueSetting, p, prepSettings, outputFormats, jsonFormat, resultCount,
          !inputFile.isDirectory(), isTimed);
      // pass the threshold
      proc.setThreshold(threshold);
      proc.setChunkThreadCount(chunkThreadCount);
      if(multiDoc) {
        proc.enableMultiDocsPerFile(docDelim);
      }
      es.execute(proc);
    }

    es.shutdown();
    es.awaitTermination(1, TimeUnit.DAYS);
    if (es.isTerminated()) {
      if (writeTimingInfo) {
        String content = RunningTimer.getDetailedOverview();
        FileUtils.writeFileContent(new File(timingDir + File.separator + "overall_timing_" + new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss'.txt'").format(new Date())), content);
        content = RunningTimer.getTrackedDocumentTime();
        FileUtils.writeFileContent(new File(timingDir + File.separator + "document_timing_" + new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss'.txt'").format(new Date())), content);
      }
      if (isVerbose) {
        System.out.println(Counter.getOverview());
      }
    }
  }

  private void runDummyDisambiguation(String content, String disambiguationTechniqueSetting, Preparator p, PreparationSettings prepSettings,
      Set<OutputFormat> outputFormat, int resultCount, boolean logResults, boolean isTimed) {
    PreparedInput input = null;
    String fName = "dummyText_" + System.currentTimeMillis() + ".txt";
    try {
      input = p.prepare(fName, content.toString(), prepSettings, new ExternalEntitiesContext());
      DisambiguationSettings disSettings = new ImportanceOnlyDisambiguationSettings();
      disSettings.setNumChunkThreads(1);        
      Disambiguator d = new Disambiguator(input, disSettings);
      // ignoring the results
      d.disambiguate();
    }catch(Exception e) {
      System.err.println("Error while processing '" + fName + "': " + 
          e.getLocalizedMessage());
      e.printStackTrace();
    }
  }

  @SuppressWarnings("static-access")
  private Options buildCommandLineOptions() throws ParseException {
    Options options = new Options();
    options
    .addOption(OptionBuilder
        .withLongOpt("input")
        .withDescription(
            "Input, assumed to be a UTF-8 encoded text file. "
                + "Set -d to treat  the parameter as directory.")
                .hasArg()
                .withArgName("FILE")
                .isRequired()
                .create("i"));
    options
    .addOption(OptionBuilder
        .withLongOpt("string")
        .withDescription(
            "Set to treat the -i input as a string. Will directly disambiguate.")
            .create("s"));
    options
    .addOption(OptionBuilder
        .withLongOpt("directory")
        .withDescription(
            "Set to treat the -i input as directory. Will recursively process"
                + "all files in the directory.")
                .create("d"));
    options
    .addOption(OptionBuilder
        .withLongOpt("technique")
        .withDescription(
            "Set the disambiguation-technique to be used: PRIOR, LOCAL, FAST-LOCAL, LOCAL-IDF, LM, GRAPH, GRAPH-IDF, GRAPH-KORE, or COLLECTION. Default is GRAPH.")
            .hasArg()
            .withArgName("TECHNIQUE")
            .create("t"));
    options
    .addOption(OptionBuilder
        .withLongOpt("outputformat")
        .withDescription(
            "Set the output-format to be used: HTML, JSON, TSV, ALL. Default is HTML.")
            .hasArg()
            .withArgName("FORMAT")
            .create("o"));
    options
    .addOption(OptionBuilder
        .withLongOpt("jsonformat")
        .withDescription(
            "Set the json-format to be used: " + 
                Arrays.stream(JSONTYPE.values())
                .map(JSONTYPE::name)
                .sorted()
                .collect(Collectors.joining(", ")) + ". " +
                 "Default is DEFAULT.")
            .hasArg()
            .withArgName("JSONFORMAT")
            .create("j"));
    options
    .addOption(OptionBuilder
        .withLongOpt("inputformat")
        .withDescription(
            "Set the input-format to be used: " +
                Arrays.stream(DOCUMENT_INPUT_FORMAT.values())
                .map(DOCUMENT_INPUT_FORMAT::name)
                .sorted()
                .collect(Collectors.joining(", ")) + ". " +
            "Default is PLAIN.")
            .hasArg()
            .withArgName("INPUTFORMAT")
            .create("f"));
    options
    .addOption(OptionBuilder
        .withLongOpt("documentfield")
        .withDescription(
            "Set the field in the input to be used. Use for XML or JSON input to specify where the text is stored.")
            .hasArg()
            .withArgName("DOCUMENTFIELD")
            .create("dfield"));
    options
    .addOption(OptionBuilder
        .withLongOpt("documentid")
        .withDescription(
            "Set the field for the document ID. Use for XML or JSON input to specify where the document ID is stored.")
            .hasArg()
            .withArgName("DOCUMENTID")
            .create("did"));
    options
    .addOption(OptionBuilder
        .withLongOpt("documenttitle")
        .withDescription(
            "Set the field for the document title. Use for XML or JSON input to specify where the document title is stored.")
            .hasArg()
            .withArgName("DOCUMENTTITLE")
            .create("dtitle"));
    options
    .addOption(OptionBuilder
        .withLongOpt("threadcount")
        .withDescription(
            "Set the number of documents to be processed in parallel.")
            .hasArg()
            .withArgName("COUNT")
            .create("c"));    
    options
    .addOption(OptionBuilder
        .withLongOpt("numChunkThread")
        .withDescription("Set the maximum number of chunk disambiguation threads to be created.")
        .hasArg()
        .withArgName("NUMCHUNKTHREAD")
        .create("x"));
    options
    .addOption(OptionBuilder
        .withLongOpt("minmentioncount")
        .withDescription(
            "Set the minimum occurrence count of a mention to be considered for disambiguation. Default is 1.")
            .hasArg()
            .withArgName("COUNT")
            .create("m"));
    options
    .addOption(OptionBuilder
        .withLongOpt("manual")
        .withDescription(
            "If set, does only manual mention recognition. Mentions should be put between [[ and ]]. Otherwise, does Hybrid with StanfordNER.")
            .create("manual"));
    options
    .addOption(OptionBuilder
        .withLongOpt("maximumresultspermention")
        .withDescription(
            "Set the number of entities returned per mention in the output. Default is 10.")
            .hasArg()
            .withArgName("COUNT")
            .create("e"));  
    options
    .addOption(OptionBuilder
        .withLongOpt("confidencethreshold")
        .withDescription(
            "Sets the confidence threshold below which to drop entities. Default is given by the technique.")
            .hasArg()
            .withArgName("THRESHOLD")
            .create("r"));
    options
    .addOption(OptionBuilder
        .withLongOpt("timing")
        .withDescription(
            "Print timing details.")
            .create("z"));
    options
    .addOption(OptionBuilder
        .withLongOpt("verbose")
        .withDescription(
            "Enable verbose output.")
            .create("v"));
    options
    .addOption(OptionBuilder
        .withLongOpt("ignore-cacheloadtime")
        .withDescription(
            "Run disambiguation on dummy document to ignore the cache loading time. Disabled by default")
            .create("g"));
    options
    .addOption(OptionBuilder
        .withLongOpt("writetime")
        .withDescription(
            "To write RunningTimer overview details to file.")
            .hasArg()
            .withArgName("DIRECTORYNAME")
            .create("w"));
    options
    .addOption(OptionBuilder
        .withLongOpt("encoding")
        .withDescription(
            "String encoding of the input file(s).")
            .hasArg()
            .withArgName("ENCODING")
            .create("n"));
    options
    .addOption(OptionBuilder
        .withLongOpt("multidoc")
        .withDescription("If set, will try to extract individual documents from given file for processing.")
        .hasArg()
        .withArgName("DOC-DELIM")        
        .create("multidoc"));
    options.addOption(OptionBuilder.withLongOpt("help").create('h'));
    return options;
  }

  private void printHelp(Options commandLineOptions) {
    String header = "\n\nRun AIDA:\n\n";
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("CommandLineDisambiguator", header, 
        commandLineOptions, "", true);
    System.exit(0);
  }

  class Processor implements Runnable {
    private final String inputFile; 
    private final String disambiguationTechniqueSetting; 
    private final Preparator p; 
    private final PreparationSettings prepSettings;
    private final Set<OutputFormat> outputFormats;
    private final String jsonFormat;
    private final int resultCount;
    private int numChunkThread;
    private final boolean logResults;
    private double threshold;
    private final boolean isTimed;
    private boolean multiDoc;
    private String docDelimiter;

    public Processor(String inputFile, String disambiguationTechniqueSetting,
        Preparator p, PreparationSettings prepSettings, Set<OutputFormat> outputFormats,
        String jsonFormat, int resultCount, boolean logResults, boolean isTimed) {
      super();
      this.inputFile = inputFile;
      this.disambiguationTechniqueSetting = disambiguationTechniqueSetting;
      this.p = p;
      this.prepSettings = prepSettings;
      this.outputFormats = outputFormats;
      this.jsonFormat = jsonFormat;
      this.resultCount = resultCount;
      this.logResults = logResults;
      this.isTimed = isTimed;
    }    

    public void enableMultiDocsPerFile(String docDelim) {
      multiDoc = true;
      docDelimiter = docDelim;
    }

    public void setChunkThreadCount(int numChunkThread) {
      this.numChunkThread = numChunkThread;
    }

    public void setThreshold(double threshold) {
      this.threshold = threshold;
    }

    @Override
    public void run() {
      try {

        /*
         * if timing is enabled, then use real time tracker for detailed module level info
         * else use No op tracker for getting only the total time
         */
        if(isTimed) {
          System.out.println("Timing info requested. Enabling Real Time Tracker.");
          RunningTimer.enableRealTimeTracker();
        }

        for (OutputFormat of : outputFormats) {
          File resultFile = new File(inputFile + "." + of.toString().toLowerCase());
          if (resultFile.exists()) {
            System.out.println(of + " output for " + inputFile + " exists, skipping.");
            return;
          }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(inputFile), prepSettings.getEncoding()));
        StringBuilder content = new StringBuilder();

        for (String line = reader.readLine(); line != null; line = reader
            .readLine()) {
          content.append(line).append('\n');
        }
        reader.close();

        if(content.length() == 0) {
          System.out.println("Empty Input file : " + inputFile + ". skipping.");
          return;
        }

        List<String> inputToProcess = new ArrayList<>();
        if(!multiDoc) {
          inputToProcess.add(content.toString());
        } else {
          inputToProcess.addAll(DelimBasedTextSplitter.split(content.toString(), docDelimiter));
          System.out.println("Multidoc enabled - Number of documents extracted : " + inputToProcess.size());
        }

        PreparedInput input = null;
        Map<String, DisambiguationResults> disambiguationResults = new HashMap<>();
        Map<String, PreparedInput> inputs = new HashMap<>();
        // To keep track of failed documents in multidoc mode
        List<String> lstFailedDocs = new ArrayList<>();

        DisambiguationSettings disSettings = null;
        if (disambiguationTechniqueSetting.equals("PRIOR")) {
          disSettings = new PriorOnlyDisambiguationSettings();
        } else if (disambiguationTechniqueSetting.equals("LOCAL")) {
          disSettings = new LocalKeyphraseBasedDisambiguationWithNullSettings();
        } else if (disambiguationTechniqueSetting.equals("FAST-LOCAL")) {
          disSettings = new FastLocalKeyphraseBasedDisambiguationWithNullSettings();
        } else if (disambiguationTechniqueSetting.equals("COLLECTION")) {
          disSettings = new CocktailPartyWithHeuristicsDisambiguationWithNullSettings();
        } else if (disambiguationTechniqueSetting.equals("LOCAL-IDF")) {
          disSettings = new LocalKeyphraseIDFBasedDisambiguationWithNullSettings();
        } else if (disambiguationTechniqueSetting.equals("LM")) {
          disSettings = new CocktailPartyLangaugeModelDefaultDisambiguationSettings();
        } else if (disambiguationTechniqueSetting.equals("GRAPH")) {
          disSettings = new CocktailPartyDisambiguationWithNullSettings();
        } else if (disambiguationTechniqueSetting.equals("GRAPH-IDF")) {
          disSettings = new CocktailPartyKOREIDFDisambiguationWithNullSettings();
        } else if (disambiguationTechniqueSetting.equals("GRAPH-KORE")) {
          disSettings = new CocktailPartyKOREDisambiguationWithNullSettings();
        } else {
          System.out.println("No technique (-t) specified, using LM by default.");
          disSettings = new CocktailPartyLangaugeModelDefaultDisambiguationSettings();
        }

        if (threshold > 0.0) {
          disSettings.setNullMappingThreshold(threshold);
        }

        disSettings.setNumChunkThreads(numChunkThread);

        int multiPart = 0;
        for (String text : inputToProcess) {
          try {
            String docId = inputFile;
            if (multiDoc) {
              docId += "_" + multiPart;
              ++multiPart;
            }

            input = p.prepare(docId, text, prepSettings, new ExternalEntitiesContext());
            inputs.put(input.getDocId(), input);

            Disambiguator d = new Disambiguator(input, disSettings);
            DisambiguationResults results = d.disambiguate();
            disambiguationResults.put(input.getDocId(), results);

          } catch (Exception e) {
            // Catching exception for single doc so that rest continues.
            System.out.println("Exception while processing '" + input.getDocId() + "': " + e.getLocalizedMessage());
            lstFailedDocs.add(input.getDocId());
            System.out.println("Error while processing : " + input.getDocId() + ". Adding to errored list (" + lstFailedDocs.size() + ")");
          }
        }

        if (logResults) {
          System.out.println("Disambiguation for '" + inputFile + "' done.");
        }

        // retrieve JSON representation of Disambiguated results
        JSONTYPE jsonType = JSONTYPE.valueOf(jsonFormat);

        JSONArray jsonResults = generateJson(jsonType, disambiguationResults, inputs);

        if (outputFormats.contains(OutputFormat.JSON)) {
          File resultFile = new File(inputFile + ".json");

          String jsonStr;
          if (multiDoc) {
            jsonStr = jsonResults.toJSONString();
          } else {
            jsonStr = ((JSONObject) jsonResults.get(0)).toJSONString();
          }

          FileUtils.writeFileContent(resultFile, jsonStr);
          if (logResults) {
            System.out.println("Result written to '" + resultFile + "' in JSON (" + jsonType + ").");
          }
        }

        if (outputFormats.contains(OutputFormat.HTML)) {
          HtmlGenerator gen = new HtmlGenerator();

          if (!multiDoc) {
            File resultFile = new File(inputFile + ".html");
            // generate HTML from Disambiguated Results
            JSONObject json = ((JSONObject) jsonResults.get(0));
            FileUtils.writeFileContent(resultFile, gen.constructFromJson(inputFile, json));
            if (logResults) {
              System.out.println("Result written to '" + resultFile + "' in HTML.");
            }
          } else {
            for (int i = 0; i < jsonResults.size(); i++) {
              File resultFile = new File(inputFile + "_" + i +".html");
              JSONObject json = ((JSONObject) jsonResults.get(i));
              FileUtils.writeFileContent(resultFile, gen.constructFromJson(inputFile, json));
              if (logResults) {
                System.out.println("Result written to '" + resultFile + "' in HTML.");
              }
            }
          }
        }

        if (outputFormats.contains(OutputFormat.TSV)) {
          for (Entry<String, PreparedInput> e : inputs.entrySet()) {
            PreparedInput pInp = e.getValue();
            File resultFile = new File(inputFile + "_" + pInp.getDocId() + ".tsv");
            pInp.writeTo(resultFile);
            if (logResults) {
              System.out.println("Result written to '" + resultFile + "' in TSV.");
            }
          }
        }

        if (logResults) {
          for (Entry<String, DisambiguationResults> e : disambiguationResults.entrySet()) {
            DisambiguationResults results = e.getValue();
            System.out.println("Mentions and Entities found:");
            System.out.println("\tMention\tEntity_id\tEntity\tEntity Name\tURL");

            Set<KBIdentifiedEntity> entities = new HashSet<KBIdentifiedEntity>();
            for (ResultMention rm : results.getResultMentions()) {
              entities.add(results.getBestEntity(rm).getKbEntity());
            }
            Map<KBIdentifiedEntity, EntityMetaData> entitiesMetaData = DataAccess.getEntitiesMetaData(entities);

            for (ResultMention rm : results.getResultMentions()) {
              KBIdentifiedEntity entity = results.getBestEntity(rm).getKbEntity();
              EntityMetaData entityMetaData = entitiesMetaData.get(entity);

              if (Entities.isOokbEntity(entity)) {
                System.out.println("\t" + rm + "\t NO MATCHING ENTITY");
              } else {
                if (entityMetaData != null) {
                  System.out.println("\t" + rm + "\t" + entityMetaData.getId() + "\t" + entity + "\t" + entityMetaData.getHumanReadableRepresentation()
                      + "\t" + entityMetaData.getUrl());
                } else {
                  System.out.println("\t" + rm + "\t" + "" + "\t" + entity + "\t" + "" + "\t" + "");
                }
              }
            }
          }
        }

        // Prints Total time for default No-op tracker and detailed info for real time tracker.
        if (isTimed) {
          System.out.println(RunningTimer.getDetailedOverview());
        }
        
        // For debugging Multidoc: Writing out documents that failed.
        if (lstFailedDocs.size() > 0) {
          StringBuilder sb = new StringBuilder();
          for(int i=0;i<lstFailedDocs.size();++i) {
            sb.append(lstFailedDocs.get(i)).append("\n");
          }
          File resultFile = new File(inputFile + "_errored.txt");
          FileUtils.writeFileContent(resultFile, sb.toString());
          System.out.println("Errored Files list written to " + resultFile);
        }
      } catch (Exception e) {
        System.err.println("Error while processing '" + inputFile + "': " + 
            e.getLocalizedMessage());
        e.printStackTrace();
      }
    }

    @SuppressWarnings("unchecked")
    private JSONArray generateJson(JSONTYPE jsonType,
        Map<String, DisambiguationResults> disambiguationResults,
        Map<String, PreparedInput> inputs) {
      
      String jsonStr;
      ResultProcessor rp;
      JSONArray jsonArray = new JSONArray();
      for(String tmpDocid : inputs.keySet()) {
        DisambiguationResults tmpResult = disambiguationResults.get(tmpDocid);
        PreparedInput tmpPInp = inputs.get(tmpDocid);
        
        if(tmpResult != null && tmpPInp != null) {
          rp = new ResultProcessor(tmpResult, inputFile, tmpPInp, resultCount);
          jsonArray.add(rp.process(jsonType));
        }
      }

      return jsonArray;
    }
  }
}