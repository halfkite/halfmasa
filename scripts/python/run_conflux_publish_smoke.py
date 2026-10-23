"""26.3 real-server checks for public copies and compact selection controls."""
import run_conflux_grid_smoke as runner

original = runner.SOURCE
start = original.index('  switch(stage){')
end = original.index(' }catch(Throwable error)', start)
runner.SOURCE = original[:start] + r'''
  switch(stage){
   case 2 -> {
    check(!Configs.CONFLUX_MAP_QUICK_PUBLISH.getDefaultBooleanValue(),"quick publishing defaults off");
    Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(true);
    Configs.CONFLUX_MAP_WAYPOINT_ORDER.setOptionListValue(ConfluxMapWaypointOrder.PERSONAL_FIRST);
    store.createSet("Original group");store.assignToSet(personalId,"Original group");
    open(mc);var target=rows().stream().filter(r->r.id().equals(personalId)).findFirst().orElseThrow();hover(mc,target);
    check(actions().size()==7,"disabled option preserves original popup actions");
    Configs.CONFLUX_MAP_QUICK_PUBLISH.setBooleanValue(true);ui.tick();hover(mc,target);
    check(actions().size()==8,"enabled option adds exactly one public-copy action");
    check(action("halfmasa.conflux_map.publish").active,"server permits publishing local point");
    var card=layout().cells().getFirst();var checks=(List<AbstractWidget>)field(ui.getClass(),"halfmasa$checks").get(ui);
    var checkbox=(Button)checks.getFirst();
    check(checkbox.getX()==card.x()+card.width()-23,"compact checkbox occupies requested right edge");
    check(!bridge().selected(target),"selection starts clear");click(checkbox);
    check(bridge().selected(target),"card checkbox updates native batch selection");
    check(actions().getFirst().getMessage().getString().equals("✓"),"popup selection matches card checkbox");
    click((Button)actions().getFirst());
    check(!bridge().selected(target),"popup checkbox clears shared selection");
    checks=(List<AbstractWidget>)field(ui.getClass(),"halfmasa$checks").get(ui);
    check(checks.getFirst().getMessage().getString().isEmpty(),"card checkbox reflects popup deselection");
    check(store.list().size()==100,"selection never deletes or publishes local points");
    store.createSet("Batch destination");
    checks=(List<AbstractWidget>)field(ui.getClass(),"halfmasa$checks").get(ui);click((Button)checks.getFirst());
    field(ui.getClass(),"moveTargetSet").set(ui,"Batch destination");
    method(ui.getClass(),"moveSelection",WaypointStore.class).invoke(ui,store);
    check(store.list().stream().anyMatch(w->w.id.equals(personalId)&&w.group.equals("Batch destination")),"right-edge selection works with native batch move");
    check(!bridge().selected(target),"native batch move clears checkbox selection");
    store.assignToSet(personalId,"Original group");rebuild();
    ui.mouseScrolled(layout().left()+5,layout().top()+5,0,-1);
    checks=(List<AbstractWidget>)field(ui.getClass(),"halfmasa$checks").get(ui);
    check(checks.size()==layout().cells().stream().filter(c->rowsUnchecked().get(c.index()).local()!=null).count(),"scroll rebuilds only visible local checkboxes");
    field(ui.getClass(),"halfmasa$scroll").setInt(ui,0);rebuild();hover(mc,target);
    var button=action("halfmasa.conflux_map.publish");click(button);
    var shared=ConfluxMapClient.get().sharedWaypoints();
    check(shared.isCreatePending((Waypoint)target.local()),"publication enters native pending state");
    check(!button.active,"pending request disables repeated clicks");
    method(bridge().getClass(),"publish",ConfluxWaypointScreenBridge.Row.class).invoke(bridge(),target);
    check(store.list().stream().anyMatch(w->w.id.equals(personalId)),"original local point remains while publishing");
    next(mc,90);
   }
   case 90 -> {
    var shared=ConfluxMapClient.get().sharedWaypoints();if(shared.list().size()<8)return;ui.tick();
    var target=rows().stream().filter(r->r.id().equals(personalId)).findFirst().orElseThrow();hover(mc,target);
    var local=(Waypoint)target.local();
    var copies=shared.list().stream().filter(w->w.name().equals(local.name)&&w.x()==local.x&&w.y()==local.y&&w.z()==local.z&&w.dimensionId().equals(local.dimensionId)).toList();
    check(copies.size()==1,"one public copy retains name dimension and exact XYZ");
    check(!copies.getFirst().id().equals(personalId),"public copy has independent identity");
    check(store.list().size()==100&&local.id.equals(personalId)&&local.group.equals("Original group"),"publishing preserves local UUID count and set");
    check(!action("halfmasa.conflux_map.published").active,"successful publication shows disabled already-public state");
    check(rows().size()==108,"combined list retains both local and public rows");
    method(bridge().getClass(),"publish",ConfluxWaypointScreenBridge.Row.class).invoke(bridge(),target);
    check(!shared.isCreatePending(local),"already-public location sends no second request");
    net.minecraft.client.Screenshot.grab(mc,false);next(mc,91);
   }
   case 91 -> {
    if(mc.level.getGameTime()-since<8)return;
    check(ConfluxMapClient.get().sharedWaypoints().list().size()==8,"duplicate attempts produce no additional shared entry");
    Configs.CONFLUX_MAP_QUICK_PUBLISH.setBooleanValue(false);ui.tick();
    hover(mc,rows().stream().filter(r->r.id().equals(personalId)).findFirst().orElseThrow());
    check(actions().size()==7&&store.list().size()==100,"disabling hides only the publishing control");
    check(ConfluxMapClient.get().sharedWaypoints().list().size()==8,"disabling preserves published copies");
    Configs.CONFLUX_MAP_QUICK_PUBLISH.setBooleanValue(true);new Configs().save();Configs.CONFLUX_MAP_QUICK_PUBLISH.setBooleanValue(false);new Configs().load();
    check(Configs.CONFLUX_MAP_QUICK_PUBLISH.getBooleanValue(),"publish child option persists");
    mc.player.connection.sendCommand("confluxmap waypoints disable");next(mc,92);
   }
   case 92 -> {
    if(ConfluxMapClient.get().sharedWaypoints().canCreate())return;
    ui.tick();var target=rows().stream().filter(r->r.id().equals(ids.get(1))).findFirst().orElseThrow();hover(mc,target);
    check(!action("halfmasa.conflux_map.publish").active,"server-disabled publishing is unavailable in popup");
    check(!((ConfluxWaypointScreenBridge.Access)method(bridge().getClass(),"publishAccess",ConfluxWaypointScreenBridge.Row.class).invoke(bridge(),target)).explanation().getString().isBlank(),"disabled publishing explains why");
    check(store.list().size()==100,"server rejection never removes local waypoints");
    mc.player.connection.sendCommand("confluxmap waypoints enable");next(mc,93);
   }
   case 93 -> {
    if(!ConfluxMapClient.get().sharedWaypoints().canCreate())return;
    ui.resize(240,220);rebuild();hover(mc,rows().stream().filter(r->r.local()!=null).findFirst().orElseThrow());
    var panel=(WaypointGridLayout.Cell)field(ui.getClass(),"halfmasa$panel").get(ui);
    check(actions().stream().allMatch(a->a.getX()>=panel.x()&&a.getX()+a.getWidth()<=panel.x()+panel.width()),"narrow popup wraps every action inside panel");
    var checks=(List<AbstractWidget>)field(ui.getClass(),"halfmasa$checks").get(ui);
    check(checks.stream().allMatch(a->a.getX()>=0&&a.getX()+a.getWidth()<=ui.width),"narrow checkboxes stay within viewport");
    net.minecraft.client.Screenshot.grab(mc,false);next(mc,94);
   }
   case 94 -> {
    if(mc.level.getGameTime()-since<5)return;
    ui.resize(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());rebuild();
    Configs.CONFLUX_MAP_QUICK_PUBLISH.resetToDefault();new Configs().save();
    check(!field(ui.getClass(),"halfmasa$failed").getBoolean(ui),"optional adapter remains healthy");
    System.out.println("CONFLUX_GRID_WORLD_PASS checks="+checks);stage=100;mc.stop();
   }
  }
''' + original[end:]
runner.SOURCE = runner.SOURCE.replace('pure();', '')
if __name__ == '__main__':
    raise SystemExit(runner.main())
