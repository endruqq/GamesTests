package com.endruqq.imperium.menu;

import com.endruqq.imperium.entity.SoldierEntity;
import com.endruqq.imperium.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class SoldierEquipmentMenu extends AbstractContainerMenu {
    private final Container container;
    @Nullable
    private final SoldierEntity soldier;

    public SoldierEquipmentMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, findSoldier(inv.player, buf.readVarInt()));
    }

    private static SoldierEntity findSoldier(Player player, int entityId) {
        Entity e = player.level().getEntity(entityId);
        return e instanceof SoldierEntity s ? s : null;
    }

    public SoldierEquipmentMenu(int id, Inventory inv, @Nullable SoldierEntity soldier) {
        super(ModMenus.SOLDIER_EQUIPMENT.get(), id);
        this.soldier = soldier;
        this.container = soldier != null ? new SoldierContainer(soldier) : new SimpleContainer(6);
        for (int i = 0; i < 4; i++) {
            final EquipmentSlot slotType = SoldierContainer.SLOTS[i];
            addSlot(new Slot(container, i, 62, 8 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return soldier == null || stack.canEquip(slotType, soldier);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addSlot(new Slot(container, 4, 98, 26));
        addSlot(new Slot(container, 5, 98, 44) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        }
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }

    @Nullable
    public SoldierEntity getSoldier() {
        return soldier;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 6) {
            if (!moveItemStackTo(stack, 6, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < 6 && !moved; i++) {
                Slot target = slots.get(i);
                if (!target.hasItem() && target.mayPlace(stack)) {
                    target.set(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
