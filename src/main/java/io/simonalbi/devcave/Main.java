package io.simonalbi.devcave;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import java.io.IOException;
import java.util.Properties;

public class Main {

    private static final String applicationPropertiesPath = "application.properties";
    private static String token = null;

    private static void loadToken() throws IOException {
        Properties properties = new Properties();

        try (var input = Main.class
                .getClassLoader()
                .getResourceAsStream(applicationPropertiesPath)) {

            if (input == null) {
                throw new IllegalStateException("application.properties not found in resources");
            }

            properties.load(input);
        }

        Main.token = properties.getProperty("discord.token");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("discord.token missing in application.properties");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        loadToken();

        JDA jda = JDABuilder.createDefault(token).build();

        jda.awaitReady();
        System.out.println("DevCave Bot is ready 🚀");
    }
}