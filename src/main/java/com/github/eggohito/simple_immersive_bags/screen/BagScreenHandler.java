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

public class BagScreenHandler extends PlayerScreenHandler {

    private final BagInventory bagInventory;

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

        //  Offset the slots added in PlayerScreenHandler
        this.slots.stream()
            .filter(slot -> slot.inventory == playerInventory && slot.getIndex() < playerInventory.size() - 5)
            .forEach(slot -> ((SlotAccessor) slot).setY(slot.y + 58));

        Slot slotAnchor = slots.get(9);

        int left = slotAnchor.x;
        int top = slotAnchor.y - 58;

        int bagRows = bagInventory.getRows();
        int bagColumns = bagInventory.getColumns();

        for (int rowIndex = 0; rowIndex < bagRows; rowIndex++) {
            for (int columnIndex = 0; columnIndex < bagColumns; columnIndex++) {

                //  Calculate the slot index for the bag's slot
                int slotIndex = columnIndex + rowIndex * 9 + INVENTORY_END;

                //  Add the slot that corresponds to the bag's inventory with the calculated slot index
                this.addSlot(new BagSlot(bagInventory, slotIndex, left + columnIndex * 18, top + rowIndex * 18));

            }
        }

    }

    public static BagScreenHandler create(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {

        int initialRows = buf.readVarInt();
        int initialColumns = buf.readVarInt();

        EquipmentSlot equipmentSlot = buf.readEnumConstant(EquipmentSlot.class);
        Identifier textureId = buf.readIdentifier();

        ItemStack equippedStack = playerInventory.player.getEquippedStack(equipmentSlot);
        return new BagScreenHandler(syncId, playerInventory, playerInventory.player, new BagInventory(equippedStack, textureId, initialRows, initialColumns));

    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        bagInventory.onClose(player);
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

}