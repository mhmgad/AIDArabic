package mpi.aida.util;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MathUtilTest {

  @Test
  public void testComputeJaccardSimilarity() {
    Set<Integer> a = new HashSet<>();
    a.add(1);
    Set<Integer> b = new HashSet<>();
    b.add(1);
    assertEquals(1.0, MathUtil.computeJaccardSimilarity(a, b), 0.01);

    a.clear();
    b.clear();
    a.add(1);
    b.add(2);
    assertEquals(0.0, MathUtil.computeJaccardSimilarity(a, b), 0.01);

    a.clear();
    b.clear();
    a.add(1);
    a.add(2);
    b.add(2);
    b.add(3);
    assertEquals(0.33, MathUtil.computeJaccardSimilarity(a, b), 0.01);

    a.clear();
    b.clear();
    a.add(1);
    a.add(2);
    a.add(3);
    b.add(2);
    b.add(3);
    assertEquals(0.66, MathUtil.computeJaccardSimilarity(a, b), 0.01);

    a.clear();
    b.clear();
    a.add(1);
    a.add(2);
    a.add(3);
    b.add(2);
    b.add(3);
    b.add(4);
    b.add(5);
    assertEquals(0.4, MathUtil.computeJaccardSimilarity(a, b), 0.01);
  }
}
