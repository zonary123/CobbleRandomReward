package com.kingpixel.cobblerandomreward.config;

import com.google.gson.reflect.TypeToken;
import com.kingpixel.cobblerandomreward.CobbleRandomReward;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.PokemonChance;
import com.kingpixel.cobbleutils.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Improved by GitHub Copilot - 31/01/2025 6:19
 */
public class OldConfig {
  private static String Items = "/config/cobbleutils/random/items.json";
  private static String pokemons = "/config/cobbleutils/random/pokemons.json";
  private static String money = "/config/cobbleutils/random/money.json";

  public static void init() {

    File fileItems = Utils.getAbsolutePath(Items);
    File filePokemons = Utils.getAbsolutePath(pokemons);
    File fileMoney = Utils.getAbsolutePath(money);

    try {
      Map<String, Map<String, List<ItemChance>>> items = Utils.newGson().fromJson(Utils.readFileSync(fileItems), new TypeToken<Map<String, Map<String, List<ItemChance>>>>() {
      }.getType());
      Map<String, Map<String, List<PokemonChance>>> pokemons = Utils.newGson().fromJson(Utils.readFileSync(filePokemons)
        , new TypeToken<Map<String, Map<String, List<PokemonChance>>>>() {
        }.getType());
      Map<String, Map<String, List<ItemChance>>> money = Utils.newGson().fromJson(Utils.readFileSync(fileMoney), new TypeToken<Map<String, Map<String, List<ItemChance>>>>() {
      }.getType());
      Map<String, Map<String, List<ItemChance>>> all = new HashMap<>();
      Map<String, List<ItemChance>> pokemonMap = new HashMap<>();
      pokemons.forEach((key, value) ->
        value.forEach((subKey, subValue) -> {
          List<ItemChance> pokemonChances = new ArrayList<>();
          for (PokemonChance pokemonChance : subValue) {
            String pokemon = "";
            if (!pokemonChance.getPokemon().startsWith("pokemon:")) {
              pokemon = "pokemon:" + pokemonChance.getPokemon();
            }
            ItemChance itemChance = new ItemChance(pokemon, pokemonChance.getChance());
            pokemonChances.add(itemChance);
          }
          pokemonMap.put(subKey, pokemonChances);
        }));
      all.putAll(items);
      all.put("asd", pokemonMap);
      all.putAll(money);

      all.forEach((key, value) -> value.forEach((subKey, subValue) -> Utils.writeFileAsync(CobbleRandomReward.PATH_REWARDS, subKey + ".json", Utils.newGson().toJson(subValue))));
      File rewards = Utils.getAbsolutePath("/config/cobbleutils/random");
      deleteRecursively(rewards);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void deleteRecursively(File file) {
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        deleteRecursively(child);
      }
    }
    file.delete();
  }
}