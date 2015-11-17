package org.infernus.idea.checkstyle.ignore;

import com.intellij.psi.PsiFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

/**
 * IIgnoreHookImpl
 * <p>
 * Created by lisper on 15/11/6.
 */
public class IgnoreHookImpl implements IIgnoreHook {

    private static final Log LOG = LogFactory.getLog(IgnoreHookImpl.class);

    private final Collection<String> ignoreFileList;

    public IgnoreHookImpl(final Collection<String> iList) {
        this.ignoreFileList = iList;
    }

    @Override
    public boolean checkFile(final PsiFile file) {
        try {
            // absolute path
            return ignoreFileList.contains(file.getViewProvider().getVirtualFile().getPath());
        } catch (Exception e) {
            LOG.error(e.toString());
        }
        return false;
    }
}
