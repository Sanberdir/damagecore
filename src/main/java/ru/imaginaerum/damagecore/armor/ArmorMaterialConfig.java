package ru.imaginaerum.damagecore.armor;

import java.util.HashMap;
import java.util.Map;

public class ArmorMaterialConfig {
    public String material;

    // Храним теперь две карты для каждого типа брони
    public Map<String, Float> helmet_flat = new HashMap<>();  // Абсолютная защита
    public Map<String, Float> helmet_percent = new HashMap<>(); // Процентная защита

    public Map<String, Float> chestplate_flat = new HashMap<>();
    public Map<String, Float> chestplate_percent = new HashMap<>();

    public Map<String, Float> leggings_flat = new HashMap<>();
    public Map<String, Float> leggings_percent = new HashMap<>();

    public Map<String, Float> boots_flat = new HashMap<>();
    public Map<String, Float> boots_percent = new HashMap<>();

    // Глобальные модификаторы для всего набора
    public Map<String, Float> all_flat = new HashMap<>();
    public Map<String, Float> all_percent = new HashMap<>();

    // Вспомогательные методы для получения значений
    public float getFlat(String armorType, String damageType) {
        Map<String, Float> map = switch (armorType) {
            case "helmet" -> helmet_flat;
            case "chestplate" -> chestplate_flat;
            case "leggings" -> leggings_flat;
            case "boots" -> boots_flat;
            default -> null;
        };
        return map != null ? map.getOrDefault(damageType, 0.0f) : 0.0f;
    }

    public float getPercent(String armorType, String damageType) {
        Map<String, Float> map = switch (armorType) {
            case "helmet" -> helmet_percent;
            case "chestplate" -> chestplate_percent;
            case "leggings" -> leggings_percent;
            case "boots" -> boots_percent;
            default -> null;
        };
        return map != null ? map.getOrDefault(damageType, 0.0f) : 0.0f;
    }
}