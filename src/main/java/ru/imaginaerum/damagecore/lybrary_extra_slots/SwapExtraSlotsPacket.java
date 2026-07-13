package ru.imaginaerum.damagecore.lybrary_extra_slots;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwapExtraSlotsPacket {

    public enum Action { SWAP_0_1, SWAP_2_OFFHAND }

    private final Action action;

    public SwapExtraSlotsPacket(Action action) { this.action = action; }

    public static void encode(SwapExtraSlotsPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
    }

    public static SwapExtraSlotsPacket decode(FriendlyByteBuf buf) {
        return new SwapExtraSlotsPacket(buf.readEnum(Action.class));
    }

    public static void handle(SwapExtraSlotsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            player.getCapability(ExtraSlotCapability.INSTANCE).ifPresent(data -> {
                IItemHandlerModifiable handler = (IItemHandlerModifiable) data.getHandler();

                switch (msg.action) {
                    case SWAP_0_1 -> swap(handler, 0, 1);
                    case SWAP_2_OFFHAND -> {
                        ItemStack shieldSlot = handler.getStackInSlot(2);
                        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
                        handler.setStackInSlot(2, offhand);
                        player.setItemInHand(InteractionHand.OFF_HAND, shieldSlot);
                    }
                }
            });

            // TODO: отправьте клиенту пакет синхронизации содержимого слотов
            // (если у вас уже есть такой пакет для инвентарного меню — используйте его здесь,
            // иначе на клиенте GUI/рендер руки со щитом не обновятся сразу)
        });
        ctx.setPacketHandled(true);
    }

    private static void swap(IItemHandlerModifiable handler, int a, int b) {
        ItemStack tmp = handler.getStackInSlot(a);
        handler.setStackInSlot(a, handler.getStackInSlot(b));
        handler.setStackInSlot(b, tmp);
    }
}