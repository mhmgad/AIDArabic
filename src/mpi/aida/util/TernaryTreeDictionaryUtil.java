package mpi.aida.util;

import mpi.aida.AidaManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class TernaryTreeDictionaryUtil {

  public static File getDefaultDictionaryDirectory() throws IOException {
    String dbId = null;
    try {
      dbId = AidaManager.getAidaDbIdentifier();
    } catch (SQLException e) {
      throw new IOException();
    }
    File outDir = new File("data/dictionary/ternary_tree_dictionary_" + dbId);
    return outDir;
  }
}
