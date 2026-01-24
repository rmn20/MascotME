# MascotME  
MascotME is a MascotCapsule v3 implementation written in pure J2ME with no dependencies except for MIDP 2.0.  
Based on MCv3 implementation from [JL-Mod](https://github.com/woesss/JL-Mod/).  
  
Thanks to the lack of external dependencies MascotME can be easily intergrated into existing J2ME emulators or can be used to play MascotCapsule games on (powerful enough) cellphones without MCv3 support by simply dragging MascotME's "com" folder into your game JAR file.  
  
Source code will be released (hopefully) soon.  
# Setup  
Copy MascotME's "com" folder into your game JAR file.  
Various performance and compatibility hacks can be enabled by creating "mascotme.ini" file in JAR archive root.  
See [INI-CONFIG.md](INI-CONFIG.md) for mode details.
# Issues  
Due to inability to access MIDP Graphics framebuffer some of the semitransparent geometry [can render with artifacts](screenshots/RobotAlliance_blending.png).  
Some of the compatibility hacks (such as fbClearColor) may solve this issue, or at least make it less visible.  
# Special thanks  
Soon...
