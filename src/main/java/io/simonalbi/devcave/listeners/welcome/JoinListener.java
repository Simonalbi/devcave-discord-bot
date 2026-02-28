package io.simonalbi.devcave.listeners.welcome;

import io.simonalbi.devcave.BotConfig;
import io.simonalbi.devcave.messages.WelcomeMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ThreadLocalRandom;

public class JoinListener extends ListenerAdapter {
    private final BotConfig config;

    private final String LOST_IN_THE_MAINFRAME_ROLE_ID;
    private final String WELCOM_CHANNELS_CATEGORY_ID;

    public JoinListener(BotConfig config) {
        this.config = config;

        this.LOST_IN_THE_MAINFRAME_ROLE_ID = config.get("roles.lostInTheMainframe");
        this.WELCOM_CHANNELS_CATEGORY_ID = config.get("categories.instances");
    }

    private static String randomPid() {
        int n = ThreadLocalRandom.current().nextInt(10000, 100000);
        return String.format("%05d", n);
    }

    private static String toMathBoldDigits(String number) {
        StringBuilder sb = new StringBuilder();

        for (char c : number.toCharArray()) {
            if (Character.isDigit(c)) {
                int digit = c - '0';
                int base = 0x1D7EC; // Unicode: 𝟬
                sb.append(Character.toChars(base + digit));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        Role lostRole = guild.getRoleById(LOST_IN_THE_MAINFRAME_ROLE_ID);
        if (lostRole != null) {
            guild.addRoleToMember(member, lostRole).queue();
        }

        Category category = guild.getCategoryById(WELCOM_CHANNELS_CATEGORY_ID);
        if (category == null) return;

        String pid = randomPid();
        String channelName = "\uD835\uDDE3\uD835\uDDDC\uD835\uDDD7-" + toMathBoldDigits(pid);

        guild.createTextChannel(channelName, category)
                .queue(channel -> {
                    channel.getManager().sync(category).queue();
                    new WelcomeMessage(config, 0).send(channel);
                });
    }
}
