package org.infernus.idea.checkstyle.ignore;

import com.intellij.psi.PsiFile;

/**
 * IIgnoreHook
 * <p>
 * Created by lisper on 15/11/6.
 */
public interface IIgnoreHook {

    /**
     * Check if should ignore this file when scan.
     *
     * @param file
     * @return true if need ignore, false not.
     */
    boolean shouldIgnore(final PsiFile file);
}
