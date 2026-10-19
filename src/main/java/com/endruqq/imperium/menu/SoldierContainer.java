package com.endruqq.imperium.menu;

import com.endruqq.imperium.entity.SoldierEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Exposes a soldier's equipment slots as a container. */
public class SoldierContainer implements Container {
    public static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };
    private final SoldierEntity soldier;

    public SoldierContainer(SoldierEntity soldier) {
        this.soldier = soldier;
    }

    @Override
    public int getContainerSize() {
        return SLOTS.length;
    }

    @Override
    public boolean isEmpty() {
        for (EquipmentSlot s : SLOTS) if (!soldier.getItemBySlot(s).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int i) {
        return soldier.getItemBySlot(SLOTS[i]);
    }

    @Override
    public ItemStack removeItem(int i, int count) {
        ItemStack s = soldier.getItemBySlot(SLOTS[i]);
        if (s.isEmpty() || count <= 0) return ItemStack.EMPTY;
        ItemStack taken = s.split(count);
        soldier.setItemSlot(SLOTS[i], s.isEmpty() ? ItemStack.EMPTY : s);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ItemStack s = soldier.getItemBySlot(SLOTS[i]);
        soldier.setItemSlot(SLOTS[i], ItemStack.EMPTY);
        return s;
    }

    @Override
    public void setItem(int i, ItemStack stack) {
        soldier.setItemSlot(SLOTS[i], stack);
        if (!stack.isEmpty()) soldier.setGuaranteedDrop(SLOTS[i]);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return soldier.isAlive() && player.distanceToSqr(soldier) < 64;
    }

    @Override
    public void clearContent() {
        for (EquipmentSlot s : SLOTS) soldier.setItemSlot(s, ItemStack.EMPTY);
    }
}
