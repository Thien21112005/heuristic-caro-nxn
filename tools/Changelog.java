import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds CHANGELOG.md from Conventional Commit subjects.
 * Usage: java tools/Changelog.java <out.md> <version|git-range|date>...   (newest first)
 * e.g.   java tools/Changelog.java CHANGELOG.md "2.0.0|v1.2.0..HEAD|2026-10-30" "1.2.0|v1.1.0..v1.2.0|2026-10-28"
 * Only feat, fix and perf commits are listed; "!" or a BREAKING CHANGE footer is called out.
 */
public class Changelog {
    private static final Pattern SUBJECT = Pattern.compile("^(feat|fix|perf)(\\(([^)]+)\\))?(!)?: (.+)$");
    private static final Map<String, String> TITLES = new LinkedHashMap<>();
    static {
        TITLES.put("feat", "Features");
        TITLES.put("fix", "Bug Fixes");
        TITLES.put("perf", "Performance");
    }

    public static void main(String[] args) throws Exception {
        StringBuilder md = new StringBuilder("# Changelog\n\n"
                + "All notable changes, generated from the Conventional Commit history by `tools/Changelog.java`.\n");
        for (int i = 1; i < args.length; i++) {
            String[] part = args[i].split("\\|");
            md.append("\n## ").append(part[0]).append(" — ").append(part[2]).append("\n");
            Map<String, List<String>> groups = new LinkedHashMap<>();
            List<String> breaking = new ArrayList<>();
            for (String[] commit : log(part[1])) {
                Matcher m = SUBJECT.matcher(commit[1]);
                if (!m.matches()) continue;
                String scope = m.group(3) == null ? "" : "**" + m.group(3) + ":** ";
                String line = "- " + scope + m.group(5) + " (" + commit[0] + ")";
                groups.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(line);
                if (m.group(4) != null || commit[2].contains("BREAKING CHANGE:")) {
                    String note = commit[2].replaceAll("(?s).*BREAKING CHANGE:\\s*", "")
                            .replaceAll("(?s)\\s*Co-Authored-By:.*", "")
                            .replaceAll("\\s+", " ").trim();
                    breaking.add("- " + scope + (note.isEmpty() ? m.group(5) : note) + " (" + commit[0] + ")");
                }
            }
            if (!breaking.isEmpty()) {
                md.append("\n### ⚠ Breaking Changes\n\n").append(String.join("\n", breaking)).append("\n");
            }
            for (Map.Entry<String, String> t : TITLES.entrySet()) {
                List<String> lines = groups.get(t.getKey());
                if (lines == null) continue;
                md.append("\n### ").append(t.getValue()).append("\n\n").append(String.join("\n", lines)).append("\n");
            }
        }
        Files.writeString(Path.of(args[0]), md.toString(), StandardCharsets.UTF_8);
        System.out.println("Wrote " + args[0]);
    }

    /** {short hash, subject, body} for each non-merge commit in the range, oldest first. */
    private static List<String[]> log(String range) throws IOException, InterruptedException {
        Process p = new ProcessBuilder("git", "log", "--no-merges", "--reverse",
                "--format=%h%x1f%s%x1f%b%x1e", range).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (p.waitFor() != 0) throw new IOException("git log failed for " + range + ": " + out);
        List<String[]> commits = new ArrayList<>();
        for (String record : out.split("\u001e")) {
            String[] f = record.strip().split("\u001f", -1);
            if (f.length >= 2) commits.add(new String[]{f[0], f[1], f.length > 2 ? f[2] : ""});
        }
        return commits;
    }
}
