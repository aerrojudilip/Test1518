package com.example.copilotchat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class CopilotAgentCaller {

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    public static void main(String[] args) {
        try {
            String agentName = "OpenPagesRiskAgent";

            String userPrompt = """
                    Find duplicate risks for this description:
                    Vendor failed to complete access review on time.
                    Explain why they are similar.
                    """;

            String response = callCopilotAgent(agentName, userPrompt);

            System.out.println("===== Copilot Response =====");
            System.out.println(response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String callCopilotAgent(String agentName, String userPrompt) throws Exception {

        String finalPrompt = """
                Use the custom Copilot agent named: %s

                The agent may use its configured MCP servers if needed.

                User request:
                %s
                """.formatted(agentName, userPrompt);

        ProcessBuilder processBuilder = new ProcessBuilder("copilot");

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {

            writer.write(finalPrompt);
            writer.newLine();
            writer.flush();
        }

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
            throw new RuntimeException("Copilot CLI timed out");
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