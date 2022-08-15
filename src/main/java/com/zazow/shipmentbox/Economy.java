package com.zazow.shipmentbox;

import com.zazow.shipmentbox.config.SBConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Economy extends SavedData {
    private static Economy economy;
    private static String SAVED_DATA_NAME = SBMod.MODID + "_economy";

    public final static int MOVING_AVERAGE_SIZE = 10;
    public static final Logger LOGGER = LogManager.getLogger();

    public class ItemHistory {
        public int[] history;
        public long lastDay;
        public Map<UUID, Integer> playerSoldQuantities;
        ItemHistory() {
            this(0);
        }
        ItemHistory(long day) {
            this.history = new int[MOVING_AVERAGE_SIZE];
            this.lastDay = day;
            playerSoldQuantities = new HashMap<>();
        }

        int getPreviousDaysSum(long day) {
            if (day != lastDay) {
                // Reset
                long daysSinceLastAccessed = day - lastDay;
                for (int i = 1; i <= Math.min(daysSinceLastAccessed, MOVING_AVERAGE_SIZE); ++i) {
                    this.history[(int) ((lastDay + i) % MOVING_AVERAGE_SIZE)] = 0;
                }
                lastDay = day;
                playerSoldQuantities.clear();
                Economy.this.setDirty();
            }

            int sum = 0;
            for (int i = 1; i < MOVING_AVERAGE_SIZE; ++i) {
                sum += this.history[i % MOVING_AVERAGE_SIZE];
            }

            return sum;
        }

        public int getPlayerSoldAmount(UUID player) {
            return playerSoldQuantities.containsKey(player) ? playerSoldQuantities.get(player) : 0;
        }
        void addSoldOnDay(UUID player, int count, long day) {
            history[(int)(day % MOVING_AVERAGE_SIZE)] += count;
            playerSoldQuantities.put(player, getPlayerSoldAmount(player) + count);
        }

        public void save(CompoundTag tag) {
            tag.putIntArray("History", history);
            tag.putLong("LastDay", lastDay);

            CompoundTag playerSoldQuantitiesTag = new CompoundTag();
            for (Map.Entry<UUID, Integer> entry : playerSoldQuantities.entrySet()) {
                playerSoldQuantitiesTag.putInt(entry.getKey().toString(), entry.getValue());
            }

            tag.put("PlayerSoldQuantities", playerSoldQuantitiesTag);
        }
        public void load(CompoundTag tag) {
            this.history = tag.getIntArray("History");
            this.lastDay = tag.getLong("LastDay");
            this.playerSoldQuantities.clear();
            CompoundTag playerSoldQuantitiesTag = (CompoundTag) tag.get("PlayerSoldQuantities");
            for (String uuid : playerSoldQuantitiesTag.getAllKeys()) {
                playerSoldQuantities.put(UUID.fromString(uuid), playerSoldQuantitiesTag.getInt(uuid));
            }
        }
    }
    Map<String, ItemHistory> histories = new HashMap<>();

    public Economy() {
        super();
        this.setDirty();
    }
    public ItemHistory getItemHistory(String key, long day) {
        ItemHistory history;
        if (!histories.containsKey(key)) {
            history = new ItemHistory(day);
            histories.put(key, history);
        } else {
            history = histories.get(key);
        }

        return history;
    }

    public int getMaxAllowedToSell(UUID player, String key, long day) {
        if (!SBConfig.GENERAL.getPriceMap().containsKey(key) || player == null) {
            return 0;
        }
        SBConfig.General.PriceConfig config = SBConfig.GENERAL.getPriceMap().get(key);

        ItemHistory history = getItemHistory(key, day);

        int previousDaysSum = history.getPreviousDaysSum(day);
        int demand = config.demand;
        int maxAllowedToSell = demand * MOVING_AVERAGE_SIZE - previousDaysSum;
        return Math.max(Math.min(maxAllowedToSell, 2 * config.demand) - history.getPlayerSoldAmount(player), 0);
    }

    public void onItemSold(UUID player, String key, int count, long day) {
        if (count == 0) {
            return;
        }
        ItemHistory history = getItemHistory(key, day);
        history.addSoldOnDay(player, count, day);
        this.setDirty();
    }



    public Economy load(CompoundTag tag) {
        this.histories.clear();
        if (!tag.contains("History")) {
            LOGGER.warn("Could not load Economy from following tag: " + tag);
            return this;
        }

        CompoundTag historiesTag = (CompoundTag) tag.get("History");
        for (String item : historiesTag.getAllKeys()) {
            ItemHistory history = new ItemHistory();
            history.load((CompoundTag) historiesTag.get(item));
            this.histories.put(item, history);
        }
        this.setDirty(false);
        return this;
    }
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag historiesTag = new CompoundTag();
        for (Map.Entry<String, ItemHistory> entry : histories.entrySet()) {
            CompoundTag historyTag = new CompoundTag();
            entry.getValue().save(historyTag);
            historiesTag.put(entry.getKey(), historyTag);
        }
        tag.put("History", historiesTag);
        return tag;
    }

    public static void init(MinecraftServer server) {
        economy = server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(nbt -> new Economy().load(nbt), Economy::new, SAVED_DATA_NAME);
    }

    public static Economy get() {
        return economy;
    }

}
