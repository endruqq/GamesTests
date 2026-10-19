package com.endruqq.imperium.game;

import com.endruqq.imperium.network.ClientActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import com.endruqq.imperium.registry.ModBlocks;
import com.endruqq.imperium.registry.ModItems;
import com.endruqq.imperium.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Converting items into national resources and back. */
public final class Treasury {
    private Treasury() {}

    /** @return {resource ordinal or -1 for research, value per item} or null */
    public static double[] valueOf(ItemStack s, ServerPlayer p) {
        Item i = s.getItem();
        if (s.is(ItemTags.LOGS)) return new double[]{ResourceType.WOOD.ordinal(), 4};
        if (s.is(ItemTags.PLANKS)) return new double[]{ResourceType.WOOD.ordinal(), 1};
        if (s.is(ModTags.WOOD_RESOURCE)) return new double[]{ResourceType.WOOD.ordinal(), 1};
        if (s.is(ModTags.STONE_RESOURCE)) return new double[]{ResourceType.STONE.ordinal(), 1};
        if (i == Items.IRON_INGOT || i == Items.RAW_IRON) return new double[]{ResourceType.IRON.ordinal(), 1};
        if (i == Items.IRON_BLOCK || i == Items.RAW_IRON_BLOCK) return new double[]{ResourceType.IRON.ordinal(), 9};
        if (i == Items.IRON_NUGGET) return new double[]{ResourceType.IRON.ordinal(), 1.0 / 9};
        if (i == Items.GOLD_INGOT || i == Items.RAW_GOLD) return new double[]{ResourceType.GOLD.ordinal(), 1};
        if (i == Items.GOLD_BLOCK) return new double[]{ResourceType.GOLD.ordinal(), 9};
        if (i == Items.GOLD_NUGGET) return new double[]{ResourceType.GOLD.ordinal(), 1.0 / 9};
        if (i == Items.EMERALD) return new double[]{ResourceType.GOLD.ordinal(), 2};
        if (i == Items.DIAMOND) return new double[]{ResourceType.GOLD.ordinal(), 6};
        if (i == ModItems.STEEL_INGOT.get()) return new double[]{ResourceType.STEEL.ordinal(), 1};
        if (i == ModItems.STEEL_BLOCK.get()) return new double[]{ResourceType.STEEL.ordinal(), 9};
        if (i == ModItems.STEEL_NUGGET.get()) return new double[]{ResourceType.STEEL.ordinal(), 1.0 / 9};
        if (i == Items.WHEAT) return new double[]{ResourceType.FOOD.ordinal(), 1};
        if (i == Items.HAY_BLOCK) return new double[]{ResourceType.FOOD.ordinal(), 9};
        if (i == Items.BOOK || i == Items.WRITABLE_BOOK) return new double[]{-1, 3};
        if (i == Items.WRITTEN_BOOK) return new double[]{-1, 8};
        if (i == Items.ENCHANTED_BOOK) return new double[]{-1, 10};
        if (i == Items.PAPER || i == Items.MAP) return new double[]{-1, 0.5};
        if (i == Items.BOOKSHELF) return new double[]{-1, 8};
        if (s.isEdible()) {
            FoodProperties f = s.getFoodProperties(p);
            if (f != null && f.getNutrition() > 0) return new double[]{ResourceType.FOOD.ordinal(), f.getNutrition()};
        }
        return null;
    }

    public static void deposit(ServerPlayer p, InteractionHand hand, BlockPos pos) {
        GameData d = GameData.get(p.getServer());
        Nation n = d.nationOf(p.getUUID());
        if (n == null) {
            p.displayClientMessage(Component.translatable("imperium.err.no_nation").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (d.phase == GamePhase.RUNNING && d.nationIdAt(pos) != n.id) {
            p.displayClientMessage(Component.translatable("imperium.err.foreign_treasury").withStyle(ChatFormatting.RED), true);
            return;
        }
        ItemStack s = p.getItemInHand(hand);
        double[] v = valueOf(s, p);
        if (v == null) {
            p.displayClientMessage(Component.translatable("imperium.err.not_resource").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        double amount = v[1] * s.getCount();
        Component what;
        if (v[0] < 0) {
            n.research += amount;
            what = Component.translatable("imperium.research_points");
        } else {
            ResourceType t = ResourceType.values()[(int) v[0]];
            n.stock.add(t, amount);
            what = t.label();
        }
        s.setCount(0);
        p.level().playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8F, 1.3F);
        p.displayClientMessage(Component.translatable("imperium.msg.deposited", String.format("%.1f", amount), what)
                .withStyle(ChatFormatting.GREEN), true);
        d.markChanged();
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        CompoundTag t = new CompoundTag();
        t.putLong("pos", pos.asLong());
        ImperiumNet.sendTo(p, new ClientActionPacket("open_treasury", t));
    }

    public static void withdraw(ServerPlayer p, BlockPos pos, ResourceType type, int amount) {
        GameData d = GameData.get(p.getServer());
        Nation n = d.nationOf(p.getUUID());
        if (n == null || amount <= 0) return;
        if (!p.level().getBlockState(pos).is(ModBlocks.TREASURY.get()) || p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64) return;
        if (d.phase == GamePhase.RUNNING && d.nationIdAt(pos) != n.id) return;
        Item item;
        int per = 1;
        switch (type) {
            case WOOD -> item = Items.OAK_PLANKS;
            case STONE -> item = Items.COBBLESTONE;
            case IRON -> item = Items.IRON_INGOT;
            case GOLD -> item = Items.GOLD_INGOT;
            case STEEL -> item = ModItems.STEEL_INGOT.get();
            case FOOD -> {
                item = Items.BREAD;
                per = 5;
            }
            default -> {
                p.displayClientMessage(Component.translatable("imperium.err.not_withdrawable").withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        int items = Math.min(amount, (int) Math.floor(n.stock.get(type) / per));
        if (items <= 0) {
            p.displayClientMessage(Component.translatable("imperium.err.not_enough").withStyle(ChatFormatting.RED), true);
            return;
        }
        n.stock.add(type, -items * per);
        int left = items;
        while (left > 0) {
            int c = Math.min(left, 64);
            ItemStack st = new ItemStack(item, c);
            if (!p.getInventory().add(st)) p.drop(st, false);
            left -= c;
        }
        d.markChanged();
    }
}
