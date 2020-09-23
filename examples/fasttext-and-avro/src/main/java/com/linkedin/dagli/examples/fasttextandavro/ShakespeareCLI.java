package com.linkedin.dagli.examples.fasttextandavro;

import com.linkedin.dagli.dag.DAG1x1;
import com.linkedin.dagli.math.distribution.DiscreteDistribution;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Scanner;


/**
 * A simple console program that shows how trained models can be loaded and applied for inference.
 * The model can be read from a resource file or, if provided as a command-line argument, a file of your choosing.
 */
public class ShakespeareCLI {
  private ShakespeareCLI() { }

  @SuppressWarnings("unchecked")
  public static void main(String[] arguments) throws FileNotFoundException, IOException, ClassNotFoundException {
    // read the model using standard Java deserialization
    DAG1x1.Prepared<CharacterDialog, DiscreteDistribution<String>> model;
    try (InputStream is = arguments.length == 0 ? ShakespeareCLI.class.getResourceAsStream(
        "/shakespeare-demo-model.bin")
        : new FileInputStream(arguments[0]); ObjectInputStream ois = new ObjectInputStream(is)) {
      model = (DAG1x1.Prepared<CharacterDialog, DiscreteDistribution<String>>) ois.readObject();
    }

    System.out.println(
        "Model loaded.  Please input a line of dialog and the Shakespearean character will be predicted.\n");

    // REPL
    Scanner scanner = new Scanner(System.in);
    String line;
    while ((line = scanner.nextLine()) != null) {
      // here we just apply the model to a new example (represented as a CharacterDialogStruct) and then print the
      // result to stdout:
      DiscreteDistribution<String> prediction = model.apply(CharacterDialogStruct.Builder.setDialog(line).build());
      prediction.stream().limit(10).forEach(System.out::println);
      System.out.println();
    }
  }
}
