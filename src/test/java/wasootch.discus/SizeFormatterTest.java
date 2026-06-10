package wasootch.discus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SizeFormatterTest {

    @Test
    void bytes() {
        assertEquals("0 B",    SizeFormatter.formatSize(0));
        assertEquals("1 B",    SizeFormatter.formatSize(1));
        assertEquals("1023 B", SizeFormatter.formatSize(1023));
    }

    @Test
    void kilobytes() {
        assertEquals("1.00 KB", SizeFormatter.formatSize(1024));
        assertEquals("1.50 KB", SizeFormatter.formatSize(1536));
        assertEquals("2.00 KB", SizeFormatter.formatSize(2048));
    }

    @Test
    void megabytes() {
        assertEquals("1.00 MB", SizeFormatter.formatSize(1024L * 1024));
        assertEquals("1.50 MB", SizeFormatter.formatSize(1024L * 1024 + 512 * 1024));
        assertEquals("2.00 MB", SizeFormatter.formatSize(2L * 1024 * 1024));
    }

    @Test
    void gigabytes() {
        assertEquals("1.00 GB", SizeFormatter.formatSize(1024L * 1024 * 1024));
        assertEquals("1.50 GB", SizeFormatter.formatSize(1024L * 1024 * 1024 + 512L * 1024 * 1024));
        assertEquals("2.00 GB", SizeFormatter.formatSize(2L * 1024 * 1024 * 1024));
    }

    @Test
    void terabytes() {
        assertEquals("1.00 TB", SizeFormatter.formatSize(1024L * 1024 * 1024 * 1024));
        assertEquals("2.00 TB", SizeFormatter.formatSize(2L * 1024 * 1024 * 1024 * 1024));
    }

    @Test
    void boundariesAreExclusive() {
        // Last value in each unit before crossing to the next
        assertEquals("1023 B",   SizeFormatter.formatSize(1023));
        assertEquals("1.00 KB",  SizeFormatter.formatSize(1024));
        assertEquals("1.00 MB",  SizeFormatter.formatSize(1024L * 1024));
        assertEquals("1.00 GB",  SizeFormatter.formatSize(1024L * 1024 * 1024));
        assertEquals("1.00 TB",  SizeFormatter.formatSize(1024L * 1024 * 1024 * 1024));
    }
}