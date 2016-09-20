package mpi.experiment.reader;


public class ParallelDataFileReader extends AidaFormatCollectionReader{

	
	  

	
	public ParallelDataFileReader(String collectionPath, String fileName,int from,int to,
			CollectionReaderSettings crs) {
		super(collectionPath, fileName,from,to, crs);
	}


	@Override
	protected int[] getCollectionPartFromTo(CollectionPart cp) {
		// TODO Auto-generated method stub
		return null;
	}

}
