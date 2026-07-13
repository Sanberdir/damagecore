package ru.imaginaerum.damagecore.api.damage_book_protection.node_variant;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeClientSync;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class SyncNodeVariantsClientProxy {
    public static void apply(int treeId, Map<String, Integer> variants) {
        SkillTreeClientSync.applyVariants(treeId, variants);
    }
}
