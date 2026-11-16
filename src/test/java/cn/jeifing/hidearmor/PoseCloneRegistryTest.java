package cn.jeifing.hidearmor;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class PoseCloneRegistryTest {
 @Test void scopedPoseIdentityAndLifecycle() {
  PoseCloneRegistry r=new PoseCloneRegistry();UUID owner=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID(),clone=UUID.randomUUID();
  r.profile(a,clone,"test");r.spawn(a,10,clone);assertNull(r.owner(a,10));
  r.begin("test",owner);r.profile(a,clone,"test");r.spawn(a,10,clone);
  assertEquals(owner,r.owner(a,10));assertNull(r.owner(b,10));
  assertEquals(java.util.List.of(10),r.ids(a,owner));
  r.spawn(a,10,UUID.randomUUID());assertNull(r.owner(a,10));
  r.spawn(a,11,clone);r.destroy(a,new int[]{11});assertNull(r.owner(a,11));
  r.profile(a,clone,"test");r.spawn(a,12,clone);r.end(owner);assertNull(r.owner(a,12));
  r.begin("test",owner);r.profile(a,owner,"test");r.spawn(a,13,owner);assertNull(r.owner(a,13));
  r.profile(a,clone,"test");r.spawn(a,14,clone);r.viewerLeft(a);assertNull(r.owner(a,14));
 }
}
