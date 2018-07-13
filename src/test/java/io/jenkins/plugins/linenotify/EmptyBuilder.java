package io.jenkins.plugins.linenotify;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

public class EmptyBuilder extends Builder implements SimpleBuildStep {

    private final String name;

    @DataBoundConstructor
    public EmptyBuilder(String name) {
        this.name = name;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath fp, Launcher lnchr, TaskListener tl) throws InterruptedException, IOException {
        tl.getLogger().println("Bonjour 1234567890");
    }
}
