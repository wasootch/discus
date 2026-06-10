package wasootch.discus;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

class DirectoryNode extends DefaultMutableTreeNode {
    private final Path path;
    private long size;
    private int fileCount;
    private int directoryCount;
    private boolean scanned;
    private boolean scanning;

    DirectoryNode(Path path) {
        super(path.getFileName() != null ? path.getFileName().toString() : path.toString());
        this.path = path;
    }

    void addSize(long delta) { this.size += delta; }
    void addFile() { fileCount++; }
    void markScanning() { scanning = true; }
    void markScanned() { scanned = true; scanning = false; }

    public void add(DirectoryNode child) {
        super.add(child);
        directoryCount++;
    }

    long getSize() { return size; }
    int getFileCount() { return fileCount; }
    int getDirectoryCount() { return directoryCount; }
    boolean isScanning() { return scanning; }
    boolean isScanned() { return scanned; }
    Path getFilePath() { return path; }

    @Override
    public String toString() {
        String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
        if (scanning) return name + " [scanning...]";
        if (!scanned) return name + " [not scanned]";
        return name + " (" + DiskSpaceAnalyzer.formatSize(size) + ")";
    }
}
