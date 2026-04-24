I want to build a Java application that uses the GitHub Copilot SDK for Java to call an LLM through GitHub Copilot.

Create a complete working example with the following requirements:

1. Use Java 17 or higher.
2. Use Maven.
3. Use the GitHub Copilot SDK for Java dependency.
4. The application should:
   - Start a CopilotClient.
   - Create a Copilot session.
   - Send a user prompt to the model.
   - Stream or collect the assistant response.
   - Print the final response to the console.
   - Print token usage if available.
   - Handle errors properly.
   - Close all resources correctly.

5. Include:
   - Complete `pom.xml`.
   - Complete Java source code.
   - Required imports.
   - Setup instructions.
   - How to install and verify GitHub Copilot CLI.
   - How to authenticate with GitHub Copilot.
   - How to run the Maven project.
   - Troubleshooting section for common issues.

6. Also explain:
   - Whether GitHub Copilot SDK directly calls an LLM or communicates through the Copilot CLI runtime.
   - Whether a GitHub Copilot subscription is required.
   - Whether this SDK is production-ready or still preview.
   - What Java version is required.
   - How this approach compares with calling OpenAI, Azure OpenAI, Anthropic, or AWS Bedrock directly.

7. Add an optional Spring Boot version:
   - Create a REST endpoint `/ask`.
   - Accept a JSON request like:
     {
       "prompt": "Explain Java ExecutorService"
     }
   - Return the LLM response as JSON.
   - Keep the Copilot client/session handling clean and reusable.

8. Add enterprise considerations:
   - Authentication/security.
   - Logging without exposing prompts or secrets.
   - Timeout handling.
   - Retry strategy.
   - Model selection.
   - Audit logging.
   - Whether this is suitable for IBM OpenPages integration.

Please write the answer as a practical developer guide with code that I can copy and run.