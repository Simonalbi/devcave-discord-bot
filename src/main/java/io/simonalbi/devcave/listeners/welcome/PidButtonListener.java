package io.simonalbi.devcave.listeners.welcome;

import io.simonalbi.devcave.messages.WelcomeMessage;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class PidButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (!id.startsWith(WelcomeMessage.STEP_BUTTON_PREFIX)) return;

        int step = Integer.parseInt(id.substring(WelcomeMessage.STEP_BUTTON_PREFIX.length()));

        WelcomeMessage welcomeMessage = new WelcomeMessage(step);

        event.editMessage(welcomeMessage.getContent())
                .setComponents(welcomeMessage.buildComponents(event.getGuild()))
                .queue();
    }
}
