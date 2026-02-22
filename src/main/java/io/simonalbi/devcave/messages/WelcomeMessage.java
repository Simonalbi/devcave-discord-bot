package io.simonalbi.devcave.messages;

import io.simonalbi.devcave.Main;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WelcomeMessage extends GenericMessage {

    private static final String DEFAULT_PATH = "messages/welcome/%d.md";
    private static final int LAST_STEP = 4;
    public static final String STEP_BUTTON_PREFIX = "welcome-";

    private final int step;

    private enum StepsButtons {
        STEP_0("ssh unknown@devcave"),
        STEP_1("run INSTRUCTIONS_PROTOCOL.exe"),
        STEP_2("cat NETWORK_RULES.cfg"),
        STEP_3("cat NETWORK_OBJECTIVE.md");

        private final String label;

        StepsButtons(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public WelcomeMessage(int step) {
        super(String.format(DEFAULT_PATH, step));
        this.step = step;
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

    public List<ActionRow> buildComponents(Guild guild) {
        if (step < LAST_STEP) {
            int nextStep = step + 1;

            StepsButtons stepButton = StepsButtons.valueOf("STEP_" + step);
            var nextButton = Button.primary(
                    "welcome-" + nextStep,
                    "▶ " + stepButton.getLabel()
            );

            return List.of(ActionRow.of(nextButton));
        }

        List<Role> selectableRoles = getSelectableCompanyRoles(guild);
        StringSelectMenu.Builder menu = StringSelectMenu.create("pickrole_menu")
                .setPlaceholder("Seleziona la tua compagnia…")
                .setRequiredRange(1, 1);

        for (Role role : selectableRoles) {
            menu.addOption(role.getName().trim(), role.getId());
        }

        return List.of(ActionRow.of(menu.build()));
    }

    @Override
    public void send(TextChannel channel) {
        channel.sendMessage(getContent())
                .setComponents(buildComponents(channel.getGuild()))
                .queue();
    }
}