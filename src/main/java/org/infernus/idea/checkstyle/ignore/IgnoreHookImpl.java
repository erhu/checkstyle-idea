package org.infernus.idea.checkstyle.ignore;

import com.intellij.psi.PsiFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.List;

/**
 * IIgnoreHookImpl
 * <p>
 * Created by lisper on 15/11/6.
 */
public class IgnoreHookImpl implements IIgnoreHook {

    private static final Log LOG = LogFactory.getLog(IgnoreHookImpl.class);

    private final List<String> ignoreFileList;
    private final String projectRootPath;

    public IgnoreHookImpl(String pRoot, final List<String> iList) {
        this.projectRootPath = pRoot;
        this.ignoreFileList = iList;
    }

    @Override
    public boolean checkFile(final PsiFile file) {
        try {
            String dir = file.getViewProvider().getVirtualFile().getParent().toString();
            int srcIndex = dir.indexOf(projectRootPath) + projectRootPath.length() + 1;
            // filePath like this 'src/com/demo/checkstyle/Demo.java'
            String filePath = dir.substring(srcIndex) + File.separator + file.getName();
            return ignoreFileList.contains(filePath);
        } catch (Exception e) {
            LOG.error(e.toString());
        }
        return false;
    }
}
