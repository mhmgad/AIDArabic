package mpi.experiment.trace.measures;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import mpi.aida.access.DataAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UnitMeasureTracer extends MeasureTracer {
  private static Logger sLogger_ = LoggerFactory.getLogger(UnitMeasureTracer.class);
  
	public static int countForUI = 0;
	
	private static final TIntObjectHashMap<String> id2word;
	static {
		sLogger_.debug("Reading all word ids for tracing.");
		id2word = getAllWordIds();
		sLogger_.debug("Reading all word ids for tracing done.");
	}
	
	private int matchedUnits = 0;
	private int entityUnitCount;
	private List<UnitTracingObject> units = null;
	private DecimalFormat formatter = new DecimalFormat("#0.00000");
	
	public UnitMeasureTracer(String name, double weight, int entityUnitCount) {
		super(name, weight);
    if (weight < 0.0) {
      sLogger_.error("Weight should not be < 0");
    }
		units = new LinkedList<>();
		this.entityUnitCount = entityUnitCount;
	}
	
  public static TIntObjectHashMap<String> getAllWordIds() {
    TObjectIntHashMap<String> wordIds = DataAccess.getAllWordIds();
    TIntObjectHashMap<String> idWords = new TIntObjectHashMap<>(wordIds.size());
    for (TObjectIntIterator<String> itr = wordIds.iterator(); itr.hasNext(); ) {
      itr.advance();
      idWords.put(itr.value(), itr.key());
    }    
    return idWords;
  }

  @Override
	public String getOutput() {
		Collections.sort(units);
				
		TIntLinkedList wordIds = new TIntLinkedList();
		for (UnitTracingObject uto : units) {
			wordIds.add(uto.unit);
		}		
		StringBuilder sb = new StringBuilder();
		sb.append("<strong style='color: #0000FF;'> score = " + formatter.format(score) + ", matched " + 
			matchedUnits + " out of " + entityUnitCount + " in " + units.size() + " units</strong><br />");
		int unitCount = 0;
		for(UnitTracingObject unit : units) {
			if(unitCount == 5) {
				countForUI++;
				sb.append("<a class='showMore' onclick=\"setVisibility('div"
						+ countForUI
						+ "', 'block');\">More ...</a>&nbsp;&nbsp;&nbsp;<a class='showLess' onclick=\"setVisibility('div"
						+ countForUI + "', 'none');\">Less ...</a>");
				sb.append("<div id='div" + countForUI + "' style='display:none'>");
			}
			sb.append("<span style='color: #005500;'>" + formatter.format(unit.score) + "</span> - ");
			sb.append("<span");
			if (unit.entityUnit)
				sb.append(" style='color: #0099CC;'");
			sb.append(">\"<strong>" + id2word.get(unit.unit) + "</strong>\"</span>");
			sb.append("<br />");
			unitCount++;
		}
		if(unitCount >= 5) {
			sb.append("</div>");
		}
		return sb.toString();
	}

	/**
	 * @param unit the unit to add
	 * @param score how much score this unit contributes to the total similarity
	 */
	public void addUnitTraceInfo(int unit, double score, boolean entityUnit) {
		units.add(new UnitTracingObject(unit, score, entityUnit));
		if (entityUnit) matchedUnits++;
	}
	

	private class UnitTracingObject implements Comparable<UnitTracingObject>{
		private int unit;
		private double score;
		private boolean entityUnit;

		public UnitTracingObject(int unit, double score, boolean entityUnit) {
			this.unit = unit;
			this.score = score;
			this.entityUnit = entityUnit;
		}

		@Override
		public int compareTo(UnitTracingObject o) {
			if(score < o.score)
				return 1;
			else if (score == o.score)
				return 0;
			else
				return -1;
		}
	}
}
