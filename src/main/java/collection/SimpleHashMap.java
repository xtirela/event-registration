package collection;

import java.util.*;
import java.util.function.Function;

public class SimpleHashMap<K, V> {
  static final int DEFAULT_INITIAL_CAPACITY = 16;
  static final float DEFAULT_LOAD_FACTOR = 0.75F;

  private int capacity;
  private int threshold;
  private int size = 0;
  private Entry<K, V>[] entryArr;

  public SimpleHashMap() {
    this.capacity = DEFAULT_INITIAL_CAPACITY;
    this.threshold = (int) (capacity * DEFAULT_LOAD_FACTOR);
    this.entryArr = new Entry[capacity];
  }

  private int getIndex(K key) {
    if (key == null) {
      return 0;
    }

    int h = key.hashCode();
    int hash = h ^ (h >>> 16);
    return (hash & Integer.MAX_VALUE) % capacity;
  }

  private static class Entry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private V value;
    private Entry<K, V> next;

    public Entry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return this.key;
    }

    @Override
    public V getValue() {
      return this.value;
    }

    @Override
    public V setValue(V value) {
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }
  }

  public Set<Map.Entry<K, V>> entrySet() {
    Set<Map.Entry<K, V>> entries = new HashSet<>();

    for (int i = 0; i < entryArr.length; i++) {
      Entry<K, V> current = entryArr[i];
      while (current != null) {
        entries.add(current);
        current = current.next;
      }
    }

    return entries;
  }

  public V put(K key, V value) {
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    if (value == null) {
      throw new NullPointerException("value is null");
    }

    if (size >= threshold) {
      resize();
    }
    int index = getIndex(key);

    Entry<K, V> e = entryArr[index];
    if (e == null) {
      entryArr[index] = new Entry<K, V>(key, value);
      size++;
    } else {
      while (e.next != null) {
        if (e.getKey().equals(key)) {
          V oldValue = e.getValue();
          e.setValue(value);
          return oldValue;
        }
        e = e.next;
      }

      if (e.getKey().equals(key)) {
        V oldValue = e.getValue();
        e.setValue(value);
        return oldValue;
      }

      e.next = new Entry<K, V>(key, value);
      size++;
    }
    return null;
  }

  private void resize() {
    int newCapacity = capacity * 2;
    Entry[] newArr = new Entry[newCapacity];

    for (int i = 0; i < entryArr.length; i++) {
      Entry<K, V> current = entryArr[i];

      while (current != null) {
        Entry<K, V> next = current.next;

        int h = current.key.hashCode();
        int hash = h ^ (h >>> 16);
        int newIndex = (hash & Integer.MAX_VALUE) % newCapacity;

        current.next = newArr[newIndex];

        newArr[newIndex] = current;

        current = next;
      }
    }

    entryArr = newArr;
    capacity = newCapacity;
    threshold = (int) (capacity * DEFAULT_LOAD_FACTOR);
  }

  public V get(K key) {
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    int index = getIndex(key);

    Entry<K, V> e = entryArr[index];
    if (e == null) {
      return null;
    } else {
      while (e != null) {
        if (e.getKey().equals(key)) {
          return e.getValue();
        }
        e = e.next;
      }
      return null;
    }
  }

  public V remove(K key) {
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    int index = getIndex(key);
    Entry<K, V> e = entryArr[index];
    if (e == null) {
      return null;
    }

    if (e.getKey().equals(key)) {
      entryArr[index] = e.next;
      e.next = null;
      size--;
      return e.getValue();
    }

    Entry<K, V> previous = e;
    e = e.next;

    while (e != null) {
      if (e.getKey().equals(key)) {
        previous.next = e.next;
        e.next = null;
        size--;
        return e.getValue();
      }
      previous = e;
      e = e.next;
    }

    return null;
  }

  public boolean containsKey(K key) {
    if (key == null) {
      throw new NullPointerException("key is null");
    }

    int index = getIndex(key);
    Entry<K, V> current = entryArr[index];

    while (current != null) {
      if (current.getKey().equals(key)) {
        return true;
      }
      current = current.next;
    }
    return false;
  }

  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    if (key == null) {
      throw new NullPointerException("key is null");
    }
    if (mappingFunction == null) {
      throw new NullPointerException("mappingFunction is null");
    }

    V value = get(key);
    if (value != null) {
      return value;
    }

    V newValue = mappingFunction.apply(key);
    if (newValue == null) {
      throw new NullPointerException("mappingFunction returned null");
    }

    put(key, newValue);
    return newValue;
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public void clear() {
    Arrays.fill(entryArr, null);
    size = 0;
  }

  public Collection<V> values() {
    Collection<V> values = new LinkedList<>();

    if (entryArr != null && size > 0) {
      for (int i = 0; i < entryArr.length; i++) {
        Entry<K, V> current = entryArr[i];
        while (current != null) {
          values.add(current.getValue());
          current = current.next;
        }
      }
    }

    return values;
  }

  public Set<K> keySet() {
    Set<K> keys = new HashSet<>();

    if (entryArr != null && size > 0) {
      for (int i = 0; i < entryArr.length; i++) {
        Entry<K, V> current = entryArr[i];
        while (current != null) {
          keys.add(current.key);
          current = current.next;
        }
      }
    }

    return keys;
  }
}
