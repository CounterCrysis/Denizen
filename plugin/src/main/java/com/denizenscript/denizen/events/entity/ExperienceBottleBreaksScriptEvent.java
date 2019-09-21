package com.denizenscript.denizen.events.entity;

import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;

public class ExperienceBottleBreaksScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // experience bottle breaks
    //
    // @Regex ^on experience bottle breaks$
    //
    // @Switch in <area>
    //
    // @Cancellable true
    //
    // @Triggers when a thrown experience bottle breaks.
    //
    // @Context
    // <context.entity> returns the EntityTag of the thrown experience bottle.
    // <context.experience> returns the amount of experience to be spawned.
    // <context.show_effect> returns whether the effect should be shown.
    //
    // @Determine
    // "EXPERIENCE:" + ElementTag(Number) to specify the amount of experience to be created.
    // "EFFECT:" + ElementTag(Boolean) to specify if the particle effects will be shown.
    //
    // -->

    public ExperienceBottleBreaksScriptEvent() {
        instance = this;
    }

    public static ExperienceBottleBreaksScriptEvent instance;
    public ExpBottleEvent event;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("experience bottle breaks");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return runInCheck(path, event.getEntity().getLocation());
    }

    @Override
    public String getName() {
        return "ExperienceBottleBreaks";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        String lower = determinationObj.toString().toLowerCase();
        if (lower.startsWith("experience:")) {
            int experience = Argument.valueOf(lower.substring(11)).asElement().asInt();
            event.setExperience(experience);
        }
        else if (lower.startsWith("effect:")) {
            boolean effect = Argument.valueOf(lower.substring(7)).asElement().asBoolean();
            event.setShowEffect(effect);
        }
        else {
            return super.applyDetermination(path, determinationObj);
        }
        return true;
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("entity")) {
            return new EntityTag(event.getEntity());
        }
        else if (name.equals("experience")) {
            return new ElementTag(event.getExperience());
        }
        else if (name.equals("show_effect")) {
            return new ElementTag(event.getShowEffect());
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onExperienceBottleBreaks(ExpBottleEvent event) {
        this.event = event;
        fire(event);
    }
}
