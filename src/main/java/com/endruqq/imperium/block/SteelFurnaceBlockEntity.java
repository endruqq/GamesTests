package com.endruqq.imperium.block;

import com.endruqq.imperium.game.BuildingManager;
import com.endruqq.imperium.menu.SteelFurnaceMenu;
import com.endruqq.imperium.registry.ModBlockEntities;
import com.endruqq.imperium.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class SteelFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_IRON = 0;
    public static final int SLOT_COAL = 1;
    public static final int SLOT_OUT = 2;
    public static final int PROCESS_TIME = 160;

    private final SimpleContainer items = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            SteelFurnaceBlockEntity.this.setChanged();
        }
    };
    private int progress;
    private int burn;
    private int burnMax = 1;
    private boolean smelteryOk;
    private int checkTimer;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> PROCESS_TIME;
                case 2 -> burn;
                case 3 -> burnMax;
                case 4 -> smelteryOk ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 2 -> burn = v;
                case 3 -> burnMax = v;
                case 4 -> smelteryOk = v != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public SteelFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_FURNACE.get(), pos, state);
    }

    public SimpleContainer getItems() {
        return items;
    }

    public static boolean isFuel(ItemStack s) {
        return s.is(ItemTags.COALS) || s.is(Items.COAL_BLOCK);
    }

    private static int fuelTime(ItemStack s) {
        return s.is(Items.COAL_BLOCK) ? 1800 : 200;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteelFurnaceBlockEntity be) {
        if (--be.checkTimer <= 0) {
            be.checkTimer = 40;
            be.smelteryOk = level instanceof ServerLevel sl && BuildingManager.isInActiveSmeltery(sl, pos);
        }
        ItemStack in = be.items.getItem(SLOT_IRON);
        ItemStack fuel = be.items.getItem(SLOT_COAL);
        ItemStack out = be.items.getItem(SLOT_OUT);
        boolean canWork = be.smelteryOk && in.is(Items.IRON_INGOT)
                && (out.isEmpty() || (out.is(ModItems.STEEL_INGOT.get()) && out.getCount() < out.getMaxStackSize()));
        boolean changed = false;
        if (be.burn > 0) be.burn--;
        if (canWork && be.burn <= 0 && isFuel(fuel)) {
            be.burnMax = be.burn = fuelTime(fuel);
            fuel.shrink(1);
            changed = true;
        }
        if (canWork && be.burn > 0) {
            if (++be.progress >= PROCESS_TIME) {
                be.progress = 0;
                in.shrink(1);
                if (out.isEmpty()) be.items.setItem(SLOT_OUT, new ItemStack(ModItems.STEEL_INGOT.get()));
                else out.grow(1);
                changed = true;
            }
        } else if (be.progress > 0) {
            be.progress = Math.max(0, be.progress - 2);
        }
        boolean lit = be.burn > 0;
        if (state.getValue(SteelFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(SteelFurnaceBlock.LIT, lit), 3);
            changed = true;
        }
        if (changed) be.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.imperium.steel_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SteelFurnaceMenu(id, inv, items, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", ContainerHelper.saveAllItems(new CompoundTag(), itemsList()));
        tag.putInt("Progress", progress);
        tag.putInt("Burn", burn);
        tag.putInt("BurnMax", burnMax);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(3, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("Items"), list);
        for (int i = 0; i < 3; i++) items.setItem(i, list.get(i));
        progress = tag.getInt("Progress");
        burn = tag.getInt("Burn");
        burnMax = Math.max(1, tag.getInt("BurnMax"));
    }

    private net.minecraft.core.NonNullList<ItemStack> itemsList() {
        net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(3, ItemStack.EMPTY);
        for (int i = 0; i < 3; i++) list.set(i, items.getItem(i));
        return list;
    }
}
