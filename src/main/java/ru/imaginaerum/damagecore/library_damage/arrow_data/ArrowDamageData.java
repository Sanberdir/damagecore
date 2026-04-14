package ru.imaginaerum.damagecore.library_damage.arrow_data;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.HashMap;
import java.util.Map;

public class ArrowDamageData {
    private final Map<DamageType, Double> damageMap;
    private float baseSpeed;
    private float baseRange;               // -1 = бесконечно
    private float postRangeDamageScale;    // коэф. после порога дальности
    private float postRangeSpeedScale;
    private final Map<DamageType, Double> effectChances;
    private float gravityScale;
    private boolean flatDamage = false;
    public ArrowDamageData() {
        this.damageMap          = new HashMap<>();
        this.baseSpeed          = 3.0f;
        this.baseRange          = -1f;
        this.postRangeDamageScale = 0f;
        this.postRangeSpeedScale  = 0f;
        this.effectChances      = new HashMap<>();
        this.gravityScale = 1.0f;
    }

    public static ArrowDamageData fromJson(JsonObject json) {
        ArrowDamageData data = new ArrowDamageData();
        if (json.has("flat_damage"))
            data.flatDamage = GsonHelper.getAsBoolean(json, "flat_damage");
        // Урон по типам
        for (DamageType type : DamageType.values()) {
            String key = type.getDamageName();
            if (json.has(key)) {
                data.damageMap.put(type, GsonHelper.getAsDouble(json, key));
            }
        }

        // Характеристики полёта
        if (json.has("gravity_scale"))
            data.gravityScale = GsonHelper.getAsFloat(json, "gravity_scale");
        if (json.has("base_speed"))
            data.baseSpeed = GsonHelper.getAsFloat(json, "base_speed");
        if (json.has("base_range"))
            data.baseRange = GsonHelper.getAsFloat(json, "base_range");
        if (json.has("post_range_damage_scale"))
            data.postRangeDamageScale = GsonHelper.getAsFloat(json, "post_range_damage_scale");
        if (json.has("post_range_speed_scale"))
            data.postRangeSpeedScale = GsonHelper.getAsFloat(json, "post_range_speed_scale");

        // Шансы эффектов
        if (json.has("effect_chances")) {
            JsonObject chances = json.getAsJsonObject("effect_chances");
            for (DamageType type : DamageType.values()) {
                String key = type.getDamageName();
                if (chances.has(key)) {
                    data.effectChances.put(type, GsonHelper.getAsDouble(chances, key));
                }
            }
        }

        return data;
    }

    // Геттеры
    public boolean isFlatDamage() { return flatDamage; }
    public Map<DamageType, Double> getDamageMap()     { return damageMap; }
    public float getGravityScale() { return gravityScale; }
    public float getBaseSpeed()                        { return baseSpeed; }
    public float getBaseRange()                        { return baseRange; }
    public float getPostRangeDamageScale()             { return postRangeDamageScale; }
    public float getPostRangeSpeedScale()              { return postRangeSpeedScale; }
    public double getEffectChance(DamageType type)     { return effectChances.getOrDefault(type, 0.0); }
    public boolean hasEffectChance(DamageType type)    { return effectChances.containsKey(type); }
    public double getTotalDamage() {
        return damageMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}