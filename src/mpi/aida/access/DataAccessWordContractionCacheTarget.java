package mpi.aida.access;

import java.io.File;


public class DataAccessWordContractionCacheTarget extends DataAccessIntIntCacheTarget {

  public static final String ID = "WORD_CONTRACTION";
  
  @Override
  public String getId() {
    return ID;
  }

  @Override
  protected File getCacheFile() {
    return new File("aida-word_contractions.cache");
  }

  @Override
  protected void loadFromDb() {
    data_ = DataAccess.getAllWordContractions();    
  }
}
