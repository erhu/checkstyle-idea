package org.infernus.idea.checkstyle.ignore;

import com.intellij.psi.PsiFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

import static org.infernus.idea.checkstyle.ignore.IgnoreType.DIR;
import static org.infernus.idea.checkstyle.ignore.IgnoreType.FILE;
import static org.infernus.idea.checkstyle.ignore.IgnoreType.FILE_TYPE;

/**
 * IIgnoreHookImpl
 * <p>
 * Created by lisper on 15/11/6.
 */
public class IgnoreHookImpl implements IIgnoreHook {

    private static final Log LOG = LogFactory.getLog(IgnoreHookImpl.class);

    private final Collection<IgnoreItem> ignoreFileList;

    public IgnoreHookImpl(final Collection<IgnoreItem> iList) {
        this.ignoreFileList = iList;
    }

    @Override
    public boolean shouldIgnore(final PsiFile file) {
        try {
            // absolute path
            String filePath = file.getViewProvider().getVirtualFile().getPath();

            // doCheck
            for (IgnoreItem it : ignoreFileList) {
                if ((it.type == FILE && filePath.equals(it.content)) // src/a/b/c.java
                        || (it.type == DIR && filePath.contains(it.content)) // src/a/b/c/
                        || (it.type == FILE_TYPE && filePath.endsWith(it.content) && filePath.contains(it.extra))) { // *.class
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.error(e.toString());
        }
        return false;
    }
}
