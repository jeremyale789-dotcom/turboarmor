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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Armadura inspirada en Metal Sonic (vista frontal y trasera de las referencias):
 *  - Casco: cúpula azul, orejas grandes con interior dorado, 3 púas largas hacia atrás, ceja negra con luces rojas
 *    y CARA ABIERTA (se ve tu skin; no lleva nariz/hocico).
 *  - Pechera: torso redondeado, turbina del pecho (aro dorado + plata + centro negro), collar con púa, hombros de
 *    esfera plateada con púa, TURBINA CÓNICA en la espalda (de ahí sale el turbo), brazos finos (se ve la skin) con codos, antebrazos gruesos,
 *    puños dorados y guantes negros con garras largas. SIN mochila.
 *  - Pantalones: caderas plateadas, muslos finos (se ve la skin), rodilleras plateadas grandes y espinilleras.
 *  - Botas: zapatos rojos grandes con puntera larga, franja blanca, suela blanca y anillo plateado.
 *
 * Textura 128x128: arriba a la izquierda el UV del humanoide (con zonas transparentes) y 7 muestras de material.
 */
public class TurboArmorModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation OUTER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "turbo_armor"), "outer");
    public static final ModelLayerLocation INNER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TurboArmor.ID, "turbo_armor"), "inner");

    private static final int[][] SW = {{64, 0}, {0, 32}, {64, 32}, {0, 64}, {64, 64}, {0, 96}, {64, 96}};
    private static final int BLUE = 0, SILVER = 1, YELLOW = 2, RED = 3, BLACK = 4, ORANGE = 5, WHITE = 6;
    private static int counter;

    public TurboArmorModel(ModelPart root) { super(root); }

    public void setVisibility(EquipmentSlot slot) {
        head.visible = slot == EquipmentSlot.HEAD;
        hat.visible = false;
        body.visible = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
        rightArm.visible = slot == EquipmentSlot.CHEST;
        leftArm.visible = slot == EquipmentSlot.CHEST;
        rightLeg.visible = slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
        leftLeg.visible = rightLeg.visible;
    }

    // ------------------------------------------------------------------ utilidades
    private static void box(PartDefinition p, int sw, float x, float y, float z, float w, float h, float d) {
        p.addOrReplaceChild("x" + (counter++),
                CubeListBuilder.create().texOffs(SW[sw][0], SW[sw][1]).addBox(x, y, z, w, h, d), PartPose.ZERO);
    }

    /** Caja reflejada en X si es la parte izquierda. */
    private static void boxM(PartDefinition p, boolean left, int sw, float x, float y, float z, float w, float h, float d) {
        box(p, sw, left ? -(x + w) : x, y, z, w, h, d);
    }

    /**
     * Cadena de n cajas que van avanzando en la dirección (dx,dy,dz) y se encogen (factor shrink) en cada paso:
     * sirve para púas, orejas picudas y garras. Se escribe para el lado derecho; left=true la refleja.
     */
    private static void chain(PartDefinition p, boolean left, int sw, float cx, float cy, float cz,
                              float dx, float dy, float dz, int n, float w0, float h0, float d0, float shrink) {
        for (int i = 0; i < n; i++) {
            float k = (float) Math.pow(shrink, i);
            float w = w0 * k, h = h0 * k, d = d0 * k;
            float x = cx + dx * i, y = cy + dy * i, z = cz + dz * i;
            if (left) x = -x;
            box(p, sw, x - w / 2f, y - h / 2f, z - d / 2f, w, h, d);
        }
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

    // ------------------------------------------------------------------ CASCO
    private static void helmet(PartDefinition head) {
        // cúpula redondeada (3 capas) y bultos en las sienes
        box(head, BLUE, -5.6f, -10.2f, -5.6f, 11.2f, 2.4f, 11.2f);
        box(head, BLUE, -4.8f, -11.6f, -4.6f, 9.6f, 1.6f, 9.2f);
        box(head, BLUE, -3.6f, -12.6f, -3.4f, 7.2f, 1.2f, 6.8f);
        box(head, BLUE, -5.9f, -9.4f, -3.5f, 1.0f, 6.0f, 7.0f);
        box(head, BLUE, 4.9f, -9.4f, -3.5f, 1.0f, 6.0f, 7.0f);

        // ceja negra con las luces rojas de los ojos (la cara queda abierta debajo)
        box(head, BLACK, -5.2f, -8.6f, -5.8f, 10.4f, 2.0f, 1.0f);
        box(head, RED, -3.8f, -7.8f, -6.2f, 2.8f, 0.8f, 0.5f);
        box(head, RED, 1.0f, -7.8f, -6.2f, 2.8f, 0.8f, 0.5f);

        // mejillas y aros plateados de las orejas
        box(head, BLUE, -5.8f, -6.0f, -5.4f, 1.8f, 4.6f, 2.6f);
        box(head, BLUE, 4.0f, -6.0f, -5.4f, 1.8f, 4.6f, 2.6f);
        box(head, SILVER, -6.3f, -5.2f, -1.2f, 0.8f, 3.0f, 3.0f);
        box(head, SILVER, 5.5f, -5.2f, -1.2f, 0.8f, 3.0f, 3.0f);

        for (int i = 0; i < 2; i++) {
            boolean left = i == 0;
            // orejas grandes y picudas (azul por fuera, dorado por dentro)
            chain(head, left, BLUE, 3.6f, -12.2f, -0.4f, 0.55f, -2.0f, 0.1f, 6, 4.6f, 2.6f, 2.6f, 0.84f);
            chain(head, left, YELLOW, 3.7f, -12.0f, -1.7f, 0.55f, -2.0f, 0.1f, 5, 2.6f, 2.6f, 0.5f, 0.85f);
            // púas laterales hacia atrás
            chain(head, left, BLUE, 2.6f, -6.8f, 3.4f, 0.55f, -0.1f, 2.6f, 6, 3.8f, 3.6f, 3.8f, 0.82f);
        }
        // púa central larga hacia atrás y arriba
        chain(head, false, BLUE, 0f, -10.0f, 3.0f, 0f, -0.55f, 2.6f, 7, 5.4f, 4.2f, 4.2f, 0.83f);
    }

    // ------------------------------------------------------------------ PECHERA
    private static void chest(PartDefinition body, PartDefinition rArm, PartDefinition lArm) {
        // torso redondeado (panza delante y espalda lisa detrás)
        box(body, BLUE, -5.8f, 0.8f, -3.9f, 11.6f, 8.4f, 1.2f);
        box(body, BLUE, -5.8f, 0.8f, 2.7f, 11.6f, 8.4f, 1.2f);
        box(body, BLUE, -5.4f, -1.6f, -3.5f, 10.8f, 2.6f, 7.0f);
        box(body, BLUE, -4.6f, 9.0f, -3.4f, 9.2f, 3.6f, 6.8f);
        box(body, BLUE, -6.2f, 1.2f, -2.6f, 1.2f, 7.6f, 5.2f);
        box(body, BLUE, 5.0f, 1.2f, -2.6f, 1.2f, 7.6f, 5.2f);

        // turbina del pecho: aro dorado, anillo plateado y centro negro
        box(body, YELLOW, -3.8f, 2.2f, -5.1f, 7.6f, 7.6f, 1.2f);
        box(body, SILVER, -2.9f, 3.1f, -6.0f, 5.8f, 5.8f, 1.0f);
        box(body, BLACK, -1.8f, 4.2f, -6.7f, 3.6f, 3.6f, 0.8f);

        // collar plateado con la púa bajo la barbilla
        box(body, SILVER, -4.8f, -2.4f, -3.4f, 9.6f, 1.6f, 6.8f);
        chain(body, false, SILVER, 0f, -0.6f, -4.3f, 0f, 0.5f, -0.9f, 3, 1.8f, 1.8f, 2.4f, 0.65f);

        // cintura
        box(body, SILVER, -4.7f, 9.8f, -3.3f, 9.4f, 1.2f, 6.6f);

        // TURBINA DE LA ESPALDA (de aquí sale el turbo): cono escalonado con aro dorado, dientes plateados y tobera negra
        box(body, YELLOW, -4.0f, 2.0f, 3.9f, 8.0f, 8.0f, 1.2f);          // aro dorado de la base
        box(body, SILVER, -3.2f, 2.8f, 5.1f, 6.4f, 6.4f, 1.2f);          // anillo plateado
        for (int k = 0; k < 8; k++) {                                     // dientes alrededor del anillo
            double ang = k * Math.PI / 4.0;
            float cx = (float) (Math.cos(ang) * 3.9), cy = 6.0f + (float) (Math.sin(ang) * 3.9);
            box(body, SILVER, cx - 0.55f, cy - 0.55f, 5.4f, 1.1f, 1.1f, 1.6f);
        }
        box(body, YELLOW, -2.4f, 3.6f, 6.3f, 4.8f, 4.8f, 1.0f);          // segundo escalón dorado
        box(body, BLACK, -1.6f, 4.4f, 7.3f, 3.2f, 3.2f, 0.9f);           // tobera
        box(body, ORANGE, -0.8f, 5.2f, 8.1f, 1.6f, 1.6f, 0.3f);          // brillo del fondo

        arm(rArm, false);
        arm(lArm, true);
    }

    private static void arm(PartDefinition a, boolean left) {
        boxM(a, left, SILVER, -5.6f, -3.6f, -3.2f, 6.8f, 5.6f, 6.4f);                 // hombro de esfera plateada
        chain(a, left, SILVER, -6.0f, -0.8f, 0f, -0.8f, -0.2f, 0f, 3, 2.4f, 2.4f, 2.4f, 0.62f);   // púa del hombro
        // (brazo fino: aquí se ve la skin)
        boxM(a, left, SILVER, -3.4f, 4.8f, -2.4f, 4.8f, 1.6f, 4.8f);                  // codo
        boxM(a, left, BLUE, -3.9f, 6.4f, -2.9f, 5.8f, 4.0f, 5.8f);                    // antebrazo grueso
        boxM(a, left, YELLOW, -4.1f, 10.4f, -3.1f, 6.2f, 1.6f, 6.2f);                 // puño dorado
        boxM(a, left, BLACK, -4.2f, 12.0f, -3.2f, 6.4f, 1.0f, 6.4f);
        boxM(a, left, BLACK, -3.4f, 13.0f, -2.4f, 4.8f, 2.4f, 4.8f);                  // guante
        for (int i = 0; i < 4; i++) {                                                  // garras largas y curvas
            chain(a, left, SILVER, -2.5f + i * 1.1f, 15.2f, -1.8f, 0f, 1.1f, -0.4f, 4, 0.9f, 1.8f, 0.9f, 0.78f);
        }
    }

    // ------------------------------------------------------------------ PANTALONES
    private static void leggings(PartDefinition body, PartDefinition rLeg, PartDefinition lLeg) {
        box(body, SILVER, -4.9f, 9.6f, -2.9f, 9.8f, 1.8f, 5.8f);                       // cinturón / pelvis
        for (int i = 0; i < 2; i++) {
            PartDefinition l = i == 0 ? rLeg : lLeg;
            boolean left = i == 1;
            boxM(l, left, SILVER, -3.0f, -0.8f, -3.0f, 5.0f, 3.6f, 6.0f);              // cadera (esfera)
            boxM(l, left, BLUE, -2.6f, 3.4f, -3.0f, 4.2f, 3.0f, 0.8f);                 // placa frontal del muslo (el resto: skin)
            boxM(l, left, SILVER, -3.2f, 5.6f, -3.2f, 5.2f, 2.2f, 6.4f);               // rodilla grande
            boxM(l, left, SILVER, -2.2f, 5.4f, -3.8f, 3.6f, 2.6f, 1.0f);               // rótula
            boxM(l, left, BLUE, -3.0f, 8.0f, -3.0f, 5.0f, 3.6f, 6.0f);                 // espinillera
        }
    }

    // ------------------------------------------------------------------ BOTAS
    private static void boots(PartDefinition leg, boolean left) {
        boxM(leg, left, SILVER, -3.8f, 6.6f, -3.6f, 5.6f, 1.8f, 7.2f);                 // anillo del tobillo
        boxM(leg, left, RED, -4.1f, 8.4f, -3.8f, 6.2f, 4.6f, 7.6f);                    // zapato grande
        boxM(leg, left, RED, -4.1f, 10.0f, -8.0f, 6.2f, 3.4f, 4.4f);                   // puntera larga
        boxM(leg, left, RED, -3.6f, 10.8f, -9.0f, 5.2f, 2.6f, 1.2f);                   // punta redondeada
        boxM(leg, left, RED, -3.9f, 9.6f, 3.8f, 5.8f, 3.2f, 1.4f);                     // talón
        boxM(leg, left, WHITE, -4.2f, 10.2f, -5.6f, 6.6f, 0.8f, 1.2f);                 // franja blanca
        boxM(leg, left, WHITE, -4.3f, 12.4f, -8.4f, 6.6f, 0.9f, 13.2f);                // suela
    }
}
