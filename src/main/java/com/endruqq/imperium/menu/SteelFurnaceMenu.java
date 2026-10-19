package com.endruqq.imperium.menu;

import com.endruqq.imperium.block.SteelFurnaceBlockEntity;
import com.endruqq.imperium.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SteelFurnaceMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public SteelFurnaceMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(3), new SimpleContainerData(5));
    }

    public SteelFurnaceMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.STEEL_FURNACE.get(), id);
        this.container = container;
        this.data = data;
        addSlot(new Slot(container, SteelFurnaceBlockEntity.SLOT_IRON, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.IRON_INGOT);
            }
        });
        addSlot(new Slot(container, SteelFurnaceBlockEntity.SLOT_COAL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SteelFurnaceBlockEntity.isFuel(stack);
            }
        });
        addSlot(new Slot(container, SteelFurnaceBlockEntity.SLOT_OUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        }
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int progress() {
        return data.get(0);
    }

    public int maxProgress() {
        return Math.max(1, data.get(1));
    }

    public int burn() {
        return data.get(2);
    }

    public int burnMax() {
        return Math.max(1, data.get(3));
    }

    public boolean smelteryOk() {
        return data.get(4) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.IRON_INGOT)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (SteelFurnaceBlockEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
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
