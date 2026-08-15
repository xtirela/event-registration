package collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Tests for {@link SimpleLinkedList} iteration. */
public class SimpleLinkedListTest {

  @Test
  void givenThreeElements_whenIterate_thenElementsInOrder() {
    SimpleLinkedList<String> list = new SimpleLinkedList<>();
    list.addLast("one");
    list.addLast("two");
    list.addLast("three");

    List<String> result = new ArrayList<>();
    for (String element : list) {
      result.add(element);
    }

    assertEquals(List.of("one", "two", "three"), result);
  }

  @Test
  void givenElements_whenIterate_thenHasNextBecomesFalse() {
    SimpleLinkedList<String> list = new SimpleLinkedList<>();
    list.addLast("one");

    Iterator<String> iterator = list.iterator();

    assertTrue(iterator.hasNext());
    assertEquals("one", iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  void givenExhaustedIterator_whenNext_thenNoSuchElement() {
    SimpleLinkedList<String> list = new SimpleLinkedList<>();
    list.addLast("one");

    Iterator<String> iterator = list.iterator();
    iterator.next();

    assertThrows(NoSuchElementException.class, iterator::next);
  }

  @Test
  void givenEmptyList_whenIterate_thenNoElements() {
    SimpleLinkedList<String> list = new SimpleLinkedList<>();

    assertFalse(list.iterator().hasNext());
  }
}
