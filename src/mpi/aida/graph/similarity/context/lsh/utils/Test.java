package mpi.aida.graph.similarity.context.lsh.utils;

import java.util.HashSet;
import java.util.Set;

import mpi.aida.data.Entities;
import mpi.aida.data.Entity;
import mpi.aida.graph.similarity.context.WeightedKeyphrasesContext;
import mpi.aida.util.YagoUtil;


public class Test {
	public static void main(String args[]) throws Exception{
		Set<String> el = new HashSet<String>(Common.getContent("./data/entity_keyphrases/entities"));
		
	    Entities entities = new Entities();
	    
//	    int counter = 0;
	    for (String entity : el) {
//	    	if(++counter == 3)
//	    		break;
//	        String entity = entityUnNorm;
//	        System.out.println(entity);
	        entities.add(YagoUtil.getEntityForYagoId(entity));
	    }
	    
	    WeightedKeyphrasesContext wkc = new WeightedKeyphrasesContext(entities);
	    
//	    Entity a = null;
//	    Entity b = null;
	    
	    for(Entity entity: entities){
	    	if(entity.getIdentifierInKb().startsWith("Yahoo")){
	    		int[] kps = wkc.getEntityKeyphraseIds(entity);
		    	
		    	System.out.println(entity.getIdentifierInKb() + " " + kps.length);
		    	
		    	for(int i = 0; i < kps.length; i++){
		    		System.out.println(wkc.getKeyphraseForId(kps[i]));
		    		System.out.println(wkc.getCombinedKeyphraseMiIdfWeight(entity, kps[i]));
		    	}
		    	break;
	    	}
	    	
	    }
	    
//	    String kp = "Annotated google map of the earthquake area\\";
//	    System.out.println(kp);
//	    System.out.println(wkc.getCombinedKeyphraseMiIdfWeight("2005_Kashmir_earthquake", wkc.getIdForKeyphrase(kp)));
	    
//	    int[] kpsA = wkc.getEntityKeyphraseIds(a.getName());
//	    int[] kpsB = wkc.getEntityKeyphraseIds(b.getName());
	    
//	    String kpAname = wkc.getKeyphraseForId(kpsA[0]);
//	    
//	    double kpAweight = wkc.getCombinedKeyphraseMiIdfWeight(a.getName(), kpsA[0]);
		
		
		
//		FileInputStream fis = new FileInputStream("/var/tmp/aida/keyphrases_sorted.csv");
//	    InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
//	    BufferedReader bufReader = new BufferedReader(isr);
//
//	    String line;
//	    int counter = 0;
//	    boolean found = false;//Annotated google map of the earthquake area\
//	    while (true) {
//	      counter++;
//	      if (counter % 10000 == 0) {
//	        System.out.println(counter);
//	        // break;
//	      }
//
//	      line = bufReader.readLine();
//	      if (line == "" || line == null) break;
//
//	      String[] tmp = line.split(",");
//	      
//	     
//
//	      if(tmp[0].equalsIgnoreCase("2005_Kashmir_earthquake")){
//	    	  found = true;
//	    	  System.out.println(line);
//	      }
//	      else if(found)
//	    	  break;
//	    }
//
//	    isr.close();
//	    fis.close();
	}
}
