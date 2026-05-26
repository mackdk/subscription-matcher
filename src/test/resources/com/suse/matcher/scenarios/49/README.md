Scenario 49 - Adding part numbers 874-008397 and 874-008517-C
==============================================================

This scenario tests the fix for bsc#1264256 (https://bugzilla.suse.com/show_bug.cgi?id=1264256)

There are 2 other new SKU showed up missing in subscription matching for one of our customers:

Unsupported part number detected 874-008397 (SUSE Linux Enterprise Server, x86-64, 1-2 Sockets with Unlimited Virtual Machines, Standard Subscription, 1 Year)
Unsupported part number detected 874-008517-C (SUSE Linux Enterprise Server with SUSE Multi-Linux Manager Lifecycle Management+, X86-64, 1-2 Sockets or 1-2 Virtual Machines, Standard Subscription, 1 Year)

The scenario files were originally taken from the user support config,
and then simplified to check that the actual match works
  
Result
------

Part numbers 874-008397 and 874-008517-C are now correctly detected
