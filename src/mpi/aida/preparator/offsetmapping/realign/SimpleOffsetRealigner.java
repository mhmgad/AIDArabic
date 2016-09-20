package mpi.aida.preparator.offsetmapping.realign;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mpi.aida.data.Mention;
import mpi.aida.data.Mentions;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.PreparedInputChunk;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.Tokens;


public class SimpleOffsetRealigner implements OffsetRealignInterface {

  private final TIntIntMap newToOldOffsetMappings = new TIntIntHashMap();
    
  private String content;
  
  @Override
  public void process(String text) {
    content = text;
    char[] arr = text.toCharArray();
    boolean stopProcess = false;
    int currOffset=0;    
    int newOffset = 0;
    while(currOffset<arr.length) {
      if(stopProcess) {
        if(arr[currOffset] == ';') {
          stopProcess = false;
        }
        currOffset++;
        continue;
      } 
      
      if(arr[currOffset] == '&') {
        stopProcess = true;                       
      } 

      arr[newOffset] = arr[currOffset];
      newToOldOffsetMappings.put(newOffset, currOffset);      
      currOffset++;
      newOffset++;      
    }
//    System.out.println(newToOldOffsetMappings);
  }

  @Override
  public PreparedInput reAlign(PreparedInput preparedInput) {
    if(newToOldOffsetMappings.size() == 0)
      return preparedInput;
    Iterator<PreparedInputChunk> chunkIter = preparedInput.iterator();
    List<PreparedInputChunk> lstChunks = new ArrayList<>();
    
    while(chunkIter.hasNext()) {
      PreparedInputChunk chunk = chunkIter.next();
      String chunkId = chunk.getChunkId();
      Tokens tokens = chunk.getTokens();
      Mentions mentions = chunk.getMentions();

      // final list of tokens and mentions for chunk.
      Tokens finalTokens = new Tokens();
      Mentions finalMentions = new Mentions();
     
      for(Token token : tokens) {  
        int currStartOffset = token.getBeginIndex();        
        int oldStartOffset = newToOldOffsetMappings.get(currStartOffset);                
        int oldEndOffset = newToOldOffsetMappings.get(token.getEndIndex());        
        if(oldEndOffset == 0) {          
          oldEndOffset = oldStartOffset + token.getOriginal().length();
//          System.out.println(token.getEndIndex() + "(" + token.getOriginal() + ") is not present in mapping. Setting to : " + oldEndOffset);
        }
//        System.out.println(token.getOriginal() + "("+currStartOffset+", "+token.getEndIndex()+")=>("+oldStartOffset+", "+oldEndOffset+")");
        Token tmp = new Token(token.getStandfordId(), 
            token.getOriginal(), 
            token.getOriginalEnd(), 
            oldStartOffset, 
            oldEndOffset, 
            token.getSentence(), 
            token.getParagraph(), 
            token.getPOS(), 
            token.getNE());
        finalTokens.addToken(tmp);
//      System.out.println("* " + tmp.getOriginal() + " -- " + tmp.getBeginIndex() + " " + tmp.getEndIndex());
        if(mentions.containsOffset(currStartOffset)) {
            Mention mention = mentions.getMentionForOffset(currStartOffset);          
            Mention finalMention = new Mention(mention.getMention(), 
                mention.getStartToken(), 
                mention.getEndToken(), 
                mention.getStartStanford(), 
                mention.getEndStanford(), 
                mention.getSentenceId());
            finalMention.setCharOffset(oldStartOffset);
            finalMention.setCharLength(mention.getCharLength());
            finalMentions.addMention(finalMention);
        }
      }

      // final re-adjustements of mention length based on the actual text length.
      for(Mention mention : finalMentions.getMentions()) {
        int startIdx = finalTokens.getToken(mention.getStartToken()).getBeginIndex();
        int endIdx = finalTokens.getToken(mention.getEndToken()).getEndIndex();
        
        String actualContent = content.substring(startIdx, endIdx);
        
        if(!actualContent.equals(mention.getMention())) {
//          System.out.println(actualContent + "(" + actualContent.length() + ") in text is different from mention : " + mention.getMention()+"("+mention.getCharLength()+")");
          mention.setCharLength(actualContent.length());
        }
      }
      
      lstChunks.add(new PreparedInputChunk(chunkId, finalTokens, finalMentions));      
    }    
    return new PreparedInput(preparedInput.getDocId(), preparedInput.getOriginalText(), lstChunks);
  }

  @Override
  public int getMappedOffset(int offset) {
    if(newToOldOffsetMappings.size() == 0)
      return offset;
    return newToOldOffsetMappings.get(offset);
  }
}