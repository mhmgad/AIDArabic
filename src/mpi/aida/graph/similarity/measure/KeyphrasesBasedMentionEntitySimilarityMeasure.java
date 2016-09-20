package mpi.aida.graph.similarity.measure;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.List;

import mpi.aida.data.Context;
import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aida.graph.similarity.context.EntitiesContext;
import mpi.aida.graph.similarity.context.KeyphrasesContext;
import mpi.aida.util.InputTextInvertedIndex;
import mpi.aida.util.MinCover;
import mpi.aida.util.MinCoverCalculator;
import mpi.aida.util.StopWord;
import mpi.aida.util.timing.RunningTimer;
import mpi.experiment.trace.NullTracer;
import mpi.experiment.trace.Tracer;
import mpi.experiment.trace.measures.KeyphrasesMeasureTracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class KeyphrasesBasedMentionEntitySimilarityMeasure extends MentionEntitySimilarityMeasure {
  private static final Logger logger = 
      LoggerFactory.getLogger(KeyphrasesBasedMentionEntitySimilarityMeasure.class);
  
  protected InputTextInvertedIndex originalInputTextindex;
  //used to keep any extra context other the input text
  protected InputTextInvertedIndex extraContextIndex;

  private MinCoverCalculator minCoverCalculator;

  protected KeyphrasesContext keyphrasesContext;

  // TODO(jhoffart,mamir) below needs to be passed
  protected boolean normalize;
  private boolean removeStopwords = true;
  
  private boolean isTracing = false;
  
  public KeyphrasesBasedMentionEntitySimilarityMeasure(Tracer tracer) {
    super(tracer);
    Integer id = RunningTimer.recordStartTime("KeyphraseBasedMESim:init");
    normalize = false;

    minCoverCalculator = new MinCoverCalculator();
    extraContextIndex = new InputTextInvertedIndex();
    
    isTracing = !(tracer instanceof NullTracer);
    RunningTimer.recordEndTime("KeyphraseBasedMESim:init", id);
  }

  @Override
  public double calcSimilarity(Mention mention, Context context, Entity entity, EntitiesContext entitiesContext) {    
    Integer id = RunningTimer.recordStartTime("KeyphraseBasedMESim:calcSimilarity");
    keyphrasesContext = (KeyphrasesContext) entitiesContext;
    if (originalInputTextindex == null) {
      originalInputTextindex = new InputTextInvertedIndex(context.getTokenIds(), removeStopwords);
    }

    int[] keyphrases = entitiesContext.getContext(entity);
    
    double similarity = 0;

    int matchedKPs = 0;
    KeyphrasesMeasureTracer mt = null;
    if (isTracing) {
      mt = new KeyphrasesMeasureTracer(getIdentifier(), 0.0);
    }

    if (keyphrases != null) {
      for (int keyphrase : keyphrases) {
        int[] keyphraseTokens = ((KeyphrasesContext) entitiesContext).getKeyphraseTokens(keyphrase);
        int[] cleanedKeyphraseTokens = cleanKeyphrase(keyphraseTokens, mention, context.getTokens(), entity, entitiesContext);
        TIntDoubleHashMap matchedKeywords = new TIntDoubleHashMap();
        double keyphraseSimilarity = calculateContextKeyphraseSimilarity(mention, entity, keyphrase, cleanedKeyphraseTokens, matchedKeywords, entitiesContext, context.getTokenIds());
        
        double keyphraseSourceWeight = keyphrasesContext.getKeyphraseSourceWeight(entity, keyphrase);
        similarity += keyphraseSourceWeight * keyphraseSimilarity;
  
        if (matchedKeywords.size() > 0) {
          matchedKPs++;
          if (isTracing) {
            mt.addKeyphraseTraceInfo(keyphraseTokens, 0, keyphraseSimilarity, matchedKeywords);
          }
        }
      }
    }

    if (normalize && matchedKPs > 0) {
      similarity = similarity / matchedKPs;
    }
    
    if (isTracing) {
      mt.setScore(similarity);
      tracer.addMeasureForMentionEntity(mention, entity.getId(), mt);
    }
    RunningTimer.recordEndTime("KeyphraseBasedMESim:calcSimilarity", id);
    return similarity;
  }
  

  /**
   * this method encapsulates the cleaning of the keyphrases e.g. by removing
   * stop words numbers, mention occurrences ...etc Subclasses should override
   * this method if needed
   * 
   * @param keyphraseTokens
   * @param entitiesContext
   * @param entity
   * @param mentionTokens
   * @param mention
   * @return cleaned version of the keyphrase
   */
  protected int[] cleanKeyphrase(int[] keyphraseTokens, Mention mention, List<String> mentionTokens, Entity entity, EntitiesContext entitiesContext) {
    return keyphraseTokens;
  }

  protected abstract double getKeywordScore(Entity entity, int keyword);
  
  private double calculateContextKeyphraseSimilarity(Mention mention, Entity entity, int keyphrase, int[] cleanedKeyphraseTokens, TIntDoubleHashMap matchedKeywords, EntitiesContext entitiesContext, int[] is) {
    List<List<Integer>> commonWordsPositions = new ArrayList<List<Integer>>();

    double allKeywordsTotalScore = 0;
    double commonKeywordsTotalScore = 0;
    double keywordScore;
    double phraseImportance = 0;

    for (int keyword : cleanedKeyphraseTokens) {
      if (StopWord.isStopwordOrSymbol(keyword)) {
        continue;
      }
      
      int expandedKeyword = keyphrasesContext.expandTerm(keyword);
      keywordScore = getKeywordScore(entity, keyword);
      phraseImportance += keywordScore;
      allKeywordsTotalScore += keywordScore;
      if (Double.isInfinite(phraseImportance)) {
        logger.debug("inf");
      }
      int matchedKeywordInTheOriginalContext = -1;
      if (originalInputTextindex.containsWord(keyword, mention)) matchedKeywordInTheOriginalContext = keyword;
      else if (originalInputTextindex.containsWord(expandedKeyword, mention)) matchedKeywordInTheOriginalContext = expandedKeyword;
      if (matchedKeywordInTheOriginalContext != -1) { //the word matched
        commonWordsPositions.add(originalInputTextindex.getPositions(matchedKeywordInTheOriginalContext, mention));
        if (!matchedKeywords.containsKey(keyword)) {
          matchedKeywords.put(keyword, keywordScore);
        }
        commonKeywordsTotalScore += keywordScore;
      }
      
      int matchedKeywordInExtraContext = -1;
      if (extraContextIndex.containsWord(keyword, mention)) matchedKeywordInExtraContext = keyword;
      else if (extraContextIndex.containsWord(expandedKeyword, mention)) matchedKeywordInExtraContext = expandedKeyword;
      if (matchedKeywordInExtraContext != -1) { //the word matched
        commonWordsPositions.add(extraContextIndex.getPositions(matchedKeywordInExtraContext, mention));
        if (!matchedKeywords.containsKey(keyword)) {
          matchedKeywords.put(keyword, keywordScore);
        }
        if(matchedKeywordInTheOriginalContext == -1) //if didn't match before in the original context, add it here
        	commonKeywordsTotalScore += keywordScore;
      }      
    }

    int intersectionSize = commonWordsPositions.size();
    if (intersectionSize == 0) return 0;

    MinCover minCoverData = 
        minCoverCalculator.calculateMinCover(commonWordsPositions);

    double minCover = minCoverData.length;

    double score = 0.0;
    // TODO(jhoffart,mamir): phraseImportance should actually not be used, only if we have the MI!
		if (allKeywordsTotalScore != 0) {
			score = phraseImportance
					* (intersectionSize / minCover)
					* Math.pow(
							(commonKeywordsTotalScore / allKeywordsTotalScore),
							2);
		}
                   

    // TODO(jhoffart) make this nice - this is a hack for integrating keyphrase weights.
//    if (entitiesContext instanceof WeightedKeyphrasesContext) {
//      score = score * ((WeightedKeyphrasesContext) entitiesContext).getKeyphraseMiWeight(entity, keyphrase);
//    }
    
    // check if distant keywords should be discounted
    if (isUseDistanceDiscount()) {
      int distanceToMention = getDistanceToMention(mention, minCoverData);
      int docLength = is.length + mention.getEndToken() - mention.getStartToken() + 1;
      score = score * Math.pow((1 - ((double) distanceToMention / docLength)), 2);
    }
    
    if (Double.isNaN(score)) {
      logger.warn("Entity " + entity + " keyphrase '" + 
                  keyphrasesContext.getKeyphraseForId(keyphrase) + 
                  "' (" + keyphrase + ") is borked, setting to 0.0");
      score = 0.0;
    }
    
    // Score might be negative, e.g. when the normalized pointwise mi is all
    // negative. We do not yet take into account negative evidence, so
    // the minimum will be zero.
    return Math.max(0.0, score);
  }

  private int getDistanceToMention(Mention mention, MinCover minCoverData) {
    //The distance is the smallest difference over all distances to all occurrencecs
    //Distance to occurence is the largest of difference between mention and start/end.
    int mentionPosition = mention.getStartToken();
    List<Integer> startPositions = minCoverData.startPositions;
    List<Integer> endPositions = minCoverData.endPositions;
    int distanceToMention = Math.max(Math.abs(mentionPosition - startPositions.get(0)), Math.abs(mentionPosition - endPositions.get(0)));
    for (int i = 1; i < startPositions.size(); i++) {
      int currentDistanceToMention = Math.max(Math.abs(mentionPosition - startPositions.get(i)), Math.abs(mentionPosition - endPositions.get(i)));
      distanceToMention = Math.min(distanceToMention, currentDistanceToMention);
    }
    return distanceToMention;
  }

  protected double log2(double x) {
    return Math.log(x) / Math.log(2);
  }
}
