package mpi.aida.util;

import gnu.trove.impl.Constants;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import mpi.aida.access.DataAccess;
import mpi.aida.data.Context;
import mpi.aida.data.Mention;
import mpi.aida.graph.similarity.UnitType;
import mpi.aida.graph.similarity.util.UnitBuilder;

import java.util.*;

public class InputTextWrapper {
  int[] units;
  TIntIntMap unitCounts;
  int numOfUnits;
  Mention mentionToIgnore;
  UnitType unitType;

  public InputTextWrapper(Context context, UnitType unitType, boolean removeStopwords) {
    mentionToIgnore = null;
    this.unitType = unitType;
    int unitLength = unitType.getUnitSize();
    if (context.getTokenCount() < unitLength) return;
    List<String> unitStrings = new ArrayList<>(context.getTokenCount() - (unitLength == 1 ? 0 : unitLength + 3));
    Queue<String> curTokens = new ArrayDeque<>(unitLength);
    String[] curTokensArray = new String[unitLength];
    for (String token : context.getTokens()) {
      curTokens.add(token);
      if (curTokens.size() == unitLength || (!curTokens.isEmpty() && curTokens.size() - 1 == unitLength)) {
        unitStrings.add(UnitBuilder.buildUnit(curTokens.toArray(curTokensArray)));
        curTokens.remove();
      }
    }

    TObjectIntHashMap<String> wordIds = DataAccess.getIdsForWords(unitStrings);
    units = new int[unitStrings.size()];
    unitCounts = new TIntIntHashMap((int) (wordIds.size()/Constants.DEFAULT_LOAD_FACTOR), Constants.DEFAULT_LOAD_FACTOR);
    numOfUnits = 0;
    for (int i = 0; i < unitStrings.size(); i++) {
      int unitId = wordIds.get(unitStrings.get(i));
      if (unitId == 0)
        continue;
      int contractedUnitId = DataAccess.contractTerm(unitId);
      if (contractedUnitId != 0)
        unitId = contractedUnitId;
      if (removeStopwords && StopWord.isStopwordOrSymbol(unitId))
        continue;
      units[i] = unitId;
      unitCounts.adjustOrPutValue(unitId, 1, 1);
      numOfUnits++;
    }
  }
  
  public int getUnitCount(int unit) {
    return unitCounts.get(unit);
  }
  
  public int getSize() {
    if (mentionToIgnore == null)
      return numOfUnits;
    else {
      return numOfUnits - 
        ((unitType.getUnitSize() == 1 ? 0 : 3 - unitType.getUnitSize()) 
          + (mentionToIgnore.getEndToken() - mentionToIgnore.getStartToken()));
    }
  }
  
  public int[] getUnits() {
    // If there is no context, return empty array.
    if (unitCounts == null) {
      return new int[0];
    }

    if (mentionToIgnore == null)
      return unitCounts.keys();
    else {
      int[] result = unitCounts.keys();
      int start = mentionToIgnore.getStartToken() - (unitType.getUnitSize() == 1 ? 0 : unitType.getUnitSize() - 2);
      int end = start + unitType.getUnitSize() + (mentionToIgnore.getEndToken() - mentionToIgnore.getStartToken()) - 1;
      for (int i = start; i < end; i++) {
        for (int j = 0; j < result.length; j++) {
          if (result[j] == units[i]) {
            result[j] = 0;
            break;
          }
        }
      }
      return result;
    }
  }
  
  public int[] getUnitsInContext() {
    return units;
  }

  public void mentionToIgnore(Mention mention) {
    mentionToIgnore = mention;
  }
}
