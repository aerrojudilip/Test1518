import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CopilotAgentCaller {

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    public static void main(String[] args) {
        String agentName = "OpenPagesRiskAgent";

        String userPrompt = """
                Find duplicate risks for this description:
                Vendor failed to complete access review on time.
                Explain why they are similar.
                """;

        String workingDirectory = null;
        // Example:
        // String workingDirectory = "C:/projects/my-app";

        try {
            String response = callCopilotAgent(agentName, userPrompt, workingDirectory);
            System.out.println("===== Copilot Agent Response =====");
            System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String callCopilotAgent(
            String agentName,
            String userPrompt,
            String workingDirectory
    ) throws Exception {

        String finalPrompt = """
                Use the custom Copilot agent named: %s

                The agent may use its configured MCP servers if needed.

                User request:
                %s
                """.formatted(agentName, userPrompt);

        List<String> command = new ArrayList<>();

        /*
         * Update this command based on your Copilot CLI version.
         *
         * Common possible style:
         *   copilot --prompt "<prompt>"
         *
         * If your CLI supports direct agent invocation, change it to something like:
         *   copilot agent invoke <agentName> "<prompt>"
         */

        command.add("copilot");
        command.add("--prompt");
        command.add(finalPrompt);

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        if (workingDirectory != null && !workingDirectory.isBlank()) {
            File dir = new File(workingDirectory);

            if (!dir.exists() || !dir.isDirectory()) {
                throw new IllegalArgumentException("Invalid working directory: " + workingDirectory);
            }

            processBuilder.directory(dir);
        }

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        boolean completed = process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Copilot CLI timed out after " + TIMEOUT.toSeconds() + " seconds");
        }

        int exitCode = process.exitValue();

        if (exitCode != 0) {
            throw new RuntimeException("""
                    Copilot CLI failed.
                    Exit code: %s
                    Output:
                    %s
                    """.formatted(exitCode, output));
        }

        return output.toString().trim();
    }
}