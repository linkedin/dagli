package com.linkedin.dagli.examples.fasttextandavro.util;

import com.linkedin.dagli.data.dsv.DSVReader;
import com.linkedin.dagli.examples.fasttextandavro.CharacterDialogStruct;
import com.linkedin.dagli.objectio.avro.AvroWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.csv.CSVFormat;


/**
 * This class was used to generate the Avro records used by this example from the original tab-separated value file.
 * It's not really part of the example itself, but it does demonstrate use of the ObjectIO library.
 */
public abstract class TSV2Avro {
  private TSV2Avro() { } // static class

  /**
   * Main method.
   *
   * @param args the first argument must be the source TSV file.  The second must be the destination Avro file.
   */
  public static void main(String[] args) {
    final Path tsvSource = Paths.get(args[0]);
    final Path avroDest = Paths.get(args[1]);

    // create a DSVReader to read in the data with the appropriate schema
    try (final DSVReader<CharacterDialogStruct> charDialogData = new DSVReader<>()
        .withSchema(CharacterDialogStruct.Schema.builder()
          .setDialogColumnName("Dialog")
          .setCharacterColumnName("Character")
          .build())
        .withFile(tsvSource, StandardCharsets.UTF_8)
        .withFormat(CSVFormat.TDF.withQuote(null).withFirstRecordAsHeader())) {

      // use AvroWriter's convenience function to copy all records into an Avro file:
      AvroWriter.toAvroFile(charDialogData, avroDest);
    }
  }
}
