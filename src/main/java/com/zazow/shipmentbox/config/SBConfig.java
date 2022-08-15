package com.zazow.shipmentbox.config;

import com.electronwill.nightconfig.core.Config;
import com.mojang.datafixers.util.Pair;
import net.minecraftforge.common.ForgeConfigSpec;
import org.antlr.v4.runtime.misc.MultiMap;

import java.util.*;

public class SBConfig {
    protected static final ForgeConfigSpec.Builder GENERAL_BUILDER = new ForgeConfigSpec.Builder();

    public static final General GENERAL = new General(GENERAL_BUILDER);


    public static final ForgeConfigSpec GENERAL_SPEC = GENERAL_BUILDER.build();
    public static class General {
        public class PriceConfig {
            public int count;
            public String key;
            public String reward;
            public int price;
            public int demand;
            PriceConfig(int price, String key, String reward, int count, int demand) {
                this.price = price;
                this.key = key;
                this.reward = reward;
                this.count = count;
                this.demand = demand;
            }
        }
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> priceConfig;
        private Map<String, PriceConfig> cachedPrices;

        public void computeCachedPrices() {
            cachedPrices = new HashMap<>();
            for (String s : priceConfig.get()) {
                String[] keyCountArr = s.split(",");
                if (keyCountArr.length != 5) {
                    continue;
                }
                String key = keyCountArr[1].trim();
                String reward = keyCountArr[4].trim();
                try {
                    int count = Integer.parseInt(keyCountArr[0].trim());
                    int demand = Integer.parseInt(keyCountArr[2].trim());
                    int price = Integer.parseInt(keyCountArr[3].trim());
                    cachedPrices.put(key, new PriceConfig(price, key, reward, count, demand));
                } catch (NumberFormatException e) {}
            }
        }
        public Map<String, PriceConfig> getPriceMap() {
            if (cachedPrices != null) {
                return cachedPrices;
            }
            computeCachedPrices();
            return cachedPrices;
        }
        General(ForgeConfigSpec.Builder builder) {
            String desc;
            builder.push("General");
            desc = "Price config for items. selling quantity, item, demand, price, currency";
            List<String> list = new ArrayList<>();
            list.add("16, minecraft:melon_slice, 1000, 1, minecraft:gold_nugget");
            priceConfig = builder.comment(desc).defineList("priceConfig", list, element -> {
                if (element instanceof String) {
                    String s = (String) element;
                    return s.split(",").length == 5;
                }
                return false;
            });
            builder.pop();
        }
    }
}
