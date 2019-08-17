# How To Work with Zip Files using Java

## Introduction

In this article I cover the basics of creating, interacting with, inspecting, and extracting zip archive files using Java (OpenJDK 11 to be specific). The code samples used in this article are in the form of a Gradle project and hosted on GitHub for you to run and experiment with (at your own risk).

#### Contents

* Key Java Classes for Working with Zip Archives
* Common File Paths for the Code Examples
* Inspecting the Contents of a Zip Archive
* Extracting a Zip Archive
* Writing Files Directly into a New Zip Archive
* Zipping an Existing File into a New Zip Archive
* Zipping a Folder Into a New Zip Archive

## Key Java Classes for Working with Zip Archives

I feel its a good idea to start things off by identifying some of the prominant classes that are commonly used when dealing with zip archives in Java. These classe live in either the java.util.zip or java.nio.file packages.

* [java.util.zip.ZipFile](https://docs.oracle.com/javase/8/docs/api/index.html?java/util/zip/ZipFile.html) is used to read in and interact with items (ZipEntry instances) in a zip archive
* [java.util.zip.ZipEntry](https://docs.oracle.com/javase/8/docs/api/java/util/zip/ZipEntry.html) is an abstraction representing a item such as a file or directory in a zip archive (ie, ZipFile instance)
* [java.util.zip.ZipOutputStream]() is an implementation of the abstract [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html) class and used to write items to a Zip file
* [java.nio.file.Files](https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html) is very handy utilities class for streaming and copying file data into ZipOutputStream instances or out of ZipFile instances  
* [java.nio.file.Path](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Paths.html) another handy utilities class for effectively working with file paths

## Common File Paths for the Code Examples

I use two common directories to write and read data to/from which are both relative to the root of my Gradle project being used in this example. Take a look at the linked Repo int the introduction, or better yet, run the samples but, definitely keep these two Path variables in mind.

```
public class App {

    static final Path zippedDir = Path.of("ZippedData");
    static final Path inputDataDir = Path.of("InputData");
    
    // ... other stuff
    
}
```

## Inspecting the Contents of a Zip Archive

You can instantiate a ZipFile class passing it the path to an existing zip archive and inspect the contents by querying the ZipEntry enumeration contained inside it. Note that ZipFile implements the [AutoCloseable](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) interface making it a great candiate for the try-with-resources construct shown below and throughout the examples here. 


```
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
```

Running the Gradle project 

```
./gradlew run
```

yeilds output for this showZipContents method of

```
> Task :run
Inspecting contents of: ZipToInspect.zip

Item: ZipToInspect/ 
Type: directory 
Size: 0

Item: ZipToInspect/greetings.txt 
Type: file 
Size: 160

Item: ZipToInspect/InnerFolder/ 
Type: directory 
Size: 0

Item: ZipToInspect/InnerFolder/About.txt 
Type: file 
Size: 39
```

## Extracting a Zip Archive

Extracting the contents of a zip archive onto disk requires nothing more than replicating the same directory structure as what is inside the ZipFile then copying the files represented in the ZipEntry instances onto disk.

```
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
```


## Writing Files Directly Into a New Zip Archive

Since writing a zip archive is really nothing more than writing a stream of data to some source (a Zip file in this case) then writing data to a zip archive is only different in that you need to match data being written with ZipEntry instances added to the ZipOutputStream. Again, ZipOutputStream implements the AutoCloseable interface so its best to use with try-with-resources. The only real catch is to remember to close your ZipEntry when you are done with each one to make it clear when it should no longer receive data.

```
static void zipSomeStrings() {
    Map<String, String> stringsToZip = Map.ofEntries(
        entry("file1", "This is the first file"),
        entry("file2", "This is the second file"),
        entry("file3", "This is the third file")
    );
    var zipPath = zippedDir.resolve("ZipOfStringData.zip");
    try (var zos = new ZipOutputStream(
    						new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
        for (var entry : stringsToZip.entrySet()) {
            zos.putNextEntry(new ZipEntry(entry.getKey()));
            zos.write(entry.getValue().getBytes());
            zos.closeEntry();
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

## Zipping an Existing File into a New Zip Archive

If you've copied a File in Java before then you are essentially already a PRO at creating a zip archive from an existing file (or directory for that matter). Again, the only real difference is that you need to take a little extra caution to be sure your are matching files up to appropriate ZipEntry instances. In this example I create a input file "FileToZip.txt" and write some data to it "Howdy There Java Friends!" then use the [Files.copy(Path, OutputStream)](https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#copy(java.nio.file.Path,%20java.io.OutputStream)) to associate the ZipEntry with the FileToZip.txt file inside the ZippedFile.zip zip archive I'm creating with a ZipOutoutStream instance.

```
static void zipAFile() {
    var inputPath = inputDataDir.resolve("FileToZip.txt");
    var zipPath = zippedDir.resolve("ZippedFile.zip");
    
    try (var zos = new ZipOutputStream(
    						new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
    						
        Files.writeString(inputPath, "Howdy There Java Friends!\n");

        zos.putNextEntry(new ZipEntry(inputPath.toString()));
        Files.copy(inputPath, zos);
        zos.closeEntry();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```



## Zipping a Folder Into a New Zip Archive

Zipping a non-empty directory becomes a little more involved, expecially if you want to maintain empty directoies within the parent directory. To maintain the presence of an empty directory within a zip archive you need to be sure to create an entry that suffixed with the file system directory separator when creating it's ZipEntry then immediately close it.

In this example I create a directory named "foldertozip" containing the structure shown below then zip it into a zip archive.

```
tree .
.
└── foldertozip
    ├── emptydir
    ├── file1.txt
    └── file2.txt
```

Notice that I use the Files.walk(Path) method to traverse the directory tree of "foldertozip" and look for empty directories ("emptydir" in this example) and if / when found I concatenate the directory separator to the name of the ZipEntry then close it as soon as I add it to the ZipOutputStream instance. I also use a slightly different approach to injecting the non-directory files into the ZipOutputStream compared to the last example but, I am just using this different approach for sake of variety in the examples.

```
static void zipADirectoryWithFiles() {
    var foldertozip = inputDataDir.resolve("foldertozip"); 
    var dirFile1 = foldertozip.resolve("file1.txt");
    var dirFile2 = foldertozip.resolve("file2.txt"); 

    var zipPath = zippedDir.resolve("ZippedDirectory.zip");
    try (var zos = new ZipOutputStream(
    						new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
    						
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
                        zos.putNextEntry(new ZipEntry(
                        		reliativePath.toString() + File.separator));
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
```


## Conclusion

In this article I have discussed and demonstrated a modern approach to working with zip archives in Java using pure Java. 

As always, thanks for reading and don't be shy about commenting or critiquing below.
