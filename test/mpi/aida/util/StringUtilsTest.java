package mpi.aida.util;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class StringUtilsTest {

  @Test
  public void testGetNgrams() {
    Set<String> ngrams = StringUtils.getNgrams("a", 2);
    assertEquals(2, ngrams.size());
    assertTrue(ngrams.contains("_a"));
    assertTrue(ngrams.contains("a_"));

    ngrams = StringUtils.getNgrams("a", 1);
    assertEquals(1, ngrams.size());
    assertTrue(ngrams.contains("a"));

    ngrams = StringUtils.getNgrams("ab", 1);
    assertEquals(2, ngrams.size());
    assertTrue(ngrams.contains("a"));
    assertTrue(ngrams.contains("b"));

    ngrams = StringUtils.getNgrams("ab", 2);
    assertEquals(3, ngrams.size());
    assertTrue(ngrams.contains("_a"));
    assertTrue(ngrams.contains("ab"));
    assertTrue(ngrams.contains("b_"));

    ngrams = StringUtils.getNgrams("abcd", 3);
    assertEquals(6, ngrams.size());
    assertTrue(ngrams.contains("__a"));
    assertTrue(ngrams.contains("_ab"));
    assertTrue(ngrams.contains("abc"));
    assertTrue(ngrams.contains("bcd"));
    assertTrue(ngrams.contains("cd_"));
    assertTrue(ngrams.contains("d__"));
  }
}
