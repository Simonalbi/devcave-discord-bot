package io.simonalbi.devcave.messages;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.simonalbi.devcave.BotConfig;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class GenericMessage {
    protected final BotConfig config;

    private final String messagePath;

    public GenericMessage(BotConfig config, String messagePath) {
        this.config = config;
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

    public String getFormattedContent(Map<String, Object> context) throws IOException {
        Handlebars handlebars = new Handlebars();
        Template template = handlebars.compileInline(this.getContent());

        return template.apply(context);
    }

    public abstract void send(TextChannel channel) throws IOException;
}
