package com.warmod.util;

/**
 * Mirror of the authoritative server gun state, filled by {@code GunStatePacket} on the client.
 * Deliberately free of any client-only Minecraft class so shared code can reference it safely.
 */
public final class ClientGunState {
    public static String gunId = "";
    public static int ammo;
    public static int magSize = 1;
    public static int fireMode;
    public static long reloadEnd;
    public static boolean aiming;
    public static int fireCounter;
    public static int lastFireCounter;
    public static long lastFireTime;
    public static long reloadStart;
    public static float recoilImpulse;
    public static float adsProgress;
    public static boolean reloading;

    private ClientGunState() {}

    public static void apply(String id, int ammoIn, int magIn, int mode, long reloadEndTick,
                             boolean ads, int fireCounterIn) {
        gunId = id;
        ammo = ammoIn;
        magSize = Math.max(1, magIn);
        fireMode = mode;
        reloadEnd = reloadEndTick;
        aiming = ads;
        if (fireCounterIn != fireCounter) {
            fireCounter = fireCounterIn;
            lastFireTime = System.currentTimeMillis();
        }
    }

    public static boolean justFired() {
        return System.currentTimeMillis() - lastFireTime < 90L;
    }

    public static void tickClient(float partialTick, long gameTime) {
        reloading = reloadEnd > gameTime;
        float target = aiming ? 1.0F : 0.0F;
        adsProgress += (target - adsProgress) * 0.25F;
        recoilImpulse *= 0.72F;
    }
}
