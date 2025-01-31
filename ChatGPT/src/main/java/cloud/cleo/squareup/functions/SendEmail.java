package cloud.cleo.squareup.functions;

import static cloud.cleo.squareup.ChatGPTLambda.crtAsyncHttpClient;
import cloud.cleo.squareup.FaceBookOperations;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import software.amazon.awssdk.services.ses.SesAsyncClient;

/**
 * Send an Email message
 *
 * @author sjensen
 * @param <Request>
 */
public class SendEmail<Request> extends AbstractFunction {

    protected final static SesAsyncClient sesAsyncClient = SesAsyncClient.builder()
            .httpClient(crtAsyncHttpClient)
            .build();

    @Override
    public String getName() {
        return "send_email_message";
    }

    @Override
    public String getDescription() {
        return "Send Email message to an employee, use to relay information to an employee";
    }

    @Override
    public Class getRequestClass() {
        return Request.class;
    }

    /**
     *
     * @return
     */
    @Override
    public Function<Request, Object> getExecutor() {
        return (var r) -> {

            try {
                // Put the callingNumber in the subject if it exists, it might not if using lex console for example
                final String subject = switch (getChannelPlatform()) {
                    case CHIME, CONNECT ->
                        "[From Voice " + getCallingNumber() + "] " + r.subject;
                    case TWILIO ->
                        "[From SMS " + getCallingNumber() + "] " + r.subject;
                    case FACEBOOK ->
                        "[From Facebook User " + FaceBookOperations.getFacebookName(getSessionId()) + "] " + r.subject;
                    default ->
                        "[From " + getChannelPlatform() + "/" + getSessionId() + "] " + r.subject;
                };

                final var id = sesAsyncClient.sendEmail((email) -> {
                    email.destination(dest -> dest.toAddresses(r.employee_email))
                            .message((mesg) -> {
                                mesg.body((body) -> {
                                    body.text(cont -> cont.data(r.message));
                                }).subject(cont -> cont.data(subject));
                            }).source("chatgpt@copperfoxgifts.com");
                }).join();

                log.info("Sent email to " + r.employee_email + " with id " + id.messageId());
                log.info("Subject: " + subject);
                log.info("Message: " + r.message);
                return mapper.createObjectNode().put("status", "SUCCESS").put("message", "The email has been successfuly sent.");
            } catch (CompletionException e) {
                log.error("Unhandled Error", e.getCause());
                return mapper.createObjectNode().put("status", "FAILED").put("message", "An error has occurred, the email could not be sent.");
            } catch (Exception e) {
                log.error("Unhandled Error", e);
                return mapper.createObjectNode().put("status", "FAILED").put("message", "An error has occurred, the email could not be sent.");
            }
        };
    }

    private static class Request {

        @JsonPropertyDescription("The employee email address")
        @JsonProperty(required = true)
        public String employee_email;

        @JsonPropertyDescription("Subject for the email message in English Language")
        @JsonProperty(required = true)
        public String subject;

        @JsonPropertyDescription("The message body to relay to the employee in English Language")
        @JsonProperty(required = true)
        public String message;
    }

    /**
     * Square must be enabled or their won't be a way to get email addresses
     *
     * @return
     */
    @Override
    protected boolean isEnabled() {
        return isSquareEnabled();
    }

}
