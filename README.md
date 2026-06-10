# discus

A desktop disk space analyzer that visualizes folder sizes in an interactive tree view.

## Features

- Scans any directory and displays folder sizes in a collapsible tree
- Launches with the system drive's top-level folders pre-loaded; scan on demand
- Right-click any folder to rescan just that subtree without rebuilding the whole tree
- Live updates as the scan progresses, with folders marked scanning (blue) until complete
- Unscanned folders are shown in grey until a scan is run
- Color-coded size indicators: orange (> 1 GB), red (> 10 GB)
- Shows recursive file count, directory count, and total size per folder
- Cancel an in-progress scan at any time
- Browse for a directory via dialog or type a path directly
- F5 to re-scan the current path

## Requirements

- Java 16 or later
- Maven 3.6 or later (for running tests)

## Running

```bat
run.bat
```

Compiles sources with `javac` and launches the application.

## Testing

```bat
mvn test
```

## Releasing

Push a version tag to trigger the release workflow, which builds the JAR and publishes it as a GitHub Release:

```bat
git tag v1.0.0
git push origin v1.0.0
```

The JAR attached to the release requires Java 16 or later to run:

```bat
java -jar discus-1.0.0.jar
```

## Project structure

```
src/
  main/java/    Application source files
  test/java/    Unit tests
pom.xml         Maven build file
run.bat         Compile-and-run script (no Maven required)
```