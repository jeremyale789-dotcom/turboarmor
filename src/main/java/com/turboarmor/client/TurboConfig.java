package com.turboarmor.client;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Preferencia del jugador (solo cliente): interfaz del casco activada o no. Se guarda en config/turboarmor-client.properties. */
public final class TurboConfig {
    /** true = filtro azul, arranque, memes y panel de estado visibles. */
    public static volatile boolean uiEnabled = true;

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("turboarmor-client.properties");

    static {
        try (InputStream in = Files.newInputStream(FILE)) {
            Properties p = new Properties();
            p.load(in);
            uiEnabled = Boolean.parseBoolean(p.getProperty("ui_enabled", "true"));
        } catch (IOException ignored) {
            // primera vez: valores por defecto
        }
    }

    private TurboConfig() {}

    public static boolean toggle() {
        uiEnabled = !uiEnabled;
        try (OutputStream out = Files.newOutputStream(FILE)) {
            Properties p = new Properties();
            p.setProperty("ui_enabled", Boolean.toString(uiEnabled));
            p.store(out, "Turbo Armor (cliente)");
        } catch (IOException ignored) {
        }
        return uiEnabled;
    }
}
