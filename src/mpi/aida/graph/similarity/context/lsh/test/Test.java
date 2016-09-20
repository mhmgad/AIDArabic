package mpi.aida.graph.similarity.context.lsh.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpi.aida.AidaManager;
import mpi.aida.access.DataAccess;
import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.graph.similarity.context.lsh.IntHashTable;
import mpi.aida.graph.similarity.context.lsh.utils.Common;
import mpi.aida.graph.similarity.context.lsh.utils.Config;


public class Test {
	/*
	 * method = 1 means using min-wise hashing.
	 * method = 2 means using lsh.
	 * method = 3 means clurstering keyphrases via lsh.
	 */
	public static int method;
	
//	private static String MIPs = "/home/datnb/workspace/LSH/data/keyphrases/entityMIPs_24";
	
	static String entityT = "Mac_Pro";
	static int idT;
	
	
	private static void testLSH() throws IOException{
		Map<Integer, String> idToEntity = new HashMap<Integer, String>();
		
		List<String> ens = Common
				.getContent("data/entity_keyphrases/entities");
		Entities entities = new Entities();
		for(String entityUnNorm: ens){
		  String entity = entityUnNorm.replace("\\\\", "\\");
//			String entity = entityUnNorm;
//			System.out.println(entity);
			
			
			Entity tmp = AidaManager.getEntity(new KBIdentifiedEntity(entity, "YAGO"));
			if(entity.equalsIgnoreCase(entityT))
				idT = tmp.getId();
			idToEntity.put(tmp.getId(), entity);
			
			entities.add(tmp);
		}
		    
    // get the entity-representation    
    IntHashTable hashTable = new IntHashTable(DataAccess.getEntityLSHSignatures(entities), Config.k, Config.l);
    
    // generate the LSH table from all entities in this context 
    hashTable.put(entities.getUniqueIds());
    
    //duplicate
    int ids[] = hashTable.deduplicate(idT).toArray();
    System.out.println("duplicate for " + entityT);
    for(int i = 0; i < ids.length; i++)
    	System.out.print(idToEntity.get(ids[i]) + "\t");
    System.out.println();
    
	}
	public static void main(String args[]) throws IOException{
//		testMinWiseHashing();
		
		testLSH();
		
	}
}
