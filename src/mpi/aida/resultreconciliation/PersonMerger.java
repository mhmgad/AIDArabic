package mpi.aida.resultreconciliation;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.data.ChunkDisambiguationResults;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.data.Type;
import mpi.aida.util.Counter;
import mpi.tools.basics2.Basics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If a text contains multiple person names which are substring of each other,
 * use the same entity for all the names. The entity to choose is the one
 * assigned to the longest mention (which is the most specific and thus
 * most easy to resolve). If there are multiple mentions with the same length
 * and different assigned entities, do not do anything
 * 
 * The rationale is that if there is a "Giuseppe Verdi" in the text, later 
 * references to "Verdi" are most likely to the same person.
 */
public class PersonMerger {

  Logger logger_ = LoggerFactory.getLogger(PersonMerger.class);
  
  public void reconcile(
      ChunkDisambiguationResults disambiguationResults) {
    // Get all person mentions.
    List<ResultMention> personMentions = getPersonMentions(disambiguationResults);
    
    // Assign the best entity based on the longest parent.
    assignBestEntity(personMentions, disambiguationResults);    
  }

  private List<ResultMention> getPersonMentions(
      ChunkDisambiguationResults disambiguationResults) {
    return disambiguationResults.getResultMentions();

//    List<ResultMention> personMentions = new ArrayList<>();
//    Map<KBIdentifiedEntity, ResultMention> kbEntities2mentions = new HashMap<>();
//    for (Entry<ResultMention, List<ResultEntity>> e :
//      disambiguationResults.getAllResults().entrySet()) {
//      List<ResultEntity> rankedEntities = e.getValue();
//      if (rankedEntities != null && rankedEntities.size() > 0) {
//        kbEntities2mentions.put(
//            rankedEntities.get(0).getKbEntity(), e.getKey());
//      }
//    }
//
//    Entities entities = AidaManager.getEntities(kbEntities2mentions.keySet());
//    TIntObjectHashMap<Set<Type>> entityTypes = DataAccess.getTypes(entities);
//    Type person = new Type("YAGO", Basics.PERSON);
//    for (Entity e : entities) {
//      Set<Type> types = entityTypes.get(e.getId());
//      if (types.contains(person)) {
//        personMentions.add(kbEntities2mentions.get(e.getKbIdentifiedEntity()));
//      }
//    }
//
//    return personMentions;
  }

  private void assignBestEntity(
      List<ResultMention> personMentions,
      ChunkDisambiguationResults disambiguationResults) {
    // TODO(jhoffart,mamir): better to sort by length descending and check upwards.
    
    // Sort by increasing length of the mention string.
    Collections.sort(personMentions, new Comparator<ResultMention>() {
      @Override
      public int compare(ResultMention r1, ResultMention r2) {
        return Integer.compare(r1.getCharacterLength(), r2.getCharacterLength());
      }
    });
    
    // Create tokenized mentions for token-wise contains operations.
    List<Set<String>> mentionTokens = new ArrayList<>();
    for (ResultMention m : personMentions) {
      String[] tokens = m.getMention().split(" ");
      Set<String> tokenSet = new HashSet<String>(tokens.length, 1.0f);
      mentionTokens.add(tokenSet);
      for (String t : tokens) {
        tokenSet.add(t);
      }
    }
    
    for (int i = 0; i < personMentions.size(); ++i) {
      Set<String> mTokens = mentionTokens.get(i);
      
      // Check all longer mentions if this is contained, use first one 
      // encountered.
      for (int j = personMentions.size() - 1; j > i; --j) {
        Set<String> superTokens = mentionTokens.get(j);
        if (superTokens.containsAll(mTokens)) {
          ResultMention m = personMentions.get(i);
          ResultMention sm = personMentions.get(j);
          ResultEntity assigned = disambiguationResults.getBestEntity(m);
          ResultEntity superAssigned = disambiguationResults.getBestEntity(sm);
          if (!assigned.getKbEntity().equals(superAssigned.getKbEntity())) {
            // Note: One could also check that the candidate of the
            // superMention is actually also present in the current mention,
            // then just put this one first.            
            logger_.debug("PersonMerger: [" + m + "//" + assigned + "] => [" + sm + "//" + superAssigned + "]");
            Counter.incrementCount("PERSONS_MERGED");
            disambiguationResults.getResultEntities(m).clear();
            disambiguationResults.getResultEntities(m).add(superAssigned);
          }
          // Stop the inner loop search.         
          break;
        }
      }
    }
  }  
}
