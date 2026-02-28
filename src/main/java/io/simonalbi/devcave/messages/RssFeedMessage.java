package io.simonalbi.devcave.messages;

import io.simonalbi.devcave.BotConfig;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.IOException;
import java.util.Map;

// TODO Send message as image to generated image with SuppressEmbeds true and add button to open url for style consistency
public class RssFeedMessage extends GenericMessage {

    private static final String DEFAULT_PATH = "messages/rss-feed.md";

    private final String url;
    private final String title;
    private final String summary;
    private final boolean showUrlPreview;

    public RssFeedMessage(BotConfig config, String url, String title, String summary, boolean showUrlPreview) {
        super(config, DEFAULT_PATH);
        this.url = url;
        this.title = title;
        this.summary = summary;
        this.showUrlPreview = showUrlPreview;
    }

    @Override
    public void send(TextChannel channel) throws IOException {
        Map<String, Object> placeholders = Map.of(
            "url", url,
            "title", title,
            "summary", summary
        );

        channel.sendMessage(getFormattedContent(placeholders))
                .setSuppressEmbeds(!this.showUrlPreview)
                .queue();
    }
}
