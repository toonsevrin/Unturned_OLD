package org.exorath.unturned.libraries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
/**
 * <p>A utility class for handling converting an inventory to/from a String
 * to be stored into the MySQL database or any sort of file.</p>
 */
public class InventorySerializer {

    private static String M_SEP = "�";
    private static String S_SEP = "�";

    /**
     * <p>Serializes the player's inventory into a string.</p>
     *
     * @param inventory inventory to serialize
     * @return          the serialized string
     */
    public static String serialize(PlayerInventory inventory) {
        ItemStack[] content;
        StringBuilder sb = new StringBuilder();

        // Main content
        content = inventory.getContents();
        for (int i = 0; i < content.length; i++) {
            serialize(content[i], sb, "i" + i);
        }

        // Armor
        content = inventory.getArmorContents();
        for (int i = 0; i < content.length; i++) {
            serialize(content[i], sb, "a" + i);
        }

        return sb.length() == 0 ? "" : sb.substring(0, sb.length() - 1);
    }

    /**
     * <p>Deserializes the inventory from the string</p>
     * <p>The current contents of the inventory will be cleared
     * before the deserialized items are added to it.</p>
     *
     * @param inv        inventory to fill with the result
     * @param dataString inventory serialized string to deserialize
     */
    public static void deserialize(PlayerInventory inv, String dataString) {
        inv.clear();
        if (dataString.length() == 0) return;
        ItemStack[] armor = new ItemStack[4];
        String[] data = dataString.split(M_SEP);
        for (int i = 0; i < data.length; i += 7) {

            // Deserialize the data
            String key = data[i];
            ItemStack item = deserialize(data, i);
            int slot = Integer.parseInt(key.substring(1));

            // Armor slots
            if (key.charAt(0) == 'a') {
                armor[slot] = item;
            }

            // Main inventory
            else if (key.charAt(0) == 'i') {
                inv.setItem(slot, item);
            }
        }
        inv.setArmorContents(armor);
    }

    /**
     * <p>Deserializes the armor contents from the inventory only.</p>
     * <p>The main inventory contents will be ignored.</p>
     * <p>If an armor piece was not in the data, it will be null instead.</p>
     * <p>The returned array is in the same order as a player inventory's
     * armor array.</p>
     *
     * @param dataString inventory serialized string to deserialize
     * @return           armor contents
     */
    public static ItemStack[] deserializeArmor(String dataString) {
        ItemStack[] armor = new ItemStack[4];
        if (dataString.length() == 0) return armor;
        String[] data = dataString.split(M_SEP);
        for (int i = 0; i < data.length; i += 7) {

            // Deserialize the data
            String key = data[i];

            // Armor slots
            if (key.charAt(0) == 'a') {
                ItemStack item = deserialize(data, i);
                int slot = Integer.parseInt(key.substring(1));
                armor[slot] = item;
            }
        }

        return armor;
    }

    private static void serialize(ItemStack item, StringBuilder sb, String id) {

        // Ignore empty slots
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;

        // Serialize the item
        sb.append(id);
        sb.append(M_SEP);
        sb.append(item.getType().name());
        sb.append(M_SEP);
        sb.append(item.getData().getData());
        sb.append(M_SEP);
        sb.append(item.getType().getMaxDurability() > 0 ? item.getDurability() : item.getAmount());
        sb.append(M_SEP);
        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (enchants.size() > 0) {
            for (Map.Entry<Enchantment, Integer> enchant : enchants.entrySet()) {
                sb.append(enchant.getValue());
                sb.append(S_SEP);
                sb.append(enchant.getKey().getName());
                sb.append(S_SEP);
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(M_SEP);
        sb.append(meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "");
        sb.append(M_SEP);
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                sb.append(line.length());
                sb.append(S_SEP);
                sb.append(line);
            }
        }
        sb.append(M_SEP);
    }

    private static ItemStack deserialize(String[] data, int start) {
    	Bukkit.getLogger().info("--- Data array ---");
        for (String string : data)
        {
            Bukkit.getLogger().info(string);
        }
        Bukkit.getLogger().info("------------------");
        Material mat = Material.valueOf(data[start + 1]);
        byte matData = Byte.parseByte(data[start + 2]);
        int amount = mat.getMaxDurability() > 0 ? 1 : Integer.parseInt(data[start + 3]);
        short dur = mat.getMaxDurability() > 0 ? Short.parseShort(data[start + 3]) : 0;
        ItemStack item = new ItemStack(mat, amount, dur, matData);
        if (data.length > start + 4 && data[start + 4].length() > 0) {
            String[] enchantData = data[start + 4].split(S_SEP);
            for (int i = 0; i < enchantData.length; i += 2) {
                item.addEnchantment(Enchantment.getByName(enchantData[i + 1]), Integer.parseInt(enchantData[i]));
            }
        }
        ItemMeta meta = item.getItemMeta();
        if (data.length > start + 5 && data[start + 5].length() > 0) meta.setDisplayName(data[start + 5]);
        if (data.length > start + 6 && data[start + 6].length() > 0) {
            List<String> lore = new ArrayList<String>();
            int index = 0;
            while(index < data[start + 6].length()) {
                int next = data[start + 6].indexOf(S_SEP, index);
                int length = Integer.parseInt(data[start + 6].substring(index, next));
                index = next + S_SEP.length() + length;
                if (index > data[start + 6].length()) index = data[start + 6].length();
                lore.add(length == 0 ? "" : data[start + 6].substring(next + S_SEP.length(), index));
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}