package wasootch.discus;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

class DirectoryNode extends DefaultMutableTreeNode {
    private final Path path;
    private long size;
    private int fileCount;
    private int directoryCount;

    DirectoryNode(Path path) {
        super(path.getFileName() != null ? path.getFileName().toString() : path.toString());
        this.path = path;
    }

    void addSize(long delta) { this.size += delta; }
    void addFile() { fileCount++; }

    public void add(DirectoryNode child) {
        super.add(child);
        directoryCount++;
    }

    long getSize() { return size; }
    int getFileCount() { return fileCount; }
    int getDirectoryCount() { return directoryCount; }
    Path getFilePath() { return path; }

    @Override
    public String toString() {
        String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
        return name + " (" + DiskSpaceAnalyzer.formatSize(size) + ")";
    }
}
