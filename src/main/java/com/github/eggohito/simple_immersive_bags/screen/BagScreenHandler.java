package com.github.eggohito.simple_immersive_bags.screen;

import com.github.eggohito.simple_immersive_bags.inventory.BagInventory;
import com.github.eggohito.simple_immersive_bags.mixin.ScreenHandlerAccessor;
import com.github.eggohito.simple_immersive_bags.mixin.SlotAccessor;
import com.github.eggohito.simple_immersive_bags.screen.slot.BagSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.joml.Vector2i;

public class BagScreenHandler extends PlayerScreenHandler {

    public static final int BAG_TITLE_Y_OFFSET = 11;

    private final BagInventory bagInventory;

    private final Vector2i topPos;
    private final Vector2i offhandPos;

    private final int bagStart;
    private final int bagEnd;

    public BagScreenHandler(int syncId, PlayerInventory playerInventory, PlayerEntity player, BagInventory bagInventory) {
        super(playerInventory, !player.getWorld().isClient, player);

        this.bagInventory = bagInventory;
        bagInventory.onOpen(player);

        this.bagStart = this.slots.size();
        this.bagEnd = bagStart + bagInventory.size();

        //  Override the sync ID constant and screen handler type set in PlayerScreenHandler
        ((ScreenHandlerAccessor) this).setSyncId(syncId);
        ((ScreenHandlerAccessor) this).setType(BagScreenHandlerTypes.GENERIC_BAG);

        //  Query the top-left most slot from the player's inventory
        Slot topSlot = this.getSlot(9);
        this.topPos = new Vector2i(topSlot.x, topSlot.y);

        //  Offset the player's inventory and hotbar slots added in PlayerScreenHandler
        this.slots.stream()
            .filter(slot -> slot.inventory == playerInventory && slot.getIndex() < playerInventory.size() - 5)
            .forEach(slot -> ((SlotAccessor) slot).setY(slot.y + (bagInventory.getRows() * 18 + 4 + BAG_TITLE_Y_OFFSET)));

        //  Query the offhand slot from the player's inventory
        Slot offhandSlot = this.getSlot(OFFHAND_ID);
        this.offhandPos = new Vector2i(offhandSlot.x, offhandSlot.y);

        int bagRows = bagInventory.getRows();
        int bagColumns = bagInventory.getColumns();

        int bagSlotIndex = bagStart;

        for (int rowIndex = 0; rowIndex < bagRows; rowIndex++) {
            for (int columnIndex = 0; columnIndex < bagColumns; columnIndex++) {

                //  Calculate the X and Y values for the bag's slot
                int bagSlotX = topPos.x + columnIndex * 18;
                int bagSlotY = (topPos.y + rowIndex * 18) + BAG_TITLE_Y_OFFSET;

                //  Add the slot that corresponds to the bag's inventory with the calculated slot index
                this.addSlot(new BagSlot(bagInventory, bagSlotIndex++, bagStart, bagSlotX, bagSlotY));

            }
        }

    }

    public static BagScreenHandler create(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        PlayerEntity player = playerInventory.player;
        return new BagScreenHandler(syncId, playerInventory, player, BagInventory.receive(player, buf));
    }

    @Override
    public void onClosed(PlayerEntity player) {
        bagInventory.onClose(player);
        super.onClosed(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {

        Slot slot = this.getSlot(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getStack();
        ItemStack previousStack = stackInSlot.copy();

        EquipmentSlot equipmentSlot = LivingEntity.getPreferredEquipmentSlot(previousStack);
        int equipmentIndex = 8 - equipmentSlot.getEntitySlotId();

        if (slotIndex == 0) {

            if (!this.insertItem(stackInSlot, INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }

            slot.onQuickTransfer(stackInSlot, previousStack);

        }

        else if (slotIndex >= 1 && slotIndex < EQUIPMENT_START) {

            if (!this.insertItem(stackInSlot, INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }

        }

        else if (slotIndex >= EQUIPMENT_START && slotIndex < EQUIPMENT_END) {

            if (!this.insertItem(stackInSlot, INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }

        }

        else if (equipmentSlot.isArmorSlot() && !this.getSlot(equipmentIndex).hasStack()) {

            if (!this.insertItem(stackInSlot, equipmentIndex, equipmentIndex + 1, false)) {
                return ItemStack.EMPTY;
            }

        }

        else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.getSlot(OFFHAND_ID).hasStack()) {

            if (!this.insertItem(stackInSlot, OFFHAND_ID, OFFHAND_ID + 1, false)) {
                return ItemStack.EMPTY;
            }

        }

        else if (slotIndex >= INVENTORY_START && slotIndex < INVENTORY_END) {

            if (!this.insertItem(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }

        }

        else if (slotIndex >= HOTBAR_START && slotIndex < HOTBAR_END) {

            if (!this.insertItem(stackInSlot, bagStart, bagEnd, false)) {
                return super.quickMove(player, slotIndex);
            }

        }

        else {
            return super.quickMove(player, slotIndex);
        }

        if (stackInSlot.isEmpty()) {
            slot.setStack(ItemStack.EMPTY, previousStack);
        }

        else {
            slot.markDirty();
        }

        if (stackInSlot.getCount() == previousStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTakeItem(player, stackInSlot);
        if (slotIndex == 0) {
            player.dropItem(stackInSlot, false);
        }

        return previousStack;

    }

    public Identifier getScreenTextureId() {
        return bagInventory.getScreenTextureId();
    }

    public ItemStack getSourceStack() {
        return bagInventory.getSourceStack();
    }

    public BagInventory getBagInventory() {
        return bagInventory;
    }

    public Vector2i getTopPos() {
        return topPos;
    }

    public Vector2i getOffhandPos() {
        return offhandPos;
    }

}
