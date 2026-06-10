package wasootch.discus;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.nio.file.Path;

import static wasootch.discus.SizeFormatter.formatSize;

class SizeTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof DirectoryNode node) {
            Path path = node.getFilePath();
            String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();

            if (node.isScanning()) {
                setFont(getFont().deriveFont(Font.ITALIC));
                setText(name + " [scanning...]");
                setForeground(sel ? getTextSelectionColor() : new Color(0, 100, 200));
            } else if (!node.isScanned()) {
                setText(name + " [not scanned]");
                setForeground(sel ? getTextSelectionColor() : Color.GRAY);
            } else {
                setFont(leaf ? getFont() : getFont().deriveFont(Font.BOLD));
                setText(String.format("%s - %s [%d files, %d dirs]",
                        name, formatSize(node.getSize()), node.getFileCount(), node.getDirectoryCount()));

                if (node.getSize() > 10L * 1024 * 1024 * 1024) {
                    setForeground(new Color(200, 0, 0));
                } else if (node.getSize() > 1024 * 1024 * 1024) {
                    setForeground(new Color(200, 100, 0));
                } else {
                    setForeground(sel ? getTextSelectionColor() : getTextNonSelectionColor());
                }
            }
        }

        return this;
    }
}