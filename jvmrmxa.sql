-- Remove some portion of the dictionary objects created by initxa.sql

-- This script must be run as a subscript of a script which sets the
-- variable jvmrmaction.
-- Possible values are
--  FULL_REMOVAL:        remove all java related objects
--  GRADE_REMOVAL:       remove java related objects for general up/downgrade
--  DOWNGRADE_x.y.z_TO_a.b.c: remove or massage system objects as appropriate
--                       when downgrading to release a.b.c
--  NONE:                do nothing

print jvmrmaction

begin if :jvmrmaction = 'FULL_REMOVAL' or
         (:jvmrmaction = 'GRADE_REMOVAL' and
          initjvmaux.startstep('JVMRMXA')) then

initjvmaux.drp('drop package JAVA_XA');
initjvmaux.drp('drop PUBLIC SYNONYM JAVA_XA');

if :jvmrmaction = 'GRADE_REMOVAL' then initjvmaux.endstep; end if;

end if;end;
/
