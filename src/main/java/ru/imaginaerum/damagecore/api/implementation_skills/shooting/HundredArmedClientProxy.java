package ru.imaginaerum.damagecore.api.implementation_skills.shooting;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HundredArmedClientProxy {
    public static void apply(boolean hasSkill) {
        ClientHundredArmedData.hasSkill = hasSkill;
    }
}