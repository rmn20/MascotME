# MascotME  
MascotME is a MascotCapsule v3 implementation written in pure J2ME with no dependencies except for MIDP 2.0.  
Based on MCv3 implementation from [JL-Mod](https://github.com/woesss/JL-Mod/).  
  
Thanks to the lack of external dependencies MascotME can be easily integrated into existing J2ME emulators ([see integration notes for details](/INTEGRATION-NOTES.md)) or can be used to play MascotCapsule games on (powerful enough) cellphones without MCv3 support by simply dragging MascotME's "com" folder into your game JAR file.  
# Screenshots  
![Screenshot of a Coast Racer](/screenshots/CoastRacer.png) ![Screenshot of a Bomberman 3D](/screenshots/Bomberman3D.png) ![Screenshot of a Blades and Magic](/screenshots/BladesAndMagic.png)  
# Setup  
Copy MascotME's "com" folder into your game JAR file.  
Various performance and compatibility hacks can be enabled by creating "mascotme.ini" file in JAR archive root.  
See [INI-CONFIG.md](INI-CONFIG.md) for more detail.
# Known issues  
Due to inability to access MIDP Graphics framebuffer some of the semitransparent geometry [can render with artifacts](/screenshots/RobotAlliance_blending.png).  
Some of the compatibility hacks (such as fbClearColor) may solve this issue, or at least make it less visible.  
Since there's also no proper way to get framebuffer resolution, some games that use viewport clipping can look stretched or render at incorrect screen coordinates. fbSizeWorkaround can solve this issue in some cases.  
# Special thanks to...  
[woesss](https://github.com/woesss/) for MascotCapsule v3 implementation in [JL-Mod](https://github.com/woesss/JL-Mod/)  
[klaxons1](https://github.com/klaxons1/) for continuous testing and moral support  
[shinovon](https://github.com/shinovon/) for additional help  
[minexew](https://github.com/minexew/) for [MascotCapsule Archaeology](https://github.com/j2me-preservation/MascotCapsule/)