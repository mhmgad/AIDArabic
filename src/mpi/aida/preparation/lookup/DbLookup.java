package mpi.aida.preparation.lookup;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mpi.aida.access.DataAccess;
import mpi.aida.data.Entities;
import mpi.aida.data.Mention;
import mpi.aida.util.timing.RunningTimer;

class DbLookup extends EntityLookup {

  @Override
  public Entities getEntitiesForMention(Mention mention, double maxEntityRank, int topByPrior, boolean mentionIsPrefix) {
    if (mentionIsPrefix) {
      throw new IllegalArgumentException(
              DbLookup.class + " does not support prefix-based candidate lookup. " +
                      "Use DictionaryLookup if this functionality is needed.");
    }
    int id = RunningTimer.recordStartTime("dbLookup:Entity");
    Set<String> normalizedMentions = mention.getNormalizedMention();
    Entities entities = new Entities();
    Map<String, Entities> entitiesMap = DataAccess.getEntitiesForMentions(normalizedMentions, maxEntityRank, topByPrior);
    for(Entry<String, Entities> entry : entitiesMap.entrySet()) {
      entities.addAll(entry.getValue());
    }
    RunningTimer.recordEndTime("dbLookup:Entity", id);
    return entities;
  }
}
