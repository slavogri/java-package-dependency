package com.slavogri;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class DirectoryTraverser {

    public static final String PACKAGE = "package ";
    public static final String IMPORT_STATIC = "import static ";
    public static final String IMPORT = "import ";
    public static final String CLASS = " class ";

    public void traverseDirectory(String rootDirectory) {
        Map<String, String> ourClassesAndTheirPackages = new HashMap<>();
        Map <String, Set<String>> packageDependency = new HashMap<>(); // key is a package, value is a list (Set) of dependant packages
        try (Stream<Path> paths = Files.walk(Paths.get(rootDirectory))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            parseFileImports(path, packageDependency, ourClassesAndTheirPackages);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            System.out.println("FAILED. " + e.getMessage());
            e.printStackTrace();
        }


        // remove packages that are not from our application.
        Set<String> ourPackages = packageDependency.keySet();
        packageDependency.values().forEach(dependantPackages -> {
            dependantPackages.removeIf(currentPackageOrClass -> !isOurPackage(currentPackageOrClass, ourPackages, ourClassesAndTheirPackages.keySet()));
        });

        // group the dependencies by package, omitting the information about concrete class dependency.
        packageDependency.entrySet().forEach(stringSetEntry -> {
            String packageName = stringSetEntry.getKey();
            Set<String> dependencies = stringSetEntry.getValue();
            Set<String> processedDependencies = new HashSet<>(); // the changed value
            Set<String> dependenciesToBeRemoved = new HashSet<>(); // the old value
            dependencies.forEach(dependency -> {
                String relatedPackage = ourClassesAndTheirPackages.get(dependency);
                if (relatedPackage != null) {
                    processedDependencies.add(relatedPackage);
                    dependenciesToBeRemoved.add(dependency);
                }
            });
            dependencies.removeAll(dependenciesToBeRemoved);
            dependencies.addAll(processedDependencies);

        });


        // print out the package dependency
        System.out.println("package dependencies:");
        ourPackages.stream().toList().stream().sorted().forEach(packageName -> {
            System.out.println(packageName + "                        --> " +
                    Arrays.toString(packageDependency.get(packageName).toArray()));
        });
    }

    private boolean isOurPackage(String currentPackageOrClass, Set<String> ourPackages, Set<String> ourClasses) {
        if (currentPackageOrClass.endsWith("*") && ourPackages.contains(currentPackageOrClass.split("\\*")[0])) {
            return true;
        }
        return ourClasses.contains(currentPackageOrClass);
    }

    public void parseFileImports(Path path, Map <String, Set<String>> packageDependency, Map<String, String> ourClasses) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            FileParsedMetadata parsedFileMetadata = readPackageNamesAndClassNames(reader);

            Set<String> dependantPackages = packageDependency.computeIfAbsent(parsedFileMetadata.packageName(), key -> new HashSet<>());
            dependantPackages.addAll(parsedFileMetadata.dependantPackages());

            parsedFileMetadata.classes().forEach(className -> {
                ourClasses.put(className, parsedFileMetadata.packageName());
            });
        }
    }

    private FileParsedMetadata readPackageNamesAndClassNames(BufferedReader reader) throws IOException {
        String firstLine = reader.readLine();
        while (!firstLine.contains(PACKAGE)) {
            firstLine = reader.readLine();
        }
        String packageName = firstLine.substring(PACKAGE.length()).split("[\\s;]")[0].trim();

        List<String> dependantPackages = new ArrayList<>();
        List<String> classes = new ArrayList<>();
        Stream<String> lines = reader.lines();
        lines.forEach(line->{
            if (line.startsWith(PACKAGE)) {
            } else if (line.startsWith(IMPORT_STATIC)){
                dependantPackages.add(line.substring(IMPORT_STATIC.length()).split("[\\s;]")[0].trim());
            } else if (line.startsWith(IMPORT)){
                dependantPackages.add(line.substring(IMPORT.length()).split("[\\s;]")[0].trim());
            } else if (line.contains(CLASS)) {
                String className = getClassname(line);
                classes.add(packageName + "." + className);
            }
        });
        return new FileParsedMetadata(packageName, dependantPackages, classes);
    }

    private String getClassname(String line) {
        int classnameStart = line.indexOf(CLASS) + CLASS.length();
        return line.substring(classnameStart).split("[\\s{]")[0].trim();
    }

//    public static void main(String[] args) {
//        //test
//        new DirectoryTraverser ().getClassname("  class aaa");
//        new DirectoryTraverser ().getClassname("  class aaa{");
//        new DirectoryTraverser ().getClassname("  class aaa extends bbb {");
//        new DirectoryTraverser ().getClassname("  class aaa ");
//        new DirectoryTraverser ().getClassname("  class aaa implements ccc { ");
//    }
}

