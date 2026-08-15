package view;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

/** Tests for {@link Main}. */
public class MainTest {

  String captureOutput(Runnable action) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    PrintStream original = System.out;
    System.setOut(new PrintStream(buffer));
    try {
      action.run();
    } finally {
      System.setOut(original);
    }
    return buffer.toString();
  }

  @Test
  void givenExitChoice_whenMain_thenTerminationMessagePrinted() {
    System.setIn(new ByteArrayInputStream("16\n".getBytes()));

    String output = captureOutput(() -> Main.main(new String[0]));

    assertTrue(output.contains("Завершение работы"));
  }

  @Test
  void givenInvalidInput_whenMain_thenErrorMessagePrinted() {
    System.setIn(new ByteArrayInputStream("abc\n16\n".getBytes()));

    String output = captureOutput(() -> Main.main(new String[0]));

    assertTrue(output.contains("Неверный ввод, введите номер пункта меню"));
  }
}
