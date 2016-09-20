package mpi.lsh;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies locality-sensitive-hashing (LSH) to minhash-signatures (generated by
 * MinHasher). It is configurable by the band size and count, given that 
 * the provided signatures are long enough.
 * It provides one method to access the added items:
 *  - getSimilarItems(T itemId) [for the LSH using Objects as keys]
 */
public class LSH<T> {
  private static final Logger logger = 
      LoggerFactory.getLogger(LSH.class);

  private int bandCount_;

  private int bandSize_;

  private LSHFeatureExtractor featureExtractor_;

  private MinHasher minHasher_;

  private ArrayList<MinHashTable<T>> minhashTables_;

  private Map<T, int[]> itemSignature_;

  /**
   * Constructs an LSH table from a Map of items and their (pre-computed)
   * minhashes (using {@link mpi.lsh.MinHasher}).
   * 
   * @param itemMinhashes Items and their pre-computed minhashes
   * @param featureExtractor Converts features to an int[] representation used for minhashing.
   * @param bandSize  size of a band
   * @param bandCount total number of bands
   */
  private LSH(Map<T, int[]> itemMinhashes,
              LSHFeatureExtractor featureExtractor, int bandSize, int bandCount) {
    logger.info(itemMinhashes.size() + " items, " + bandSize + ", " + bandCount);

    bandCount_ = bandCount;
    bandSize_ = bandSize;

    // Keep featureExtractor for later, if new items without precomputed signatures are added.
    featureExtractor_ = featureExtractor;

    // Create a MinHasher for later use.
    minHasher_ = new MinHasher(bandCount * bandSize, 1);


    boolean checkLimitation = false;

    // Create bandCount minhash tables
    minhashTables_ = new ArrayList<>(bandCount_);
    for (int i = 0; i < bandCount_; i++) {
      minhashTables_.add(new MinHashTable<T>(i));
    }

    // Pre-aggregate the signatures by summing up all values in each band,
    // afterwards the band size is assumed to be one.
    itemSignature_ = new HashMap<>(itemMinhashes.size());

    for (Entry<T, int[]> entry : itemMinhashes.entrySet()) {
      T itemId = entry.getKey();
      int[] sig = entry.getValue();

      if(!checkLimitation){
        if(bandSize  * bandCount_ > sig.length)
          logger.error("Requested signature length is not available. " +
                  bandSize + " x " + bandCount_ + " > " + sig.length);
        checkLimitation = true;
      }
      int[] lshSig = createLshSignature(sig);

      // Keep the signature for
      itemSignature_.put(itemId, lshSig);
    }

    // Hash all items.
    for (T item : itemMinhashes.keySet()) {
      for (int i = 0; i < bandCount; i++) {
        minhashTables_.get(i).put(item);
      }
    }
  }

  /**
   * Creates the band-concatenated LSH representation.
   *
   * @param minHashes MinHashes to use.
   * @return  LSH representation.
   */
  private int[] createLshSignature(int[] minHashes) {
    int[] lshSig = new int[bandCount_];

    for (int i=0;i<lshSig.length;i++) {
      int sum = 0;
      int start = bandSize_*i;
      for (int j=start;j<start+bandSize_;j++) {
        sum += minHashes[j];
      }
      lshSig[i] = sum;
    }

    return lshSig;
  }

  /**
   * Factory method for LSH. Use this if all the features for items are already known.
   *
   * @param itemFeatures items with their features.
   * @param bandSize  size of a band.
   * @param bandCount total number of bands.
   * @param threadCount number of parallel threads to use.
   */
  public static <T,F> LSH<T>
  createLSH(Map<T,int[]> itemFeatures,
            int bandSize, int bandCount, int threadCount) throws InterruptedException {
    MinHasher<T> minHasher = new MinHasher<>(bandSize * bandCount, threadCount);
    Map<T, int[]> itemMinhashes = minHasher.createSignatures(itemFeatures);
    return new LSH(itemMinhashes, null, bandSize, bandCount);
  }

  /**
   * Factory method for LSH. Use this if the features should be pre-processed by
   * a featureExtractor.
   *
   * @param itemFeatures items with their features.
   * @param bandSize  size of a band.
   * @param bandCount total number of bands.
   * @param threadCount number of parallel threads to use.
   */
  public static <T,F> LSH<T>
  createLSH(Map<T,Collection<F>> itemFeatures, LSHFeatureExtractor<F> featureExtractor,
            int bandSize, int bandCount, int threadCount) throws InterruptedException {
    // Transform features to int arrays.
    Map<T, int[]> itemFeatureIds = new HashMap<>(itemFeatures.size());

    for (Entry<T, Collection<F>> entry : itemFeatures.entrySet()) {
      T item = entry.getKey();
      Collection<F> features = entry.getValue();
      int[] featureIds = featureExtractor.convert(features);
      itemFeatureIds.put(item, featureIds);
    }

    MinHasher<T> minHasher = new MinHasher<>(bandSize * bandCount, threadCount);
    Map<T, int[]> itemMinhashes = minHasher.createSignatures(itemFeatureIds);
    return new LSH(itemMinhashes, featureExtractor, bandSize, bandCount);
  }

  /**
   * Factory method for LSH. Use this if the items themselves are the features.
   *
   * @param items Items - are also used as features.
   * @param bandSize  size of a band.
   * @param bandCount total number of bands.
   * @param threadCount number of parallel threads to use.
   */
  public static <T,F> LSH<T>
  createLSH(Set<T> items, LSHFeatureExtractor<T> featureExtractor,
            int bandSize, int bandCount, int threadCount) throws InterruptedException {
    Map<T, Collection<T>> itemFeatures = new HashMap<>();
    for (T item : items) {
      Collection<T> singleCollection = new ArrayList<>(1);
      singleCollection.add(item);
      itemFeatures.put(item, singleCollection);
    }
    return createLSH(itemFeatures, featureExtractor, bandSize, bandCount, threadCount);
  }

  /**
   * @param item ID of the item. The item is expected to have been present during LSH creation.
   * @return  all item IDs similar to the item according to LSH parameters.
   */
  public Set<T> getSimilarItems(T item) {
    int[] sig = itemSignature_.get(item);

    if (sig == null) {
      throw new IllegalArgumentException("No signature for '" + item + "'. " +
              "Use getSimilarItems(Collection<F> features).");
    }

    return getSimilarItemsBySignature(sig);
  }

  /**
   * @param feature A single feature from the same space as the original items.
   * @return  All item IDs similar to the feature according to LSH parameters.
   */
  public <F> Set<T> getSimilarItemsForFeature(F feature) {
    List<F> singleCollection = new ArrayList<>(1);
    singleCollection.add(feature);
    return getSimilarItemsForFeatures(singleCollection);
  }

  /**
   * @param features A collection of features from the same space as the original items.
   * @return  All item IDs similar to the features according to LSH parameters.
   */
  public <F> Set<T> getSimilarItemsForFeatures(Collection<F> features) {
    int[] featureIds = featureExtractor_.convert(features);
    int[] minhashes = minHasher_.minhash(featureIds);
    int[] lshSignature = createLshSignature(minhashes);
    return getSimilarItemsBySignature(lshSignature);
  }

  /**
   * @param featureIds An array of feature ids from the same space as the original items.
   * @return  All item IDs similar to the item according to LSH parameters.
   */
  public <F> Set<T> getSimilarItemsForFeatureIds(int[] featureIds) {
    int[] minhhashes = minHasher_.minhash(featureIds);
    int[] lshSignature = createLshSignature(minhhashes);
    return getSimilarItemsBySignature(lshSignature);
  }

  private Set<T> getSimilarItemsBySignature(int[] sig) {
    Set<T> all = new HashSet<>();
    for (MinHashTable<T> table : minhashTables_) {
      Set<T> items = table.getBucket(sig);
      if (items != null) {
        all.addAll(items);
      }
    }
    return all;
  }

  private class MinHashTable<U> {

    private int id;
    
    private Map<Integer, Set<U>> buckets;

    // Draw k random permutations for each table
    private MinHashTable(int id) {
      this.id = id;      
      buckets = new HashMap<>();
    }

    private void put(U item) {      
      int hashCode = itemSignature_.get(item)[id];

      Set<U> bucket = buckets.get(hashCode);

      if (bucket == null) {
        bucket = new HashSet<U>();
        buckets.put(hashCode, bucket);
      }

      bucket.add(item);
    }

    private Set<U> getBucket(int[] sig) {
      int bucketId = sig[id];
      return buckets.get(bucketId);
    }
    
    @SuppressWarnings("unused")
    public Set<Integer> getBucketIds() {
      return buckets.keySet();
    }

    @SuppressWarnings("unused")
    public Set<U> getBucketById(int bucketid) {
      return buckets.get(bucketid);
    }
  }
}