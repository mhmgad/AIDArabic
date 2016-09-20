package mpi.aida.access;

import java.io.File;
import java.io.IOException;

import mpi.aida.config.DMapConfig;
import mpi.aida.protobufdmap.ProtobufDMapNotLoadedException;
import mpi.aida.protobufdmap.SqlToProtobufDMap;
import mpi.aida.protobufdmap.WrongProtoFileFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors;

import de.jhoff.dmap.DMap;

public class DataAccessDMapHandler {
  private static final Logger logger =
    LoggerFactory.getLogger(DataAccessDMapHandler.class);

  
  private static DataAccessDMapHandler ownInstance = new DataAccessDMapHandler();
  public static DataAccessDMapHandler singleton() {
    return ownInstance;
  }

  // holds the loaded maps
  private DMap[] loadedMaps;
  
  private DataAccessDMapHandler() {
    loadedMaps = new DMap[DatabaseDMap.values().length];
    for (DatabaseDMap databaseDMap : DatabaseDMap.values()) {
      try {
        loadDMap(databaseDMap);
      } catch (Descriptors.DescriptorValidationException | IOException | WrongProtoFileFormatException e) {
        logger.error("Could not load DMap: " + databaseDMap.getName() + " (" + e.getMessage() + ")");
      }
    }
  }

  /**
   * @param databaseDMap the DatabaseDMap
   * @return the DMap for the given name.
   * @throws ProtobufDMapNotLoadedException
   */
  public DMap getDMap(DatabaseDMap databaseDMap) throws ProtobufDMapNotLoadedException {
    DMap dMap = loadedMaps[databaseDMap.ordinal()];
    if (dMap==null) {
      throw new ProtobufDMapNotLoadedException();
    }
    return dMap;
  }
  
  private void loadDMap(DatabaseDMap databaseDMap) throws Descriptors.DescriptorValidationException, WrongProtoFileFormatException, IOException {
    if (!DMapConfig.shouldLoadMap(databaseDMap)) return;
    boolean shouldPreloadKeys = DMapConfig.shouldPreloadKeys(databaseDMap);
    boolean shouldPreloadValues = DMapConfig.shouldPreloadValues(databaseDMap);
    logger.info("Loading DMap: " + databaseDMap.getName() + " (cache keys: " + shouldPreloadKeys + ", cache values: " + shouldPreloadValues + ")");
    DMap.Builder dMapBuilder =
      new DMap.Builder(SqlToProtobufDMap.getFile(SqlToProtobufDMap.DMAP_FILETYPE,
        new File(DMapConfig.getDirectory(), databaseDMap.getName())));
    if (shouldPreloadKeys) dMapBuilder.preloadOffsets();
    if (shouldPreloadValues) dMapBuilder.preloadValues();
    loadedMaps[databaseDMap.ordinal()] = dMapBuilder.build();
    if (loadedMaps[databaseDMap.ordinal()].size() == 0)
      logger.warn("DMap '" + databaseDMap.getName() + "' loaded but empty.");
  }
}
