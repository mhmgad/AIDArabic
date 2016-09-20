# AIDArabic - Accurate Online Disambiguation of Entities for Arabic Text

AIDArabic is part of [AIDA][AIDA]. 


The following guide follows the Original [AIDA github][AIDAGit] .
  
**Important** AIDArabic uses [EDRAK][EDRAKData] as a data resource and this guid is updated to use it.

**Note 1:** AIDArabic only does NED (i.e. works in the manual mode). The NER is not yet integrated.

**Note 2:** It is recommended to pre-process the input using [Stanford Arabic Segmenter][ArabSeg] to get better results. It will mainly remove the connected propositions ..etc. For more details are presented [here][AIDArabicThesis].

AIDArabic is described in the following literature [AIDArabic][AIDArabic], [AIDArabic++][AIDAplus].


## AIDA Overview

[AIDA][AIDA] is the named entity disambiguation system created by the Databases and Information Systems Department at the [Max Planck Institute for Informatics in Saarbücken, Germany][MPID5]. It identifies mentions of named entities (persons, organizations, locations, songs, products, ...) in English language text and links them to a unique identifier. Most names are ambiguous, especially family names, and AIDA resolves this ambiguity. See the EMNLP 2011 publication [EMNLP2011] for a detailed description of how it works and the VLDB 2011 publication [VLDB2011] for a description of our Web demo. Read on for a more in-depth introduction and hands-on examples of how to use AIDA.

If you want to be notified about AIDA news or new releases, subscribe to our announcement mailing list by sending a mail to:

```
aida-news-subscribe@lists.mpi-inf.mpg.de
```

You can also ask questions or discuss AIDA on our aida-users mailing list:
 
 ```
 aida-users-subscribe@lists.mpi-inf.mpg.de or https://lists.mpi-inf.mpg.de/listinfo/aida-users
 ```
 


AIDA is a framework and online tool for entity detection and disambiguation. Given a natural-language text, it maps mentions of ambiguous names onto canonical entities (e.g., individual people or places) registered in the Wikipedia-derived [YAGO2][YAGO] [YAGO2] knowledge base. 

Take the example sentence below:

```
When Page played Kashmir at Knebworth, his Les Paul was uniquely tuned.
```

Aida will first spot all the names: "Page", "Kashmir", "Knebworth", and "Les Paul".

These ambiguous names are resolved by identifying the entity each name means. In the example, "Page" is Jimmy Page of Led Zeppelin fame, "Kashmir" means the song, not the region bordering India, China, and Pakistan. "Knebworth" refers to the festival, not the city, and finally "Les Paul" refers to the famous guitar, not its designer.

The output will be YAGO2 identifiers and Wikipedia URLs describing all these entities:

* "Page": http://en.wikipedia.org/wiki/Jimmy_Page
* "Kashmir": http://en.wikipedia.org/wiki/Kashmir_(song)
* "Knebworth": http://en.wikipedia.org/wiki/Knebworth_Festival_1979
* "Les Paul": http://en.wikipedia.org/wiki/Gibson_Les_Paul 

This knowledge is useful for multiple tasks, for example:

* Build an entity index. This allows one kind of semantic search, retrieve all documents where a given entity was mentioned.
* Extract knowledge about the entities, for example relations between entities mention in the text.

Note that AIDA does not annotate common words (like song, musician, idea, ... ). Also, AIDA does not identify mentions that have no entity in the repository. Once a name is in the dictionary containing all candidates for surface strings, AIDA will map to the best possible candidate, even if the correct one is not in the entity repository

## Requirements

 * Java 8
 * A [Postgres][Postgres] 9.2 database to run. Might work with previous version but is untested.
 * The machine AIDA runs on should have a reasonable amount of main memory. If you are using graph coherence (see the Section *Configuring AIDA*), the amount of memory grows quadratically with the number of entities and thus the length of the document. Anything above 10,000 candidates will be too much for a regular desktop machine (at the time of writing) to handle and should run on a machine with more than 20GB of main memory. AIDA does the most intensive computations in parallel and thus benefits from multi-core machine.

## Setting up the Entity Repository

AIDA was developed to disambiguate to the [YAGO2][YAGO] knowledge base, returning the YAGO2 identifier for disambiguated entities, which can in turn be transformed directly to Wikipedia URLs. However, you can use AIDA for any entity repository, given that you have keyphrases and weights for all entities. The more common case is to use AIDA with YAGO2. If you want to set it up with your own repository, see the Advanced Configuration section.

To use AIDA with YAGO2, download the repository we provide on our [AIDA website][AIDA] as a Postgres dump and import it into your database server. This will take some time, maybe even a day depending on the speed of the machine Postgres is running on. Once the import is done, you can start using AIDA immediately by adjusting the `settings/database_aida.properties` to point to the database. AIDA will then use nearly 3 million named entities harvested from Wikipedia for disambiguation.

Get the Entity Repository (22 GB):

    curl -O http://resources.mpi-inf.mpg.de/yago-naga/aida/download/entity-repository/edrak_en20150112_ar20141218.sql.bz2
    
Import it into a postgres database:

    bzcat AIDA_entity_repository_2010-08-17v10.sql.bz2 | psql <DATABASE>
    
where <DATABASE> is a database on a PostgreSQL server.

A database dump on a more recent version of Wikipedia is also available: http://resources.mpi-inf.mpg.de/yago-naga/aida/download/entity-repository/AIDA_entity_repository_2014-01-02v10.sql.bz2

## Setting up AIDA

To build aida, run `mvn package` (see [Maven](http://maven.apache.org)) in the directory of the cloned repository. This will create an aida-VERSION.jar including all dependencies in the `target` subdirectory.

The main configuration is done in the files in the `settings/` directory. The following files can be adjusted:

* `aida.properties`: take the `sample_settings/aida.properties` and adjust it accordingly. The default values are reasonable, so if you don't want to change anything, the file is not needed at all.
* `database_aida.properties`: take the `sample_settings/database_aida.properties`, put it here and adjust it accordingly. The settings should point to the Postgres database server that holds the entity repository - how to set this up is explained below.

After changing these settings, run `mvn package` again to update the jar with the current settings.

## Hands-On API Example

If you want to use AIDA in a maven project, add mpi.aida:aida-3.X as dependency. Otherwise, build the jar using `mvn package` and add `target/aida-VERSION.jar` to your project's classpath.  

The main classes in AIDA are `mpi.aida.Preparator` for preparing an input document and `mpi.aida.Disambiguator` for running the disambiguation on the prepared input.

```
// Define the input.
String inputText = "When [[Page]] played Kashmir at Knebworth, his Les Paul was uniquely tuned.";

// Prepare the input for disambiguation. The Stanford NER will be run
// to identify names. Strings marked with [[ ]] will also be treated as names.
PreparationSettings prepSettings = new StanfordHybridPreparationSettings();
Preparator p = new Preparator();
PreparedInput input = p.prepare(inputText, prepSettings);

// Disambiguate the input with the graph coherence algorithm.
DisambiguationSettings disSettings = new CocktailPartyDisambiguationSettings();    
Disambiguator d = new Disambiguator(input, disSettings);
DisambiguationResults results = d.disambiguate();

// Print the disambiguation results.
for (ResultMention rm : results.getResultMentions()) {
  ResultEntity re = results.getBestEntity(rm);
  System.out.println(rm.getMention() + " -> " + re);
}
```

The `ResultEntity` contains the AIDA ID via the `getEntity()` method. If you want to get the entity metadata (e.g. the Wikipedia URL or the types),
you can do the following:


```
Set<KBIdentifiedEntity> entities = new HashSet<KBIdentifiedEntity>();
for (ResultMention rm : results.getResultMentions()) {
  entities.add(results.getBestEntity(rm).getKbEntity());
}

Map<KBIdentifiedEntity, EntityMetaData> entitiesMetaData = 
  DataAccess.getEntitiesMetaData(entities);
 
for (ResultMention rm : results.getResultMentions()) {
  KBIdentifiedEntity entity = results.getBestEntity(rm).getKbEntity();
  EntityMetaData entityMetaData = entitiesMetaData.get(entity);

  if (Entities.isOokbEntity(entity)) {
    System.out.println("\t" + rm + "\t NO MATCHING ENTITY");
  } else {
    System.out.println("\t" + rm + "\t" + entityMetaData.getId() + "\t"
      + entity + "\t" + entityMetaData.getHumanReadableRepresentation()
      + "\t" + entityMetaData.getUrl());
  }
}  
```

See the `mpi.aida.config.settings.disambiguation` package for all possible predefined configurations, passed to the `Disambiguator`:

* `PriorOnlyDisambiguationSettings`: Annotate each mention with the most prominent entity.
* `LocalDisambiguationSettings`: Use the entity prominence and the keyphrase-context similarity to disambiguate.
* `FastLocalDisambiguationSettings`: Same as above but sacrificing a bit of accuracy for roughly 5 times quicker disambiguation by dropping low weight keyphrases.
* `CocktailPartyDisambiguationSettings`: Use a graph algorithm on the entity coherence graph ([MilneWitten] link coherence) to disambiguate. 
* `FastCocktailPartyDisambiguationSettings`: Same as above but sacrificing a bit of accuracy for roughly 5 times quicker disambiguation by dropping low weight keyphrases
* `CocktailPartyKOREDisambiguationSettings`: Use a graph algorithm on the entity coherence graph ([KORE] link coherence) to disambiguate. 

## Hands-On Command Line Call Example

1. Build AIDA:

    `mvn package`
    
1. Run the CommandLineDisambiguator:

    `java -Xmx12G -cp target/aida-3.X.X-jar-with-dependencies.jar mpi.aida.CommandLineDisambiguator -t GRAPH -s -i "Einstein was born in Ulm"`

To process a file, remove `-s` and pass `<INPUT-FILE>` as parameter to `-i` instead, which is then treated as path to the text file to be annotated with entities. The format for `<INPUT-FILE>` should be plain text with UTF-8 encoding.

Instead of `GRAPH`, you can put one of the following, corresponding to the settings described above:

* `PRIOR`: PriorOnlyDisambiguationSettings
* `LOCAL`: LocalDisambiguationSettings
* `GRAPH`: CocktailPartyDisambiguationSettings
* `GRAPH-KORE`: CocktailPartyKOREDisambiguationSettings

The output will be an HTML file with annotated mentions, linking to the corresponding Wikipedia page. It also contains the IDs of the entities in the entity repository used.

## Hands-On AIDA Web Service Example

Start the AIDA web service with

```
export MAVEN_OPTS="-Xmx12G"
mvn jetty:run
```

This will expose the RESTful API, which can be accessed at the URL:

`http://localhost:8080/aida/service/disambiguate`

The most basic example calls this convenience wrapper with just one parameter, 'text', which contains the input text to disambiguate. 
In general, the input is expected as HTTP POST request containing application/x-www-form-urlencoded parameters specifying the settings and input text.
However, JSON as input is also supported.

The output is a JSON object containing the disambiguation results.

Please look at `mpi.aida.service.web.RequestProcessor` for details about the parameters it expects. The most simple call is

```
curl --data text="Einstein was born in Ulm." http://localhost:8080/aida/service/disambiguate
```

which returns a JSON string containing the following fields (among others)

```
{
  "formatVersion": "2.3",
  "annotatedText": "[[YAGO:Albert_Einstein|Einstein]] was born in [[YAGO:Ulm|Ulm]].",
  "originalText": "Einstein was born in Ulm.",
  "overallTime": "29460",
  "allEntities": [
    "YAGO:Ulm",
    "YAGO:Albert_Einstein"
  ],
  "entityMetadata": {
    "YAGO:Ulm": {
      "knowledgebase": "YAGO",
      "depictionurl": "http:\/\/upload.wikimedia.org\/wikipedia\/commons\/4\/46\/Ulm_Donauschwabenufer1.jpg",
      "depictionthumbnailurl": "http:\/\/upload.wikimedia.org\/wikipedia\/commons\/thumbUlm_Donauschwabenufer1.jpg\/200px-Ulm_Donauschwabenufer1.jpg",
      "importance": 0.0015091850539984,
      "entityId": "Ulm",
      "type": [
        "YAGO_wordnet_district_108552138",
        "YAGO_yagoPermanentlyLocatedEntity",
        "YAGO_yagoLegalActorGeo",
        "YAGO_wordnet_urban_area_108675967",
        "YAGO_wikicategory_Populated_places_on_the_Danube",
		...
	  ],
      "readableRepr": "Ulm",
      "url": "http:\/\/en.wikipedia.org\/wiki\/Ulm"
    },
    "YAGO:Albert_Einstein": {
      "knowledgebase": "YAGO",
      "depictionurl": "http:\/\/upload.wikimedia.org\/wikipedia\/commons\/3\/3e\/Einstein_1921_by_F_Schmutzer_-_restoration.jpg",
      "depictionthumbnailurl": "http:\/\/upload.wikimedia.org\/wikipedia\/commons\/thumbEinstein_1921_by_F_Schmutzer_-_restoration.jpg\/200px-Einstein_1921_by_F_Schmutzer_-_restoration.jpg",
      "importance": 0.00055403637364391,
      "entityId": "Albert_Einstein",
      "type": [
        "YAGO_wordnet_absentee_109757653",
        "YAGO_wordnet_laureate_110249011",
        "YAGO_wikicategory_People_from_Ulm",
        "YAGO_wordnet_pantheist_110396594",
		... 
      ],
      "readableRepr": "Albert Einstein",
      "url": "http:\/\/en.wikipedia.org\/wiki\/Albert%20Einstein"
    }
  },
  "mentions": [
    {
      "allEntities": [
        {
          "kbIdentifier": "YAGO:Albert_Einstein",
          "disambiguationScore": "1"
        }
      ],
      "offset": 0,
      "name": "Einstein",
      "length": 8,
      "bestEntity": {
        "kbIdentifier": "YAGO:Albert_Einstein",
        "disambiguationScore": "1"
      }
    },
    {
      "allEntities": [
        {
          "kbIdentifier": "YAGO:Ulm",
          "disambiguationScore": "0.50725"
        }
      ],
      "offset": 21,
      "name": "Ulm",
      "length": 3,
      "bestEntity": {
        "kbIdentifier": "YAGO:Ulm",
        "disambiguationScore": "0.50725"
      }
    }
  ],
  "allTypes": [
    "YAGO_wordnet_scholar_110557854",
    "YAGO_wikicategory_German_physicists",
    "YAGO_wikicategory_ETH_Zurich_alumni",
    "YAGO_wordnet_cosmologist_109819667",
    "YAGO_wikicategory_Swiss_agnostics",
    "YAGO_wordnet_agnostic_109779124",
    "YAGO_wikicategory_German_Nobel_laureates",
	...
  ]
}
```

If you only need the entities, pass `--data jsonType="compact"` as additional parameter.

## Input Format

The input of AIDA is an English language text (as Java String) or file in UTF-8 encoding. By default, named entities are recognized by the Stanford NER component of the [CoreNLP][CoreNLP] tool suite. In addition, mentions can be marked up by square brackets, as in this example "Page":

    When [[Page]] played Kashmir at Knebworth, his Les Paul was uniquely tuned.
    
The mention recognition can be configured by using different `PreparationSettings` in the `mpi.aida.config.settings.preparation` package:

* `StanfordHybridPreparationSettings`: Use Stanford CoreNLP NER and allow manual markup using [[...]]
* `ManualPreparationSettings`: Use Stanford CoreNLP only for tokenization and sentence splitting, mentions need to be marked up by [[...]].

The `PreparationSettings` are passed to the `Preparator`, see the Hands-On API Example.

## Comparing Your NED Algorithm against AIDA

### Configuring AIDA

To get the best results for AIDA, please use the `mpi.aida.config.settings.disambiguation.CocktailPartyDisambiguationSettings` for the Disambiguator, as described in _Pre-configured DisambiguationSettings_ . You can also compare your results on the datasets where we already ran AIDA, see below.

### Available Datasets

There are two main datasets we created to do research on AIDA. Both are available on the [AIDA website][AIDA].

* CONLL-YAGO: A collection of 1393 Newswire documents from the Reuters RCV-1 collection. All names are annotated with their respective YAGO2 entities. We make the annotations available for research purposes, however the Reuters RCV-1 collection must be purchased to use the dataset.
* KORE50: A collection of 50 handcrafted sentences from 5 different domains.

We provide readers for these two datasets in the `mpi.experiment.reader` package which will produce `PreparedInput` objects for each document in the collection. See the respective `CoNLLReader` and `KORE50Reader` classes for the location of the data.

## Advanced Configuration

### Configuring the DisambiguationSettings

The `mpi.aida.config.settings.DisambiguationSettings` contain all the configurations for the weight computation of the disambiguation graph. The best way to configure the DisambiguationSettings for constructing the disambiguation graph is to use one of the predefined settings objects in the `mpi.aida.config.settings.disambiguation` package, see below.

### Pre-configured DisambiguationSettings

These pre-configured `DisambiguatorSettings` objects can be passed to the `Disambiguator`:

* `CocktailPartyWithHeuristicsDisambiguationWithNullSettings`: The default configuration that should be used. Thresholds for discovering nil entities.

Other possibilities (mainly to experiment with) are the following. All of these settings assume that all mentions in the input should be linked:

* `PriorOnlyDisambiguationSettings`: Annotate each mention with the most prominent entity.
* `LocalDisambiguationSettings`: Use the entity prominence and the keyphrase-context similarity to disambiguate.
* `CocktailPartyDisambiguationSettings`: Use a graph algorithm on the entity coherence graph ([MilneWitten] link coherence) to disambiguate.
* `CocktailPartyKOREDisambiguationSettings`: Use a graph algorithm on the entity coherence graph ([KORE] link coherence) to disambiguate. 

#### DisambiguationSettings Parameters

The principle parameters are (corresponding to all the instance variables of the `DisambiguationSettings` object):

* `alpha`: Balances the mention-entity edge weights (alpha) and the entity-entity edge weights (1-alpha).
* `disambiguationTechnique`: Technique to solve the disambiguation graph with. Most commonly this is LOCAL for mention-entity similarity edges only and GRAPH to include the entity coherence.
* `disambiguationAlgorithm`: If TECHNIQUE.GRAPH is chosen above, this specifies the algorithm to solve the disambiguation graph. Can be COCKTAIL_PARTY for the full disambiguation graph and COCKTAIL_PARTY_SIZE_CONSTRAINED for a heuristically pruned graph.
* `useExhaustiveSearch`: Set to true to use exhaustive search in the final solving stage of ALGORITHM.COCKTAIL_PARTY. Set to false to do a hill-climbing search from a random starting point.
* `useNormalizedObjective`: Set to true to normalize the minimum weighted degree in the ALGORITHM.COCKTAIL_PARTY by the number of graph nodes. This prefers smaller solutions.
* `entitiesPerMentionConstraint`: Number of candidates to keep for for ALGORITHM.COCKTAIL_PARTY_SIZE_CONSTRAINED.
* `useCoherenceRobustnessTest`: Set to true to enable the coherence robustness test, fixing mentions with highly similar prior and similarity distribution to the most promising candidate before running the graph algorithm.
* `cohRobustnessThreshold`: Threshold of the robustness test, below which the the L1-norm between prior and sim results in the fixing of the entity candidate.
* `similaritySettings`: Settings to compute the edge-weights of the disambiguation graph. Details see below.
* `coherenceSimilaritySetting`: Settings to compute the initial mention-entity edge weights when using coherence robustness.

The edge weights of the disambiguation graph are configured in the `similaritySettings` object of `DisambiguationSettings`. They have a major impact on the outcome of the disambiguation.

#### SimilaritySettings Parameters

* `mentionEntitySimilarities`: a list of mention-entity similarity triples. The first one is the SimilarityMeasure, the second the EntitiesContext, the third the weight of this mentionEntitySimilarity. Note that they need to add up to 1.0, including the number for the priorWeight option. If loading from a file, the triples are separated by ":". The mentionEntitySimilarities option also allows to enable or disable the first or second half of the mention-entity similarities based on the priorThreshold option. If this is present, the first half of the list is used when the prior is disable, the second one when it is enabled. Note that still the whole list weights need to sum up to 1 with the prior, the EnsembleMentionEntitySimilarity class will take care of appropriate re-scaling.
* `priorWeight`: The weight of the prior probability. Needs to sum up to 1.0 with all weights in mentionEntitySimilarities.
* `priorThreshold`: If set, the first half of mentionEntitySimilarities will be used for the mention-entity similarity when the best prior for an entity candidate is below the given threshold, otherwise the second half of the list together with the prior is used.
* `minimumEntityKeyphraseWeight`: The minimum weight of a keyphrase to be considered for disambiguation. Use this to trade of quality and running time. A value of 0.002 made the disambiguation 5 times faster with little loss in accuracy (in combination with `maxEntityKeyphraseCount`, see below).
* `maxEntityKeyphraseCount`: The maximum number of keyphrases per entity to be considered for disambiguation. Use this to trade of quality and running time. A value of 1000 made the disambiguation 5 times faster with little loss in accuracy (in combination with `minimumEntityKeyphraseWeight`, see above).
* `entityEntitySimilarity`: The name and the weight of the entity-entity similarity to use, as pairs of name and weight. If loading from a file, the pairs are ":"-separated.

Take our default configuration as example (in File syntax):

```
mentionEntitySimilarities = UnnormalizedKeyphrasesBasedMISimilarity:KeyphrasesContext:2.23198783427544E-6 UnnormalizedKeyphrasesBasedIDFSimilarity:KeyphrasesContext:2.6026462624132183E-4 UnnormalizedKeyphrasesBasedMISimilarity:KeyphrasesContext:0.0817134645946377 UnnormalizedKeyphrasesBasedIDFSimilarity:KeyphrasesContext:0.3220317242447891
priorWeight = 0.5959923145464976
priorThreshold = 0.9
entityEntitySimilarity = MilneWittenEntityEntitySimilarity:1.0
```

It is possible to create a SimilaritySettings object programmatically, however we recommend using the preconfigured settings in the `mpi.aida.config.settings.disambiguation` package.

### Adjusting the StopWords

If you want to add your own stopwords, you can add them to `settings/tokens/stopwords6.txt`.

### Using AIDA with your own Entity Repository

You can deploy AIDA with any set of named entities, given that you have descriptive keyphrases and weights for them. The database layout has to conform to the one described here. For a good example instance of all the data please download the YAGO2-based AIDA entity repository from our website.

#### Database Tables
    
The mandatory database tables are:

* dictionary
* entity_ids
* entity_keyphrases
* keyword_counts
* word_ids
* word_expansion

Each one is described in detail below, starting with the table name plus column names and SQL types.
    
    dictionary (
      mention text, entity integer, source text, prior double precision
    )
    
The _dictionary_ is used for looking up _entity_ candidates for a given surface form of a _mention_. Each mention-entity pair can have an associated prior probability. Mentions with the length of 4 characters or more are case-conflated to all-upper case.
    
    entity_ids (
      entity text, knowledgebase text, id integer
    )
    
This table is used for mapping the integer ids to the original _entity_ in the _knowledgebase_.

    entity_metadata (
      entity integer, humanreadablererpresentation text, url text, knowledgebase text, depictionurl text, description text
    )
    
Contains metadata for each entity, most importantly the human readable representation (used to display the entity) and the url to link it.

    keyword_counts (
      keyword integer, count integer
    )
    
The counts should reflect the number of times the given keyword occurs in the collection and is used to compute the IDF weight for all keywords. This means high counts will result in low weights.
    
    word_ids (
      word text, id integer
    )
    
All keyphrase and keyword ids must be present here. The input text will be transformed using the table and then matched against all entity keyphrases.
    
    word_expansion (
      word integer, expansion integer
    )
    
AIDA tries to match ALL_CAPS variants of mixed-case keywords. Put the ids of the UPPER_CASED word it in this table.

    entity_keyphrases (
      entity integer, keyphrase integer, source integer, weight double precision, count integer
    )
    
    entity_keywords (
      entity integer, keyword integer, count integer, weight double precision
    )
        
This is the meat of AIDA. All entities are associated with (optionally weighted) keyphrases and keywords (connected by the _keyphrase_tokens_ table), represented by an integer id. As the keyphrases are matched partially against input text. The mandatory fields are:

* entity: The id corresponds to the id in the _dictionary_ and the _entity_ids_ table.
* keyphrase/keyword: The id corresponds to the id in the _word_ids_ table.

The optional fields are:

* source: Keyphrases can be filtered by source
* count: This can be used to keep the co-occurrence counts of the entity-keyphrase pairs, but is superflous if all the weights are pre-computed
* weight: Weight derived by the co-occurrence counts.

#### Optional Tables
    
    entity_inlinks (
      entity integer, inlinks integer[]
    )
    
If you want to use coherence based on a link graph (_MilneWittenEntityEntitySimilarity_) instead of keyphrases (_KOREEntityEntitySimilarity_), this table needs to be populated with all entities and their inlinks.

Other data that is not explicitly described here includes types and global entity weights.

## DMap usage

### DMap creation

#### Add a new DMap

To add a new DMap to the system you have to edit the `mpi.aida.access.DatabaseDMap` enum file.
You can use on of three constructors:

The main constructor:   
`DatabaseDMap(String name, String source, boolean isSourceSorted, boolean isSourceTable, String... keys)`   

The constructor for tables:   
`DatabaseDMap(String tableName, String... keys)`

The constructor for sql commands:   
`DatabaseDMap(String namePrefix, String sqlCommand, boolean isSourceSorted, String... keys)`

Detailed descriptions for the arguments are in de comments of the enum file itself.


#### Settings

The Configuration for the DMap creation is done in the `settings/preparation.properties` file.   
There are four keys that can/must be set:

`dMapTargetDirectory`   
The directory where the .dmap and .proto files are saved. (The default is: `dMaps`)

`dMapProtoClassesTargetPackage`   
The package where the classes from the .proto files should go. (This is required)

`aidaSourceFolder`   
The root directory for the package path. (The default is: `src`)

`protocPath`   
Path to the protoc executable. (The default is: `protoc`)

#### Execution

To create the DMaps just run the `mpi.aida.datapreparation.PrepareData` like so:

`mpi.aida.datapreparation.PrepareData <workingDir> DMAP_CREATION`

### Use DMaps

To use the DMaps as a data source the value of `dataAccess` in the `settings/aida.properties` has to be `dmap`.

#### Settings

The settings for the usage of the DMaps are saved in the `settings/dmap_aida.properties` file.   
There are four keys with fixed names:

`directoryName`   
The directory where the dMaps are located. (The default is: `dMaps`)

`mapsToLoad`   
This is a comma separated list of all maps that should be loaded. (The default is: all)

`default.preloadKeys` and `default.preloadValues`   
Defines if the Keys/Values should be preloaded into memory.

It is possible to set the preload behavior for each DMap individually:   
`<DMapName>.preloadKeys` and `<DMapName>.preloadValues`   
Defines if the Keys/Values of a specific DMap should be preloaded into memory.

## Further Information

If you are using AIDA, any parts of it or any datasets we made available, please give us credit by referencing AIDA in your work. If you are publishing scientific work based on AIDA, please cite our [EMNLP2011] paper referenced at the end of this document.

* Our AIDA project website: [http://www.mpi-inf.mpg.de/yago-naga/aida/](http://www.mpi-inf.mpg.de/yago-naga/aida/)
* Our news mailing list: Mail to [aida-news-subscribe@lists.mpi-inf.mpg.de](mailto:aida-news-subscribe@lists.mpi-inf.mpg.de) to get news and updates about releases.
* Build status: [![Build Status](https://travis-ci.org/yago-naga/aida.png)](https://travis-ci.org/yago-naga/aida)

## Developers

The AIDA developers are (in alphabetical order):

* Ilaria Bordino
* Johannes Hoffart ( http://www.mpi-inf.mpg.de/~jhoffart )
* Felix Keller
* Edwin Lewis-Kelham
* Dragan Milchevski ( http://www.mpi-inf.mpg.de/~dmilchev )
* Dat Ba Nguyen ( http://www.mpi-inf.mpg.de/~datnb )
* Stephan Seufert ( http://www.mpi-inf.mpg.de/~sseufert )
* Vasanth Venkatraman ( http://www.mpi-inf.mpg.de/~vvenkatr )
* Mohamed Amir Yosef ( http://www.mpi-inf.mpg.de/~mamir )

## License

AIDA by Max-Planck-Institute for Informatics, Databases and Information Systems is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

## Libraries Used

We thank the authors of the following pieces of software, without which the development of AIDA would not have been possible. The needed software is available under different licenses than the AIDA source code, namely:

* Apache Commons, all licensed under Apache 2.0
	* cli, collections, io, lang
* MPI D5 utilities, all licensed under CC-BY 3.0
* JavaEWAH, licensed under Apache 2.0
* JUnit, licensed under CPL 1.0
* log4j, licensed under Apache 2.0
* postgresql-jdbc, licensed under the BSD License
* slf4j, licensed under MIT License
* Stanford CoreNLP, licensed under the GPL v2
	* Dependencies: 
		* jgrapht, licensed under LGPL v2.1
		* xom, licensed under LGPL v2.1
		* joda-time, licensed under Apache 2.0
* Trove, licensed under the LGPL, parts under a license by CERN

All libraries are included as dependencies by maven.
	
### Licenses of included Software

All licenses can be found in the licenses/ directory or at the following URLs:

* Apache License 2.0: http://www.apache.org/licenses/LICENSE-2.0
* Creative Commons CC-BY 3.0: http://creativecommons.org/licenses/by/3.0/ 
* GNU GPL v2: http://www.gnu.org/licenses/gpl-2.0.html
* GNU LGPL v2.1: http://www.gnu.org/licenses/lgpl-2.1.html

## Citing AIDA

If you use AIDA in your research, please cite AIDA:

    @inproceedings{AIDA2011,
      author = {Hoffart, Johannes and Yosef, Mohamed Amir and Bordino, Ilaria and F{\"u}rstenau, Hagen and Pinkal, Manfred and Spaniol, Marc and Taneva, Bilyana and Thater, Stefan and Weikum, Gerhard},
      title = {{Robust Disambiguation of Named Entities in Text}},
      booktitle = {Conference on Empirical Methods in Natural Language Processing, EMNLP 2011, Edinburgh, Scotland},
      year = {2011},
      pages = {782--792}
    }

## References

* [EMNLP2011]: J. Hoffart, M. A. Yosef, I. Bordino, H. Fürstenau, M. Pinkal, M. Spaniol, B. Taneva, S. Thater, and G. Weikum, "Robust Disambiguation of Named Entities in Text," Conference on Empirical Methods in Natural Language Processing, EMNLP 2011, Edinburgh, Scotland, 2011, pp. 782–792.
* [VLDB2011]: M. A. Yosef, J. Hoffart, I. Bordino, M. Spaniol, and G. Weikum, “AIDA: An Online Tool for Accurate Disambiguation of Named Entities in Text and Tables,” Proceedings of the 37th International Conference on Very Large Databases, VLDB 2011, Seattle, WA, USA, 2011, pp. 1450–1453.
* [YAGO2]: J. Hoffart, F. M. Suchanek, K. Berberich, and G. Weikum, “YAGO2: A spatially and temporally enhanced knowledge base from Wikipedia,” Artificial Intelligence, vol. 194, pp. 28–61, 2013.
* [MilneWiten]: D. Milne and I. H. Witten, “An Effective, Low-Cost Measure of Semantic Relatedness Obtained from Wikipedia Links,” Proceedings of the AAAI 2008 Workshop on Wikipedia and Artificial Intelligence (WIKIAI 2008), Chicago, IL, 2008.
* [KORE]: J. Hoffart, S. Seufert, D. B. Nguyen, M. Theobald, and G. Weikum, “KORE: Keyphrase Overlap Relatedness for Entity Disambiguation,” Proceedings of the 21st ACM International Conference on Information and Knowledge Management, CIKM 2012, Hawaii, USA, 2012, pp. 545–554.

## References AIDArabic
* [AIDArabic]:  M. A. Yosef, M. Spaniol and G. Weikum, "A Named-Entity Disambiguation Framework for Arabic Text," In 8th Workshop on Exploiting Semantic Annotations in Information Retrieval, CIKM 2015, ACM 2015. 
* [AIDAplus]: M. H. Gad-Elrab, M. A. Yosef and G. Weikum, "Named Entity Disambiguation for Resource-Poor Languages," In 8th Workshop on Exploiting Semantic Annotations in Information Retrieval, CIKM 2015, ACM 2015. 
* [EDRAK]: M. H. Gad-Elrab, M. A. Yosef and G. Weikum, "EDRAK: Entity-Centric Data Resource for Arabic Knowledge," In 2nd Workshop on Arabic NLP, ACL 2015.  



[AIDA]: http://www.mpi-inf.mpg.de/yago-naga/aida/
[MPID5]: http://www.mpi-inf.mpg.de/departments/d5/index.html
[Postgres]: http://www.postgresql.org
[YAGO]: http://www.yago-knowledge.org
[CoreNLP]: http://nlp.stanford.edu/software/corenlp.shtml
[AIDAGit]: https://github.com/yago-naga/aida
[ArabSeg]: http://nlp.stanford.edu/projects/arabic.shtml
[AIDArabicThesis]: http://people.mpi-inf.mpg.de/~gadelrab/downloads/Mohamed_Gadelrab_thesis.pdf
[EDRAKData]: https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/aida/downloads/
