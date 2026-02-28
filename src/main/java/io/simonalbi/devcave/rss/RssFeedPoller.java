package io.simonalbi.devcave.rss;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import io.simonalbi.devcave.BotConfig;
import io.simonalbi.devcave.messages.MessagesUtils;
import io.simonalbi.devcave.messages.RssFeedMessage;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jsoup.Jsoup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

public class RssFeedPoller implements AutoCloseable {

    private final String feedUrl;
    private final Duration period;
    private final BotConfig config;
    private final TextChannel channel;

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;

    private final HttpClient httpClient;
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    public RssFeedPoller(ScheduledExecutorService scheduler, String feedUrl, Duration period, BotConfig config, TextChannel channel) {
        this.scheduler = scheduler;
        this.feedUrl = feedUrl;
        this.period = period;
        this.config = config;
        this.channel = channel;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void start() {
        task = scheduler.scheduleAtFixedRate(
                this::pollSafe,
                0,
                period.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void close() {
        if (task != null) {
            task.cancel(true);
        }
    }

    private void pollSafe() {
        try {
            poll();
        } catch (Exception e) {
            onError(e);
        }
    }

    private Date getEntryDateSafe(SyndEntry e) {
        if (e.getPublishedDate() != null) return e.getPublishedDate();
        if (e.getUpdatedDate() != null) return e.getUpdatedDate();
        return new Date(0);
    }

    private String stableId(SyndEntry e) {
        if (e.getUri() != null) return e.getUri();
        if (e.getLink() != null) return e.getLink();
        return e.getTitle();
    }

    private void poll() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(feedUrl))
                .GET()
                .header("User-Agent", "DevCave/1.0")
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP error: " + response.statusCode());
        }

        ZoneId zone = ZoneId.of("Europe/Rome");
        Instant todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();

        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(response.body()))) {
            SyndFeed feed = new SyndFeedInput().build(reader);

            List<SyndEntry> entries = new ArrayList<>(feed.getEntries());
            entries.sort(Comparator.comparing(this::getEntryDateSafe));

            entries.removeIf(e -> {
                Date d = getEntryDateSafe(e);
                return d == null || d.toInstant().isBefore(todayStart);
            });

            entries.sort(Comparator.comparing(this::getEntryDateSafe));

            for (SyndEntry entry : entries) {
                String id = stableId(entry);
                if (seen.add(id)) {
                    onNewEntry(feed, entry);
                }
            }
        }
    }

    private String getFeedEntrySummary(SyndEntry entry, int maxLen) {
        String rawHtml = null;

        // RSS: <description>
        SyndContent description = entry.getDescription();
        if (description != null && description.getValue() != null && !description.getValue().isBlank()) {
            rawHtml = description.getValue();
        }

        // Atom: <content> (se description non disponibile)
        if (rawHtml == null || rawHtml.isBlank()) {
            List<SyndContent> contents = entry.getContents();
            if (contents != null && !contents.isEmpty()) {
                for (SyndContent c : contents) {
                    if (c != null && c.getValue() != null && !c.getValue().isBlank()) {
                        rawHtml = c.getValue();
                        break;
                    }
                }
            }
        }

        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }

        String cleanText = Jsoup.parse(rawHtml).text();
        cleanText = cleanText.replaceAll("\\s+", " ").trim();

        if (cleanText.length() <= maxLen) {
            return cleanText;
        }

        return cleanText.substring(0, Math.max(0, maxLen - 1)).trim() + "…";
    }

    private void onError(Exception e) {
        System.err.println("[RSS ERROR] " + e.getMessage());
    }

    private void onNewEntry(SyndFeed feed, SyndEntry entry) {
        String title = entry.getTitle();
        String url = entry.getLink();
        String summary = getFeedEntrySummary(entry, 300);
        boolean hasPreviewImage = MessagesUtils.urlHasPreview(url);

        try {
            new RssFeedMessage(config, url, title, summary, hasPreviewImage).send(channel);
        } catch (IOException e) {
            onError(e);
        }
    }
}