package com.warmod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

/**
 * All tunables of the mod. A SERVER spec (gameplay: damage, spread, range, bot waves) and a
 * CLIENT spec (music volume, ADS zoom, camera shake, tracers).
 */
public final class WarConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Server, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();

        Pair<Client, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }

    private WarConfig() {}

    /** Values that only exist on a dedicated server / integrated server. Safe defaults off-thread. */
    public static double damageMul() { return serverDouble(() -> SERVER.damageMultiplier.get(), 1.0D); }
    public static double spreadMul() { return serverDouble(() -> SERVER.spreadMultiplier.get(), 1.0D); }
    public static double rangeMul() { return serverDouble(() -> SERVER.rangeMultiplier.get(), 1.0D); }
    public static double recoilMul() { return serverDouble(() -> SERVER.recoilMultiplier.get(), 1.0D); }
    public static boolean friendlyFire() { return serverBool(() -> SERVER.friendlyFire.get(), false); }
    public static boolean botWaves() { return serverBool(() -> SERVER.botWavesEnabled.get(), true); }
    public static int botWaveInterval() { return (int) serverDouble(() -> SERVER.botWaveInterval.get().doubleValue(), 180.0D); }
    public static int botWaveSize() { return (int) serverDouble(() -> SERVER.botWaveSize.get().doubleValue(), 3.0D); }
    public static int maxBotsPerTeam() { return (int) serverDouble(() -> SERVER.maxBotsPerTeam.get().doubleValue(), 24.0D); }
    public static double botAccuracy() { return serverDouble(() -> SERVER.botAccuracy.get(), 0.55D); }
    public static boolean ambientSounds() { return serverBool(() -> SERVER.ambientSoundsEnabled.get(), true); }
    public static int ambientMinSeconds() { return (int) serverDouble(() -> SERVER.ambientMinSeconds.get().doubleValue(), 10.0D); }
    public static int ambientMaxSeconds() { return (int) serverDouble(() -> SERVER.ambientMaxSeconds.get().doubleValue(), 30.0D); }
    public static boolean vehiclesNeedFuel() { return serverBool(() -> SERVER.vehiclesNeedFuel.get(), false); }

    public static double musicVolume() { return clientDouble(() -> CLIENT.musicVolume.get(), 0.6D); }
    public static boolean darkSky() { return clientBool(() -> CLIENT.darkSky.get(), true); }
    public static boolean tracers() { return clientBool(() -> CLIENT.tracers.get(), true); }
    public static double adsZoom() { return clientDouble(() -> CLIENT.adsZoomMultiplier.get(), 1.0D); }
    public static boolean cameraShake() { return clientBool(() -> CLIENT.cameraShake.get(), true); }

    private static double serverDouble(Supplier<Double> getter, double def) {
        try { Double v = getter.get(); return v == null ? def : v; } catch (Throwable ignored) { return def; }
    }
    private static boolean serverBool(Supplier<Boolean> getter, boolean def) {
        try { Boolean v = getter.get(); return v == null ? def : v; } catch (Throwable ignored) { return def; }
    }
    private static double clientDouble(Supplier<Double> getter, double def) {
        try { Double v = getter.get(); return v == null ? def : v; } catch (Throwable ignored) { return def; }
    }
    private static boolean clientBool(Supplier<Boolean> getter, boolean def) {
        try { Boolean v = getter.get(); return v == null ? def : v; } catch (Throwable ignored) { return def; }
    }

    public static class Server {
        public final ForgeConfigSpec.DoubleValue damageMultiplier;
        public final ForgeConfigSpec.DoubleValue spreadMultiplier;
        public final ForgeConfigSpec.DoubleValue rangeMultiplier;
        public final ForgeConfigSpec.DoubleValue recoilMultiplier;
        public final ForgeConfigSpec.BooleanValue friendlyFire;
        public final ForgeConfigSpec.BooleanValue botWavesEnabled;
        public final ForgeConfigSpec.DoubleValue botWaveInterval;
        public final ForgeConfigSpec.DoubleValue botWaveSize;
        public final ForgeConfigSpec.DoubleValue maxBotsPerTeam;
        public final ForgeConfigSpec.DoubleValue botAccuracy;
        public final ForgeConfigSpec.BooleanValue ambientSoundsEnabled;
        public final ForgeConfigSpec.DoubleValue ambientMinSeconds;
        public final ForgeConfigSpec.DoubleValue ambientMaxSeconds;
        public final ForgeConfigSpec.BooleanValue vehiclesNeedFuel;

        Server(ForgeConfigSpec.Builder b) {
            b.comment("Weapon balance").push("weapons");
            damageMultiplier = b.comment("Global damage multiplier applied on top of per-weapon damage")
                    .defineInRange("damageMultiplier", 1.0D, 0.0D, 20.0D);
            spreadMultiplier = b.comment("Global spread multiplier (1.0 = weapon defaults)")
                    .defineInRange("spreadMultiplier", 1.0D, 0.0D, 10.0D);
            rangeMultiplier = b.comment("Global range multiplier (1.0 = weapon defaults)")
                    .defineInRange("rangeMultiplier", 1.0D, 0.1D, 10.0D);
            recoilMultiplier = b.comment("Global recoil multiplier")
                    .defineInRange("recoilMultiplier", 1.0D, 0.0D, 10.0D);
            friendlyFire = b.comment("Allow damaging players/soldiers of your own team")
                    .define("friendlyFire", false);
            b.pop();

            b.comment("AI soldiers and bot waves").push("bots");
            botWavesEnabled = b.comment("Automatically spawn enemy bot waves in the battlefield")
                    .define("botWavesEnabled", true);
            botWaveInterval = b.comment("Seconds between bot waves")
                    .defineInRange("botWaveInterval", 180.0D, 15.0D, 7200.0D);
            botWaveSize = b.comment("Bots per wave")
                    .defineInRange("botWaveSize", 3.0D, 1.0D, 32.0D);
            maxBotsPerTeam = b.comment("Hard cap of living bots per team")
                    .defineInRange("maxBotsPerTeam", 24.0D, 1.0D, 200.0D);
            botAccuracy = b.comment("Bot accuracy 0..1 (0 = stormtrooper, 1 = aimbot)")
                    .defineInRange("botAccuracy", 0.55D, 0.0D, 1.0D);
            b.pop();

            b.comment("Ambient atmosphere").push("ambient");
            ambientSoundsEnabled = b.comment("Play random ambient war sounds in the battlefield dimension")
                    .define("ambientSoundsEnabled", true);
            ambientMinSeconds = b.comment("Minimum delay between ambient sounds (seconds)")
                    .defineInRange("ambientMinSeconds", 10.0D, 3.0D, 600.0D);
            ambientMaxSeconds = b.comment("Maximum delay between ambient sounds (seconds)")
                    .defineInRange("ambientMaxSeconds", 30.0D, 5.0D, 1200.0D);
            b.pop();

            b.comment("Vehicles").push("vehicles");
            vehiclesNeedFuel = b.comment("Vehicles burn fuel while driving (tanks start at 100%)")
                    .define("vehiclesNeedFuel", false);
            b.pop();
        }
    }

    public static class Client {
        public final ForgeConfigSpec.DoubleValue musicVolume;
        public final ForgeConfigSpec.BooleanValue darkSky;
        public final ForgeConfigSpec.BooleanValue tracers;
        public final ForgeConfigSpec.DoubleValue adsZoomMultiplier;
        public final ForgeConfigSpec.BooleanValue cameraShake;

        Client(ForgeConfigSpec.Builder b) {
            b.push("client");
            musicVolume = b.comment("Volume of the looping battlefield music (0 disables it)")
                    .defineInRange("musicVolume", 0.6D, 0.0D, 1.0D);
            darkSky = b.comment("Apply the dark battlefield sky/fog")
                    .define("darkSky", true);
            tracers = b.comment("Render bullet tracers")
                    .define("tracers", true);
            adsZoomMultiplier = b.comment("Multiplier for ADS field-of-view zoom")
                    .defineInRange("adsZoomMultiplier", 1.0D, 0.1D, 5.0D);
            cameraShake = b.comment("Recoil camera shake")
                    .define("cameraShake", true);
            b.pop();
        }
    }
}
