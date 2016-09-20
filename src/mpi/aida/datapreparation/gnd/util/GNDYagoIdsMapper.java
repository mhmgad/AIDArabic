package mpi.aida.datapreparation.gnd.util;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mpi.aida.access.DataAccess;
import mpi.aida.data.KBIdentifiedEntity;
import mpi.aida.datapreparation.util.YagoIdsMapper;
import mpi.aida.util.ClassPathUtils;
import mpi.tools.basics2.Normalize;

public class GNDYagoIdsMapper implements YagoIdsMapper {

  private String mappingFileLocation = "gnd/dewiki_list.txt";

  private static GNDYagoIdsMapper instance = null;

  private Map<String, String> gndYagoIdsMap;

  private Map<String, String> yagoGNDIdsMap;
  
  private TIntIntHashMap gndYagoInternalIdsMap = null;
  
  private TIntIntHashMap yagoGNDInternalIdsMap = null;
  
  private static final String YAGO = "YAGO";
  private static final String GND = "GND";

  private GNDYagoIdsMapper() {
    
  }
  
  public static synchronized GNDYagoIdsMapper getInstance(boolean loadInternalIds) {
    if (instance == null || (loadInternalIds && !instance.isInternalIdsLoaded()) ) {
      instance = new GNDYagoIdsMapper();
      instance.init(loadInternalIds);
    } 
    return instance;
  }
  
  private boolean isInternalIdsLoaded() {
    return gndYagoInternalIdsMap != null;
  }

  private void init(boolean loadInternalIds) {
    Set<KBIdentifiedEntity> allEntities = new HashSet<KBIdentifiedEntity>();
    yagoGNDIdsMap = new HashMap<String, String>();
    gndYagoIdsMap = new HashMap<String, String>();

    
    try {
      List<String> lines = ClassPathUtils.getContent(mappingFileLocation);

      for (String line : lines) {
        String[] lineParts = line.split("\\|");
        String yagoId = lineParts[0];
        yagoId = Normalize.entity(yagoId);
        String gndId = lineParts[1];
        gndId = GNDUtils.getGNDDataFullURI(gndId);
        
        yagoGNDIdsMap.put(yagoId, gndId);
        gndYagoIdsMap.put(gndId, yagoId);
        allEntities.add(new KBIdentifiedEntity(yagoId, YAGO));
        allEntities.add(new KBIdentifiedEntity(gndId, GND));
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    if(loadInternalIds) {
      //load the ids from the DB
      TObjectIntHashMap<KBIdentifiedEntity> allEntitiesIds = 
          DataAccess.getInternalIdsForKBEntities(allEntities);
      
      yagoGNDInternalIdsMap = new TIntIntHashMap();
      gndYagoInternalIdsMap = new TIntIntHashMap();
      
      for(Entry<String, String> entry: yagoGNDIdsMap.entrySet()) {
        KBIdentifiedEntity yagoKBEntity = new KBIdentifiedEntity(entry.getKey(), YAGO);
        int yagoInternalId = allEntitiesIds.get(yagoKBEntity);
        
        KBIdentifiedEntity gndKBEntity = new KBIdentifiedEntity(entry.getValue(), GND);
        int gndInternalId = allEntitiesIds.get(gndKBEntity);
        
        yagoGNDInternalIdsMap.put(yagoInternalId, gndInternalId);
        gndYagoInternalIdsMap.put(gndInternalId, yagoInternalId);
      }
    }
    
  }
  
  @Override
  public String mapToYagoId(String gndId) {
    return getYagoId(gndId);
  }

  @Override
  public String mapFromYagoId(String yagoId) {
    return getGNDId(yagoId);
  }

  public String getYagoId(String gndId) {
    return gndYagoIdsMap.get(gndId);
  }

  public String getGNDId(String yagoId) {
    return yagoGNDIdsMap.get(yagoId);
  }

  public Set<String> getYagoEntitiesIdsSetForGNDEntities() {
    return yagoGNDIdsMap.keySet();
  }


  @Override
  public int mapToYagoId(int otherKBInternalId) {
    return gndYagoInternalIdsMap.get(otherKBInternalId);
  }

  @Override
  public int mapFromYagoId(int yagoInernalId) {
    return yagoGNDInternalIdsMap.get(yagoInernalId);
  }
  
  public static void main(String[] args) {
    YagoIdsMapper mapper = GNDYagoIdsMapper.getInstance(false);
    System.out.println(mapper.mapFromYagoId("Einsteinturm"));
    System.out.println(mapper.mapToYagoId("http://d-nb.info/gnd/10271049X"));
    System.out.println(mapper.mapToYagoId("http://d-nb.info/gnd/16076279-0"));
    
    mapper = GNDYagoIdsMapper.getInstance(true);
    System.out.println(mapper.mapFromYagoId(5));
    System.out.println(mapper.mapToYagoId(5));
  }



}
