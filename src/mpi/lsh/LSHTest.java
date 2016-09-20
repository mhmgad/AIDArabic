package mpi.lsh;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.Keyphrases;
import mpi.aida.util.StringUtils;
import mpi.aida.util.YagoUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class LSHTest {

  public void testEntities() throws InterruptedException {
    AidaManager.init();

    Entities entities = new Entities();
    entities.add(YagoUtil.getEntityForYagoId("Albert_Einstein"));
    entities.add(YagoUtil.getEntityForYagoId("Niels_Bohr"));
    entities.add(YagoUtil.getEntityForYagoId("Ulm"));
    entities.add(YagoUtil.getEntityForYagoId("Stuttgart"));
    entities.add(YagoUtil.getEntityForYagoId("Google"));
    entities.add(YagoUtil.getEntityForYagoId("Facebook"));
    entities.add(YagoUtil.getEntityForYagoId("Tokyo"));

    Keyphrases keyphrases = DataAccess.getEntityKeyphrases(entities, null, 0.0,
        Integer.MAX_VALUE);

    TIntObjectHashMap<int[]> entityIdKeyphrases = keyphrases
        .getEntityKeyphrases();
    Map<Entity, int[]> entityKeyphrases = new HashMap<Entity, int[]>();
    for (TIntObjectIterator<int[]> itr = entityIdKeyphrases.iterator(); itr
        .hasNext();) {
      itr.advance();
      entityKeyphrases.put(AidaManager.getEntity(itr.key()), itr.value());
    }

    MinHasher<Entity> minHasher = new MinHasher<>(2000, 2);
    Map<Entity, int[]> entitySigs = minHasher
        .createSignatures(entityKeyphrases);
    LSH<Entity> lsh = LSH.createLSH(entitySigs, 2, 1000, 2);
    for (Entity e : entities) {
      System.out.println(e);
      for (Entity f : lsh.getSimilarItems(e)) {
        System.out.println("\t" + f);
      }
    }
  }
  public void testStrings() throws InterruptedException, IOException {
    AidaManager.init();

    Set<String> names = DataAccess.getDictionary().keySet();
//    Set<String> names = new HashSet<>();
//    names.add("ab");

    LSH<String> lsh = LSH.createLSH(names, new LSHStringNgramFeatureExtractor(3), 4, 6, 2);

//    System.out.println(lsh.getSimilarItems());

    while (true) {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("Enter name: ");
      String name = br.readLine();
      System.out.println("Similar names: \n" + lsh.getSimilarItemsForFeature(name));
    }
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    new LSHTest().testStrings();
  }
}
