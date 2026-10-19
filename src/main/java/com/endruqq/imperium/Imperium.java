package com.endruqq.imperium;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Imperium.MODID)
public class Imperium {
    public static final String MODID = "imperium";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Imperium() {
        LOGGER.info("Imperium loading");
    }
}
