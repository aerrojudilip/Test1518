--
-- Makes the specified openpages user a superuser. OP5.5 and later
--
-- Positional input parameters:
--   1 - not used
--   2 - not used
--   3 - user name
--

set linesize 120
set serveroutput on

declare v_actorid actors.actorid%type;

begin
  select actorid into v_actorid from actorinfo where name='OPSystem';
  OP_SESSION_MGR.SET_ACTOR_ID_PRIVATE(v_actorid);
end;
/

declare
  type group_list_t is varray(100) of actorinfo.name%type;
  type grp_info_list_t is table of actorinfo%rowtype index by pls_integer;
  --
  lsc_actor_name   constant actorinfo.name%type := '&&3';
  lsc_module_name  constant op_globals.db_objname_short_t := 'PLSQL Block.Make superuser';
  lbc_do_commit    constant boolean       := true;
  --
  lv_actorinfo_rw   actorinfo%rowtype;
  lv_sys_info_rw    actorinfo%rowtype;
  lv_group_info_rw  actorinfo%rowtype;
  lv_actor_rw       actors%rowtype;
  lv_gm_rw          groupmemberships%rowtype;
  lv_group_list     group_list_t;
  lv_grp_info_list  grp_info_list_t;
  lv_grp_cnt        pls_integer := lv_group_list.count();
  --
  lv_divider        op_globals.plsql_max_string_t%type := lpad('-', 120, '-');
  lv_modified       boolean := false;
begin
  lv_group_list(1) := op_actor_mgr.gc_system_users_group;
  lv_group_list(2) := op_actor_mgr.gc_everyone_group;
  lv_group_list(3) := 'OpenPagesApplicationUsers';
  lv_group_list(4) := 'OPAdministrators';
  lv_group_list(5) := 'OPTasksUsers';
  lv_group_list(6) := 'WorkflowAdministrators';
  lv_group_list(7) := 'WorkflowJobOwners';
  lv_group_list(8) := 'WorkflowHierarchicalJobOwners';
  lv_grp_cnt       := lv_group_list.count;

  op_actor_mgr.get_actorinfo_by_actor_name
  (
   p_actor_name   => op_actor_mgr.gc_system_user,
   p_actorinfo_rw => lv_sys_info_rw
  );
  --
  op_actor_mgr.get_actorinfo_by_actor_name
  (
   p_actor_name   => lsc_actor_name,
   p_actorinfo_rw => lv_actorinfo_rw
  );
  --
  op_actor_mgr.get_actor
  (
   p_actor_id => lv_actorinfo_rw.actorid,
   p_actor_rw => lv_actor_rw
  );
  --
  if lv_actor_rw.type != op_actor_mgr.at_user
  then
     op_exception_mgr.push_parameter_varchar(lsc_module_name, lsc_actor_name);
     op_exception_mgr.throw_error(op_exception_mgr.rc_actor_not_user);
  end if;
  --
  for i in 1..lv_grp_cnt
  loop
    lv_group_info_rw := NULL;
    --
    op_actor_mgr.get_actorinfo_by_actor_name
    (
     p_actor_name   => lv_group_list(i),
     p_actorinfo_rw => lv_group_info_rw
    );
    --
    lv_grp_info_list(i) := lv_group_info_rw;
  end loop;
  --
  if lv_actorinfo_rw.adminlevel < op_actor_mgr.al_super_user
  then
     update actorinfo
     set    adminlevel = op_actor_mgr.al_super_user
     where  actorid    = lv_actor_rw.actorid;
     --
     lv_modified := sql%rowcount > 0;
  end if;
  --
  for i in 1..lv_grp_cnt
  loop
    lv_gm_rw := NULL;
    --
    op_actor_mgr.get_group_membership
    (
     p_group_id            => lv_grp_info_list(i).actorid,
     p_member_id           => lv_actor_rw.actorid,
     p_group_membership_rw => lv_gm_rw,
     p_raise_not_found     => op_globals.nc_false
    );
    --
    if lv_gm_rw.groupmembershipid is NULL
    then
       op_actor_mgr.add_group_membership
       (
        p_invoker_id           => lv_sys_info_rw.actorid,
        p_group_membership_id  => lv_gm_rw.groupmembershipid,
        p_group_id             => lv_grp_info_list(i).actorid,
        p_member_id            => lv_actor_rw.actorid
       );
       --
       lv_modified := true;
    end if;
  end loop;
  --
  if lv_modified
  then
     if lbc_do_commit = true
     then
        commit;
     end if;
     --
     op_utilities.print_line(lv_divider);
     op_utilities.print_line('Successfully made user ' || op_utilities.enclose_string(lsc_actor_name) || ' a super user');
     op_utilities.print_line(lv_divider);
     --
     op_utilities.print_line
     (
      'Execution completed successfully. Data changes ' ||
       case when lbc_do_commit = true
            then 'were committed'
            else 'are pending. Please, don''t forget to COMMIT or ROLLBACK'
       end
     );
     --
     op_utilities.print_line(lv_divider);
  else
     op_utilities.print_line(lv_divider);
     op_utilities.print_line('User ' || op_utilities.enclose_string(lsc_actor_name) || ' is already a super user');
     op_utilities.print_line(lv_divider);
  end if;
  --
end;
/

exit;

