package io.jenkins.plugins.linenotify;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class LineNotifyNotifierTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    @Ignore
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("TestProject");
        LineNotifyNotifier builder = new LineNotifyNotifier("SET TOKEN", "SUCCESS");
        EmptyBuilder emptyBuilder = new EmptyBuilder("test");
        project.getBuildersList().add(emptyBuilder);
        project.getPublishersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
    }
}
