package mpi.aida.config.settings.preparation;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.preparation.mentionrecognition.MentionsDetector;

/**
 * Preparator setting that tokenizes the input text using the 
 * Stanford CoreNLP tokenizer. Mentions need to be marked up with square
 * bracktets. E.g.:
 * [[Einstein]] was born in [[Ulm]].
 */
public class ManualPreparationSettings extends PreparationSettings {

  private static final long serialVersionUID = 3743560957961384100L;

  public ManualPreparationSettings() {
    this.setMentionsDetectionType(MentionsDetector.type.MANUAL);
  }
}
