package com.linkedin.dagli.fasttext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Utilities for resource files.
 */
public abstract class ResourceUtil {

  private final static int BUFFER_SIZE = 1024 * 16;

  /**
   * Private constructor - this class will never be instanced
   */
  private ResourceUtil() {
  }

  /**
   * Creates a temporary file from a resource.
   *
   * The file from the JAR is copied into the system temporary directory and deleted after exiting.
   *
   * @param resourcePath The absolute path of the file inside the JAR (beginning with '/'), e.g. /package/File.ext
   * @param tempFilePrefix The prefix used to generate the temp file name
   * @param tempFileSuffix The suffix used to generate the temp file name
   * @throws IOException If temporary file creation or read/write operation fails
   * @throws FileNotFoundException If resourcePath does not exist
   */
  public static File createTempFileFromResource(String resourcePath, String tempFilePrefix, String tempFileSuffix)
      throws IOException {

    // Prepare temporary file
    File temp = File.createTempFile(tempFilePrefix, tempFileSuffix);
    temp.deleteOnExit();

    copyResourceToFile(resourcePath, temp, false);

    return temp;
  }

  /**
   * Copies a resource to a file.
   *
   * @param resourcePath The absolute path of the file inside the JAR (beginning with '/'), e.g. /package/File.ext
   * @param target the target file to write to
   * @param append true to append to the end of target; otherwise, target is overwritten
   * @throws IOException If target file creation or read/write operation fails
   * @throws FileNotFoundException If target cannot be opened for writing, or the resource cannot be found
   */
  public static void copyResourceToFile(String resourcePath, File target, boolean append)
      throws IOException {
    // Open and check input stream
    try (InputStream is = ResourceUtil.class.getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new FileNotFoundException("Resource " + resourcePath + " was not found.");
      }

      // Prepare buffer for data copying
      byte[] buffer = new byte[BUFFER_SIZE];
      int readBytes;

      // Open output stream and copy data between source file in JAR and the target file
      try (OutputStream os = new FileOutputStream(target, append)) {
        while ((readBytes = is.read(buffer)) != -1) {
          os.write(buffer, 0, readBytes);
        }
      }
    }
  }
}