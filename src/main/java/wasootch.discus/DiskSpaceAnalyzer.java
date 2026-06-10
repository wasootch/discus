package wasootch.discus;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DiskSpaceAnalyzer extends JFrame {
    private static final Logger logger = Logger.getLogger(DiskSpaceAnalyzer.class.getName());

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

        JMenuItem scanFolderItem = new JMenuItem("Scan this folder");
        scanFolderItem.addActionListener(e -> {
            TreePath selected = tree.getSelectionPath();
            if (selected != null && selected.getLastPathComponent() instanceof DirectoryNode node) {
                rescanNode(node);
            }
        });
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(scanFolderItem);

        tree.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { maybeShowPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }

            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) return;
                tree.setSelectionPath(path);
                scanFolderItem.setEnabled(scanButton.isEnabled());
                contextMenu.show(tree, e.getX(), e.getY());
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

    private void populateRoot(String path) {
        File dir = new File(path);
        Path dirPath = dir.toPath();
        DirectoryNode root = new DirectoryNode(dirPath);
        treeModel = new DefaultTreeModel(root, true);
        tree.setModel(treeModel);
        tree.expandPath(new TreePath(treeModel.getRoot()));

        executor.submit(() -> {
            List<DirectoryNode> children = new ArrayList<>();
            try (Stream<Path> files = Files.list(dirPath)) {
                files.filter(Files::isDirectory)
                     .map(DirectoryNode::new)
                     .forEach(children::add);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Cannot list root directory: " + dirPath, e);
            }
            SwingUtilities.invokeLater(() -> {
                children.forEach(root::add);
                treeModel.nodeStructureChanged(root);
            });
        });
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
        root.markScanning();
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
                    statusLabel.setText("Scan complete: " + SizeFormatter.formatSize(root.getSize()));
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

    private void rescanNode(DirectoryNode node) {
        long oldSize = node.getSize();
        int oldFileCount = node.getFileCount();
        int oldDirCount = node.getDirectoryCount();

        scanButton.setEnabled(false);
        cancelButton.setEnabled(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Scanning...");
        statusLabel.setText("Scanning: " + node.getFilePath());

        node.reset();
        node.markScanning();
        treeModel.nodeStructureChanged(node);

        scanFuture = executor.submit(() -> {
            try {
                scanDirectoryProgressive(node.getFilePath(), node);
                if (Thread.currentThread().isInterrupted()) {
                    SwingUtilities.invokeLater(() -> {
                        resetScanUI();
                        statusLabel.setText("Scan cancelled");
                        progressBar.setString("Cancelled");
                    });
                    return;
                }
                long sizeDelta = node.getSize() - oldSize;
                int fileCountDelta = node.getFileCount() - oldFileCount;
                int dirCountDelta = node.getDirectoryCount() - oldDirCount;
                SwingUtilities.invokeLater(() -> {
                    propagateDelta(node, sizeDelta, fileCountDelta, dirCountDelta);
                    updateSizeLabel();
                    resetScanUI();
                    progressBar.setString("Complete");
                    statusLabel.setText("Scan complete: " + SizeFormatter.formatSize(node.getSize()));
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

    private void propagateDelta(DirectoryNode node, long sizeDelta, int fileDelta, int dirDelta) {
        if (sizeDelta == 0 && fileDelta == 0 && dirDelta == 0) return;
        var parent = node.getParent();
        while (parent instanceof DirectoryNode parentNode) {
            parentNode.addSize(sizeDelta);
            parentNode.addFiles(fileDelta);
            parentNode.addDirs(dirDelta);
            treeModel.nodeChanged(parentNode);
            parent = parentNode.getParent();
        }
    }

    private void scanDirectoryProgressive(Path dir, DirectoryNode node) {
        if (Thread.currentThread().isInterrupted()) return;

        try (Stream<Path> stream = Files.list(dir)) {
            stream.forEach(child -> {
                if (Thread.currentThread().isInterrupted()) return;

                try {
                    if (Files.isDirectory(child)) {
                        DirectoryNode childNode = new DirectoryNode(child);
                        childNode.markScanning();
                        synchronized (node) { node.add(childNode); }
                        SwingUtilities.invokeLater(() -> treeModel.nodeStructureChanged(node));

                        scanDirectoryProgressive(child, childNode);
                        if (Thread.currentThread().isInterrupted()) return;

                        synchronized (node) {
                            node.addSize(childNode.getSize());
                            node.addFiles(childNode.getFileCount());
                            node.addDirs(childNode.getDirectoryCount());
                        }
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
                } catch (Exception e) {
                    logger.log(Level.FINE, "Skipping inaccessible path: " + child, e);
                }
            });
        } catch (Exception e) {
            logger.log(Level.FINE, "Cannot list directory: " + dir, e);
        }

        SwingUtilities.invokeLater(() -> {
            node.markScanned();
            treeModel.nodeChanged(node);
        });
    }

    private void cancelScan() {
        if (scanFuture != null && !scanFuture.isDone()) {
            scanFuture.cancel(true);
            cancelButton.setEnabled(false);
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
                    SizeFormatter.formatSize(node.getSize()), node.getFileCount(), node.getDirectoryCount()));
        } else if (treeModel != null && treeModel.getRoot() instanceof DirectoryNode root) {
            sizeLabel.setText("Total: " + SizeFormatter.formatSize(root.getSize()));
        }
    }

    @Override
    public void dispose() {
        executor.shutdownNow();
        super.dispose();
    }
}