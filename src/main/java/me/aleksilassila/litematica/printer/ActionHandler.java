package me.aleksilassila.litematica.printer;

import me.aleksilassila.litematica.printer.actions.Action;
import me.aleksilassila.litematica.printer.actions.PrepareAction;
import me.aleksilassila.litematica.printer.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ActionHandler {
    private final Minecraft client;
    private final LocalPlayer player;
    private final Queue<Action> actionQueue = new LinkedList<>();
    public PrepareAction lookAction = null;
    private int tick = 0;

    public ActionHandler(Minecraft client, LocalPlayer player) {
        this.client = client;
        this.player = player;
    }

    public void onGameTick() {
//        int tickRate = InitHandler.PRINTER_SPEED.getIntegerValue();
//        tick = tick % tickRate == tickRate - 1 ? 0 : tick + 1;
//        if (tick % tickRate != 0) {
//            return;
//        }
        Action nextAction = actionQueue.poll();
        if (nextAction != null) {
            Debug.write("Sending action {}", nextAction);
            nextAction.send(client, player);
        } else {
            lookAction = null;
        }
    }

    public boolean acceptsActions() {
        return actionQueue.isEmpty();
    }

    public void addActions(Action... actions) {
        if (!acceptsActions()) {
            return;
        }
        for (Action action : actions) {
            if (action instanceof PrepareAction) {
                lookAction = (PrepareAction) action;
            }
        }
        actionQueue.addAll(List.of(actions));
    }
}