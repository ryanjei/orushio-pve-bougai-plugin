package com.ryanjei.orushio.pve.economy;
import java.util.*;
public record ItemSpec(String material,int amount,Map<String,Integer> enchantments,boolean unbreakable){public ItemSpec{if(material==null||material.isBlank())throw new IllegalArgumentException("商品Materialが必要です。");if(amount<1)throw new IllegalArgumentException("商品個数が範囲外です。");enchantments=Map.copyOf(enchantments==null?Map.of():enchantments);enchantments.forEach((key,level)->{if(key==null||key.isBlank()||level==null||level<1)throw new IllegalArgumentException("Enchant設定が不正です。");});}}
