package mpi.aida.datapreparation.util;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.access.DataAccessSQL;
import mpi.aida.graph.similarity.util.UnitBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Some method so Units are built everywhere in the same way.
 */
public final class UnitUtil {
  private static final Logger logger =
    LoggerFactory.getLogger(UnitUtil.class);

  private UnitUtil() {
    
  }

  public static int getUnitId(TIntList unitTokens, TIntObjectHashMap<String> id2word,
                              TObjectIntHashMap<String> word2id) {
    if (unitTokens == null || unitTokens.size() == 0) return 0;
    if (unitTokens.size() == 1) return unitTokens.get(0);
    return word2id.get(UnitBuilder.buildUnit(unitTokens, id2word));
  }

  public static TIntObjectHashMap<TIntHashSet> loadEntityUnits(int unitSize) {
    int unitSizeMinusOne = unitSize - 1;
    TIntObjectHashMap<Set<TIntArrayList>> entitiesUnits = new TIntObjectHashMap<>();
    TIntSet usedTokens = new TIntHashSet();
    Connection con = null;
    Statement statement;

    ResultSet r;

    try {
      con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);

      con.setAutoCommit(false);
      statement = con.createStatement();
      statement.setFetchSize(10_000_000);

      TIntObjectHashMap<int[]> keyphrase_tokens = DataAccess.getAllKeyphraseTokens();
      logger.info("Size of Preloaded keyphrase_tokens : " + keyphrase_tokens.size());
      double minWeight = AIDASchemaPreparationConfig.getDouble(AIDASchemaPreparationConfig.UNIT_CREATION_THRESHOLD_MIN_WEIGHT);
      int topk = AIDASchemaPreparationConfig.getInteger(AIDASchemaPreparationConfig.UNIT_CREATION_THRESHOLD_TOPK);
      String sql;
      if (topk > 0) {
        sql = "SELECT entity, keyphrase FROM (" +
          "SELECT entity, keyphrase, weight, row_number() OVER (PARTITION BY entity ORDER BY weight DESC) AS rank " +
          "FROM " + DataAccessSQL.ENTITY_KEYPHRASES +
          ") as ekr WHERE rank<=" + topk;
        if (minWeight > 0)
          sql += " AND weight>=" + minWeight;
      } else {
        sql = "SELECT entity, keyphrase FROM " + DataAccessSQL.ENTITY_KEYPHRASES;
        if (minWeight > 0)
          sql += " WHERE weight>=" + minWeight;
      }
      
      r = statement.executeQuery(sql);
      int count = 0;
      int[] unitArray = new int[unitSize];
      while (r.next()) {
        int entity = r.getInt("entity");
        int keyphrase = r.getInt("keyphrase");
        int[] tokens = keyphrase_tokens.get(keyphrase);
        Set<TIntArrayList> unitSet = entitiesUnits.get(entity);
        if (unitSet == null) entitiesUnits.put(entity, (unitSet = new HashSet<>()));
        if (tokens != null) {
          if (unitSize > 1) {
            unitArray[0] = 0;
            // we skip the first element of the firstUnitArray because it should stay 0
            System.arraycopy(tokens, 0, unitArray, 1, unitSizeMinusOne);
            unitSet.add(new TIntArrayList(unitArray));

            unitArray[unitSize - 1] = 0;
            // we skip the last element of the lastUnitArray because it should stay 0
            System.arraycopy(tokens, tokens.length - unitSizeMinusOne, unitArray, 0, unitSizeMinusOne);
            unitSet.add(new TIntArrayList(unitArray));
          }
          if (tokens.length >= unitSize - 1) {
            for (int i = 0; i < tokens.length - unitSizeMinusOne; i++) {
              System.arraycopy(tokens, i, unitArray, 0, unitSize);
              unitSet.add(new TIntArrayList(unitArray));
            }
            usedTokens.addAll(tokens);
            if (++count % 10_000_000 == 0) logger.info("Read " + count + " keyphrases");
          }
        } else logger.info("Keyphrase : " + keyphrase + " has no toekns...");
      }
      logger.info("Finished Reading " + count + " keyphrases!");
      r.close();
      con.setAutoCommit(true);
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      AidaManager.releaseConnection(con);
    }

    TIntObjectHashMap<TIntHashSet> entitiesUnitsIds = new TIntObjectHashMap<>();
    TIntObjectHashMap<String> usedWords = DataAccess.getWordsForIds(usedTokens.toArray());
    TObjectIntHashMap<String> wordIds = DataAccess.getAllWordIds();
    TIntObjectIterator<Set<TIntArrayList>> entitiesUnitTokensIterator =
      entitiesUnits.iterator();
    while (entitiesUnitTokensIterator.hasNext()) {
      entitiesUnitTokensIterator.advance();
      TIntHashSet units = new TIntHashSet();
      entitiesUnitsIds.put(entitiesUnitTokensIterator.key(), units);
      for (TIntArrayList unit : entitiesUnitTokensIterator.value()) {
        units.add(getUnitId(unit, usedWords, wordIds));
      }
    }

    return entitiesUnitsIds;
  }
}
