package com.volmit.adapt.content.adaptation.wither;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.content.skill.SkillWither;
import com.volmit.adapt.nms.NMS;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.KMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.*;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.UUID;

public class WitherSkullYeet extends SimpleAdaptation<WitherSkullYeet.Config> {

    private final KMap<UUID, Integer> cooldowns = new KMap<>();

    public WitherSkullYeet() {
        super(SkillWither.id("yeet"));
        registerConfiguration(Config.class);
        setDescription("Unleash your inner Wither by using " + C.ITALIC + "someones" + C.GRAY + " head.");
        setIcon(Material.WITHER_SKELETON_SKULL);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setInterval(2314);
    }

    @Override
    public void addStats(int level, Element v) {
        int chance = getConfig().getBaseCooldown() - getConfig().getLevelCooldown() * level;
        v.addLore(C.GREEN + String.valueOf(chance) + "s" + C.GRAY + " cooldown between skull tosses.");
        v.addLore(C.GRAY + "Using Wither Skull: Toss a " + C.DARK_GRAY + "Wither Skull" + C.GRAY + ", exploding on impact.");
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if(e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK)
            return;
        if(e.getHand() != EquipmentSlot.HAND || e.getItem() == null || e.getMaterial() != Material.WITHER_SKELETON_SKULL)
            return;

        if(cooldowns.containsKey(e.getPlayer().getUniqueId())) {
            e.getPlayer().playSound(e.getPlayer(), Sound.BLOCK_CONDUIT_DEACTIVATE, 1F, 1F);
            return;
        }

        if(e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.getItem().setAmount(e.getItem().getAmount() - 1);
            int cooldown = (getConfig().getBaseCooldown() - getConfig().getLevelCooldown() * getLevel(e.getPlayer())) * 20;
            cooldowns.put(e.getPlayer().getUniqueId(), cooldown);
            NMS.get().sendCooldown(e.getPlayer(), Material.WITHER_SKELETON_SKULL, cooldown);
        }

        Vector dir = e.getPlayer().getEyeLocation().getDirection();
        Location spawn = e.getPlayer().getEyeLocation().add(new Vector(.5, -.5, .5)).add(dir);
        e.getPlayer().getWorld().spawn(spawn, WitherSkull.class, entity -> {
            e.getPlayer().playSound(entity, Sound.ENTITY_WITHER_SHOOT, 1, 1);
            entity.setRotation(e.getPlayer().getEyeLocation().getYaw(), e.getPlayer().getEyeLocation().getPitch());
            entity.setCharged(false);
            entity.setBounce(false);
            entity.setDirection(dir);
            entity.setShooter(e.getPlayer());
        });
    }

    @Override
    public boolean isEnabled() { return getConfig().isEnabled(); }

    @Override
    public void onTick() {
        for(UUID u : cooldowns.k()) {
            cooldowns.computeIfPresent(u, (uuid, i) -> i > 0 ? i-- : i);
            if(cooldowns.get(u) == 0) {
                cooldowns.remove(u);
                NMS.get().sendCooldown(Bukkit.getPlayer(u), Material.WITHER_SKELETON_SKULL, 0);
            }
        }
    }

    @Data
    @NoArgsConstructor
    public static class Config {
        private boolean enabled = true;
        private int baseCooldown = 37;
        private int levelCooldown = 7;
        private int baseCost = 3;
        private double costFactor = 1;
        private int maxLevel = 3;
        private int initialCost = 5;
    }
}
