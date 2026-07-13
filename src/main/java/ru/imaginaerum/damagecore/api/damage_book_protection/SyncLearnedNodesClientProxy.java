package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.Minecraft;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SyncLearnedNodesClientProxy {
    public static void apply(int treeId, List<String> learnedIds) {
        if (Minecraft.getInstance().player == null) return;
        SkillTreeClientSync.applyLearnedNodes(treeId, learnedIds);
    }
}