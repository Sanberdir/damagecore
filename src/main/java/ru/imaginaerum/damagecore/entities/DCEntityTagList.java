package ru.imaginaerum.damagecore.entities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public enum DCEntityTagList {

    ANIMAL("animals"),
    SLIME("slime"),
    PLAINTS("plaints"),
    UNDEAD("undead"),
    ABERRATION("aberrations"),
    INFERNAL("infernal"),
    ARTHROPOD("arthropods"),
    CONSTRUCTION("constructions"),
    HUMAN("human");

    public final TagKey<EntityType<?>> tag;

    DCEntityTagList(String name) {
        this.tag = TagKey.create(
                Registries.ENTITY_TYPE,
                new ResourceLocation("damagecore", name)
        );
    }

    public boolean matches(Entity entity) {
        return entity.getType().is(tag);
    }

    public boolean matches(EntityType<?> type) {
        return type.is(tag);
    }
}