package mpi.aida.preparation.documentchunking;

import java.util.ArrayList;
import java.util.List;

import mpi.aida.data.Mentions;
import mpi.aida.data.PreparedInput;
import mpi.aida.data.PreparedInputChunk;
import mpi.tokenizer.data.Token;
import mpi.tokenizer.data.Tokens;

public class PageBasedDocumentChunker extends DocumentChunker {

  @Override
  public PreparedInput process(String docId, String text, Tokens tokens, Mentions mentions) {
    List<PreparedInputChunk> chunks = new ArrayList<PreparedInputChunk>(1);
    Tokens pageTokens = null;
    Mentions pageMentions = null;
    int prevPageNum = -1;

    PreparedInputChunk singleChunk = null;
    String chunkId = null;
    for(Token token : tokens.getTokens()){
      int pageNumber = token.getPageNumber();
      if(prevPageNum != pageNumber){
        if(prevPageNum != -1){
          chunkId = docId + "_" + prevPageNum;
          singleChunk = 
            new PreparedInputChunk(chunkId, pageTokens, pageMentions);
          chunks.add(singleChunk);
        }        
        pageTokens = new Tokens();
        pageTokens.setPageNumber(pageNumber);
        pageMentions = new Mentions();                
      }
            
      pageTokens.addToken(token);
      if(mentions.containsOffset(token.getBeginIndex())){
        pageMentions.addMention(mentions.getMentionForOffset(token.getBeginIndex()));        
      }
      prevPageNum = pageNumber;
    }
    
    // Need to add the last page processed to chunk list
    chunkId = docId + "_" + prevPageNum;
    singleChunk = 
        new PreparedInputChunk(chunkId, pageTokens, pageMentions);
      chunks.add(singleChunk);
    
    PreparedInput preparedInput = new PreparedInput(docId, text, chunks);
    return preparedInput;
  }
}
