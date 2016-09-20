package mpi.aida.access;

import mpi.aida.config.AidaConfig;

/**
 * This tests all the demo examples for correct outcome.
 * Useful as a safety net :)
 * 
 */
public class DataAccessSQLRealDBTest {

  public DataAccessSQLRealDBTest() {
    AidaConfig.set("dataAccess", "sql");
  }
  
//  @Test
//  public void testGetTaxonomy() {
//    TIntObjectHashMap<int[]> taxonomy = DataAccess.getTaxonomy();
//    TObjectIntHashMap<Type> typeIds = DataAccess.getAllTypeIds();
//    TIntObjectHashMap<Type> id2type = 
//        new TIntObjectHashMap<Type>(typeIds.size());
//    for (TObjectIntIterator<Type> itr = typeIds.iterator(); itr.hasNext(); ) {
//      itr.advance();
//      id2type.put(itr.value(), itr.key());
//    }
//    Type person = new Type("YAGO", Basics.PERSON);
//    int personId = typeIds.get(person);
//    int[] personParents = taxonomy.get(personId);
//    for (int p : personParents) {
//      System.out.println(id2type.get(p));
//    }
//  }
}