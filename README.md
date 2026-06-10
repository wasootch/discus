# discus

A desktop disk space analyzer that visualizes folder sizes in an interactive tree view.

## Features

- Scans any directory and displays folder sizes in a collapsible tree
- Launches with the system drive's top-level folders pre-loaded; scan on demand
- Live updates as the scan progresses, with folders marked scanning (blue) until complete
- Unscanned folders are shown in grey until a scan is run
- Color-coded size indicators: orange (> 1 GB), red (> 10 GB)
- Shows file count, directory count, and total size per folder
- Cancel an in-progress scan at any time
- Browse for a directory via dialog or type a path directly
- F5 to re-scan the current path

## Requirements

- Java 16 or later (uses pattern-matching `instanceof`)

## Running

```bat
run.bat
```

This compiles all sources and launches the application. No build tool required.

## Project structure

```
src/
  main/java/        Java source files
out/classes/        Compiled output (generated, not committed)
run.bat             Compile-and-run script
```
