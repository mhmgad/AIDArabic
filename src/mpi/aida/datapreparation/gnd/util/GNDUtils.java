package mpi.aida.datapreparation.gnd.util;


public class GNDUtils {
  
  public static final String GND_TYPE_PROP = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
  public static final String GND_PREFIX_PROP = "http://d-nb.info/standards/elementset/gnd#prefix";
  public static final String GND_PERSONAL_NAME_PROP = "http://d-nb.info/standards/elementset/gnd#personalName";
  public static final String GND_SURNAME_PROP = "http://d-nb.info/standards/elementset/gnd#surname";
  public static final String GND_FORENAME_PROP = "http://d-nb.info/standards/elementset/gnd#forename";
  public static final String GND_VARIANT_NAME_ENTITY_FOR_THE_PERSON_PROP = "http://d-nb.info/standards/elementset/gnd#variantNameEntityForThePerson";
  public static final String GND_PREFERRED_NAME_ENTITY_FOR_THE_PERSON_PROP = "http://d-nb.info/standards/elementset/gnd#preferredNameEntityForThePerson";
  public static final String GND_VARIANT_NAME_PROP = "http://d-nb.info/standards/elementset/gnd#variantName";
  public static final String GND_PREFERRED_NAME_PROP = "http://d-nb.info/standards/elementset/gnd#preferredName";
  
  public static String TYPE_DIFFERENTIATED_PERSON = "http://d-nb.info/standards/elementset/gnd#DifferentiatedPerson";

  public static String TYPE_UNDIFFERENTIATED_PERSON = "http://d-nb.info/standards/elementset/gnd#UndifferentiatedPerson";
  
  public static String getTitleDataFullURI(String id) {
    return "http://d-nb.info/" + id;
  }
  
  public static String getGNDDataFullURI(String id) {
    return "http://d-nb.info/gnd/" + id;
  }
  

}
