package ru.imaginaerum.damagecore.library_damage;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;

public class WeaponDamageData {
    private final Map<DamageType, Double> damageMap;

    public WeaponDamageData() {
        this.damageMap = new HashMap<>();
    }

    public WeaponDamageData(Map<DamageType, Double> damageMap) {
        this.damageMap = damageMap;
    }

    public static WeaponDamageData fromJson(JsonObject json) {
        WeaponDamageData data = new WeaponDamageData();

        if (json.has("piercing")) {
            data.damageMap.put(DamageType.PIERCING, GsonHelper.getAsDouble(json, "piercing"));
        }
        if (json.has("slashing")) {
            data.damageMap.put(DamageType.SLASHING, GsonHelper.getAsDouble(json, "slashing"));
        }
        if (json.has("bludgeoning")) {
            data.damageMap.put(DamageType.BLUDGEONING, GsonHelper.getAsDouble(json, "bludgeoning"));
        }
        if (json.has("fire")) {
            data.damageMap.put(DamageType.FIRE, GsonHelper.getAsDouble(json, "fire"));
        }


        return data;
    }

    public Map<DamageType, Double> getDamageMap() {
        return damageMap;
    }

    public double getTotalDamage() {
        return damageMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public boolean isEmpty() {
        return damageMap.isEmpty();
    }
}