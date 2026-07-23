Scenario 50 - Adding part number 874-008433-C
==============================================================

This scenario tests the fix for bsc#1271075 (https://bugzilla.suse.com/show_bug.cgi?id=1271075)

There is another new SKU showed up missing in subscription matching for one of our customers:

Unsupported part number detected - 874-008433-C
SUSE Linux Enterprise Server for SAP Applications, x86-64, 1-2 Sockets or 1-2 Virtual Machines, Priority Subscription, 1 Year 

The scenario files were originally taken from the user support config,
and then simplified to check that the actual match works.

It also verifies that 874-008433-C coexists with the closely related part number
874-006905 (same product family) without either of them producing an "unknown_part_number" message. 

Result
------

Both 874-008433-C and 874-006905 are correctly detected. Neither triggers "unknown_part_number".
