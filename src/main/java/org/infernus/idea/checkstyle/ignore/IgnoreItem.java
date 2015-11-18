package org.infernus.idea.checkstyle.ignore;

/**
 * IgnoreItem
 * <p>
 * Created by lisper on 15/11/17.
 */
public class IgnoreItem {

    final String content;
    String extra;
    final IgnoreType type;

    public IgnoreItem(String content, IgnoreType type) {
        this.content = content;
        this.type = type;
    }

    @Override
    public int hashCode() {
        if (content != null) {
            return content.hashCode();
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IgnoreItem && content.equals(obj);
    }
}
