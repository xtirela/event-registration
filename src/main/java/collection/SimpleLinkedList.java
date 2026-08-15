package collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SimpleLinkedList<E> implements Iterable<E> {
  int size = 0;
  Node<E> begin;
  Node<E> end;

  private static class Node<E> {
    E element;
    Node<E> next;
    Node<E> previous;

    public Node(Node<E> previous, E element, Node<E> next) {
      this.element = element;
      this.next = next;
      this.previous = previous;
    }
  }

  private Node<E> node(int index) {
    if (index < (size >> 1)) {
      Node<E> current = begin;
      for (int i = 0; i < index; i++) {
        current = current.next;
      }
      return current;
    } else {
      Node<E> current = end;
      for (int i = size - 1; i > index; i--) {
        current = current.previous;
      }
      return current;
    }
  }

  public SimpleLinkedList() {}

  private void linkFirst(E e) {
    Node<E> first = begin;
    Node<E> newNode = new Node(null, e, first);
    begin = newNode;
    if (first == null) {
      end = newNode;
    } else {
      first.previous = newNode;
    }

    size++;
  }

  private void linkLast(E e) {
    Node<E> last = end;
    Node<E> newNode = new Node(last, e, null);
    end = newNode;
    if (last == null) {
      begin = newNode;
    } else {
      last.next = newNode;
    }
    size++;
  }

  private void linkMiddle(int index, E e) {
    Node<E> succ = node(index);
    Node<E> pred = succ.previous;
    Node<E> newNode = new Node<>(pred, e, succ);

    succ.previous = newNode;
    if (pred == null) {
      begin = newNode;
    } else {
      pred.next = newNode;
    }
    size++;
  }

  private E unlinkFirst(Node<E> first) {
    E element = first.element;
    Node<E> next = first.next;

    first.element = null;
    first.next = null;

    begin = next;
    if (next == null) {
      end = null;
    } else {
      next.previous = null;
    }

    size--;
    return element;
  }

  private E unlinkLast(Node<E> last) {
    E element = last.element;
    Node<E> previous = last.previous;
    last.previous = null;
    last.element = null;
    end = previous;
    if (previous == null) {
      begin = null;
    } else {
      previous.next = null;
    }

    size--;

    return element;
  }

  private E unlinkMiddle(Node<E> middleNode) {
    E element = middleNode.element;
    Node<E> next = middleNode.next;
    Node<E> previous = middleNode.previous;

    if (previous == null) {
      begin = next;
    } else {
      previous.next = next;
      middleNode.previous = null;
    }

    if (next == null) {
      end = previous;
    } else {
      next.previous = previous;
      middleNode.next = null;
    }

    middleNode.element = null;
    size--;

    return element;
  }

  public void addFirst(E e) {
    linkFirst(e);
  }

  public void addLast(E e) {
    linkLast(e);
  }

  public void add(int index, E element) {
    if (index >= 0 && index <= this.size) {
      if (index == 0) {
        linkFirst(element);
      } else if (index == size) {
        linkLast(element);
      } else {
        linkMiddle(index, element);
      }
    } else {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size);
    }
  }

  public E removeLast() {
    if (end == null) {
      throw new NoSuchElementException();
    }
    return unlinkLast(end);
  }

  public E removeFirst() {
    if (begin == null) {
      throw new NoSuchElementException();
    }
    return unlinkFirst(begin);
  }

  public E getFirst() {
    if (begin == null) {
      throw new NoSuchElementException();
    }
    return begin.element;
  }

  public E getLast() {
    if (end == null) {
      throw new NoSuchElementException();
    }
    return end.element;
  }

  public E get(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
    return node(index).element;
  }

  public E remove(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
    return unlinkMiddle(node(index));
  }

  public boolean removeIf(java.util.function.Predicate<? super E> filter) {
    if (filter == null) {
      throw new NullPointerException("filter is null");
    }

    boolean removed = false;
    Node<E> current = begin;

    while (current != null) {
      Node<E> next = current.next;

      if (filter.test(current.element)) {
        unlinkMiddle(current);
        removed = true;
      }

      current = next;
    }

    return removed;
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  private class Itr implements Iterator<E> {
    private Node<E> next = begin;

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public E next() {
      if (next == null) {
        throw new NoSuchElementException();
      }
      E element = next.element;
      next = next.next;
      return element;
    }
  }
}
