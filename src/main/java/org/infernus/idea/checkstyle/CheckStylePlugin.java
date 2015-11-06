package org.infernus.idea.checkstyle;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.infernus.idea.checkstyle.checker.*;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.ignore.IIgnoreHook;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * Main class for the CheckStyle scanning plug-in.
 */
public final class CheckStylePlugin implements ProjectComponent {

    public static final String ID_PLUGIN = "CheckStyle-IDEA";
    public static final String ID_MODULE_PLUGIN = "CheckStyle-IDEA-Module";

    private static final Log LOG = LogFactory.getLog(CheckStylePlugin.class);

    private final Set<AbstractCheckerThread> checksInProgress = new HashSet<>();
    private final Project project;
    private final CheckStyleConfiguration configuration;

    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public CheckStylePlugin(@NotNull final Project project) {
        this.project = project;
        this.configuration = ServiceManager.getService(project, CheckStyleConfiguration.class);

        LOG.info("CheckStyle Plugin loaded with project base dir: \"" + getProjectPath() + "\"");

        disableCheckStyleLogging();
    }

    private void disableCheckStyleLogging() {
        try {
            // This is a nasty hack to get around IDEA's DialogAppender sending any errors to the Event Log,
            // which would result in CheckStyle parse errors spamming the Event Log.
            Logger.getLogger(TreeWalker.class).setLevel(Level.OFF);
        } catch (Exception e) {
            LOG.error("Unable to suppress logging from CheckStyle's TreeWalker", e);
        }
    }

    public Project getProject() {
        return project;
    }

    @Nullable
    private File getProjectPath() {
        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        return new File(baseDir.getPath());
    }

    /**
     * Get the plugin configuration.
     *
     * @return the plug-in configuration.
     */
    public CheckStyleConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Is a scan in progress?
     * <p>
     * This is only expected to be called from the event thread.
     *
     * @return true if a scan is in progress.
     */
    public boolean isScanInProgress() {
        synchronized (checksInProgress) {
            return checksInProgress.size() > 0;
        }
    }

    public void projectOpened() {
        LOG.debug("Project opened.");
    }

    public void projectClosed() {
        LOG.debug("Project closed; invalidating checkers.");

        invalidateCheckerCache();
    }

    private void invalidateCheckerCache() {
        ServiceManager.getService(CheckerFactoryCache.class).invalidate();
    }

    @NotNull
    public String getComponentName() {
        return ID_PLUGIN;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    public static void processErrorAndLog(@NotNull final String action,
                                          @NotNull final Throwable e) {
        final CheckStylePluginException processed = CheckStylePluginException.wrap(e);
        if (processed != null) {
            LOG.error(action + " failed", processed);
        }
    }

    public void checkFiles(final List<VirtualFile> files,
                           final ConfigurationLocation overrideConfigLocation,
                           @Nullable final IIgnoreHook ignoreHook) {
        LOG.info("Scanning current file(s).");

        if (files == null || files.isEmpty()) {
            LOG.debug("No files provided.");
            return;
        }
        final CheckFilesThread checkFilesThread = new CheckFilesThread(this, files, overrideConfigLocation, ignoreHook);
        checkFilesThread.setPriority(Thread.MIN_PRIORITY);

        synchronized (checksInProgress) {
            checksInProgress.add(checkFilesThread);
        }

        checkFilesThread.start();
    }

    /**
     * Stop any checks in progress.
     */
    public void stopChecks() {
        synchronized (checksInProgress) {
            checksInProgress.forEach(AbstractCheckerThread::stopCheck);
            checksInProgress.clear();
        }
    }

    /**
     * Mark a thread as complete.
     *
     * @param thread the thread to mark.
     */
    public void setThreadComplete(final AbstractCheckerThread thread) {
        if (thread == null) {
            return;
        }

        synchronized (checksInProgress) {
            checksInProgress.remove(thread);
        }
    }

    public Map<PsiFile, List<Problem>> scanFiles(@NotNull final List<VirtualFile> files,
                                                 @Nullable final IIgnoreHook ignoreHook) {
        final Map<PsiFile, List<Problem>> results = new HashMap<>();

        if (files.isEmpty()) {
            return results;
        }

        final ScanFilesThread scanFilesThread = new ScanFilesThread(this, files, results, ignoreHook);

        synchronized (checksInProgress) {
            checksInProgress.add(scanFilesThread);
        }

        scanFilesThread.start();
        try {
            scanFilesThread.join();

        } catch (final Throwable e) {
            LOG.error("Error scanning files");

        } finally {
            synchronized (checksInProgress) {
                checksInProgress.remove(scanFilesThread);
            }
        }
        return results;
    }

    public ConfigurationLocation getConfigurationLocation(@Nullable final Module module,
                                                          @Nullable final ConfigurationLocation override) {
        if (override != null) {
            return override;
        }

        if (module != null) {
            final CheckStyleModuleConfiguration moduleConfiguration
                    = ModuleServiceManager.getService(module, CheckStyleModuleConfiguration.class);
            if (moduleConfiguration == null) {
                throw new IllegalStateException("Couldn't get checkstyle module configuration");
            }

            if (moduleConfiguration.isExcluded()) {
                return null;
            }
            return moduleConfiguration.getActiveConfiguration();

        }
        return getConfiguration().getActiveConfiguration();
    }

}
