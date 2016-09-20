package mpi.aida.preparation.mentionrecognition;

import mpi.aida.data.Mentions;
import mpi.tokenizer.data.Tokens;

public class MentionDetectionResults {
  private Tokens tokens_;
  private Mentions mentions_;
  private String text_;

  public MentionDetectionResults(Tokens tokens, Mentions mentions, String text) {
    this.tokens_ = tokens;
    this.mentions_ = mentions;
    this.text_ = text;
  }

  public Tokens getTokens() {
    return tokens_;
  }

  public void setTokens(Tokens tokens) {
    this.tokens_ = tokens;
  }

  public Mentions getMentions() {
    return mentions_;
  }

  public void setMentions(Mentions mentions) {
    this.mentions_ = mentions;
  }

  public String getText() {
    return text_;
  }

  public void setText(String text) {
    this.text_ = text;
  }
}
