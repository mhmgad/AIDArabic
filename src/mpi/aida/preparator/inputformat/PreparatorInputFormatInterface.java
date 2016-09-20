package mpi.aida.preparator.inputformat;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInput;


public interface PreparatorInputFormatInterface {
  /**
   * Public prepare method that needs to be overridden in the sub-classes. Returns prepared input for the given text.
   * 
   * @param docId The text to be disambiguated
   * @param text The document id associated with the text
   * @param prepSettings  Preparation settings
   * @return  PreparedInput instance.
   * @throws Exception
   */
  public PreparedInput prepare(String docId, String text, 
      PreparationSettings prepSettings, ExternalEntitiesContext externalContext) throws Exception;

}
