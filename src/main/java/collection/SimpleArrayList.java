package collection;

import java.util.Arrays;
import java.util.Objects;

public class SimpleArrayList<E> {
  private static final Object[] DEFAULT_CAPACITY_EMPTY_ELEMENT_DATA = new Object[0];
  private static final Object[] EMPTY_ELEMENT_DATA = new Object[0];

  private static final int DEFAULT_INITIAL_CAPACITY = 10;
  Object[] elementArray;
  private int size = 0;

  public SimpleArrayList() {
    this.elementArray = DEFAULT_CAPACITY_EMPTY_ELEMENT_DATA;
  }

  public SimpleArrayList(int initialCapacity) {
    if (initialCapacity > 0) {
      this.elementArray = new Object[initialCapacity];
    } else if (initialCapacity != 0) {
      throw new IllegalArgumentException("Capacity cannot be negative: " + initialCapacity);
    }

    this.elementArray = EMPTY_ELEMENT_DATA;
  }

  public boolean add(E element) {
    if (size == elementArray.length) {
      elementArray = grow(size + 1);
    }
    elementArray[size++] = element;
    return true;
  }

  public void add(int index, E element) {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    if (size == elementArray.length) {
      elementArray = grow(size + 1);
    }
    if (index < size) {
      System.arraycopy(elementArray, index, elementArray, index + 1, size - index);
    }

    elementArray[index] = element;
    size++;
  }

  private Object[] grow(int minCapacity) {
    int oldCapacity = elementArray.length;
    if (oldCapacity <= 0 && elementArray == DEFAULT_CAPACITY_EMPTY_ELEMENT_DATA) {
      return new Object[Math.max(DEFAULT_INITIAL_CAPACITY, minCapacity)];
    } else {
      int newCapacity = oldCapacity + Math.max((oldCapacity >> 1), (minCapacity - oldCapacity));
      return Arrays.copyOf(elementArray, newCapacity);
    }
  }

  public E get(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    return (E) elementArray[index];
  }

  public E remove(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
    E removedElement = get(index);
    System.arraycopy(elementArray, index + 1, elementArray, index, size - index - 1);

    elementArray[--size] = null;

    return removedElement;
  }

  public boolean remove(E element) {
    int index = indexOf(element);
    if (index < 0) {
      return false;
    }
    remove(index);
    return true;
  }

  public int size() {
    return size;
  }

  public void print() {
    for (int i = 0; i < size; i++) {
      System.out.println(elementArray[i]);
    }
  }

  public int indexOf(Object obj) {
    for (int i = 0; i < size; i++) {
      if (Objects.equals(obj, elementArray[i])) {
        return i;
      }
    }
    return -1;
  }

  public boolean contains(Object obj) {
    return indexOf(obj) >= 0;
  }
}
