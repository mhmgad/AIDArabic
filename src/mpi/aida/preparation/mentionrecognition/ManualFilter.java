package mpi.aida.preparation.mentionrecognition;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import mpi.aida.config.settings.PreparationSettings.LANGUAGE;
import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.TokenizerManager;
import mpi.tokenizer.data.Tokens;
import mpi.tools.javatools.datatypes.Pair;

public class ManualFilter {

  public static final String startTag = "[[";

  public static final String endTag = "]]";

  protected static final int tagSize = startTag.length();;

  public Pair<String, Mentions> filter(String text) {
    Mentions mentions = new Mentions();
    String filteredText = filterText(text, mentions);
    //Tokens tokens = tokenize(filteredText, by, mentions, language);
    Pair<String, Mentions> filteredTextMentions = new Pair<String, Mentions>(filteredText, mentions);
    return filteredTextMentions;
  }

  private String filterText(String text, Mentions mentions) {
    StringBuffer sb = new StringBuffer();
    int s = 0;
    int e = text.indexOf(startTag);
    while (e >= 0) {
      if (s != e) {
        sb.append(text.substring(s, e));
      }
      s = e;
      e = text.indexOf(endTag, s);
      if (e == -1) {
        sb.append(text.substring(s));
        s = text.length();
        break;
      }
      String name = text.substring(s + tagSize, e);
      if (name.trim().length() > 0) {
        Mention mention = new Mention();
        mention.setCharOffset(s - (mentions.getMentions().size() * tagSize * 2));
        mention.setCharLength(e - (s + tagSize));
        mention.setMention(name);
        mentions.addMention(mention);
      }
      sb.append(name);
      s = e + tagSize;
      e = text.indexOf(startTag, s);
    }
    sb.append(text.substring(s));
    return sb.toString();
  }

  public Tokens tokenize(String filteredText, TokenizerManager.TokenizationType tokenizerType, Mentions mentions, LANGUAGE language) {
    Tokens tokens = TokenizerManager.tokenize(filteredText, tokenizerType, false);
    List<String> textContent = new LinkedList<String>();
    for (int p = 0; p < tokens.size(); p++) {
      Token token = tokens.getToken(p);
      textContent.add(token.getOriginal());
    }
    int startToken = -1;
    int endToken = -1;
    int t = 0;
    int i = 0;
    Mention mention = null;
    Token token = null;
    while (t < tokens.size() && i < mentions.getMentions().size()) {
      mention = mentions.getMentions().get(i);
      token = tokens.getToken(t);
      if (startToken >= 0) {
        if (token.getEndIndex() > mention.getCharOffset() + mention.getCharLength()) {
          mention.setStartToken(startToken);
          mention.setStartStanford(tokens.getToken(startToken).getStandfordId());
          mention.setSentenceId(tokens.getToken(startToken).getSentence());
          mention.setId(startToken);
          mention.setEndToken(endToken);
          mention.setEndStanford(tokens.getToken(endToken).getStandfordId());
          if (mention.getMention() == null) {
            mention.setMention(tokens.toText(startToken, endToken));
          }
          startToken = -1;
          endToken = -1;
          i++;
        } else {
          endToken = token.getId();
          t++;
        }
      } else {
        if (token.getBeginIndex() >= mention.getCharOffset() && mention.getCharOffset() <= token.getEndIndex()) {
          startToken = token.getId();
          endToken = token.getId();
        } else {
          t++;
        }
      }
    }
    if (startToken >= 0) {
      if (token.getEndIndex() >= mention.getCharOffset() + mention.getCharLength()) {
        mention.setStartToken(startToken);
        mention.setStartStanford(tokens.getToken(startToken).getStandfordId());
        mention.setSentenceId(tokens.getToken(startToken).getSentence());
        mention.setId(startToken);
        mention.setEndToken(endToken);
        mention.setEndStanford(tokens.getToken(endToken).getStandfordId());
        if (mention.getMention() == null) {
          mention.setMention(tokens.toText(startToken, endToken));
        }    
      }
    }
    List<Mention> tobeRemoved = new LinkedList<Mention>();
    for (int m = 0; m < mentions.getMentions().size(); m++) {
      Mention check = mentions.getMentions().get(m);
      Token start = tokens.getToken(check.getStartToken());
      Token end = tokens.getToken(check.getEndToken());
      if (start.getBeginIndex() != check.getCharOffset() || end.getEndIndex() != check.getCharOffset() + check.getCharLength()) {
        tobeRemoved.add(check);
      }
    }
    Iterator<Mention> iter = tobeRemoved.iterator();
    while (iter.hasNext()) {
      mentions.remove(iter.next());
    }
    return tokens;
  }
}
