package io.jenkins.plugins.linenotify;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;

public class LineNotifyNotifier extends Notifier {

    private static final String apiUrl = "https://notify-api.line.me/api/notify";
    private final String token;
    private final String when;
    private String message;
    private String imageThumbnail;
    private String imageFullsize;

    public String getWhen() {
        return when;
    }

    public String getToken() {
        return token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImageThumbnail() {
        return imageThumbnail;
    }

    @DataBoundSetter
    public void setImageThumbnail(String imageThumbnail) {
        this.imageThumbnail = imageThumbnail;
    }

    @DataBoundSetter
    public String getImageFullsize() {
        return imageFullsize;
    }

    @DataBoundSetter
    public void setImageFullsize(String imageFullsize) {
        this.imageFullsize = imageFullsize;
    }

    @DataBoundConstructor
    public LineNotifyNotifier(String token, String when) {
        super();
        this.token = token;
        this.when = when;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        final Result buildResult = build.getResult();
        final Result whenResult = getWhenResult();
        if (whenResult != null && !buildResult.equals(whenResult)) {
            listener.getLogger().println("Do not notify because BuildResult is different from the value set.");
            return true;
        }

        HttpURLConnection con = null;
        try {
            URL url = new URL(apiUrl);

            con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + token);
            con.setRequestProperty("Accept", "multipart/form-data");

            String sendMessage = getSendMessage(build);
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

                listener.getLogger().println(content.toString());
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace(listener.getLogger());
            return false;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
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

    private String getSendMessage(AbstractBuild<?, ?> build) {
        String msg = message;
        if (msg == null || "".equals(msg)) {
            msg = "$PROJECTNAME $BUILDRESULT $BUILDDURATION";
        }
        String sendMessage = replaceAdditionalEnvironmentalVariables(msg, build);
        return sendMessage;
    }

    private String replaceAdditionalEnvironmentalVariables(String input, AbstractBuild<?, ?> build) {
        if (build == null) {
            return input;
        }
        String buildResult = "";
        Result result = build.getResult();
        if (result != null) {
            buildResult = result.toString();
        }
        String buildDuration = build.getDurationString().replaceAll("and counting", "");
        input = input.replaceAll("\\$BUILDRESULT", buildResult);
        input = input.replaceAll("\\$BUILDDURATION", buildDuration);
        input = input.replaceAll("\\$PROJECTNAME", build.getProject().getName());

        return input;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public String getDisplayName() {
            return "Line Notification";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> type) {
            return true;
        }
    }
}
