package mpi.ner;

import java.util.List;

public interface NER {
	public List<Name> findNames(String docId, String text);
	public String getId();
}
