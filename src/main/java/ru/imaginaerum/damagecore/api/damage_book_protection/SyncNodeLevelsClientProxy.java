package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class SyncNodeLevelsClientProxy {
    public static void apply(int treeId, Map<String, Integer> levels) {
        SkillTreeClientSync.applyNodeLevels(treeId, levels);
    }
}