package repository;

import collection.SimpleArrayList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Minimal CSV reader/writer backed by {@link SimpleArrayList}. */
public final class CsvFile {

  private CsvFile() {}

  public static SimpleArrayList<String> read(String path) {
    SimpleArrayList<String> lines = new SimpleArrayList<>();
    if (path == null) {
      return lines;
    }
    Path file = Paths.get(path);
    if (!Files.exists(file)) {
      return lines;
    }
    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        lines.add(line);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read CSV file " + path, e);
    }
    return lines;
  }

  public static void write(String path, SimpleArrayList<String> lines) {
    if (path == null) {
      return;
    }
    Path file = Paths.get(path);
    try {
      if (file.getParent() != null) {
        Files.createDirectories(file.getParent());
      }
      StringBuilder content = new StringBuilder();
      for (int i = 0; i < lines.size(); i++) {
        content.append(lines.get(i)).append('\n');
      }
      Files.write(file, content.toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write CSV file " + path, e);
    }
  }
}
