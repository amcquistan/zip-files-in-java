
package com.thecodinginterface.howtozip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import static java.util.Map.entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class App {

    static final Path zippedDir = Path.of("ZippedData");
    static final Path inputDataDir = Path.of("InputData");


    public static void main(String[] args) {
        try {
            initialize(zippedDir);
            initialize(inputDataDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        showZipContents();
        unzipAZip();
        zipSomeStrings();
        zipAFile();
        zipADirectoryWithFiles();


        try {
            copyInputs(zippedDir, Path.of("CopiedZipData"));
            copyInputs(inputDataDir, Path.of("CopiedData"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void showZipContents() {
        try (var zf = new ZipFile("ZipToInspect.zip")) {
            System.out.println(String.format("Inspecting contents of: %s\n", zf.getName()));
            Enumeration<? extends ZipEntry> zipEntries = zf.entries();
            zipEntries.asIterator().forEachRemaining(entry -> {
                System.out.println(String.format(
                    "Item: %s \nType: %s \nSize: %d\n",
                    entry.getName(),
                    entry.isDirectory() ? "directory" : "file",
                    entry.getSize()
                ));
            });
        } catch (IOException e) {
          e.printStackTrace();
        }
    }

    static void unzipAZip() {
        var outputPath = Path.of("UnzippedContents");

        try (var zf = new ZipFile("ZipToInspect.zip")) {
            // delete if exists, then create a fresh empty directory to put the zip archive contents
            initialize(outputPath);

            Enumeration<? extends ZipEntry> zipEntries = zf.entries();
            zipEntries.asIterator().forEachRemaining(entry -> {
                try {
                    if (entry.isDirectory()) {
                        var dirToCreate = outputPath.resolve(entry.getName());
                        Files.createDirectories(dirToCreate);
                    } else {
                        var fileToCreate = outputPath.resolve(entry.getName());
                        Files.copy(zf.getInputStream(entry), fileToCreate);
                    }
                } catch(IOException ei) {
                    ei.printStackTrace();
                }
             });
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writing String data straight to zip entries (files in the zip archive)
     */
    static void zipSomeStrings() {
        Map<String, String> stringsToZip = Map.ofEntries(
            entry("file1", "This is the first file"),
            entry("file2", "This is the second file"),
            entry("file3", "This is the third file")
        );
        var zipPath = zippedDir.resolve("ZipOfStringData.zip");
        try (var zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
            for (var entry : stringsToZip.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes());
                zos.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void zipAFile() {
        var inputPath = inputDataDir.resolve("FileToZip.txt");
        var zipPath = zippedDir.resolve("ZippedFile.zip");
        
        try (var zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
            Files.writeString(inputPath, "Howdy There Java Friends!\n");

            zos.putNextEntry(new ZipEntry(inputPath.toString()));
            Files.copy(inputPath, zos);
            zos.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void zipADirectoryWithFiles() {
        var foldertozip = inputDataDir.resolve("foldertozip"); 
        var dirFile1 = foldertozip.resolve("file1.txt");
        var dirFile2 = foldertozip.resolve("file2.txt"); 

        var zipPath = zippedDir.resolve("ZippedDirectory.zip");
        try (var zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
            Files.createDirectory(foldertozip);
            Files.createDirectory(foldertozip.resolve("emptydir"));
            Files.writeString(dirFile1, "Does this Java get you rev'd up or what?");
            Files.writeString(dirFile2, "Java Java Java ... Buz Buz Buz!");

            Files.walk(foldertozip).forEach(path -> {
                try {
                    var reliativePath = inputDataDir.relativize(path);
                    var file = path.toFile();
                    if (file.isDirectory()) {
                        var files = file.listFiles();
                        if (files == null || files.length == 0) {
                            zos.putNextEntry(new ZipEntry(reliativePath.toString() + File.separator));
                            zos.closeEntry();
                        }
                    } else {
                        zos.putNextEntry(new ZipEntry(reliativePath.toString()));
                        zos.write(Files.readAllBytes(path));
                        zos.closeEntry();
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            });
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    static void copyInputs(Path intputsDir, Path copyDir) throws IOException {
        if (Files.exists(intputsDir)){
            deleteDirectory(copyDir);

            Files.walkFileTree(intputsDir, new CopyVisitor(copyDir));
        }
    }

    static void initialize(Path intputsDir) throws IOException {
        deleteDirectory(intputsDir);
        Files.createDirectory(intputsDir);
    }
    
    static void deleteDirectory(Path pathToDelete) throws IOException {
        if (Files.exists(pathToDelete)) {
            Files.walk(pathToDelete).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}





final class CopyVisitor extends SimpleFileVisitor<Path> {
  final Path dstPath;
  Path srcPath;

  CopyVisitor(Path dstPath) throws IOException {
      this.dstPath = dstPath;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      if (srcPath == null) {
          srcPath = dir;
      }

      Path dirToCreate = dstPath.resolve(srcPath.relativize(dir));
      if (!Files.exists(dirToCreate)) {
          Files.createDirectories(dirToCreate);
      }
      
      return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      var fileToCopy = dstPath.resolve(srcPath.relativize(file));
      Files.copy(file, fileToCopy, StandardCopyOption.REPLACE_EXISTING);
      return FileVisitResult.CONTINUE;
  }
}
