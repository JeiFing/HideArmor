package cn.jeifing.hidearmor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Detached packet identities, scoped to each viewer and an active GSit pose. */
final class PoseCloneRegistry {
    private record Profile(UUID owner, UUID profile) {}
    private final Map<String,UUID> posing = new ConcurrentHashMap<>();
    private final Map<UUID,Map<UUID,UUID>> profiles = new ConcurrentHashMap<>();
    private final Map<UUID,Map<Integer,Profile>> entities = new ConcurrentHashMap<>();
    void begin(String name, UUID owner) { posing.put(name,owner); }
    void end(UUID owner) {
        posing.values().removeIf(owner::equals);
        profiles.values().forEach(m->m.values().removeIf(owner::equals));
        entities.values().forEach(m->m.values().removeIf(p->p.owner.equals(owner)));
    }
    void profile(UUID viewer, UUID profile, String name) {
        if(viewer==null||profile==null||name==null)return;
        UUID owner=posing.get(name);
        if(owner==null||owner.equals(profile))return;
        Map<UUID,UUID> m=profiles.computeIfAbsent(viewer,k->new ConcurrentHashMap<>());
        if(m.size()<4096)m.put(profile,owner);
    }
    void spawn(UUID viewer, int entity, UUID profile) {
        Map<Integer,Profile> m=entities.get(viewer);if(m!=null)m.remove(entity);
        UUID owner=profiles.getOrDefault(viewer,Map.of()).get(profile);
        if(owner==null||!posing.containsValue(owner))return;
        m=entities.computeIfAbsent(viewer,k->new ConcurrentHashMap<>());
        if(m.size()<4096)m.put(entity,new Profile(owner,profile));
    }
    UUID owner(UUID viewer,int entity) {
        Profile p=entities.getOrDefault(viewer,Map.of()).get(entity);return p==null?null:p.owner;
    }
    List<Integer> ids(UUID viewer,UUID owner) {
        return entities.getOrDefault(viewer,Map.of()).entrySet().stream()
            .filter(e->e.getValue().owner.equals(owner)).map(Map.Entry::getKey).toList();
    }
    void destroy(UUID viewer,int[] ids) {
        Map<Integer,Profile> m=entities.get(viewer);if(m==null)return;
        for(int id:ids){Profile p=m.remove(id);if(p!=null){Map<UUID,UUID> ps=profiles.get(viewer);if(ps!=null)ps.remove(p.profile);}}
    }
    void viewerLeft(UUID viewer) { profiles.remove(viewer);entities.remove(viewer); }
    void clear() { posing.clear();profiles.clear();entities.clear(); }
}
