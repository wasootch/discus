package wasootch.discus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryNodeTest {

    private DirectoryNode node;

    @BeforeEach
    void setUp() {
        node = new DirectoryNode(Path.of("testdir"));
    }

    // --- Initial state ---

    @Test
    void initialStateIsNotScanned() {
        assertFalse(node.isScanned());
        assertFalse(node.isScanning());
        assertEquals(0, node.getSize());
        assertEquals(0, node.getFileCount());
        assertEquals(0, node.getDirectoryCount());
        assertEquals(0, node.getChildCount());
    }

    // --- State transitions ---

    @Test
    void markScanningSetsFlag() {
        node.markScanning();
        assertTrue(node.isScanning());
        assertFalse(node.isScanned());
    }

    @Test
    void markScannedClearsScanningFlag() {
        node.markScanning();
        node.markScanned();
        assertTrue(node.isScanned());
        assertFalse(node.isScanning());
    }

    // --- Counter accumulation ---

    @Test
    void addSizeAccumulates() {
        node.addSize(100);
        node.addSize(200);
        assertEquals(300, node.getSize());
    }

    @Test
    void addFileAccumulates() {
        node.addFile();
        node.addFile();
        assertEquals(2, node.getFileCount());
    }

    @Test
    void addFilesAccumulates() {
        node.addFile();
        node.addFiles(5);
        assertEquals(6, node.getFileCount());
    }

    @Test
    void addIncrementsDirCountAndAddsChild() {
        DirectoryNode child1 = new DirectoryNode(Path.of("child1"));
        DirectoryNode child2 = new DirectoryNode(Path.of("child2"));
        node.add(child1);
        node.add(child2);
        assertEquals(2, node.getDirectoryCount());
        assertEquals(2, node.getChildCount());
    }

    @Test
    void addDirsAccumulatesOnTopOfDirectAdd() {
        node.add(new DirectoryNode(Path.of("child")));
        node.addDirs(3);
        assertEquals(4, node.getDirectoryCount());
    }

    // --- reset() ---

    @Test
    void resetClearsAllStateAndChildren() {
        node.addSize(500);
        node.addFile();
        node.addFiles(10);
        node.add(new DirectoryNode(Path.of("child")));
        node.markScanning();

        node.reset();

        assertEquals(0, node.getSize());
        assertEquals(0, node.getFileCount());
        assertEquals(0, node.getDirectoryCount());
        assertFalse(node.isScanning());
        assertFalse(node.isScanned());
        assertEquals(0, node.getChildCount());
    }

    @Test
    void resetAllowsRescanFromCleanState() {
        node.addSize(100);
        node.addFile();
        node.markScanned();

        node.reset();
        node.markScanning();
        node.addSize(200);
        node.addFile();
        node.addFile();
        node.markScanned();

        assertEquals(200, node.getSize());
        assertEquals(2, node.getFileCount());
        assertTrue(node.isScanned());
    }

    // --- toString() ---

    @Test
    void toStringNotScanned() {
        assertEquals("testdir [not scanned]", node.toString());
    }

    @Test
    void toStringScanning() {
        node.markScanning();
        assertEquals("testdir [scanning...]", node.toString());
    }

    @Test
    void toStringScanningTakesPrecedenceOverScanned() {
        // markScanned clears scanning, but verify the order matters in toString
        node.markScanning();
        assertFalse(node.isScanned());
        assertTrue(node.isScanning());
        assertEquals("testdir [scanning...]", node.toString());
    }

    @Test
    void toStringScannedNoSize() {
        node.markScanned();
        assertEquals("testdir (0 B)", node.toString());
    }

    @Test
    void toStringScannedWithSize() {
        node.addSize(1024);
        node.markScanned();
        assertEquals("testdir (1.00 KB)", node.toString());
    }

    // --- getFilePath() ---

    @Test
    void getFilePathReturnsConstructorPath() {
        Path path = Path.of("some", "nested", "dir");
        assertEquals(path, new DirectoryNode(path).getFilePath());
    }
}