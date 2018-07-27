package io.jenkins.plugins.linenotify;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.security.ACL;
import hudson.util.ListBoxModel.Option;
import java.util.ArrayList;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class LineNotifyNotifier extends Notifier implements SimpleBuildStep {

    private static final String apiUrl = "https://notify-api.line.me/api/notify";
    @CheckForNull
    private String credentialsId;
    private String when = DescriptorImpl.defaultWhen;
    private String message = DescriptorImpl.defaultMessage;
    private String imageThumbnail = DescriptorImpl.defaultImageThumbnail;
    private String imageFullsize = DescriptorImpl.defaultImageFullsize;

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getWhen() {
        return when;
    }

    public String getMessage() {
        return message;
    }

    public String getImageThumbnail() {
        return imageThumbnail;
    }

    public String getImageFullsize() {
        return imageFullsize;
    }

    @DataBoundSetter
    public void setCredentialsId(@Nonnull String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setWhen(String when) {
        this.when = when;
    }

    @DataBoundSetter
    public void setMessage(String message) {
        this.message = message;
    }

    @DataBoundSetter
    public void setImageThumbnail(String imageThumbnail) {
        this.imageThumbnail = imageThumbnail;
    }

    @DataBoundSetter
    public void setImageFullsize(String imageFullsize) {
        this.imageFullsize = imageFullsize;
    }

    @DataBoundConstructor
    public LineNotifyNotifier() {
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath fp, Launcher lnchr, TaskListener tl) throws InterruptedException, IOException {
        final Result buildResult = run.getResult();
        final Result whenResult = getWhenResult();

        if (isInValidNotifyStatus(buildResult, whenResult)) {
            tl.getLogger().println("Do not notify because BuildResult is different from the value set.");
            return;
        }

        HttpURLConnection con = null;
        try {
            URL url = new URL(apiUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + getToken(run));
            con.setRequestProperty("Accept", "multipart/form-data");

            String sendMessage = getSendMessage(run);
            String parameters = "message=" + URLEncoder.encode(sendMessage, "UTF-8");
            if (!"".equals(imageThumbnail) && "".equals(imageThumbnail)) {
                parameters += "&imageThumbnail=" + URLEncoder.encode(imageThumbnail, "UTF-8");
            }
            if (!"".equals(imageFullsize) && "".equals(imageFullsize)) {
                parameters += "&imageFullsize=" + URLEncoder.encode(imageFullsize, "UTF-8");
            }

            byte[] postData = parameters.getBytes("UTF-8");

            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postData);
            }

            StringBuilder content;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                String line;
                content = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
                tl.getLogger().println(content.toString());
            }
        } catch (IOException e) {
            e.printStackTrace(tl.getLogger());
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private Boolean isInValidNotifyStatus(Result buildResult, Result whenResult) {
        if (buildResult == null || whenResult == null) {
            return false;
        } else {
            return buildResult.ordinal != whenResult.ordinal;
        }
    }

    private String getToken(Run<?, ?> run) {
        StringCredentials token = CredentialsProvider.findCredentialById(credentialsId, StringCredentials.class, run, new ArrayList<DomainRequirement>());
        if (token != null) {
            return token.getSecret().getPlainText();
        }
        return "";
    }

    private Result getWhenResult() {
        if ("ALL".equalsIgnoreCase(when)) {
            return null;
        }
        if ("SUCCESS".equalsIgnoreCase(when)) {
            return Result.SUCCESS;
        } else {
            return Result.FAILURE;
        }
    }

    private String getSendMessage(Run<?, ?> run) {
        String sendMessage = replaceAdditionalEnvironmentalVariables(message, run);
        return sendMessage;
    }

    private String replaceAdditionalEnvironmentalVariables(String input, Run<?, ?> run) {
        if (run == null) {
            return input;
        }
        String buildResult;
        Result result = run.getResult();
        if (result == null) {
            buildResult = "UNKNOWN";
        } else {
            buildResult = result.toString();
        }
        String buildDuration = run.getDurationString().replaceAll("and counting", "");
        input = input.replaceAll("\\$BUILDRESULT", buildResult);
        input = input.replaceAll("\\$BUILDDURATION", buildDuration);
        input = input.replaceAll("\\$PROJECTNAME", run.getFullDisplayName());
        return input;
    }

    @Symbol("lineNotify")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public static final String defaultMessage = "$PROJECTNAME $BUILDRESULT $BUILDDURATION";
        public static final String defaultWhen = "SUCCESS";
        public static final String defaultImageThumbnail = "";
        public static final String defaultImageFullsize = "";
        private static final String[] optionsWhen = new String[]{"ALL", "SUCCESS", "FAILURE"};

        private String tokenCredentialId;

        public String getTokenCredentialId() {
            return tokenCredentialId;
        }

        public void setTokenCredentialId(String tokenCredentialId) {
            this.tokenCredentialId = tokenCredentialId;
        }

        public ListBoxModel doFillCredentialsIdItems(String credentialsId) {
            if (credentialsId == null) {
                credentialsId = "";
            }

            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel()
                        .includeCurrentValue(credentialsId);
            }

            return new StandardListBoxModel()
                    .includeEmptyValue().includeAs(ACL.SYSTEM, Jenkins.getInstance(), StringCredentials.class)
                    .includeCurrentValue(credentialsId);
        }

        public ListBoxModel doFillWhenItems(@QueryParameter String when) {
            String selectedValue = StringUtils.isBlank(when) ? defaultWhen : when;
            StandardListBoxModel listbox = new StandardListBoxModel();
            for (String option : optionsWhen) {
                listbox.add(new Option(option, option, option.equals((selectedValue))));
            }
            listbox.includeCurrentValue(selectedValue);
            return listbox;
        }

        @Override
        public String getDisplayName() {
            return "Naver Line Notification";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> type) {
            return true;
        }
    }
}
