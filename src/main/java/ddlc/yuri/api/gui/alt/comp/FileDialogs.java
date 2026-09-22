package ddlc.yuri.api.gui.alt.comp;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class FileDialogs {

    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean LINUX = OS_NAME.contains("linux") || OS_NAME.contains("nix") || OS_NAME.contains("bsd");
    private static final boolean MAC = OS_NAME.contains("mac");

    private FileDialogs() {
    }

    public static File openFile(String title, String filterName, String... extensions) {
        Result result = null;
        if (LINUX) result = linuxDialog(title, filterName, extensions);
        else if (MAC) result = macDialog(title, extensions);
        if (result == null) return swingDialog(title, filterName, extensions);
        return result.file;
    }

    private static final class Result {
        final File file;

        Result(File file) {
            this.file = file;
        }
    }

    private static Result linuxDialog(String title, String filterName, String[] extensions) {
        StringBuilder pattern = new StringBuilder();
        for (String extension : extensions) pattern.append("*.").append(extension).append(' ');

        List<String[]> candidates = new ArrayList<>();
        candidates.add(new String[]{"zenity", "--file-selection", "--title=" + title,
                "--file-filter=" + filterName + " | " + pattern.toString().trim(), "--file-filter=All files | *"});
        candidates.add(new String[]{"kdialog", "--title", title, "--getopenfilename", System.getProperty("user.home", "."),
                pattern.toString().trim() + "|" + filterName});
        candidates.add(new String[]{"yad", "--file", "--title=" + title, "--file-filter=" + filterName + " | " + pattern.toString().trim()});

        for (String[] command : candidates) {
            String output = run(command);
            if (output == null) continue;
            String path = output.trim();
            File file = path.isEmpty() ? null : new File(path);
            return new Result(file != null && file.isFile() ? file : null);
        }
        return null;
    }

    private static Result macDialog(String title, String[] extensions) {
        StringBuilder types = new StringBuilder();
        for (String extension : extensions) {
            if (types.length() > 0) types.append(", ");
            types.append('"').append(extension).append('"');
        }
        String script = "POSIX path of (choose file with prompt \"" + title + "\" of type {" + types + "})";
        String output = run(new String[]{"osascript", "-e", script});
        if (output == null) return null;
        String path = output.trim();
        return new Result(path.isEmpty() ? null : new File(path));
    }

    private static File swingDialog(String title, String filterName, String[] extensions) {
        AtomicReference<File> chosen = new AtomicReference<>();
        Runnable show = () -> {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(title);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (extensions.length > 0) chooser.setFileFilter(new FileNameExtensionFilter(filterName, extensions));
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chosen.set(chooser.getSelectedFile());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) show.run();
            else SwingUtilities.invokeAndWait(show);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chosen.get();
    }

    private static String run(String[] command) {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(false).start();
        } catch (IOException e) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append('\n');
            if (!process.waitFor(10, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                return "";
            }
            return process.exitValue() <= 1 ? output.toString() : "";
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }
}
