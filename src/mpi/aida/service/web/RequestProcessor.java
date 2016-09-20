package mpi.aida.service.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import mpi.aida.AidaManager;
import mpi.aida.Disambiguator;
import mpi.aida.access.DataAccess;
import mpi.aida.config.AidaConfig;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.JsonSettings.JSONTYPE;
import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.Settings.ALGORITHM;
import mpi.aida.config.settings.Settings.TECHNIQUE;
import mpi.aida.config.settings.disambiguation.*;
import mpi.aida.config.settings.preparation.StanfordHybridPreparationSettings;
import mpi.aida.data.DisambiguationResults;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.EntityMetaData;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.ResultProcessor;
import mpi.aida.data.Type;
import mpi.aida.graph.similarity.EntityEntitySimilarity;
import mpi.aida.graph.similarity.util.SimilaritySettings;
import mpi.aida.preparation.mentionrecognition.MentionsDetector.type;
import mpi.aida.preparator.Preparator;
import mpi.aida.service.web.logger.WebCallLogger;
import mpi.experiment.trace.GraphTracer;
import mpi.experiment.trace.GraphTracer.TracingTarget;
import mpi.experiment.trace.NullTracer;
import mpi.experiment.trace.Tracer;
import mpi.keyphraseextraction.KeyphraseExtractor;
import mpi.keyphraseextraction.NounPhrase;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class defining the HTTP interface to call the AIDA disambiugation. Call
 * /service/disambiguate-defaultsettings or /service/disambiguate to call the
 * actual disambiguation service. See the methods for the expected parameters.
 */

@Path("/service")
public class RequestProcessor {

	private static long processCount = 0l;

	static {
		AidaManager.init();
	}

	private static final Logger logger = LoggerFactory
			.getLogger(RequestProcessor.class);

	@Path("/status")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String status() {
		return "Processed " + processCount + " docs.";
	}

	/**
	 * Does not work anymore! Please use processWebRequest or processJSONWebRequest.
	 */
	@Path("/disambiguate-defaultsettings")
	@POST
	@Produces(MediaType.TEXT_PLAIN)
	@Deprecated
	public String process(
	    @Context HttpServletRequest req,
	    @FormParam("text") String text,
			@FormParam("tech") String technique,
			@FormParam("tag_mode") String mTagMode) {
		return "ERROR: This method has been removed. Please call '/disambiguate' instead.";
	}
	
	private String getCallerIp(HttpServletRequest req) {
	   String ip = req.getRemoteAddr();
	   // Make sure to get the actual IP of the requester if 
	   // the service works behind a gateway.
	   String forward = req.getHeader("X-Forwarded-For");
	   if (forward != null) {
	     ip = forward;
	   }
	   return ip;
	}
	
	@Path("/callerinfo")
	@GET
	@Produces(MediaType.TEXT_PLAIN)	
	public String getCallerInfo(@Context HttpServletRequest req) {
	  String remoteHost = req.getRemoteHost();
    String remoteAddr = req.getRemoteAddr();
    int remotePort = req.getRemotePort();
    String forward = req.getHeader("X-Forwarded-For");
    
    String msg = remoteHost + " (" + remoteAddr + ":" + remotePort + ") forward for " + forward;
    return msg;
	}
	
	/**
	 *
	 * Method to call the AIDA disambiguation service.
   * 
   * Input: HTTP POST request containing "application/json"
   * parameters specifying the settings and the input text. See below for expected
   * values. Output: JSON containing the disambiguation results.
   * <br />
   * A JSON input object should contain the following input parameters
   * <br /><br />
   * {<br />
   *  "text" : "...",<br />
   *  "inputType" : "...",<br />
   *  "tagMode" : "...",<br />
   *  "docId" : "...",<br />
   *  "technique" : "...",<br />
   *  "algorithm" : "...",<br />
   *  "coherenceMeasure" : "...",<br />
   *  "alpha" : "DOUBLE VALUE",<br />
   *  "ppWeight" : "DOUBLE VALUE",<br />
   *  "importanceWeight" : "DOUBLE VALUE",<br />
   *  "ambiguity" : "INTEGER VALUE",<br />
   *  "coherence" : "DOUBLE VALUE",<br />
   *  "isWebInterface" : "BOOLEAN VALUE",<br />
   *  "exhaustiveSearch" : "BOOLEAN VALUE",<br />
   *  "fastMode" : "BOOLEAN VALUE",<br />
   *  "filteringTypes" : "...",<br />
   *  "keyphrasesSourceWeightsStr" : "...",<br />
   *  "maxResults" : "INTEGER VALUE",<br />
   *  "nullMappingThreshold" : "DOUBLE VALUE", <br />
   *  "mentionDictionary" : Map<String, List<String>", <br />
   *  "entityKeyphrases" : Map<String, List<String>" <br />
   * }<br />
   * <br />
   * Where:<br />
   * 
   * <strong>text</strong>
   *            The input text to disambiguate<br />
   * <strong>type</strong>
   *            The type of the input text (TEXT, TABLE, XML - default is
   *            TEXT)<br />
   * <strong>tagMode</strong>
   *            Set to 'manual' to give AIDA pre-defined mentions as part of
   *            the text input (marked with [[..]])<br />
   * <strong>docId</strong>
   *            Specify the document id.<br />
   * <strong>technique</strong>
   *            The technique to use (LOCAL or GRAPH - default is LOCAL)<br />
   * <strong>algorithm</strong>
   *            Algorithm to use when GRAPH is set as tech. Default is CPSC
   *            (size constrained).<br />
   * <strong>coherenceMeasure</strong>
   *            Coherence measure to use when GRAPH is set as tech. Default is
   *            MilneWitten (alternatives are Jaccard or KORE). <br />
   * <strong>alpha</strong>
   *            alpha is multiplied to ME edges, 1-alpha to EE edges (a is in
   *            [0.0, 1.0])<br />
   * <strong>ppWeight</strong>
   *            Weight to balance the prior probability of mention-entity
   *            pairs and contextual similarity.<br />
   * <strong>ambiguity</strong>
   *            Number of candidates to use in the CPSC setting.<br />
   * <strong>coherence</strong>
   *            Threshold to use for the coherence robustness test (in [0.0,
   *            2.0])<br />
   * <strong>isWebInterface</strong> Set to true for the webaida demo.<br />
   * <strong>exhaustiveSearch</strong>
   *            Set to false to not do a exhaustive post-processing after the
   *            graph algorithm for selecting the correct entity. Default is
   *            true.<br />
   * <strong>fastMode</strong>
   *            Set to true to cut down on the number of keyphrases used per
   *            entity candidate, using only the most specific ones. Speeds up
   *            processing by a factor of 5 with little impact on quality.<br />
   * <strong>filteringTypes</strong>
   *            Semantic (YAGO) types to restrict the entity candidates to.
   *            Format: KB:typename,KB:typename,... <br />
   * <strong>keyphrasesSourceWeightsStr</strong>
   *            Set by the webaida demo.<br />
   * <strong>maxResults</strong>
   *            Number of entity candidates per mention to include in the
   *            returned JSON object.<br />
   * <strong>mentionDictionary</strong>
   *            Format: Map<String, List<String>>
   *            Map of Entity names and list of KB identifiers in format "KBIdentifier:EntityIdentifier"
   * <strong>entityKeyphrases</strong>
   *            Format: Map<String, List<String>>
   *            Map of KB identifiers in format "KBIdentifier:EntityIdentifier" and list of key phrases
   *                        
   * 
	 * @param req HTTP Servlet Request
	 * @return JSON object with the disambiguated text
	 * @throws Exception generic exception from the disambiguation 
	 * @author dmilchev
	 */
	
	 @POST
   @Path("/disambiguate")
	 @Produces(MediaType.APPLICATION_JSON)
   @Consumes(MediaType.APPLICATION_JSON)
   public JSONObject processJSONWebRequest(@Context HttpServletRequest req, String requestParam) throws Exception {

	   JSONParser jsonParser = new JSONParser();
	   JSONObject inputJson;
	   try {
	      inputJson = (JSONObject) jsonParser.parse(requestParam);
	   } catch (ParseException e) {
	     logger.error("Error parsing the input JSON object "+e.getMessage());
	     throw new ParseException(e.getErrorType());
	   }
	   
	   logger.info("Processing JSON request with input "+inputJson);
	   
	   String text = null; 
	   String inputType = null; 
	   String tagMode = null;
	   String docId = null;
	   String technique = null;
	   String algorithm = null;
	   String coherenceMeasure = null;
	   Double alpha = null;
	   Double ppWeight = null;
	   Double importanceWeight = null;
	   Integer ambiguity = null;
	   Double coherence = null;
	   Boolean isWebInterface = null;
	   Boolean exhaustiveSearch = null;
	   Boolean fastMode = null;
	   String filteringTypes = null;
	   String keyphrasesSourceWeightsStr = null;
	   Integer maxResults = null;
	   Double nullMappingThreshold = null;
		 String jsonType = null;
	   
	   if(inputJson.get("text")!=null){
	     text = inputJson.get("text").toString();
	   }
	   if(inputJson.get("inputType")!=null){
	     inputType = inputJson.get("inputType").toString();
     }
	   if(inputJson.get("tagMode")!=null){
	     tagMode = inputJson.get("tagMode").toString();
     }
	   if(inputJson.get("docId")!=null){
	     docId = inputJson.get("docId").toString();
     }
	   if(inputJson.get("technique")!=null){
	     technique = inputJson.get("technique").toString();
     }
	   if(inputJson.get("algorithm")!=null){
	     algorithm = inputJson.get("algorithm").toString();
     }
	   if(inputJson.get("coherenceMeasure")!=null){
	     coherenceMeasure = inputJson.get("coherenceMeasure").toString();
     }
	   if(inputJson.get("alpha")!=null){
	     alpha = Double.parseDouble(inputJson.get("alpha").toString());
     }
	   if(inputJson.get("ppWeight")!=null){
	     ppWeight = Double.parseDouble(inputJson.get("ppWeight").toString());
     }
	   if(inputJson.get("importanceWeight")!=null){
	     importanceWeight = Double.parseDouble(inputJson.get("importanceWeight").toString());
     }
	   if(inputJson.get("entitiesPerMention")!=null){
	     ambiguity = Integer.parseInt(inputJson.get("entitiesPerMention").toString());
     }
	   if(inputJson.get("coherenceTreshold")!=null){
	     coherence = Double.parseDouble(inputJson.get("coherenceTreshold").toString());
     }
	   if(inputJson.get("isWebInterface")!=null){
	     isWebInterface = Boolean.parseBoolean(inputJson.get("isWebInterface").toString());
     }
	   if(inputJson.get("exhaustiveSearch")!=null){
	     exhaustiveSearch = Boolean.parseBoolean(inputJson.get("exhaustiveSearch").toString());
     }
	   if(inputJson.get("fastMode")!=null){
	     fastMode = Boolean.parseBoolean(inputJson.get("fastMode").toString());
     }
	   if(inputJson.get("filteringTypes")!=null){
	     filteringTypes = inputJson.get("filteringTypes").toString();
     }
	   if(inputJson.get("keyphrasesSourceWeightsStr")!=null){
	     keyphrasesSourceWeightsStr = inputJson.get("keyphrasesSourceWeightsStr").toString();
     }
	   if(inputJson.get("maxResults")!=null){
	     maxResults = Integer.parseInt(inputJson.get("maxResults").toString());
     }
	   if(inputJson.get("nullMappingThreshold")!=null){
	     nullMappingThreshold = Double.parseDouble(inputJson.get("nullMappingThreshold").toString());
     }
		 if(inputJson.get("jsonType")!=null){
			 jsonType = inputJson.get("jsonType").toString();
		 }

	   ExternalEntitiesContext eec = null;
	   Map<String, List<KBIdentifiedEntity>> mentionEntityDictionary = new HashMap<>();
     Map<KBIdentifiedEntity, List<String>> entityKeyphrases = new HashMap<>();
     
     //Reading and casting the external entity context from the JSON object.
     //Get the mention dictionary
     if(inputJson.get("mentionDictionary")!=null){
	     JSONObject mentionDictionaryJSON =  (JSONObject) inputJson.get("mentionDictionary");
	     
	     //Check if can be cast to map
	     if(mentionDictionaryJSON instanceof Map){
	       //When its cast it need to be traversed and save to another object, because just casting its not working.
	       Map<String, List<String>> tmpDictionary = (Map<String, List<String>>)mentionDictionaryJSON;
	       for(Map.Entry<String, List<String>> entry : tmpDictionary.entrySet()){
	         
	         List<KBIdentifiedEntity> kbIds = new ArrayList<>();
	         for(String value : entry.getValue()){
	           kbIds.add(new KBIdentifiedEntity(value));
	         }
	         mentionEntityDictionary.put(entry.getKey(), kbIds);
	       }
	     }
	     
	   }
     //Get the entity keyphrases
	   if(inputJson.get("entityKeyphrases")!=null){
	     
	     JSONObject keyPhrasesJSON =  (JSONObject) inputJson.get("entityKeyphrases");
	     if(keyPhrasesJSON instanceof Map){
  	     Map<String, List<String>>  tmpPhrases =  (Map<String, List<String>>) keyPhrasesJSON;
  	     
  	     for(Entry<String, List<String>> entry : tmpPhrases.entrySet()){
  	       List<String> phrases = new ArrayList<>();
  	       
  	       KBIdentifiedEntity kbId = new KBIdentifiedEntity(entry.getKey());
  	       for(String value : entry.getValue()){
  	         phrases.add(value);
  	       }
  	       entityKeyphrases.put(kbId, phrases);
  	     }
	     }
	   }
	   
	   if(!mentionEntityDictionary.isEmpty() && !entityKeyphrases.isEmpty()){
	     eec = new ExternalEntitiesContext(mentionEntityDictionary, entityKeyphrases);
	   }
	   

	   //Process the request and return the result
	   JSONObject result = processRequest(
	       text,
	       inputType,
	       tagMode,
	       docId,
	       technique,
	       algorithm,
	       coherenceMeasure,
	       alpha,
	       ppWeight,
	       importanceWeight,
	       ambiguity,
	       coherence,
	       isWebInterface,
	       exhaustiveSearch,
	       fastMode,
	       filteringTypes,
	       keyphrasesSourceWeightsStr,
	       maxResults,
	       nullMappingThreshold,
	       getCallerIp(req),
				 jsonType,
	       eec
	       );
	   //JSONObject result = new JSONObject();
	   return result;
   }

	/**
	 * Method to call the AIDA disambiguation service.
	 * 
	 * Input: HTTP POST request containing application/x-www-form-urlencoded
	 * parameters specifying the settings and input text. See below for expected
	 * values. Output: JSON containing the disambiguation results.
	 * 
	 * @param text
	 *            The input text to disambiguate
	 * @param inputType
	 *            The type of the input text (TEXT, TABLE, XML - default is
	 *            TEXT)
	 * @param tagMode
	 *            Set to 'manual' to give AIDA pre-defined mentions as part of
	 *            the text input (marked with [[..]])
	 * @param docId
	 *            Specify the document id.
	 * @param technique
	 *            The technique to use (LOCAL or GRAPH - default is LOCAL)
	 * @param algorithm
	 *            Algorithm to use when GRAPH is set as tech. Default is CPSC
	 *            (size constrained).
	 * @param coherenceMeasure
   *            Coherence measure to use when GRAPH is set as tech. Default is
   *            MilneWitten (alternatives are Jaccard or KORE). 
	 * @param alpha
	 *            alpha is multiplied to ME edges, 1-alpha to EE edges (a is in
	 *            [0.0, 1.0])
	 * @param ppWeight
	 *            Weight to balance the prior probability of mention-entity
	 *            pairs and contextual similarity.
	 * @param entitiesPerMention
	 *            Number of candidates to use in the CPSC setting.
	 * @param coherenceThreshold
	 *            Threshold to use for the coherence robustness test (in [0.0,
	 *            2.0])
	 * @param isWebInterface Set to true for the webaida demo.
	 * @param exhaustive_search
	 *            Set to false to not do a exhaustive post-processing after the
	 *            graph algorithm for selecting the correct entity. Default is
	 *            true.
	 * @param fastMode
	 *            Set to true to cut down on the number of keyphrases used per
	 *            entity candidate, using only the most specific ones. Speeds up
	 *            processing by a factor of 5 with little impact on quality.
	 * @param filteringTypes
	 *            Semantic (YAGO) types to restrict the entity candidates to.
	 *            Format: KB:typename,KB:typename,...
	 * @param keyphrasesSourceWeightsStr
	 *            Set by the webaida demo.
	 * @param maxResults
	 *            Number of entity candidates per mention to include in the
	 *            returned JSON object.
	 * @param jsonType
	 * 						Specifies the data that is returned in the JSON object.
	 * 					  @see mpi.aida.data.ResultProcessor for possible options.
	 */
	@Path("/disambiguate")
	@POST
	@Produces(MediaType.TEXT_PLAIN)
	public String processWebRequest(
	    @Context HttpServletRequest req,
			@FormParam("text") String text,
			@FormParam("type") String inputType,
			@FormParam("tag_mode") String tagMode,
			@FormParam("doc_id") String docId,
			@FormParam("tech") String technique,
			@FormParam("algo") String algorithm,
			@FormParam("coh_measure") String coherenceMeasure,
			@FormParam("alpha") Double alpha,
			@FormParam("ppWeight") Double ppWeight,
			@FormParam("importance_weight") Double importanceWeight,
			@FormParam("entities_per_mention") Integer entitiesPerMention,
			@FormParam("coherence_treshold") Double coherenceThreshold,
			@FormParam("interface") Boolean isWebInterface,
			@FormParam("exhaustive_search") Boolean exhaustive_search,
			@FormParam("fast_mode") Boolean fastMode,
			@FormParam("filtering_types") String filteringTypes,
			@FormParam("keyphrasesSourceWeightsStr") String keyphrasesSourceWeightsStr,
			@FormParam("maxResults") Integer maxResults,
			@FormParam("nullMappingThreshold") Double nullMappingThreshold,
			@FormParam("jsonType") String jsonType)
			throws Exception {

	  //Process the request and return the result
    JSONObject result = processRequest(
        text,
        inputType,
        tagMode,
        docId,
        technique,
        algorithm,
        coherenceMeasure,
        alpha,
        ppWeight,
        importanceWeight,
        entitiesPerMention,
        coherenceThreshold,
        isWebInterface,
        exhaustive_search,
        fastMode,
        filteringTypes,
        keyphrasesSourceWeightsStr,
        maxResults,
        nullMappingThreshold,
        getCallerIp(req),
				jsonType,
        null);
    
		return result.toJSONString();
	}

	@SuppressWarnings("unchecked")
	@Path("/loadKeyphraseWeights")
	@POST
	@Produces(MediaType.TEXT_PLAIN)
	public String processSettingsRequest() {
		// this will load settings from DB
		Map<String, Double> hshWeights = DataAccess.getKeyphraseSourceWeights();
		JSONObject jObj = new JSONObject();
		for (Entry<String, Double> e : hshWeights.entrySet()) {
			jObj.put(e.getKey(), e.getValue());
		}

		return jObj.toJSONString();
	}

	 @SuppressWarnings("unchecked")
	@Path("/loadEntityMetaData")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject loadEntityMetaData(@FormParam("entity") Integer entity) {
	  EntityMetaData entityMetaData = DataAccess.getEntityMetaData(entity);
    double importance = DataAccess.getEntityImportance(entity);
    JSONObject jObj = new JSONObject();
    jObj.put("readableForm",
        entityMetaData.getHumanReadableRepresentation());
    jObj.put("url", entityMetaData.getUrl());
    jObj.put("importance", importance);
    jObj.put("knowledgebase", entityMetaData.getKnowledgebase());
    jObj.put("depictionurl", entityMetaData.getDepictionurl());
    jObj.put("description", entityMetaData.getDescription());
    jObj.put("depictionthumbnailurl",
        entityMetaData.getDepictionthumbnailurl());
    
    return jObj;
	}
	

	// for the webaida entity.jsp page
	@Path("/loadKeyphrases")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JSONArray loadKeyphrases(@FormParam("entity") Integer entity) {
		JSONArray keyphrasesJsonArray = EntityDetailsLoader.loadKeyphrases(entity);
		// JSONArray entityTypesJsonArray =

		return keyphrasesJsonArray;
	}
	
	
   
	/**
	 * Extracts list of NounPhrases (keyphrases) from a given text
	 * 
	 * @param req
	 * @param text
	 * @return JSON representation of the list of NounPhrases
	 * @throws Exception
	 */
	@POST
	@Path("/extractKeyphrases")
	@Produces(MediaType.APPLICATION_JSON)
	public List<NounPhrase> extractKeyphrases(@Context HttpServletRequest req, @FormParam("text") String text) throws Exception {
	 
	  KeyphraseExtractor kpe = new KeyphraseExtractor();
	  List<NounPhrase> phrases = kpe.findKeyphrases(text);
	  
	  return phrases;
	}

	// for the webaida keyphrases.jsp page
	@Path("/loadTypes")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JSONArray loadTypes(@FormParam("entity") int entity) {
		JSONArray typesJsonArray = EntityDetailsLoader.loadEntityTypes(entity);
		return typesJsonArray;
	}

	
	@Path("/entityKbId2Id")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String entityKbId2Id(@FormParam("kbId") String kbId) {
	  
    KBIdentifiedEntity kbIdEntity = new KBIdentifiedEntity(kbId);
    Entity e = AidaManager.getEntity(kbIdEntity);
    
    return String.valueOf(e.getId());
  }
	
	@SuppressWarnings("unchecked")
	@Path("/computeMilneWittenRelatedness")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject computeRelatedness(
	    @Context HttpServletRequest req,
	    @FormParam("source") List<String> sources,
			@FormParam("target") List<String> targets) {
	  long start = System.currentTimeMillis();
	  
		Set<KBIdentifiedEntity> sourceIds = new HashSet<>(sources.size());
		for (String s : sources) {
			sourceIds.add(new KBIdentifiedEntity(s));
		}
		Set<KBIdentifiedEntity> targetIds = new HashSet<>(targets.size());
		for (String t : targets) {
			targetIds.add(new KBIdentifiedEntity(t));
		}
		Entities sourceEntities = AidaManager.getEntities(sourceIds);
		Entities targetEntities = AidaManager.getEntities(targetIds);
		Entities allEntities = new Entities();
		allEntities.addAll(sourceEntities);
		allEntities.addAll(targetEntities);

		EntityEntitySimilarity eeSim = null;
		try {
			eeSim = EntityEntitySimilarity.getMilneWittenSimilarity(
					allEntities, new NullTracer());
		} catch (Exception e1) {
			JSONObject error = new JSONObject();
			error.put("Error", "Error creating EE-Similarity processor.");
			return error;
		}

		JSONObject result = new JSONObject();

		int count = 0;

		for (Entity s : sourceEntities) {
			JSONObject sObject = new JSONObject();
			for (Entity t : targetEntities) {
				try {
					double d = eeSim.calcSimilarity(s, t);
					sObject.put(t.getKbIdentifiedEntity().getDictionaryKey(), d);

					if (++count % 100_000 == 0) {
						logger.debug("Computed " + count + " relatedness scores.");
					}
				} catch (Exception e1) {
					JSONObject error = new JSONObject();
					error.put("Error", "Error computing similarity.");
					return error;
				}
			}
			result.put(s.getKbIdentifiedEntity().getDictionaryKey(), sObject);
		}

		long dur = System.currentTimeMillis() - start;
		
    StringBuilder sb = new StringBuilder();
    sb.append("RELATEDNESS ");
    sb.append(sources.size()).append(" ");
    sb.append(targets.size()).append(" ");
    sb.append(dur).append("ms");
    logger.info(sb.toString());

		return result;
	}

	private void adjustSimSettingsForNewPriorWeight(
			SimilaritySettings simSettings, double newPriorWeight) {
		double oldPriorWeight = simSettings.getPriorWeight();
		if (oldPriorWeight == 1)
			return;
		List<SimilaritySettings.MentionEntitySimilarityRaw> mentionEntitySims = simSettings
				.getMentionEntitySimilaritiesWithPrior();
		for (SimilaritySettings.MentionEntitySimilarityRaw sim : mentionEntitySims) {
			double oldWeight = sim.getWeight();
			double newWeight = oldWeight
					* ((1 - newPriorWeight) / (1 - oldPriorWeight));
      sim.setWeight(newWeight);
		}
		simSettings.setMentionEntitySimilaritiesWithPrior(mentionEntitySims);
		simSettings.setPriorWeight(newPriorWeight);
	}

	@SuppressWarnings("unused")
	private void adjustSimSettingsForNewImportanceWeight(
			SimilaritySettings simSettings, double newImportanceWeight) {
    { // adjust weights with prior 
      // this works only assuming there is one entity importance source
      List<SimilaritySettings.EntityImportancesRaw> importancesSettings = simSettings
        .getEntityImportancesWithPrior();
      if (importancesSettings == null) {// the measure has no importances
        // settings
        return;
      }
      if (importancesSettings.size() != 1) {
        logger.error("SimilaritySettings has more than one entity imporance source"
          + ", cannot readjust the weights!");
        throw new IllegalArgumentException(
          "Mutliple Importance, cannot readjust the weights");
      }

      double oldImportanceWeight = importancesSettings.get(0).getWeight();
      if (oldImportanceWeight == 1)
        return;
      importancesSettings.get(0).setWeight(newImportanceWeight);

      List<SimilaritySettings.MentionEntitySimilarityRaw> mentionEntitySims = simSettings
        .getMentionEntitySimilaritiesWithPrior();
      for (SimilaritySettings.MentionEntitySimilarityRaw sim : mentionEntitySims) {
        double oldWeight = sim.getWeight();
        double newWeight = oldWeight
          * ((1 - newImportanceWeight) / (1 - oldImportanceWeight));
        sim.setWeight(newWeight);
      }
      simSettings.setMentionEntitySimilaritiesWithPrior(mentionEntitySims);

      double oldPriorWeight = simSettings.getPriorWeight();
      double newPriorWeight = oldPriorWeight
        * ((1 - newImportanceWeight) / (1 - oldImportanceWeight));
      simSettings.setPriorWeight(newPriorWeight);
    }
    
    { // adjust weights no prior
      // this works only assuming there is one entity importance source
      List<SimilaritySettings.EntityImportancesRaw> importancesSettings = simSettings
        .getEntityImportancesNoPrior();
      if (importancesSettings == null) {// the measure has no importances
        // settings
        return;
      }
      if (importancesSettings.size() != 1) {
        logger.error("SimilaritySettings has more than one entity imporance source"
          + ", cannot readjust the weights!");
        throw new IllegalArgumentException(
          "Mutliple Importance, cannot readjust the weights");
      }

      double oldImportanceWeight = importancesSettings.get(0).getWeight();
      if (oldImportanceWeight == 1)
        return;
      importancesSettings.get(0).setWeight(newImportanceWeight);

      List<SimilaritySettings.MentionEntitySimilarityRaw> mentionEntitySims = simSettings
        .getMentionEntitySimilaritiesNoPrior();
      for (SimilaritySettings.MentionEntitySimilarityRaw sim : mentionEntitySims) {
        double oldWeight = sim.getWeight();
        double newWeight = oldWeight
          * ((1 - newImportanceWeight) / (1 - oldImportanceWeight));
        sim.setWeight(newWeight);
      }
      simSettings.setMentionEntitySimilaritiesNoPrior(mentionEntitySims);
    }
	}

	// format of the string source1:weight1,source2:weight2, ....
	private List<String[]> buildKeyphrasesSourcesWeights(String allWeightsString) {
		List<String[]> keyphrasesSourcesWeights = new LinkedList<String[]>();
		for (String entry : allWeightsString.split(",")) {
			int lastIndexOfColon = entry.lastIndexOf(":");
			String source = entry.substring(0, lastIndexOfColon);
			String weight = entry.substring(lastIndexOfColon + 1);

			keyphrasesSourcesWeights.add(new String[] { source, weight });
		}

		return keyphrasesSourcesWeights;
	}

	public static synchronized void incrememtProcessCount() {
		++processCount;
	}
	 
	 private JSONObject processRequest(
       String   text,
       String   inputType,
       String   tagMode,
       String   docId,
       String   technique,
       String   algorithm,
       String   coherenceMeasure,
       Double   alpha,
       Double   ppWeight,
       Double   importanceWeight,
       Integer  ambiguity,
       Double   coherence,
       Boolean  isWebInterface,
       Boolean  exhaustiveSearch,
       Boolean  fastMode,
       String   filteringTypes,
       String   keyphrasesSourceWeightsStr,
       Integer  maxResults,
       Double   nullMappingThreshold,
       String   IP,
			 String   jsonType,
       ExternalEntitiesContext eec
       ) throws Exception{
     long time = System.currentTimeMillis();

      // 2. generate preparedSettings and set all required parameters
      // including filter types
      PreparationSettings prepSettings;

      if (tagMode != null) {
        if (tagMode.equals("stanfordNER")) {
          prepSettings = new StanfordHybridPreparationSettings();
        //} else if (tagMode.equals("dictionary")) {
          //TODO: This is now configured in "ner.properties", 
          // and cannot be changed in the run time
          //Check NERManager for details
          //prepSettings = new DictionaryBasedNerPreparationSettings();
        } else { // if (tagMode.equals("manual"))
          prepSettings = new PreparationSettings();
          prepSettings.setMentionsDetectionType(type.MANUAL);
        }
      } else {
        // default to stanfordNER if tag mode not used
        prepSettings = new StanfordHybridPreparationSettings();
      }

      if (filteringTypes != null && !filteringTypes.equals("")) {
        String[] typesStrings;
        typesStrings = filteringTypes.split(",");
        Type[] types = new Type[typesStrings.length];
        int count = 0;
        for (String typeString : typesStrings) {
          int colonIndex = typeString.indexOf(":");
          String kb = typeString.substring(0, colonIndex);
          String typeName = typeString.substring(colonIndex + 1);
          types[count++] = new Type(kb, typeName);
        }

        prepSettings.setFilteringTypes(types);

      } else {
        prepSettings.setFilteringTypes(null);
      }

      // 3a. generate disambiguateSettings and define all the parameters
      // The default is to do nil-thresholding.
      DisambiguationSettings disSettings = new CocktailPartyWithHeuristicsDisambiguationWithNullSettings();
      if (technique != null) {
        if (technique.equals("PRIOR")) {
          disSettings = new PriorOnlyDisambiguationSettings();
        } else if (technique.equals("LOCAL")) {
          if (fastMode != null && fastMode) {
            disSettings = new FastLocalKeyphraseBasedDisambiguationSettings();
          } else {
            disSettings = new LocalKeyphraseBasedDisambiguationSettings();
          }
        } else if (technique.equals("LOCAL-IDF")) {
          disSettings = new LocalKeyphraseIDFBasedDisambiguationSettings();
        } else if (technique.equals("LM")) {
          disSettings = new CocktailPartyLangaugeModelDefaultDisambiguationSettings();
        } else if (technique.equals("GRAPH")) {
          if (fastMode != null && fastMode) {
						disSettings = new FastCocktailPartyDisambiguationSettings();
					} else {
						disSettings = new CocktailPartyDisambiguationSettings();
					}
          if (algorithm != null) {
            if (algorithm.equalsIgnoreCase("cpsc")) {
							disSettings
											.setDisambiguationAlgorithm(ALGORITHM.COCKTAIL_PARTY_SIZE_CONSTRAINED);
						}
            // else if(algorithm.equalsIgnoreCase("random"))
            // disSettings.setDisambiguationAlgorithm(ALGORITHM.RANDOM_WALK);
          }       
        } else if (technique.equals("GRAPH-IDF")) {
          disSettings = new CocktailPartyKOREIDFDisambiguationSettings();
        } else if (technique.equals("GRAPH-KORE")) {
          disSettings = new CocktailPartyKOREDisambiguationSettings();
        } else if (technique.equals("GRAPH-JACCARD")) {
          disSettings = new CocktailPartyJaccardDisambiguationSettings();
        } else {
          // TODO return something that makes sense.. like a json with error
          // code
          JSONObject json = new JSONObject();
          json.put("ERROR", "Please specify a valid technique. Valid technuques are: LOCAL, LOCAL-IDF, GRAPH, GRAPH-IDF, GRAPH-KORE, GRAPH-KORELSH, GRAPH-JACCARD.");
          return json;
        }
      }
      
      // Update coherence measure if it is set.
      if (coherenceMeasure != null) {
        String cohMeasureId;
        switch (coherenceMeasure) {
          case "MilneWitten":
            cohMeasureId = "MilneWittenEntityEntitySimilarity";
            break;
          case "Jaccard":
            cohMeasureId = "InlinkOverlapEntityEntitySimilarity";
            break;
          case "KORE":
            cohMeasureId = "KOREEntityEntitySimilarity";
            break;
          default:
            cohMeasureId = "MilneWittenEntityEntitySimilarity";
            break;
        }
        List<String[]> cohConfigs = new LinkedList<String[]>();
        cohConfigs.add(new String[] { cohMeasureId, "1.0" });
        disSettings.getSimilaritySettings().setEntityEntitySimilarities(cohConfigs);
      }
      
      // 3b. set disambiguation parameters
     if (alpha != null) {
				disSettings.getGraphSettings().setAlpha(alpha);
      }
		  if (ambiguity != null) {
				disSettings.getGraphSettings().setEntitiesPerMentionConstraint(
								ambiguity);
			}
		  if (coherence != null) {
				disSettings.getGraphSettings().setCohRobustnessThreshold(coherence);
			}
      Tracer tracer = null;
		  if (isWebInterface == null) {
				isWebInterface = false;
			}
      if (isWebInterface) {
        disSettings.setTracingTarget(TracingTarget.WEB_INTERFACE);
        GraphTracer.gTracer = new GraphTracer();
        if (technique.equals("PRIOR") || technique.equals("LOCAL")
            || technique.equals("LOCAL-IDF")) {
          tracer = new Tracer(docId);
        } else {
          tracer = new NullTracer();
        }
      } else {
        tracer = new NullTracer();
      }
      // 4a. make sure to update similarity settings with prior weight and/or
      // importance weight
      SimilaritySettings simSettings = disSettings.getSimilaritySettings();
		  if (ppWeight == null) {
				ppWeight = simSettings.getPriorWeight();
			}
      if (disSettings.getDisambiguationTechnique() == TECHNIQUE.GRAPH) {
				adjustSimSettingsForNewPriorWeight(simSettings, ppWeight);
			}

      if (importanceWeight != null) {
        // TODO mamir,jhoffart: switched off for now, need to get it back
        // and adjust it work for
        // multiple importances if needed
        // adjustSimSettingsForNewImportanceWeight(simSettings,
        // importanceWeight);
      }

		  if (exhaustiveSearch != null) {
				disSettings.getGraphSettings()
								.setUseExhaustiveSearch(exhaustiveSearch);
			}
      if (nullMappingThreshold != null) {
        disSettings.setNullMappingThreshold(nullMappingThreshold);
      }

      // 4b. set keyphrases sources weights passed from the UI
      if (keyphrasesSourceWeightsStr != null
          && !keyphrasesSourceWeightsStr.equals("")) {
        List<String[]> mentionEntityKeyphraseSourceWeightsList = buildKeyphrasesSourcesWeights(keyphrasesSourceWeightsStr);
        simSettings
            .setMentionEntityKeyphraseSourceWeights(mentionEntityKeyphraseSourceWeightsList);
      }

      // 4c. prepare input
      if (docId == null) {
        docId = text.hashCode() + "_" + System.currentTimeMillis();
      }

		 if(eec == null){
			 eec = new ExternalEntitiesContext();
		 }

      Preparator p = new Preparator();
      PreparedInput preInput = p.prepare(docId, text, prepSettings, eec);

      // 5. disambiguator instantiated and run disambiguate
      Disambiguator disambiguator = new Disambiguator(preInput, disSettings, tracer, eec);
      DisambiguationResults results = disambiguator.disambiguate();
      incrememtProcessCount();

      // 6. resultprocessor instantiated and json retrieved from results
      // based on the interface json returned will have HTML string / JSON
      // repr of Disambi Results

      int maxNum = 15;
      if (maxResults != null) {
        maxNum = maxResults;
      }
      ResultProcessor rp = new ResultProcessor(results, null, preInput,
          maxNum);
      long duration = System.currentTimeMillis() - time;
      rp.setOverallTime(duration);
		 JSONTYPE resultType = JSONTYPE.DEFAULT;
      JSONObject json;
      if (isWebInterface) {
				resultType = JSONTYPE.WEB;
      } else if (jsonType != null) {
			 	resultType = JSONTYPE.valueOf(jsonType);
      }
		  json = rp.process(resultType);
      
      // log request details
		  if (IP == null) {
				IP = "127.0.0.1";
			}
      RequestLogger.logProcess(IP, preInput, prepSettings.getClass().getName(), disSettings.getDisambiguationTechnique(),
					disSettings.getDisambiguationAlgorithm(), duration);
		  if (AidaConfig.getBoolean(AidaConfig.LOG_WEB_CALLS)) {
				WebCallLogger.log(text, json.toJSONString(), prepSettings.getClass().getName(), technique, algorithm);
			}
      return json;
   }
}