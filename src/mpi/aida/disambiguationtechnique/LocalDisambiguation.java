package mpi.aida.disambiguationtechnique;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import mpi.aida.AidaManager;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.data.Mention;
import mpi.aida.data.PreparedInputChunk;
import mpi.aida.data.ResultEntity;
import mpi.aida.data.ResultMention;
import mpi.aida.graph.algorithms.DisambiguationAlgorithm;
import mpi.aida.graph.similarity.EnsembleMentionEntitySimilarity;
import mpi.aida.graph.similarity.util.SimilaritySettings;
import mpi.aida.util.CollectionUtils;
import mpi.experiment.trace.Tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalDisambiguation extends DisambiguationAlgorithm {
  private static final Logger logger = 
      LoggerFactory.getLogger(LocalDisambiguation.class);
  
	protected SimilaritySettings ss;

	protected String chunkId;

	protected boolean includeNullAsEntityCandidate;
	
	protected boolean computeConfidence;

	private NumberFormat nf;
	
  public LocalDisambiguation(PreparedInputChunk input, ExternalEntitiesContext externalContext,
                             DisambiguationSettings settings, Tracer tracer) {
    super(input, externalContext, settings, tracer);
    nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
    nf.setMaximumFractionDigits(2);
    logger.debug("Preparing '" + input.getChunkId() + "' (" + 
            input.getMentions().getMentions().size() + " mentions)");

    this.ss = settings.getSimilaritySettings();
    this.chunkId = input.getChunkId();
    this.includeNullAsEntityCandidate = settings.isIncludeNullAsEntityCandidate();
    this.computeConfidence = settings.shouldComputeConfidence();

    logger.debug("Finished preparing '" + input.getChunkId() + "'");
  }

	@Override
	public Map<ResultMention, List<ResultEntity>> disambiguate() throws Exception {
	  Map<ResultMention, List<ResultEntity>> solutions = 
	      new HashMap<ResultMention, List<ResultEntity>>();
		EnsembleMentionEntitySimilarity mes = prepapreMES();
    disambiguate(mes, solutions);
		return solutions;
	}
	
	private EnsembleMentionEntitySimilarity prepapreMES() {
    Entities entities = AidaManager.getAllEntities(input_.getMentions(), externalContext_, tracer_);
		
		if (includeNullAsEntityCandidate) {
			entities.setIncludesOokbeEntities(true);
		}

		EnsembleMentionEntitySimilarity mes = null;
		try {
			mes = new EnsembleMentionEntitySimilarity(input_.getMentions(), entities, 
			    input_.getContext(), externalContext_, ss, tracer_);
			return mes;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected void disambiguate(EnsembleMentionEntitySimilarity mes, 
	    Map<ResultMention, List<ResultEntity>> solutions) throws Exception {
		for (Mention mention : input_.getMentions().getMentions()) {		
			List<ResultEntity> entities = new LinkedList<ResultEntity>();
			
			// Compute all scores.
			Map<KBIdentifiedEntity, Double> entityScores = new HashMap<KBIdentifiedEntity, Double>();
			for (Entity entity : mention.getCandidateEntities()) { 
			  double sim = mes.calcSimilarity(mention, input_.getContext(), entity);
			  entityScores.put(entity.getKbIdentifiedEntity(), sim);
			}
			
			if (computeConfidence) {
  	    // Normalize similarities so that they sum up to one. The mass of the
        // score that the best entity accumulates will also be a measure of the
        // confidence that the mapping is correct.
			  entityScores = CollectionUtils.normalizeValuesToSum(entityScores);
			}
			
			// Create ResultEntities.
			for (Entry<KBIdentifiedEntity, Double> e : entityScores.entrySet()) {
			  entities.add(new ResultEntity(
			      e.getKey().getIdentifier(), e.getKey().getKnowledgebase(), e.getValue()));
			}
  			
	     // Distinguish a the cases of empty, unambiguous, and ambiguous mentions.
      if (entities.isEmpty()) {
        // Assume a 95% confidence, as the coverage of names of the dictionary
        // is quite good.
        ResultEntity re = ResultEntity.getNoMatchingEntity();
        if (computeConfidence) {
          re.setDisambiguationScore(0.95);
        }
        entities.add(re);
      } else if (entities.size() == 1 && computeConfidence) {
        // Do not give full confidence to unambiguous mentions, as there might
        // be meanings missing.
        entities.get(0).setDisambiguationScore(0.95);
      } 
			
			// Sort the candidates by their score.
			Collections.sort(entities);

			// Fill solutions.
			ResultMention rm = new ResultMention(chunkId, mention.getMention(), 
			    mention.getCharOffset(), mention.getCharLength());
			solutions.put(rm, entities);
		}
	}


}
