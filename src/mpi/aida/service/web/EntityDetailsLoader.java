package mpi.aida.service.web;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.data.Type;
import mpi.aida.graph.similarity.measure.WeightComputation;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class EntityDetailsLoader {

  public static JSONArray loadKeyphrases(int entityId) {
		Map<String, JSONObject> keyphrases = new LinkedHashMap<String, JSONObject>();
		int totalCollectionSize = DataAccess.getCollectionSize();
		String sql = "select u.word keyphrase, kps.source source, wi.word keyword, kc.count "
				+ "from ( "
				+ "select w.word word, kp.source source, kt.token token_id, kp.weight weight "
				+ "from entity_keyphrases as kp, word_ids as w, keyphrase_tokens as kt "
				+ "where kp.entity = "
				+ entityId
				+ " and w.id = kp.keyphrase "
				+ " and kp.keyphrase = kt.keyphrase"
				+ " order by source "
				+ ") as u, word_ids as wi, keyword_counts as kc, keyphrase_sources as kps where u.token_id = wi.id and u.token_id = kc.keyword and u.source = kps.source_id";

		Connection con = null;
		Statement statement = null;
		try {
			con = AidaManager.getConnectionForDatabase(AidaManager.DB_AIDA);
			statement = con.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				String keyphrase = rs.getString("keyphrase");
				String source = rs.getString("source");
				String keyword = rs.getString("keyword");
				int keywordCount = rs.getInt("count");
				JSONObject kp = keyphrases.get(keyphrase);
				if (kp == null) {
					kp = new JSONObject();
					kp.put("keyphrase", keyphrase);
					kp.put("source", source);
					kp.put("keywordsWeights", new JSONArray());
					keyphrases.put(keyphrase, kp);
				}
				double idf = 0.0;
				if (keywordCount > 0) {
					idf = WeightComputation.log2((double) totalCollectionSize
							/ (double) keywordCount);
				}
				JSONArray keywordWeights = (JSONArray) kp
						.get("keywordsWeights");
				JSONObject keywordJson = new JSONObject();
				keywordJson.put("keyword", keyword);
				keywordJson.put("weight", idf);
				keywordWeights.add(keywordJson);
			}
			rs.close();
			statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			AidaManager.releaseConnection(con);
		}

		JSONArray keyphrasesJSONArray = new JSONArray();
		for (String kpName : keyphrases.keySet()) {
			keyphrasesJSONArray.add(keyphrases.get(kpName));
		}
		return keyphrasesJSONArray;
	}
	
	public static JSONArray loadEntityTypes(int entityId) {
	  int[] typesIds = DataAccess.getTypeIdsForEntityId(entityId);
	  TIntObjectHashMap<Type> entityTypes = DataAccess.getTypesForIds(typesIds);
		
		JSONArray entityTypesJson = new JSONArray();
		for(TIntObjectIterator<Type> itr = entityTypes.iterator(); itr.hasNext();) {
		  itr.advance();
		  Type type = itr.value();
			JSONObject entityType = new JSONObject();
			entityType.put("name", type.getName());
			entityType.put("knowledgebase", type.getKnowledgeBase());
			entityTypesJson.add(entityType);
		}
		
		return entityTypesJson;
	}
}
