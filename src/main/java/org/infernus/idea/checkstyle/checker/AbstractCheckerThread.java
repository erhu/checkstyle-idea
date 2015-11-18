package org.infernus.idea.checkstyle.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.ignore.IIgnoreHook;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractCheckerThread extends Thread {

    private final List<PsiFile> files = new ArrayList<>();
    private final Map<Module, Set<PsiFile>> moduleToFiles = new HashMap<>();
    private final CheckStylePlugin plugin;
    private final ConfigurationLocation overrideConfigLocation;

    private Map<PsiFile, List<Problem>> fileResults;
    private ConfigurationLocationStatus configurationLocationStatus = ConfigurationLocationStatus.PRESENT;
    private boolean running = true;

    AbstractCheckerThread(@NotNull final CheckStylePlugin checkStylePlugin,
                          @NotNull final List<VirtualFile> virtualFiles,
                          @Nullable final ConfigurationLocation overrideConfigLocation,
                          @Nullable final IIgnoreHook ignoreHook) {
        this.plugin = checkStylePlugin;
        this.overrideConfigLocation = overrideConfigLocation;
        final PsiManager psiManager = PsiManager.getInstance(this.plugin.getProject());
        for (final VirtualFile virtualFile : virtualFiles) {
            buildFilesList(psiManager, virtualFile);
        }

        for (final PsiFile file : files) {
            if (ignoreHook != null && ignoreHook.shouldIgnore(file)) {
                continue;
            }

            final Module module = ModuleUtil.findModuleForPsiElement(file);
            Set<PsiFile> filesForModule = moduleToFiles.get(module);
            if (filesForModule == null) {
                filesForModule = new HashSet<>();
                moduleToFiles.put(module, filesForModule);
            }
            filesForModule.add(file);
        }
    }

    Map<PsiFile, List<Problem>> getFileResults() {
        return fileResults;
    }

    void setFileResults(final Map<PsiFile, List<Problem>> fileResults) {
        this.fileResults = fileResults;
    }

    List<PsiFile> getFiles() {
        return files;
    }

    @Nullable
    CheckStyleToolWindowPanel toolWindowPanel() {
        return CheckStyleToolWindowPanel.panelFor(plugin.getProject());
    }

    void markThreadComplete() {
        plugin.setThreadComplete(this);
    }

    private synchronized boolean isRunning() {
        return running;
    }

    synchronized void setRunning(final boolean running) {
        this.running = running;
    }

    ConfigurationLocationStatus getConfigurationLocationStatus() {
        return configurationLocationStatus;
    }

    public void stopCheck() {
        setRunning(false);
    }

    /**
     * Process each virtual file, adding to the map or finding children if a container.
     *
     * @param psiManager  the current manager.
     * @param virtualFile the file to process.
     */
    private void buildFilesList(final PsiManager psiManager, final VirtualFile virtualFile) {
        ApplicationManager.getApplication().runReadAction(() -> {
            VfsUtilCore.visitChildrenRecursively(virtualFile, new AddChildFiles(virtualFile, psiManager));
        });
    }

    void processFilesForModuleInfoAndScan() throws Throwable {
        for (final Module module : moduleToFiles.keySet()) {
            if (!isRunning()) {
                break;
            }
            if (module == null) {
                continue;
            }

            final Set<PsiFile> filesForModule = moduleToFiles.get(module);

            final FileScanner fileScanner = new FileScanner(plugin, filesForModule, overrideConfigLocation);
            runFileScanner(fileScanner);

            configurationLocationStatus = fileScanner.getConfigurationLocationStatus();

            //noinspection ThrowableResultOfMethodCallIgnored
            if (fileScanner.getError() != null) {
                throw fileScanner.getError();
            }

            for (final PsiFile psiFile : filesForModule) {
                final List<Problem> resultsForFile = fileScanner.getResults().get(psiFile);
                if (resultsForFile != null && !resultsForFile.isEmpty()) {
                    getFileResults().put(psiFile, new ArrayList<>(resultsForFile));
                }
            }
        }
    }

    protected abstract void runFileScanner(FileScanner fileScanner);

    private class AddChildFiles extends VirtualFileVisitor {
        private final VirtualFile virtualFile;
        private final PsiManager psiManager;

        AddChildFiles(final VirtualFile virtualFile, final PsiManager psiManager) {
            this.virtualFile = virtualFile;
            this.psiManager = psiManager;
        }

        @Override
        public boolean visitFile(@NotNull final VirtualFile file) {
            if (!file.isDirectory()) {
                final PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    files.add(psiFile);
                }
            }
            return true;
        }
    }
}
