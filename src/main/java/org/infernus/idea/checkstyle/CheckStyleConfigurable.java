package org.infernus.idea.checkstyle;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.checker.CheckerFactoryCache;
import org.infernus.idea.checkstyle.checker.ModuleClassPathBuilder;
import org.infernus.idea.checkstyle.ui.CheckStyleConfigPanel;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

public class CheckStyleConfigurable implements Configurable {
    private static final Log LOG = LogFactory.getLog(CheckStyleConfigurable.class);

    private final Project project;

    private final CheckStyleConfigPanel configPanel;

    public CheckStyleConfigurable(@NotNull final Project project) {
        this.project = project;

        configPanel = new CheckStyleConfigPanel(project);
    }

    public String getDisplayName() {
        return CheckStyleBundle.message("plugin.configuration-name");
    }

    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        reset();

        return configPanel;
    }

    public boolean isModified() {
        try {
            return configPanel.isModified();

        } catch (IOException e) {
            LOG.error("Failed to read properties from one of " + configPanel.getConfigurationLocations(), e);
            Notifications.showError(project, CheckStyleBundle.message("checkstyle.file-not-found"));
            return true;
        }
    }

    public void apply() throws ConfigurationException {
        final CheckStyleConfiguration configuration = getConfiguration();
        configuration.setConfigurationLocations(configPanel.getConfigurationLocations());
        configuration.setActiveConfiguration(configPanel.getActiveLocation());

        configuration.setScanningTestClasses(configPanel.isScanTestClasses());
        configuration.setScanningNonJavaFiles(configPanel.isScanNonJavaFiles());
        configuration.setSuppressingErrors(configPanel.isSuppressingErrors());

        final List<String> thirdPartyClasspath
                = configPanel.getThirdPartyClasspath();
        configuration.setThirdPartyClassPath(thirdPartyClasspath);

        reset(); // save current data as unmodified

        getCheckerFactoryCache().invalidate();

        resetModuleClassBuilder();
    }

    private void resetModuleClassBuilder() {
        final ModuleClassPathBuilder moduleClassPathBuilder = ServiceManager.getService(project, ModuleClassPathBuilder.class);
        if (moduleClassPathBuilder != null) {
            moduleClassPathBuilder.reset();
        }
    }

    private CheckStyleConfiguration getConfiguration() {
        return ServiceManager.getService(project, CheckStyleConfiguration.class);
    }

    private CheckerFactoryCache getCheckerFactoryCache() {
        return ServiceManager.getService(CheckerFactoryCache.class);
    }

    public void reset() {
        final CheckStyleConfiguration configuration = getConfiguration();
        configPanel.setConfigurationLocations(configuration.getConfigurationLocations());
        configPanel.setPresetLocations(configuration.getPresetLocations());
        configPanel.setActiveLocation(configuration.getActiveConfiguration());
        configPanel.setScanTestClasses(configuration.isScanningTestClasses());
        configPanel.setScanNonJavaFiles(configuration.isScanningNonJavaFiles());
        configPanel.setSuppressingErrors(configuration.isSuppressingErrors());
        configPanel.setThirdPartyClasspath(configuration.getThirdPartyClassPath());
    }

    public void disposeUIResources() {

    }

}
