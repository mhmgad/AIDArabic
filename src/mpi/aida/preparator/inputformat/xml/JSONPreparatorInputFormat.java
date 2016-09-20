package mpi.aida.preparator.inputformat.xml;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInput;
import mpi.aida.preparator.Preparator;
import mpi.aida.preparator.inputformat.PreparatorInputFormatInterface;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class JSONPreparatorInputFormat implements PreparatorInputFormatInterface {

  private JSONParser parser = new JSONParser();

  @Override
  public PreparedInput prepare(String docId, String content, PreparationSettings prepSettings, ExternalEntitiesContext externalContext)
      throws Exception {
    JSONObject json = (JSONObject) parser.parse(content);
    String text = (String)json.get(prepSettings.getDocumentField());
    String documentId = (String)json.get(prepSettings.getDocumentId());
    String title = (String)json.get(prepSettings.getDocumentTitle());
    
    PreparedInput pInp = Preparator.prepareInputData(text, documentId, externalContext, prepSettings);
    pInp.setTitle(title);
    
    return pInp;
  }
}