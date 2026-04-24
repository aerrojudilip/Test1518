package com.example;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.generated.SessionUsageInfoEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

public class CopilotJavaDemo {

    public static void main(String[] args) throws Exception {

        String prompt = """
                Explain Java ExecutorService in simple terms.
                Give one small example.
                """;

        StringBuilder response = new StringBuilder();

        try (CopilotClient client = new CopilotClient()) {

            client.start().get();

            var session = client.createSession(
                    new SessionConfig()
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setModel("claude-sonnet-4.5")
            ).get();

            session.on(AssistantMessageEvent.class, msg -> {
                String content = msg.getData().content();
                response.append(content);
                System.out.print(content);
            });

            session.on(SessionUsageInfoEvent.class, usage -> {
                var data = usage.getData();
                System.out.println();
                System.out.println("Tokens used: " + data.currentTokens().intValue());
                System.out.println("Token limit: " + data.tokenLimit().intValue());
            });

            session.sendAndWait(
                    new MessageOptions().setPrompt(prompt)
            ).get();
        }

        System.out.println("\n\nFinal response:");
        System.out.println(response);
    }
}