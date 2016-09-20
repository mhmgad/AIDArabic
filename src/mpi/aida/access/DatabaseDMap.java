package mpi.aida.access;

import edu.stanford.nlp.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Stores all the DMaps which should be crated and be able to access
 */
public enum DatabaseDMap {
  BIGRAM_COUNTS_BIGRAM(DataAccessSQL.BIGRAM_COUNTS, "bigram"),
  DICTIONARY_MENTION("dictionary", "mention"),
  ENTITY_BIGRAMS_ENTITY(DataAccessSQL.ENTITY_BIGRAMS, "entity"),
  ENTITY_IDS_ID("entity_ids", "id"),
  ENTITY_IDS_ENTITY_KNOWLEDGEBASE("entity_ids", "entity", "knowledgebase"),
  ENTITY_KEYPHRASES_ENTITY("entity_keyphrases", "entity"),
  ENTITY_KEYWORDS_ENTITY(DataAccessSQL.ENTITY_KEYWORDS, "entity"),
  ENTITY_METADATA_ENTITY("entity_metadata", "entity"),
  ENTITY_RANK_ENTITY("entity_rank", "entity"),
  KEYPHRASES_SOURCE_SOURCE_ID("keyphrase_sources", "source_id"),
  KEYPHRASES_SOURCES_WEIGHT_SOURCE("keyphrases_sources_weights", "source"),
  KEYWORD_COUNTS_KEYWORD(DataAccessSQL.KEYWORD_COUNTS, "keyword"),
  METADATA_KEY("meta", "key"),
  TYPE_IDS_ID("type_ids", "id"),
  WORD_EXPANSION_WORD("word_expansion", "word"),
  WORD_IDS_WORD("word_ids", "word"),
  KEYPHRASE_TOKENS_KEYPHRASE("keyphrase_tokens", 
    "SELECT * FROM keyphrase_tokens ORDER BY keyphrase, position", true, "keyphrase"),
  ENTITY_INLINKS_ENTITY("entity_inlinks", 
    "SELECT entity, unnest(inlinks) AS inlink FROM entity_inlinks", true, "entity"),
  ENTITY_TYPES_ENTITY("entity_types", 
    "SELECT entity, unnest(types) AS type FROM entity_types", true, "entity");
  

  private String name;
  private String source;
  private boolean isSourceSorted;
  private boolean isSourceTable;
  private TreeSet<String> keys;

  /**
   * This is the main constructor and is usually only used by the other constructors.
   * 
   * @param name The name of the DMap.
   * @param source The source the DMap should be created from (sql command or table name).
   * @param isSourceSorted Indicates if the source is sorted.
   * @param isSourceTable Indicates if the source is a table or a sql command.
   * @param keys The columns of the source which should be used as key.
   */
  DatabaseDMap(String name, String source, boolean isSourceSorted, boolean isSourceTable, String... keys) {
    this.name = name;
    this.source = source;
    this.isSourceSorted = isSourceSorted;
    this.isSourceTable = isSourceTable;
    this.keys = new TreeSet<>(Arrays.asList(keys));
  }

  /**
   * This constructor make it easier to create DMaps from tables.
   * The name of the DMap will be generated from the table name and the keys.
   * 
   * @param tableName The name of the table.
   * @param keys The columns of the table which should be used as key.
   */
  DatabaseDMap(String tableName, String... keys) {
    this(null, tableName, false, true, keys);
    this.name = getMapName(this.source, this.keys);
  }

  /**
   * This is constructor makes is easier to create DMaps from SQL commands.
   * The name of the DMap will be generated from name prefix and the keys.
   * 
   * @param namePrefix Prefix of the DMap name.
   * @param sqlCommand The SQL command the DMap is created from.
   * @param isSourceSorted Indicates if the table returned by the sql command is already correctly sorted.
   * @param keys The columns of the table returned by the sql command which should be used as key.
   */
  DatabaseDMap(String namePrefix, String sqlCommand, boolean isSourceSorted, String... keys) {
    this(null, sqlCommand, isSourceSorted, false, keys);
    this.name = getMapName(namePrefix, this.keys);
  }

  public String getName() {
    return name;
  }

  public String getSource() {
    return source;
  }

  public boolean isSourceSorted() {
    return isSourceSorted;
  }

  public boolean isSourceTable() {
    return isSourceTable;
  }

  public TreeSet<String> getKeys() {
    return keys;
  }

  private static String getMapName(String table, Set<String> keys) {
    return table + "-" + StringUtils.join(keys, "_");
  }
}
