package ru.imaginaerum.damagecore.armor;

import java.util.HashMap;
import java.util.Map;

public class ArmorMaterialConfig {
    public String material;
    public Map<String, Float> helmet = new HashMap<>();
    public Map<String, Float> chestplate = new HashMap<>();
    public Map<String, Float> leggings = new HashMap<>();
    public Map<String, Float> boots = new HashMap<>();

    // Опциональные глобальные модификаторы для всего набора
    public Map<String, Float> all = new HashMap<>();
}