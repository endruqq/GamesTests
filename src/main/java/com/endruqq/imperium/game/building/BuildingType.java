package com.endruqq.imperium.game.building;

import com.endruqq.imperium.game.Nation;
import com.endruqq.imperium.game.Resources;
import com.endruqq.imperium.game.tech.Tech;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BuildingType {
    public enum Style { SHELL, FARM }

    public final String id;
    public final BuildingCategory category;
    /** plot is (2*radius+1)^2 */
    public final int radius;
    public final int height;
    public final List<Requirement> requirements;
    public final Tech tech;
    public final Resources prefabCost;
    public final int maxHousing;
    public final int maxPerNation;
    public final Style style;
    private final Item icon;

    private BuildingType(Builder b) {
        this.id = b.id;
        this.category = b.category;
        this.radius = b.radius;
        this.height = b.height;
        this.requirements = List.copyOf(b.requirements);
        this.tech = b.tech;
        this.prefabCost = b.prefabCost;
        this.maxHousing = b.maxHousing;
        this.maxPerNation = b.maxPerNation;
        this.style = b.style;
        this.icon = b.icon;
    }

    public Component name() {
        return Component.translatable("imperium.building." + id);
    }

    public Component description() {
        return Component.translatable("imperium.building." + id + ".desc");
    }

    public ItemStack icon() {
        return new ItemStack(icon);
    }

    public boolean unlocked(Nation n) {
        return tech == null || n.has(tech);
    }

    public boolean isHousing() {
        return maxHousing > 0;
    }

    public int size() {
        return radius * 2 + 1;
    }

    public static class Builder {
        private final String id;
        private final BuildingCategory category;
        private final int radius;
        private final int height;
        private final List<Requirement> requirements = new ArrayList<>();
        private Tech tech;
        private Resources prefabCost;
        private int maxHousing;
        private int maxPerNation;
        private Style style = Style.SHELL;
        private Item icon;

        public Builder(String id, BuildingCategory category, int radius, int height, Item icon) {
            this.id = id;
            this.category = category;
            this.radius = radius;
            this.height = height;
            this.icon = icon;
        }

        public Builder req(Requirement r) {
            requirements.add(r);
            return this;
        }

        public Builder tech(Tech t) {
            this.tech = t;
            return this;
        }

        public Builder prefab(Resources cost) {
            this.prefabCost = cost;
            return this;
        }

        public Builder housing(int h) {
            this.maxHousing = h;
            return this;
        }

        public Builder max(int m) {
            this.maxPerNation = m;
            return this;
        }

        public Builder style(Style s) {
            this.style = s;
            return this;
        }

        public BuildingType build() {
            return new BuildingType(this);
        }
    }
}
