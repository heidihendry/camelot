Advanced menu
-------------

Occasionally in Camelot you may find "Advanced" buttons, like this one
in the Surveys menu:

.. figure:: screenshot/advanced-button.png
   :alt: 

You might want an Advanced menu should you find an unusual scenario
arise, and you find yourself needing to bypass the usual workflows. For
example, when attending to a trap station, it is found that the cameras
between it and a nearby trap station had been mixed up. As there are
many unusual scenarios just like this, Camelot caters for them with a
series screens which give fine-grained control over the data.

.. figure:: screenshot/advanced-menu.png
   :alt: 

An advanced menu screen is specific to one type of data. In the above,
the screen is specific to Trap Stations. All advanced menus follow the
same pattern:

1. On the left is the Sidebar. The sidebar shows you a list of entries.
   This list contains everything Camelot knows about that type of data
   (for example, everything Camelot knows about Trap Stations). You can
   click on an item in the sidebar to view it, or use the "**+**" button
   at the top to create a new entry.

2. While viewing an entry, you can see a drop down menu which provides a
   series of *Actions*. This dropdown is very important when working in
   the advanced mode, and nearly all operations in the advanced mode use
   this menu, including navigation! In the above example, this dropdown
   menu it allows for Deleting the selected entry, Editing it, and
   Viewing its associated *Sessions*.

Navigating the advanced menu
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Let's take a step back, and have a look at a map of Camelot's advanced
menus:

::

              Surveys
                 |
                 v
             Survey Sites  <---------- Sites
                 |
                 v
            Trap Stations
                 |
                 v
            Trap Station
              Sessions
                 |
                 v
            Trap Station   <--------- Cameras
           Session Cameras
                 |
                 v
               Media
                / \
               /   \
       Photo <'     `> Sightings <--- Species
     Metadata

Phew! Hopefully you'll agree that Camelot's normal interface sure does
simplify this.

The most important things to take away are these:

-  Survey, Site, Camera and Species are the roots of the data structure.
   To get to these, you must go directly to their Advanced menu.
   Advanced menus for everything else can be, and often are, accessed
   via other Advanced menu. (The exception is that there's is an
   Advanced menu button in the Camera Trap Station details screen, which
   is a handy shortcut!)
-  To go from the Survey level, to the Media or Sightings levels
   requires going through each level in between.

The pattern to go from one level to the next is:

-  select from the Sidebar the entry you want to descend in to, then,
-  select the *View ...* option from the Actions drop-down menu.

Example usage
~~~~~~~~~~~~~

Coming back to the example given earlier on when and where to use the
Advanced menus: we've just realised that the cameras assigned two trap
station (sessions) have been mixed up!

Here's a step-by-step process on how you might go about fixing that:

1.  Organisation -> Cameras -> Create a new camera called "Temporary".
2.  Access the Advanced menu for Problem Trap Station 1: Organisations
    -> Surveys -> MySurvey -> Manage Camera Trap Stations -> MyTrap1 ->
    Details -> Advanced
3.  Select the session from the Sidebar where the cameras are known to
    be wrong
4.  Actions -> View Cameras
5.  Select the camera wrongly assigned, then Actions -> Edit
6.  Set the Camera to "Temporary", as we created earlier
7.  Access the Advanced menu for Problem Trap Station 2: Manage Camera
    Trap Stations -> MyTrap2 -> Details -> Advanced
8.  Now we repeat the process: select the session, Actions -> View
    Cameras, select the wrongly assigned camera, Actions -> Edit
9.  Assign the camera we unassigned from Problem Trap Station 1.
10. And we repeat the process again: select the session, Actions -> View
    Cameras, select the wrongly assigned camera, Actions -> Edit
11. Assign the camera unassigned from Problem Trap Station 2 to complete
    the swap.
12. Finally, Organisation -> Cameras -> Remove "Temporary" to clean up
    the temporary camera.
