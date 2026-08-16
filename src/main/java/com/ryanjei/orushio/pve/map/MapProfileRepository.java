package com.ryanjei.orushio.pve.map;
import com.ryanjei.orushio.pve.domain.MapProfileId;import java.util.*;
public interface MapProfileRepository { List<MapProfile> findAll(); Optional<MapProfile> find(MapProfileId id); void save(MapProfile profile); void delete(MapProfileId id); }
