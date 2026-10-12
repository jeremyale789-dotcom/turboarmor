package com.turboarmor.client;

import com.turboarmor.TurboArmor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Modelo 3D de la armadura: el humanoide normal + piezas extra pegadas a cada parte (orejas y cresta del casco,
 * hombreras, puerto del pecho, mochila turbo con propulsores, garras, rodilleras, botas grandes con propulsor...).
 * Como las piezas extra son "hijas" de cada parte, siguen todas las animaciones del cuerpo.
 *
 * Textura (128x128): arriba a la izquierda el UV normal del humanoide; el resto son 7 muestras de material
 * (azul, plata, amarillo, rojo, negro, naranja, blanco) que usan las piezas extra.
 */
public class TurboArmorModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation OUTER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "turbo_armor"), "outer");
    public static final ModelLayerLocation INNER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "turbo_armor"), "inner");

    // origen (u,v) de cada muestra de material en la textura
    private static final int[][] SW = {{64, 0}, {0, 32}, {64, 32}, {0, 64}, {64, 64}, {0, 96}, {64, 96}};
    private static final int BLUE = 0, SILVER = 1, YELLOW = 2, RED = 3, BLACK = 4, ORANGE = 5, WHITE = 6;
    private static int counter;

    public TurboArmorModel(ModelPart root) { super(root); }

    /** Muestra solo las partes que corresponden a la pieza que se está dibujando. */
    public void setVisibility(net.minecraft.world.entity.EquipmentSlot slot) {
        head.visible = slot == net.minecraft.world.entity.EquipmentSlot.HEAD;
        hat.visible = false;
        body.visible = slot == net.minecraft.world.entity.EquipmentSlot.CHEST || slot == net.minecraft.world.entity.EquipmentSlot.LEGS;
        rightArm.visible = slot == net.minecraft.world.entity.EquipmentSlot.CHEST;
        leftArm.visible = slot == net.minecraft.world.entity.EquipmentSlot.CHEST;
        rightLeg.visible = slot == net.minecraft.world.entity.EquipmentSlot.LEGS || slot == net.minecraft.world.entity.EquipmentSlot.FEET;
        leftLeg.visible = rightLeg.visible;
    }

    // ------------------------------------------------------------------ utilidades para construir cajas
    private static void box(PartDefinition p, int sw, float x, float y, float z, float w, float h, float d) {
        p.addOrReplaceChild("x" + (counter++),
                CubeListBuilder.create().texOffs(SW[sw][0], SW[sw][1]).addBox(x, y, z, w, h, d), PartPose.ZERO);
    }

    /** Caja con su propio punto de giro (px,py,pz) y rotación. */
    private static void boxAt(PartDefinition p, int sw, float px, float py, float pz, float rx, float ry, float rz,
                              float x, float y, float z, float w, float h, float d) {
        p.addOrReplaceChild("x" + (counter++),
                CubeListBuilder.create().texOffs(SW[sw][0], SW[sw][1]).addBox(x, y, z, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }

    /** Igual que box() pero reflejada en X si es la parte izquierda. */
    private static void boxM(PartDefinition p, boolean left, int sw, float x, float y, float z, float w, float h, float d) {
        box(p, sw, left ? -(x + w) : x, y, z, w, h, d);
    }

    // ------------------------------------------------------------------ capas
    public static LayerDefinition createLayer(boolean inner) {
        counter = 0;
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(inner ? 0.5f : 1.0f), 0.0f);
        PartDefinition root = mesh.getRoot();
        if (!inner) {
            helmet(root.getChild("head"));
            chest(root.getChild("body"), root.getChild("right_arm"), root.getChild("left_arm"));
            boots(root.getChild("right_leg"), false);
            boots(root.getChild("left_leg"), true);
        } else {
            leggings(root.getChild("body"), root.getChild("right_leg"), root.getChild("left_leg"));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void helmet(PartDefinition head) {
        box(head, BLUE, -4.8f, -10.6f, -4.8f, 9.6f, 2.2f, 9.6f);              // tapa del cráneo
        box(head, BLUE, -4.3f, -9.6f, 3.8f, 8.6f, 6.2f, 4.2f);                // barrido trasero
        box(head, BLUE, -3.7f, -8.6f, 7.6f, 7.4f, 4.6f, 3.0f);
        box(head, SILVER, -3.0f, -7.6f, 10.2f, 6.0f, 3.0f, 2.4f);             // punta trasera
        for (int s = -1; s <= 1; s += 2) {                                      // orejas (3 tramos inclinados + interior amarillo)
            boxAt(head, BLUE, s * 3.4f, -10.4f, -1.6f, 0, 0, s * 0.22f, -2.0f, -2.6f, -1.0f, 4.0f, 2.6f, 2.0f);
            boxAt(head, BLUE, s * 4.0f, -12.7f, -1.6f, 0, 0, s * 0.32f, -1.5f, -2.6f, -0.9f, 3.0f, 2.6f, 1.8f);
            boxAt(head, BLUE, s * 4.7f, -15.0f, -1.6f, 0, 0, s * 0.42f, -1.0f, -2.6f, -0.8f, 2.0f, 2.6f, 1.6f);
            boxAt(head, YELLOW, s * 3.7f, -10.6f, -2.7f, 0, 0, s * 0.22f, -1.4f, -3.4f, -0.3f, 2.8f, 3.4f, 0.5f);
        }
        box(head, SILVER, -5.4f, -6.6f, -2.6f, 1.2f, 5.0f, 5.2f);              // mejillas
        box(head, SILVER, 4.2f, -6.6f, -2.6f, 1.2f, 5.0f, 5.2f);
        box(head, SILVER, -3.6f, -1.8f, -5.0f, 7.2f, 2.4f, 1.4f);              // barbilla
        box(head, BLACK, -3.9f, -6.6f, -5.0f, 7.8f, 0.7f, 0.7f);               // ceja de la visera
    }

    private static void chest(PartDefinition body, PartDefinition rArm, PartDefinition lArm) {
        box(body, SILVER, -4.6f, -1.4f, -3.0f, 9.2f, 2.4f, 6.0f);              // collar
        box(body, YELLOW, -3.2f, 3.0f, -3.4f, 6.4f, 6.4f, 1.4f);               // puerto del pecho
        box(body, BLACK, -1.6f, 4.6f, -3.9f, 3.2f, 3.2f, 0.9f);
        box(body, BLUE, -5.0f, 1.6f, -2.8f, 1.6f, 7.0f, 5.6f);                 // placas laterales
        box(body, BLUE, 3.4f, 1.6f, -2.8f, 1.6f, 7.0f, 5.6f);
        box(body, SILVER, -4.6f, 10.0f, -2.9f, 9.2f, 1.6f, 5.8f);              // cintura
        box(body, SILVER, -4.0f, 1.6f, 2.6f, 8.0f, 7.0f, 3.0f);                // mochila turbo
        box(body, BLACK, -3.2f, 8.2f, 3.1f, 2.6f, 3.2f, 2.6f);                 // toberas
        box(body, BLACK, 0.6f, 8.2f, 3.1f, 2.6f, 3.2f, 2.6f);
        box(body, ORANGE, -2.9f, 11.2f, 3.4f, 2.0f, 0.8f, 2.0f);               // brillo de las toberas
        box(body, ORANGE, 0.9f, 11.2f, 3.4f, 2.0f, 0.8f, 2.0f);
        box(body, BLUE, -5.2f, 2.4f, 3.4f, 1.2f, 5.0f, 2.2f);                  // aletas
        box(body, BLUE, 4.0f, 2.4f, 3.4f, 1.2f, 5.0f, 2.2f);
        arm(rArm, false);
        arm(lArm, true);
    }

    private static void arm(PartDefinition a, boolean left) {
        boxM(a, left, SILVER, -4.8f, -3.6f, -3.1f, 6.0f, 3.6f, 6.2f);          // hombrera
        boxM(a, left, YELLOW, -4.9f, -1.8f, -3.2f, 6.2f, 0.9f, 6.4f);          // franja amarilla
        boxM(a, left, SILVER, -3.5f, 3.6f, -2.5f, 5.0f, 1.2f, 5.0f);           // anillo del brazo
        boxM(a, left, BLUE, -3.8f, 5.6f, -2.8f, 5.6f, 4.0f, 5.6f);             // antebrazo
        boxM(a, left, YELLOW, -3.7f, 9.4f, -2.7f, 5.4f, 1.2f, 5.4f);           // banda de la muñeca
        boxM(a, left, SILVER, -3.4f, 10.6f, -2.4f, 4.8f, 2.2f, 4.8f);          // mano
        for (int i = 0; i < 3; i++) boxM(a, left, SILVER, -3.2f + i * 1.5f, 12.8f, -2.2f, 1.1f, 2.6f, 0.9f);   // garras
    }

    private static void leggings(PartDefinition body, PartDefinition rLeg, PartDefinition lLeg) {
        box(body, SILVER, -4.8f, 9.4f, -2.9f, 9.6f, 2.6f, 5.8f);               // cinturón
        box(body, YELLOW, -1.3f, 9.7f, -3.5f, 2.6f, 2.0f, 0.9f);               // hebilla
        box(body, BLUE, -5.4f, 8.8f, -2.2f, 1.2f, 3.6f, 4.4f);                 // placas de cadera
        box(body, BLUE, 4.2f, 8.8f, -2.2f, 1.2f, 3.6f, 4.4f);
        for (int i = 0; i < 2; i++) {
            PartDefinition l = i == 0 ? rLeg : lLeg;
            boolean left = i == 1;
            boxM(l, left, BLUE, -3.1f, 0.4f, -3.1f, 6.2f, 4.6f, 6.2f);          // muslo
            boxM(l, left, SILVER, -3.1f, 5.4f, -3.1f, 6.2f, 1.6f, 6.2f);        // aro de la rodilla
            boxM(l, left, SILVER, -2.3f, 5.0f, -3.7f, 4.6f, 3.0f, 1.2f);        // rótula
            boxM(l, left, BLUE, -2.9f, 7.2f, -2.9f, 5.8f, 3.6f, 5.8f);          // espinillera
        }
    }

    private static void boots(PartDefinition leg, boolean left) {
        boxM(leg, left, SILVER, -3.3f, 7.2f, -3.3f, 6.6f, 1.4f, 6.6f);         // anillo del tobillo
        boxM(leg, left, RED, -3.2f, 8.6f, -3.2f, 6.4f, 4.2f, 6.4f);            // bota
        boxM(leg, left, RED, -3.2f, 10.2f, -6.4f, 6.4f, 2.8f, 3.4f);           // puntera
        boxM(leg, left, WHITE, -3.3f, 11.3f, -6.5f, 6.6f, 0.8f, 3.6f);         // franja blanca
        boxM(leg, left, WHITE, -3.4f, 12.8f, -6.6f, 6.8f, 0.8f, 10.2f);        // suela
        boxM(leg, left, SILVER, -1.5f, 9.5f, 3.2f, 3.0f, 2.5f, 1.8f);          // propulsor del talón
        boxM(leg, left, ORANGE, -1.0f, 10.2f, 4.9f, 2.0f, 1.0f, 0.8f);
    }
}
