package mpi.aida.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Mentions implements Serializable {

  private static final long serialVersionUID = -383105468450056989L;

  private List<Mention> mentions = null;

  private HashMap<Integer, Integer> subStrings = null;
    
  /**
   * The expected types for entities to which those mentions will be disambiguated
   */
  private Set<Type> entitiesTypes = null;
  
  public Mentions() {
    mentions = new LinkedList<Mention>();
  }

  public boolean containsOffset(int offset) {
    for (Mention mention : mentions) {
      if (mention.getCharOffset() == offset) {
        return true;
      }
    }
    return false;
  }

  public Mention getMentionForOffset(int offset) {
    for (Mention mention : mentions) {
      if (mention.getCharOffset() == offset) {
        return mention;
      }
    }
    return null;
  }

  public void addMention(Mention mention) {
    mentions.add(mention);
  }

  public List<Mention> getMentions() {
    return mentions;
  }
  
  public List<String> getMentionNames() {
    List<String> names = new ArrayList<String>(mentions.size());
    for (Mention m : mentions) {
      names.add(m.getMention());
    }
    return names;
  }
  
  public ArrayList<Integer> getMentionTokenStanfordIndices()
  {
	  ArrayList<Integer> mentionTokenIndices = new ArrayList<Integer>();
	  // there's just one
	  for (Mention mention : mentions)
	  {
		  for (int i=mention.getStartStanford();i<=mention.getEndStanford();i++)
			  mentionTokenIndices.add(i);
	  }
	  return mentionTokenIndices;
  }
  
  public int getMentionTokenSentenceIndex()
  {
	  // there's just one
	  return mentions.get(0).getSentenceId();
  }

  public boolean remove(Mention mention) {
    return mentions.remove(mention);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(200);
    for (int i = 0; i < mentions.size(); i++) {
      sb.append(mentions.get(i).toString()).append('\n');
    }
    return sb.toString();
  }

  public void setSubstring(HashMap<Integer, Integer> subStrings) {
    this.subStrings = subStrings;
  }

  public HashMap<Integer, Integer> getSubstrings() {
    return subStrings;
  }

  public void sortMentions() {
    Collections.sort(mentions);
  }

  public Set<Type> getEntitiesTypes() {
    return entitiesTypes;
  }

  public void setEntitiesTypes(Set<Type> entitiesTypes) {
    this.entitiesTypes = entitiesTypes;
  }

  public Entities getAllCandidateEntities() {
    Entities candidates = new Entities();
    for (Mention m : mentions) {
      candidates.addAll(m.getCandidateEntities());
    }
    return candidates;
  }
}
