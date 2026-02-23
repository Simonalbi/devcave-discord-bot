package io.simonalbi.devcave.listeners.welcome;

import io.simonalbi.devcave.messages.WelcomeMessage;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.*;

public class PidButtonListener extends ListenerAdapter {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    private static final Set<Long> ANIMATING_MESSAGE_IDS = ConcurrentHashMap.newKeySet();

    private static final String CURSOR = "█";
    private static final long TICK_MS = 100;
    private static final String STEP_SUFFIX = "█\r\n```";

    private static int countTripleBackticks(String s) {
        int count = 0;
        int idx = 0;
        while ((idx = s.indexOf("```", idx)) != -1) {
            count++;
            idx += 3;
        }
        return count;
    }


    private static String buildSafeMarkdownFrame(String base, String partial) {
        String frame = base + partial;

        boolean insideCodeBlock = (countTripleBackticks(frame) % 2) == 1;

        if (insideCodeBlock) {
            return frame + CURSOR + "\n```";
        } else {
            return frame + CURSOR;
        }
    }

    private void animateAppendLines(ButtonInteractionEvent event,
                                    WelcomeMessage next,
                                    String base,
                                    String newPart,
                                    long messageId) {

        String[] lines = newPart.split("(?<=\n)");

        final int total = lines.length;
        final int[] i = {0};

        StringBuilder partialBuilder = new StringBuilder();

        ScheduledFuture<?>[] handle = new ScheduledFuture<?>[1];

        handle[0] = SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                if (i[0] >= total) {
                    event.getHook()
                            .editOriginal(next.getContent())
                            .setComponents(next.buildComponents(event.getGuild()))
                            .queue(
                                    ok -> ANIMATING_MESSAGE_IDS.remove(messageId),
                                    err -> ANIMATING_MESSAGE_IDS.remove(messageId)
                            );

                    handle[0].cancel(false);
                    return;
                }

                partialBuilder.append(lines[i[0]]);
                i[0]++;

                String safeFrame = buildSafeMarkdownFrame(base, partialBuilder.toString());

                event.getHook()
                        .editOriginal(safeFrame)
                        .queue();

            } catch (Exception ex) {
                ANIMATING_MESSAGE_IDS.remove(messageId);
                if (handle[0] != null) handle[0].cancel(false);
            }
        }, 0, TICK_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith(WelcomeMessage.STEP_BUTTON_PREFIX)) return;

        int step = Integer.parseInt(id.substring(WelcomeMessage.STEP_BUTTON_PREFIX.length()));

        long messageId = event.getMessageIdLong();
        if (!ANIMATING_MESSAGE_IDS.add(messageId)) {
            event.deferEdit().queue();
            return;
        }

        event.deferEdit().queue();

        WelcomeMessage next = new WelcomeMessage(step);

        String base = (step > 0) ? new WelcomeMessage(step - 1).getContent().replace(STEP_SUFFIX, "") : "";
        String target = next.getContent();

        String newPart;
        if (!base.isEmpty() && target.startsWith(base)) {
            newPart = target.substring(base.length());
        } else {
            base = "";
            newPart = target;
        }

        String firstFrame = buildSafeMarkdownFrame(base, "");
        event.getHook()
                .editOriginal(firstFrame)
                .setComponents()
                .queue();

        animateAppendLines(event, next, base, newPart, messageId);
    }
}