package com.warmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Warmod.MODID)
public class Warmod {
    public static final String MODID = "warmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Warmod() {
        LOGGER.info("War Mod bootstrap");
    }
}
