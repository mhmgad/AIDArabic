package mpi.aida.util.normalization;

/**
 * 
 * Use it for any language that doesn't require specific normalization
 *
 */
public class DummyNormalizer implements TextNormalizer {

  @Override
  public String normalize(String input) {
    return input;
  }

}
