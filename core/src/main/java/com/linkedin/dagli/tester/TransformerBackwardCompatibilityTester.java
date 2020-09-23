package com.linkedin.dagli.tester;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;


/**
 * Utility for testing the serialization of classes to ensure that old classes remain deserializable in subsequent
 * versions of your code.  Ensuring backward compatibility for Dagli transformers is quite valuable, as it allows later
 * versions to be used without having to, e.g. retrain a model DAG.
 *
 * Testing works as follows:
 * (1) If there is an existing serialization of an object saved, load it (to be sure no exception occurs)
 * (2) If there is not, serialize the object for future testing
 *
 * Command-line arguments:
 * -package [package name]: if specified, all class names are taken to be relative to this package.  E.g.
 *                          if "java.lang" is specified, and -classes specified "String", "java.lang.String" will
 *                          be used.  Use this option to make your -classes or -methods more concise.
 * -overwrite: if specified, any existing serialized classes will be overwritten.  Only specify this if you want to
 *             "wipe the slate clean" by forgetting all past serialized instances, e.g. if for some reason you
 *             absolutely must ship a breaking change.
 * -classes [list of classes]: specifies a list of classes that will be tested.  They must have no-argument
 *                             constructors.  Classes can be delimited by any non-letter, non-digit, non-period
 *                             character, e.g. ";".
 * -methods [list of methods]: specifies a list of static, no-argument methods that can be invoked to obtain instances
 *                             to be tested.  Use the class name and method, e.g.
 *                             "com.linkedin.dagli.SomeClass.SomeMethod".  Can be delimited by any non-letter,
 *                             non-digit, non-period character, e.g. ";".
 * -dir [directory]: absolute path specifying the directory where the serialized objects will be stored.
 *
 * Example use in Gradle file:
 * task testSerialization(type: JavaExec) {
 *   classpath = sourceSets.test.runtimeClasspath
 *   main = 'com.linkedin.dagli.tester.TransformerBackwardCompatibilityTester'
 *   args '-package', 'com.linkedin.dagli', '-classes', 'string.LowerCaseString', '-dir',
 *        sourceSets.test.resources.srcDirs[0].toString() + '/serialized'
 * }
 * test.dependsOn testSerialization
 */
// CHECKSTYLE:OFF
public class TransformerBackwardCompatibilityTester {
  private TransformerBackwardCompatibilityTester() { }

  private static void testSerialization(Supplier<Object> objSupplier, Path dir, String name, boolean overwrite)
      throws IOException, ClassNotFoundException {
    Path path = dir.resolve(name + ".serialized");
    if (overwrite || !Files.exists(path)) {
      OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

      ObjectOutputStream oos = new ObjectOutputStream(os);
      oos.writeObject(objSupplier.get());
      oos.close();
    } else {
      InputStream is = Files.newInputStream(path);
      ObjectInputStream ois = new ObjectInputStream(is);
      ois.readObject();
      ois.close();
    }
  }

  public static void main(String[] args) throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException,
                                                InvocationTargetException, IOException, InstantiationException {
    String[] classes = null;
    String[] methods = null;
    String packagePrefix = "";
    Path dir = null;
    boolean overwrite = false;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-package":
          packagePrefix = args[++i] + ".";
          break;
        case "-overwrite":
          overwrite = true;
          break;
        case "-classes":
          classes = args[++i].split("[^.\\w]+");
          break;
        case "-dir":
          dir = Files.createDirectories(Paths.get(args[++i]));
          break;
        case "-methods":
          methods = args[++i].split("[^.\\w]+");
          break;
        default:
          throw new IllegalArgumentException("Unrecognized command-line argument: " + args[i]);
      };
    }

    if (methods != null) {
      for (String methodString : methods) {
        methodString = packagePrefix + methodString;
        int lastPeriod = methodString.lastIndexOf('.');
        String className = methodString.substring(0, lastPeriod);
        String methodName = methodString.substring(lastPeriod + 1);

        Supplier<Object> supplier = () -> {
          try {
            Class<?> classInstance = ClassLoader.getSystemClassLoader().loadClass(className);
            Method method = classInstance.getDeclaredMethod(methodName);
            return method.invoke(null);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };
        testSerialization(supplier, dir, methodString, overwrite);
      }
    }

    if (classes != null) {
      for (String className : classes) {
        String prefixedClassName = packagePrefix + className;
        Supplier<Object> supplier = () -> {
          try {
            Class<?> classInstance = ClassLoader.getSystemClassLoader().loadClass(prefixedClassName);
            Constructor<?> constructor = classInstance.getDeclaredConstructor();
            return constructor.newInstance();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };

        testSerialization(supplier, dir, className, overwrite);
      }
    }
  }
}
// CHECKSTYLE:ON