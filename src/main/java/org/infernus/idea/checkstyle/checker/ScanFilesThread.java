package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.ignore.IIgnoreHook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ScanFilesThread extends AbstractCheckerThread {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(ScanFilesThread.class);

    /**
     * Scan Files and store results.
     *
     * @param checkStylePlugin reference to the CheckStylePlugin
     * @param vFiles           files to scan
     * @param results          Map to store scan results
     */
    public ScanFilesThread(@NotNull final CheckStylePlugin checkStylePlugin,
                           @NotNull final List<VirtualFile> vFiles,
                           @NotNull final Map<PsiFile, List<Problem>> results,
                           @Nullable final IIgnoreHook ignoreHook) {
        super(checkStylePlugin, vFiles, null, ignoreHook);
        this.setFileResults(results);
    }

    /**
     * Run scan against files.
     */
    public void run() {
        setRunning(true);

        try {
            this.processFilesForModuleInfoAndScan();

        } catch (final Throwable e) {
            final CheckStylePluginException processedError = CheckStylePluginException.wrap(
                    "An error occurred during a file scan.", e);

            if (processedError != null) {
                LOG.error("An error occurred while scanning a file.",
                        processedError);
            }
        }
    }


    public void runFileScanner(final FileScanner fileScanner) {
        fileScanner.run();
    }

}
