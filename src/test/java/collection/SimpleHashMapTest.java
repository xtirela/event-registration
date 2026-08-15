package collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for {@link SimpleHashMap}, including Integer.MIN_VALUE hash keys. */
public class SimpleHashMapTest {

  private static class MinHashKey {
    private final int id;

    MinHashKey(int id) {
      this.id = id;
    }

    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof MinHashKey)) {
        return false;
      }
      return id == ((MinHashKey) other).id;
    }
  }

  @Test
  void givenMinHashKey_whenPutAndGet_thenValueReturned() {
    SimpleHashMap<MinHashKey, String> map = new SimpleHashMap<>();
    MinHashKey key = new MinHashKey(1);

    map.put(key, "value");

    assertEquals("value", map.get(key));
  }

  @Test
  void givenMinHashKey_whenRemove_thenValueRemoved() {
    SimpleHashMap<MinHashKey, String> map = new SimpleHashMap<>();
    MinHashKey key = new MinHashKey(1);
    map.put(key, "value");

    map.remove(key);

    assertNull(map.get(key));
  }

  @Test
  void givenCollidingMinHashKeys_whenPutAndGet_thenAllValuesReturned() {
    SimpleHashMap<MinHashKey, String> map = new SimpleHashMap<>();

    map.put(new MinHashKey(1), "one");
    map.put(new MinHashKey(2), "two");
    map.put(new MinHashKey(3), "three");

    assertEquals("one", map.get(new MinHashKey(1)));
    assertEquals("two", map.get(new MinHashKey(2)));
    assertEquals("three", map.get(new MinHashKey(3)));
  }

  @Test
  void givenManyEntries_whenResize_thenAllValuesStillAccessible() {
    SimpleHashMap<Integer, String> map = new SimpleHashMap<>();

    for (int i = 0; i < 30; i++) {
      map.put(i, "v" + i);
    }

    assertEquals("v29", map.get(29));
    assertEquals(30, map.size());
  }

  @Test
  void givenCollidingMinHashKeys_whenResize_thenValuesStillAccessible() {
    SimpleHashMap<MinHashKey, String> map = new SimpleHashMap<>();

    for (int i = 0; i < 30; i++) {
      map.put(new MinHashKey(i), "v" + i);
    }

    assertEquals("v29", map.get(new MinHashKey(29)));
    assertTrue(map.containsKey(new MinHashKey(0)));
  }
}
