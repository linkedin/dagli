package com.linkedin.dagli.assorted;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Reads the dialog text for each character in the resource files and uses it to create {@link CharacterDialog}
 * examples.  This is an example of a "custom" data source solution; note that Dagli also has built-in support for
 * reading common formats like delimiter-separated value (e.g. CSV, TSV...) and Avro.
 */
public class ShakespeareCorpus {
  private ShakespeareCorpus() { }

  private static final String RESOURCE_PATH = "/shakespeare/characters";

  /**
   * Gets the files in a resource directory.  This is not guaranteed to work on all platforms/classloaders, but is good
   * enough for us.
   *
   * @param resourceDirectory path to the resource directory, not including trailing /
   * @return a list of file names in the directory (NOT full paths including the resourceDirectory)
   */
  private static List<String> getResourceDirectoryListing(String resourceDirectory) {
    List<String> result = new ArrayList<>();
    BufferedReader fileNameReader = new BufferedReader(
        new InputStreamReader(ShakespeareCorpus.class.getResourceAsStream(RESOURCE_PATH)));

    try {
      String fileName;
      while ((fileName = fileNameReader.readLine()) != null) {
        result.add(fileName);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return result;
  }

  /**
   * @return  a list of examples, each containing a line of dialog and the character who uttered it.
   */
  public static List<CharacterDialog> readExamples() {
    return getResourceDirectoryListing(RESOURCE_PATH).stream()
        .filter(fileName -> fileName.endsWith(".txt"))
        .flatMap(fileName -> {
          try (BufferedReader textReader = new BufferedReader(
              new InputStreamReader(ShakespeareCorpus.class.getResourceAsStream(RESOURCE_PATH + "/" + fileName),
                  StandardCharsets.UTF_16))) {

            final String characterName = fileName.substring(0, fileName.length() - ".txt".length());

            return textReader.lines().map(line -> line.replaceAll("<.+?>", "")) // remove non-dialog text
                .map(String::trim) // remove excess whitespace
                .filter(line -> !line.isEmpty()) // filter out empty lines
                .map(line -> CharacterDialog.Builder.setDialog(line).setCharacter(characterName).build())
                .collect(Collectors.toList()).stream(); // need to store in memory so we can close the BufferedReader
          } catch (IOException e) {
            throw new UncheckedIOException(e); // suppress the checked exception, repackage as RuntimeException
          }
        })
        .collect(Collectors.toList());
  }
}
