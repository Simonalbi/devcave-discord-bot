package io.simonalbi.devcave;

import io.simonalbi.devcave.listeners.welcome.JoinListener;
import io.simonalbi.devcave.listeners.welcome.PidButtonListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.util.Properties;

// TODO Check if all roles exists
// TODO Checks if all channels exists
public class Main {

    private static final String applicationPropertiesPath = "application.properties";
    public static Properties applicationProperties;

    private static void loadApplicationProperties() throws IOException {
        Main.applicationProperties = new Properties();

        try (var input = Main.class
                .getClassLoader()
                .getResourceAsStream(applicationPropertiesPath)) {

            if (input == null) {
                throw new IllegalStateException("application.properties not found in resources");
            }

            applicationProperties.load(input);
        }

        String token = applicationProperties.getProperty("discord.token");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("discord.token missing in application.properties");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        loadApplicationProperties();

        JDA jda = JDABuilder.createDefault(applicationProperties.getProperty("discord.token"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new JoinListener(
                        applicationProperties.getProperty("roles.welcome"),
                        applicationProperties.getProperty("categories.welcomeId")
                ))
                .addEventListeners(new PidButtonListener())
                .build();

        jda.awaitReady();
        System.out.println("DevCave Bot is ready 🚀");
    }
}