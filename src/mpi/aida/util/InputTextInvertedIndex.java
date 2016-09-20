package mpi.aida.util;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import mpi.aida.data.Mention;

import java.util.LinkedList;
import java.util.List;

public class InputTextInvertedIndex {
	private static final int POSITIONS_LIST_INIT_SIZE = 4;

	private TIntObjectHashMap<TIntArrayList> indexIncludingStopWords;
	private TIntObjectHashMap<TIntArrayList> indexWithoutStopWords;

	public InputTextInvertedIndex() {
		indexIncludingStopWords = new TIntObjectHashMap<>();
		indexWithoutStopWords = new TIntObjectHashMap<>();
	}
	
	public InputTextInvertedIndex(int[] tokens, boolean isRemoveStopWords) {
		indexIncludingStopWords = new TIntObjectHashMap<>();
		indexWithoutStopWords = new TIntObjectHashMap<>();
		int noStopwordsPosition = 0;
		for (int position = 0; position < tokens.length; ++position) {
			int token = tokens[position];
			TIntArrayList positions = indexIncludingStopWords.get(token);
			if (positions == null) {
				positions = new TIntArrayList(POSITIONS_LIST_INIT_SIZE);
				indexIncludingStopWords.put(token, positions);
			}
			positions.add(position);
			
			if(!isRemoveStopWords || !StopWord.isStopwordOrSymbol(token)) {
  			positions = indexWithoutStopWords.get(token); 
  			if (positions == null) {
					positions = new TIntArrayList(POSITIONS_LIST_INIT_SIZE);
					indexWithoutStopWords.put(token, positions);
  			}
  			positions.add(noStopwordsPosition);
        noStopwordsPosition++;
			}
		}
	}
	
	public boolean containsWord(int word, Mention mention) {
		if(!indexWithoutStopWords.containsKey(word))
			return false;
		TIntArrayList positions = indexIncludingStopWords.get(word);
		int mentionStart = mention.getStartToken();
		int mentionEnd = mention.getEndToken();
		for (int i = 0; i < positions.size(); i++) {
			int position = positions.get(i);
			if(position < mentionStart || position > mentionEnd)
				return true;
		}
		return false;
	}
	
	public List<Integer> getPositions(int word, Mention mention) {
		int mentionStart = mention.getStartToken();
		int mentionEnd = mention.getEndToken();
		int mentionLength = mentionEnd - mentionStart + 1;

		List<Integer> positions = new LinkedList<>();
		TIntArrayList positionsIncludingStopWords = indexIncludingStopWords.get(word);
		TIntArrayList positionsWithoutStopWords = indexWithoutStopWords.get(word);
		//we need to subtract the mention length if the keyword is after the mention
		for (int i = 0; i < positionsIncludingStopWords.size(); i++) {
			//get the keyword position from the full index (including stopwords)
			int position = positionsIncludingStopWords.get(i);
			//compare to know the position of the keyword relative to the mention
			if(position < mentionStart) //before the mention, return the actual position from the stopwords free index
				positions.add(positionsWithoutStopWords.get(i));
			else if((position > mentionEnd)) //if after the mention, get the actual position and subtract mention length
				positions.add(positionsWithoutStopWords.get(i) - mentionLength);
		}
		
		return positions;
	}
	
	public void addToIndex(TIntIntHashMap newIndexEntries) {
		for(int word: newIndexEntries.keys()) {
			int offset = newIndexEntries.get(word);

			TIntArrayList positions;
			positions = indexIncludingStopWords.get(word); 
			if (positions == null) {
				positions = new TIntArrayList(POSITIONS_LIST_INIT_SIZE);
				indexIncludingStopWords.put(word, positions);
			}
			positions.add(offset);
			
			positions = indexWithoutStopWords.get(word); 
			if (positions == null) {
				positions = new TIntArrayList(POSITIONS_LIST_INIT_SIZE);
				indexWithoutStopWords.put(word, positions);
			}
			positions.add(offset);
		}		
	}

}
