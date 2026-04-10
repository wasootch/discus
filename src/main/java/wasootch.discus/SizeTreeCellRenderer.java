package wasootch.discus;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.nio.file.Path;

import static wasootch.discus.DiskSpaceAnalyzer.formatSize;

class SizeTreeCellRenderer extends DefaultTreeCellRenderer {
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
