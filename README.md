# discus

A desktop disk space analyzer that visualizes folder sizes in an interactive tree view.

## Features

- Scans any directory and displays folder sizes in a collapsible tree
- Automatically scans the system drive on launch
- Live updates as the scan progresses
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
