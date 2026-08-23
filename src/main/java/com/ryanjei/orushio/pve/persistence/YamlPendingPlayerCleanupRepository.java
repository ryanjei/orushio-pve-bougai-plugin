package com.ryanjei.orushio.pve.persistence;

import java.nio.file.Path;
import java.util.*;

public final class YamlPendingPlayerCleanupRepository implements PendingPlayerCleanupRepository {
    private final AtomicYamlStore store;
    public YamlPendingPlayerCleanupRepository(Path path){store=new AtomicYamlStore(path);}
    public synchronized Set<Entry> findAll(){return decode(store.read().orElseGet(()->Map.of("schemaVersion","1","entryCount","0")));}
    public synchronized void add(Entry entry){Set<Entry> entries=new LinkedHashSet<>(findAll());if(entries.add(entry))store.write(encode(entries));}
    public synchronized void remove(Entry entry){Set<Entry> entries=new LinkedHashSet<>(findAll());if(entries.remove(entry))store.write(encode(entries));}
    private static Map<String,String> encode(Set<Entry> entries){List<Entry> ordered=entries.stream().sorted(Comparator.comparing((Entry e)->e.playerUuid().toString()).thenComparing(e->e.sessionId().toString())).toList();Map<String,String> values=new LinkedHashMap<>();values.put("schemaVersion","1");values.put("entryCount",Integer.toString(ordered.size()));for(int i=0;i<ordered.size();i++){values.put("entry."+i+".playerUuid",ordered.get(i).playerUuid().toString());values.put("entry."+i+".sessionId",ordered.get(i).sessionId().toString());}return values;}
    private static Set<Entry> decode(Map<String,String> values){try{int count=Integer.parseInt(required(values,"entryCount"));if(count<0||count>10000)throw new RepositoryException("pending cleanup件数が範囲外です。");Set<String> expected=new HashSet<>(Set.of("schemaVersion","entryCount"));Set<Entry> entries=new LinkedHashSet<>();for(int i=0;i<count;i++){String playerKey="entry."+i+".playerUuid",sessionKey="entry."+i+".sessionId";expected.add(playerKey);expected.add(sessionKey);Entry entry=new Entry(UUID.fromString(required(values,playerKey)),UUID.fromString(required(values,sessionKey)));if(!entries.add(entry))throw new RepositoryException("pending cleanup entryが重複しています。");}if(!values.keySet().equals(expected))throw new RepositoryException("pending cleanupに未知または欠落した項目があります。");return Set.copyOf(entries);}catch(RepositoryException failure){throw failure;}catch(Exception failure){throw new RepositoryException("pending cleanupの値が不正です。",failure);}}
    private static String required(Map<String,String> values,String key){String value=values.get(key);if(value==null||value.isBlank())throw new RepositoryException("pending cleanupの必須項目がありません: "+key);return value;}
}
