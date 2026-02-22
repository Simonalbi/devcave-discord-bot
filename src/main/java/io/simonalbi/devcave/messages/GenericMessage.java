package io.simonalbi.devcave.messages;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public abstract class GenericMessage {
    private final String messagePath;

    public GenericMessage(String messagePath) {
        this.messagePath = messagePath;
    }

    public String getContent() {
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream(messagePath)) {

            if (is == null) {
                throw new IllegalStateException(messagePath + " not found in resources");
            }

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + messagePath, e);
        }
    }

    public abstract void send(TextChannel channel);
}
