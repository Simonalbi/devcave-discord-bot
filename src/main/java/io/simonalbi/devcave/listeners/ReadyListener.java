package io.simonalbi.devcave.listeners;

import io.simonalbi.devcave.BotConfig;
import io.simonalbi.devcave.rss.RssFeedPoller;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ReadyListener extends ListenerAdapter {

    private final BotConfig config;

    private ScheduledExecutorService scheduler;

    public ReadyListener(BotConfig config) {
        super();
        this.config = config;
    }

    private void cleanupRssFeedChannel(JDA jda, long channelId, String feedName) {
        TextChannel channel = jda.getTextChannelById(String.valueOf(channelId));
        if  (channel == null) {
            throw new IllegalStateException("Cannot find channel with id " + channelId + " for RSS feed");
        }

        channel.getHistory().retrievePast(100).queue(messages -> {
            int size = messages.size();
            if (size == 0) {
                System.err.println("Failed to clean up RSS feed " + feedName + " (" + channelId + ") already empty");
            } else if (size == 1) {
                messages.get(0).delete().queue();
            } else {
                channel.deleteMessages(messages).queue();
            }
        });
    }

    private void scheduleRssFeedPollers(JDA jda) {
        scheduler = Executors.newScheduledThreadPool(4);
        boolean cleanup = config.getBoolean("rss.cleanupOnStartup");

        List<RssFeedPoller> pollers = new ArrayList<>();
        String feedsProperty = config.get("rss.feeds");
        for (String feedName : feedsProperty.split(",")) {
            feedName = feedName.trim();
            String prefix = "rss." + feedName;

            String url = config.get(prefix + ".url");
            long period = config.getLong(prefix + ".period");

            long channelId = config.getLong(prefix + ".channel");
            if (cleanup) {
                cleanupRssFeedChannel(jda, channelId, feedName);
            }

            RssFeedPoller poller = new RssFeedPoller(
                    scheduler,
                    url,
                    Duration.ofMinutes(period),
                    config,
                    jda.getTextChannelById(String.valueOf(channelId))
            );

            pollers.add(poller);
        }

        pollers.forEach(RssFeedPoller::start);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        JDA jda = event.getJDA();

        scheduleRssFeedPollers(jda);
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}