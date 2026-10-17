package com.turboarmor;

import com.turboarmor.net.TurboNet;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.List;

/**
 * Armadura azul metalizada (concepto tipo robot azul con articulaciones plateadas, detalles amarillos y botas rojas).
 * Cada pieza tiene su PROPIO material para poder darle dureza y resistencia al empuje distintas:
 *   Casco:     1014 dur | 5 armadura | 5 dureza | +2 resist. empuje
 *   Pechera:   1432 dur | 12 armadura| 6 dureza | +1 resist. empuje   (habilidad: agacharse + mantener salto = miniturbo)
 *   Pantalones:1304 dur | 8 armadura | 5 dureza | +2 resist. empuje
 *   Botas:     1144 dur | 5 armadura | 5 dureza | +2 resist. empuje
 * (En Minecraft "+2 de resistencia al empuje" = atributo 0.2.)
 *
 * Compatibilidad: id de mod, paquete, canal de red y recursos propios (turboarmor); no modifica nada de vanilla
 * ni de otros mods, así que convive con GelMod y los demás.
 */
@Mod(TurboArmor.ID)
public class TurboArmor {
    public static final String ID = "turboarmor";

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);

    /** Receta de recarga en la mesa de crafteo (pechera + combustible). */
    public static final RegistryObject<RecipeSerializer<?>> RECHARGE_SERIALIZER = SERIALIZERS.register("recharge",
            () -> new SimpleCraftingRecipeSerializer<>(TurboRechargeRecipe::new));

    private static RegistryObject<ArmorMaterial> material(String name, ArmorItem.Type type, int defense, float toughness, float knockback) {
        return MATERIALS.register(name, () -> {
            EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type t : ArmorItem.Type.values()) map.put(t, t == type ? defense : 0);
            return new ArmorMaterial(map, 20, SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(Items.NETHERITE_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ID, "turbo"))),
                    toughness, knockback);
        });
    }

    private static final RegistryObject<ArmorMaterial> M_HELMET = material("turbo_helmet", ArmorItem.Type.HELMET, 5, 5f, 0.2f);
    private static final RegistryObject<ArmorMaterial> M_CHEST = material("turbo_chestplate", ArmorItem.Type.CHESTPLATE, 12, 6f, 0.1f);
    private static final RegistryObject<ArmorMaterial> M_LEGS = material("turbo_leggings", ArmorItem.Type.LEGGINGS, 8, 5f, 0.2f);
    private static final RegistryObject<ArmorMaterial> M_BOOTS = material("turbo_boots", ArmorItem.Type.BOOTS, 5, 5f, 0.2f);

    private static Holder<ArmorMaterial> holder(RegistryObject<ArmorMaterial> o) { return o.getHolder().orElseThrow(); }

    public static final RegistryObject<Item> HELMET = ITEMS.register("turbo_helmet",
            () -> new TurboArmorItem(holder(M_HELMET), ArmorItem.Type.HELMET, new Item.Properties().durability(1014)));
    public static final RegistryObject<Item> CHESTPLATE = ITEMS.register("turbo_chestplate",
            () -> new TurboChestplateItem(holder(M_CHEST), new Item.Properties().durability(1432)));
    public static final RegistryObject<Item> LEGGINGS = ITEMS.register("turbo_leggings",
            () -> new TurboArmorItem(holder(M_LEGS), ArmorItem.Type.LEGGINGS, new Item.Properties().durability(1304)));
    public static final RegistryObject<Item> BOOTS = ITEMS.register("turbo_boots",
            () -> new TurboArmorItem(holder(M_BOOTS), ArmorItem.Type.BOOTS, new Item.Properties().durability(1144)));

    /** Frasco vacío de hierro / de diamante (se llenan con fragmentos de slime rosa). */
    public static final RegistryObject<Item> CAPSULE = ITEMS.register("empty_capsule",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> CAPSULE_DIAMOND = ITEMS.register("empty_capsule_diamond",
            () -> new Item(new Item.Properties().stacksTo(16)));
    /** Combustible: hierro = +5.0 de energía; diamante = recarga completa. Se usa en la mesa de crafteo con la pechera. */
    public static final RegistryObject<Item> FUEL = ITEMS.register("turbo_fuel",
            () -> new TurboFuelItem(new Item.Properties().stacksTo(16), false));
    public static final RegistryObject<Item> FUEL_DIAMOND = ITEMS.register("turbo_fuel_diamond",
            () -> new TurboFuelItem(new Item.Properties().stacksTo(16), true));

    public TurboArmor() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MATERIALS.register(bus);
        ITEMS.register(bus);
        SERIALIZERS.register(bus);
        bus.addListener(this::tabs);
        TurboNet.init();
    }

    private void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.COMBAT) {
            e.accept(HELMET.get()); e.accept(CHESTPLATE.get()); e.accept(LEGGINGS.get()); e.accept(BOOTS.get());
            e.accept(CAPSULE.get()); e.accept(CAPSULE_DIAMOND.get()); e.accept(FUEL.get()); e.accept(FUEL_DIAMOND.get());
        }
    }
}
