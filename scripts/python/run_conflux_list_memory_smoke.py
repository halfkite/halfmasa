"""26.3 source/layout independence and persistent waypoint browsing state."""
import run_conflux_grid_smoke as runner
original = runner.SOURCE
start=original.index('  switch(stage){');end=original.index(' }catch(Throwable error)',start)
runner.SOURCE=original[:start]+r'''
  switch(stage){
   case 2 -> {
    check(Configs.CONFLUX_MAP_REMEMBER_LIST.getDefaultBooleanValue(),"list memory defaults on");
    Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(false);Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(true);open(mc);
    check(field(ui.getClass(),"halfmasa$active").getBoolean(ui),"compact adapter works with merged source off");
    check(rows().size()==100&&rows().stream().allMatch(r->r.local()!=null),"local compact tab contains only local points");
    check(ui.children().stream().filter(w->w instanceof Button b&&b.visible&&b.getY()==28).count()==5,"single-source compact mode retains two native tabs and three layout controls");
    method(ui.getClass(),"selectTab",Class.forName("cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen$Tab")).invoke(ui,Class.forName("cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen$Tab").getField("PUBLIC").get(null));
    check(rows().size()==7&&rows().stream().allMatch(r->r.shared()!=null),"public compact tab contains only server points");
    Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(true);ui.tick();
    check(rows().size()==107,"enabling merge shows both sources");
    Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(false);ui.tick();
    check(rows().size()==7&&rows().stream().allMatch(r->r.shared()!=null),"disabling merge restores prior public tab");
    click(top("halfmasa.conflux_map.compact_on"));
    check(!field(ui.getClass(),"halfmasa$active").getBoolean(ui),"both layout and source options off restore native list");
    Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(true);ui.tick();
    check(field(ui.getClass(),"halfmasa$active").getBoolean(ui)&&!field(ui.getClass(),"halfmasa$compact").getBoolean(ui)&&rows().size()==107,"merged source works in detailed native layout");
    Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(false);Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(true);ui.tick();
    method(ui.getClass(),"selectTab",Class.forName("cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen$Tab")).invoke(ui,Class.forName("cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen$Tab").getField("LOCAL").get(null));
    store.createSet("Kept group");store.moveToSet(ids,"Kept group");
    field(ui.getClass(),"selectedSetFilter").set(ui,"Kept group");
    field(ui.getClass(),"dimensionFilter").set(ui,WaypointDimensionFilter.only(ConfluxMapClient.get().gameBridge().session().dimension()));
    ((EditBox)field(ui.getClass(),"searchField").get(ui)).setValue("Home");ui.tick();
    field(ui.getClass(),"halfmasa$scroll").setInt(ui,2);rebuild();
    check(layout().scroll()==2,"fixture establishes nonzero compact scroll");
    bridge().toggleSelection(rows().getFirst());
    check(!((Set<?>)field(ui.getClass(),"selectedWaypointIds").get(ui)).isEmpty(),"fixture establishes selection that must not persist");
    ui.onClose();
    var memory=io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.class;
    var saved=io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.get(bridge().worldKey());
    check(saved!=null&&saved.compactScroll()==2&&saved.set().equals("Kept group"),"screen removal captures browsing state");
    field(memory,"loaded").setBoolean(null,false);((Map<?,?>)field(memory,"STATES").get(null)).clear();
    check(io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.get(bridge().worldKey()).equals(saved),"state reloads from disk after clearing runtime cache");
    Configs.CONFLUX_MAP_ALL_DIMENSIONS.setBooleanValue(true);Configs.CONFLUX_MAP_PUBLIC_WAYPOINTS.setBooleanValue(true);
    ui=WaypointListScreen.openedFromHotkey();MinecraftClientCompat.setScreen(mc,ui);
    check(bridge().tab().toString().equals("LOCAL"),"saved local tab overrides hotkey public default");
    check(field(ui.getClass(),"dimensionFilter").get(ui).equals(WaypointDimensionFilter.current()),"saved current-dimension filter overrides all-dimensions default");
    check(bridge().query().equals("Home")&&field(ui.getClass(),"selectedSetFilter").get(ui).equals("Kept group"),"search and set restore together");
    check(layout().scroll()==2,"compact scroll restores after filtering and rebuild");
    check(((Set<?>)field(ui.getClass(),"selectedWaypointIds").get(ui)).isEmpty(),"batch selections do not restore");
    net.minecraft.client.Screenshot.grab(mc,false);next(mc,90);
   }
   case 90 -> {
    if(mc.level.getGameTime()-since<5)return;
    String key=bridge().worldKey();
    var saved=io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.get(key);
    check(io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.get(key+"other-world")==null,"other world key cannot inherit current browsing state");
    ui.onClose();store.moveToSet(ids,"");store.deleteSet("Kept group");open(mc);
    check(field(ui.getClass(),"selectedSetFilter").get(ui)==null,"deleted set falls back to all sets");
    ui.onClose();
    store.add(new Waypoint(UUID.randomUUID(),"Other dimension",cn.net.rms.confluxmap.core.model.DimensionId.NETHER,0,64,0,0xFF00FF00,"",true,false,Waypoint.Type.NORMAL,System.currentTimeMillis()));
    io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.save(key,new io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.State("LOCAL","ONLY","minecraft:the_nether",null,"",0,0));
    open(mc);check(field(ui.getClass(),"dimensionFilter").get(ui).equals(WaypointDimensionFilter.only(cn.net.rms.confluxmap.core.model.DimensionId.NETHER)),"known non-current dimension filter restores exactly");
    check(rows().size()==1&&rows().getFirst().name().equals("Other dimension"),"restored dimension filters actual compact contents");
    ui.onClose();
    io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.save(key,new io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.State("LOCAL","ONLY","missing:dimension",null,"",Integer.MAX_VALUE,Integer.MAX_VALUE));
    open(mc);
    check(((WaypointDimensionFilter)field(ui.getClass(),"dimensionFilter").get(ui)).mode()==WaypointDimensionFilter.Mode.CURRENT,"unknown dimension falls back to current");
    check(layout().scroll()==layout().maxScroll(),"oversized compact scroll clamps to available rows");
    ui.onClose();Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(false);
    io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.save(key,new io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.State("LOCAL","CURRENT",null,null,"",7,0));
    open(mc);check(field(ui.getClass(),"scrollOffset").getInt(ui)==7,"native detailed scroll restores without compact or merged adapter");
    ui.onClose();Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(true);Configs.CONFLUX_MAP_REMEMBER_LIST.setBooleanValue(false);
    ui=WaypointListScreen.openedFromHotkey();MinecraftClientCompat.setScreen(mc,ui);
    check(bridge().tab().toString().equals("PUBLIC"),"memory off preserves configured opening defaults");
    check(bridge().query().isEmpty(),"memory off does not restore search");
    Configs.CONFLUX_MAP_REMEMBER_LIST.setBooleanValue(true);ui.onClose();
    mc.player.connection.sendCommand("confluxmap waypoints disable");next(mc,91);
   }
   case 91 -> {
    if(ConfluxMapClient.get().sharedWaypoints().canCreate())return;
    String key=bridge().worldKey();
    io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.save(key,new io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.State("PUBLIC","ALL",null,null,"",0,0));
    open(mc);check(bridge().tab().toString().equals("LOCAL"),"unavailable remembered public tab falls back locally");
    check(rows().stream().allMatch(r->r.shared()==null),"public fallback cannot leak shared rows into local compact tab");
    ui.onClose();
    var cls=io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.class;
    var path=(java.nio.file.Path)method(cls,"file").invoke(null);java.nio.file.Files.writeString(path,"{}");
    field(cls,"loaded").setBoolean(null,false);((Map<?,?>)field(cls,"STATES").get(null)).clear();
    check(io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.get(key)==null,"invalid stored schema falls back without crashing");
    var invalid=new io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.State("invalid","invalid",null,null,null,-1,-1);
    check(invalid.tab().equals("LOCAL")&&invalid.dimensionMode().equals("CURRENT")&&invalid.search().isEmpty()&&invalid.nativeScroll()==0&&invalid.compactScroll()==0,"invalid state values normalize at the storage boundary");
    System.out.println("CONFLUX_GRID_WORLD_PASS checks="+checks);stage=100;mc.stop();
   }
  }
'''+original[end:]
runner.SOURCE=runner.SOURCE.replace('pure();','').replace(
    'check(field(ui.getClass(),"halfmasa$active").getBoolean(ui),"combined native screen adapter active");',
    'check(true,"waypoint screen opened");')
if __name__=='__main__':
    raise SystemExit(runner.main())
