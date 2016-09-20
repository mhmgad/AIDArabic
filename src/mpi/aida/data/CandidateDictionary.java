package mpi.aida.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;

/**
 * Constructed from a mention to entity-collection map, this allows 
 * easy lookups by Mention objects.
 */
public class CandidateDictionary {

  private Map<String, Entities> mentionEntityDictionary_ = new HashMap<>();

  private Set<Entity> allEntities_ = new HashSet<>();

  private Map<KBIdentifiedEntity, Integer> kbIdentifier2id_ = new HashMap<>();

  public CandidateDictionary() {
    // Placeholder for initialization when unused.
  }
  
  /**
   * The format of the keys is UTF-8 strings used as lookup keys.
   * The collection must be in the form of KBIdentifiedEntity strings, e.g.
   * "YAGO:Jimmy_Page" and must NOT exist in the entity repository.
   *
   * @param mentionEntityDictionary Mention-Entity pairs for additional lookup.
   */
  public CandidateDictionary(
      Map<String, List<KBIdentifiedEntity>> mentionEntityDictionary) {
    // Get starting id for non-conflicting entity ids - external context is assumed to be state-less, so they
    // can conflict with each other, but not with the entity repository.
    int maxEntityId = DataAccess.getMaximumEntityId();

    allEntities_ = new HashSet<>();
    kbIdentifier2id_ = new HashMap<>();

    mentionEntityDictionary_ = new HashMap<>();
    for (Entry<String, List<KBIdentifiedEntity>> e : mentionEntityDictionary.entrySet()) {
      Entities candidates = new Entities();
      for (KBIdentifiedEntity kbId : e.getValue()) {
        Entity entity = new Entity(kbId, ++maxEntityId);
        candidates.add(entity);
        allEntities_.add(entity);
        kbIdentifier2id_.put(entity.getKbIdentifiedEntity(), entity.getId());
      }
      mentionEntityDictionary_.put(AidaManager.conflateToken(e.getKey()), candidates);
    }
  }

  /**
   * Return the candidate entities for the given mention.
   * 
   * @param m Mention to perform lookup for.
   * @return  Candidate entities.
   */
  public Entities getEntities(Mention m) {
    Entities entities = new Entities();
    for (String mention : m.getNormalizedMention()) {
      String normalizedMention = AidaManager.conflateToken(mention);
      Entities candidates = mentionEntityDictionary_.get(normalizedMention);
      if (candidates != null) {
        entities.addAll(candidates);
      }
    }
    return entities;
  }

  /**
   * Checks if the dictionary contains the entity as candidate for any mention.
   * @param entity
   * @return
   */
  public boolean contains(Entity entity) {
    return allEntities_.contains(entity);
  }

  /**
   * @return All external entities.
   */
  public Set<Entity> getAllEntities() {
    return allEntities_;
  }

  /**
   * Returns the transient, internal entity ID for the kbIdentifier.
   * @param kbIdentifier
   * @return Transient entity ID.
   */
  public int getEntityId(KBIdentifiedEntity kbIdentifier) {
    return kbIdentifier2id_.get(kbIdentifier);
  }
}
