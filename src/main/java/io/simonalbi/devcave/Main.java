package io.simonalbi.devcave;

import io.simonalbi.devcave.listeners.ReadyListener;
import io.simonalbi.devcave.listeners.welcome.JoinListener;
import io.simonalbi.devcave.listeners.welcome.PidButtonListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;

// TODO Check if all roles exists
// TODO Checks if all channels exists
public class Main {

    private static final String applicationPropertiesPath = "application.properties";

    public static void main(String[] args) throws IOException, InterruptedException {
        BotConfig config = new BotConfig(applicationPropertiesPath);

        JDA jda = JDABuilder.createDefault(config.get("discord.token"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new JoinListener(config))
                .addEventListeners(new PidButtonListener(config))
                .addEventListeners(new ReadyListener(config))
                .build();

        jda.awaitReady();
        System.out.println("DevCave Bot is ready 🚀");
    }
}