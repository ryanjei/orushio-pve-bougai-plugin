package com.ryanjei.orushio.pve.paper;
import java.util.*;import java.util.function.Predicate;
final class SetupToolCleanup{private SetupToolCleanup(){}static<T>List<Integer> ownedSlots(List<T> items,Predicate<T> owned){List<Integer> slots=new ArrayList<>();for(int index=0;index<items.size();index++)if(owned.test(items.get(index)))slots.add(index);return slots;}}
