package wasootch.discus;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Disk Space Analyzer - Visual tool to analyze disk usage by folder
 */
public class DiskSpaceAnalyzer extends JFrame {
    private JTree tree;
    private DefaultTreeModel treeModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JTextField pathField;
    private JButton scanButton;
    private JButton cancelButton;
    private JLabel sizeLabel;
    private final ExecutorService executor;
    private Future<?> scanFuture;

    public DiskSpaceAnalyzer() {
        setTitle("Disk Space Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        executor = Executors.newSingleThreadExecutor();
        createUI();

        // Populate tree with the system drive on startup
        populateRoot(systemDrive());
    }

    /** Returns the primary system drive path (e.g. "C:\" on Windows, "/" on Unix). */
    private static String systemDrive() {
        String sysDrive = System.getenv("SystemDrive");
        if (sysDrive != null) return sysDrive + File.separator;
        File[] roots = File.listRoots();
        return roots.length > 0 ? roots[0].getAbsolutePath() : "/";
    }

    static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void createUI() {
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel pathLabel = new JLabel("Scan Path:");
        pathField = new JTextField(systemDrive());
        pathField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        scanButton = new JButton("Scan");
        scanButton.addActionListener(e -> scanDirectory());

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelScan());
        cancelButton.setEnabled(false);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseDirectory());

        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.add(pathLabel, BorderLayout.WEST);
        pathPanel.add(pathField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(scanButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(browseButton);

        topPanel.add(pathPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        tree = new JTree();
        tree.setCellRenderer(new SizeTreeCellRenderer());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(e -> updateSizeLabel());
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) tree.expandPath(path);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Directory Tree"));

        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");

        statusLabel = new JLabel("Ready to scan");
        sizeLabel = new JLabel("Total: 0 bytes");
        sizeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(sizeLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        getRootPane().getActionMap().put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scanDirectory();
            }
        });
    }

    private void browseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Directory to Scan");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void populateRoot(String path){
        File dir = new File(path);
        Path dirPath = dir.toPath();
        DirectoryNode root = new DirectoryNode(dirPath);
        treeModel = new DefaultTreeModel(root, true);
        tree.setModel(treeModel);
        tree.expandPath(new TreePath(treeModel.getRoot()));

        try (Stream<Path> files = Files.list(dirPath)){
            files.forEach(child -> {
                if (Files.isDirectory(child)) {
                    DirectoryNode childNode = new DirectoryNode(child);
                    root.add(childNode);
                    treeModel.nodeStructureChanged(root);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void scanDirectory() {
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid path",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Path does not exist or is not a directory",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        scanButton.setEnabled(false);
        cancelButton.setEnabled(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Scanning...");
        statusLabel.setText("Scanning: " + path);

        Path dirPath = dir.toPath();
        DirectoryNode root = new DirectoryNode(dirPath);
        treeModel = new DefaultTreeModel(root);
        tree.setModel(treeModel);
        tree.expandPath(new TreePath(treeModel.getRoot()));

        scanFuture = executor.submit(() -> {
            try {
                scanDirectoryProgressive(dirPath, root);
                if (Thread.currentThread().isInterrupted()) {
                    SwingUtilities.invokeLater(() -> {
                        resetScanUI();
                        statusLabel.setText("Scan cancelled");
                        progressBar.setString("Cancelled");
                    });
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    updateSizeLabel();
                    scanButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    progressBar.setIndeterminate(false);
                    progressBar.setString("Complete");
                    statusLabel.setText("Scan complete: " + formatSize(root.getSize()));
                });
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    SwingUtilities.invokeLater(() -> {
                        resetScanUI();
                        statusLabel.setText("Scan cancelled");
                        progressBar.setString("Cancelled");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "Error scanning directory: " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        resetScanUI();
                        progressBar.setString("Error");
                        statusLabel.setText("Scan failed");
                    });
                }
            }
        });
    }

    private void scanDirectoryProgressive(Path dir, DirectoryNode node) {
        if (Thread.currentThread().isInterrupted()) return;

        try (Stream<Path> stream = Files.list(dir).parallel()) {
            stream.forEach(child -> {
                if (Thread.currentThread().isInterrupted()) return;

                try {
                    if (Files.isDirectory(child)) {
                        DirectoryNode childNode = new DirectoryNode(child);
                        synchronized (node) { node.add(childNode); }
                        SwingUtilities.invokeLater(() -> treeModel.nodeStructureChanged(node));

                        scanDirectoryProgressive(child, childNode);
                        if (Thread.currentThread().isInterrupted()) return;

                        synchronized (node) { node.addSize(childNode.getSize()); }
                        SwingUtilities.invokeLater(() -> {
                            treeModel.nodeChanged(node);
                            treeModel.nodeChanged(childNode);
                        });
                    } else {
                        long size = Files.size(child);
                        synchronized (node) {
                            node.addSize(size);
                            node.addFile();
                        }
                        if (node.getFileCount() % 100 == 0) {
                            SwingUtilities.invokeLater(() -> treeModel.nodeChanged(node));
                        }
                    }
                } catch (Exception ignored) {
                    // Skip inaccessible files/directories
                }
            });
        } catch (Exception ignored) {
            // Skip inaccessible directories
        }

        SwingUtilities.invokeLater(() -> treeModel.nodeChanged(node));
    }

    private void cancelScan() {
        if (scanFuture != null && !scanFuture.isDone()) {
            scanFuture.cancel(true);
            resetScanUI();
            statusLabel.setText("Cancelling scan...");
            progressBar.setString("Cancelling...");
        }
    }

    private void resetScanUI() {
        scanButton.setEnabled(true);
        cancelButton.setEnabled(false);
        progressBar.setIndeterminate(false);
    }

    private void updateSizeLabel() {
        TreePath selection = tree.getSelectionPath();
        if (selection != null) {
            DirectoryNode node = (DirectoryNode) selection.getLastPathComponent();
            sizeLabel.setText(String.format("Size: %s | Files: %d | Directories: %d",
                    formatSize(node.getSize()), node.getFileCount(), node.getDirectoryCount()));
        } else if (treeModel != null && treeModel.getRoot() instanceof DirectoryNode root) {
            sizeLabel.setText("Total: " + formatSize(root.getSize()));
        }
    }

    static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new DiskSpaceAnalyzer().setVisible(true);
        });
    }

    @Override
    public void dispose() {
        executor.shutdown();
        super.dispose();
    }

    static class DirectoryNode extends DefaultMutableTreeNode {
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

    static class SizeTreeCellRenderer extends DefaultTreeCellRenderer {
        private Font boldFont;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DirectoryNode node) {
                Path path = node.getFilePath();
                String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
                setText(String.format("%s - %s [%d files, %d dirs]",
                        name, formatSize(node.getSize()), node.getFileCount(), node.getDirectoryCount()));

                if (boldFont == null) boldFont = getFont().deriveFont(Font.BOLD);
                setFont(leaf ? getFont() : boldFont);

                if (node.getSize() > 10L * 1024 * 1024 * 1024) {
                    setForeground(new Color(200, 0, 0));
                } else if (node.getSize() > 1024 * 1024 * 1024) {
                    setForeground(new Color(200, 100, 0));
                } else {
                    setForeground(sel ? getTextSelectionColor() : getTextNonSelectionColor());
                }
            }

            return this;
        }
    }
}