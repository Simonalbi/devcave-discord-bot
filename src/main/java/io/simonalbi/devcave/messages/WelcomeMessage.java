package io.simonalbi.devcave.messages;

import io.simonalbi.devcave.Main;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WelcomeMessage extends GenericMessage {

    private static final String DEFAULT_PATH = "messages/welcome.md";

    public WelcomeMessage() {
        super(DEFAULT_PATH);
    }

    private List<Role> getSelectableCompanyRoles(Guild guild) {
        String excludedRolesRaw = Main.applicationProperties.getProperty("roles.exclude", "");

        Set<String> excludedRoleIds = Arrays.stream(excludedRolesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        return guild.getRoles().stream()
                .filter(r -> !r.isPublicRole())     // Exclude @everyone
                .filter(r -> !r.isManaged())        // Exclude bot/integration roles
                .filter(r -> !excludedRoleIds.contains(r.getId()))
                .sorted(Comparator.comparing(Role::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public void send(TextChannel channel) {
        Guild guild = channel.getGuild();

        List<Role> selectableRoles = getSelectableCompanyRoles(guild);
        StringSelectMenu.Builder menu = StringSelectMenu.create("pickrole_menu")
                .setPlaceholder("Seleziona la tua compagnia…")
                .setRequiredRange(1, 1);

        for (Role role : selectableRoles) {
            String label = role.getName().trim();
            menu.addOption(label, role.getId());
        }

        ActionRow pickRoleMenu = ActionRow.of(menu.build());

        channel.sendMessage(getContent())
                .setComponents(pickRoleMenu)
                .queue();
    }
}