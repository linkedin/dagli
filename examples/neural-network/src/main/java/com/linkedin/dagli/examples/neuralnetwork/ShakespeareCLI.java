package com.linkedin.dagli.examples.neuralnetwork;

import com.linkedin.dagli.dag.DAG1x1;
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
    InputStream is = arguments.length == 0 ? ShakespeareCLI.class.getResourceAsStream("/model.bin")
        : new FileInputStream(arguments[0]);
    ObjectInputStream ois = new ObjectInputStream(is);
    DAG1x1.Prepared<CharacterDialog, String> model = (DAG1x1.Prepared<CharacterDialog, String>) ois.readObject();

    System.out.println(
        "Model loaded.  Please input a line of dialog and the Shakespearean character will be predicted.\n");

    // REPL
    Scanner scanner = new Scanner(System.in);
    String line;

    while ((line = scanner.nextLine()) != null) {
      System.out.println(model.apply(CharacterDialog.Builder.setDialog(line).build()) + "\n");
    }
  }
}
