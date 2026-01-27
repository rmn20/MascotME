# mascotme.ini
Various performance and compatibility hacks can be enabled by creating mascotme.ini file in jar archive root.  
See [mascotme.ini](mascotme.ini) if you need an example initialization file.  
You also can disable or enable various hacks by accessing MascotME class fields thru KEmulator's memory view.  

## Debug options:  
### showFPS (disabled by default, 0 or 1)
FPS counter.
  
## Frame buffer related hacks:  
### fbClearColor (black by default, specify color in RRGGBB format)  
Compatibility hack, does not affect performance.  
Color used to clear framebuffer when Graphics3D is bind.  
Can be used to reduce graphical artifacts in games that draw semitransparent geometry over 2D graphics.  
-1 can be used to specify to clear framebuffer with last 2D color used on screen.  
  
### halfResRender (disabled by default, 0 or 1)  
Performance hack, high performance impact.  
Reduces framebuffer resolution.  
  
### no2DInbetween (enabled by default, 0 or 1)  
Performance hack, high performance impact.  
Disables support of 2D graphics inbetween 3D geometry.  
Framebuffer will be drawn on screen only when Graphics3D is released (by API design 3D graphics should be drawn on each flush).  
  
### overwrite2D (disabled by default, 0 or 1)  
Performance hack, low performance impact.  
Overwrites existing 2D screen content by disabling alpha blending when framebuffer is drawn on the screen for the first time.  
  
### doNotClear (disabled by default, 0 or 1)
Performance hack, medium performance impact.  
Disables framebuffer clearing, useful when game fully overwrites framebuffer with geometry.  
Please use with overwrite2D for bigger performance win.  
  
### useArrayCopyClear (disabled by default, 0 or 1)  
Performance hack, medium performance impact.  
Enables alternative method to clear framebuffer using System.arraycopy.  
Can be faster on Series 40 cellphones.  
  
## Clipping related hacks:  
### noNearClipping (disabled by default, 0 or 1)  
Performance hack, medium performance impact.  
Disables clipping of polygons intersecting camera's near plane.  
Can lead to high polygon warping near camera.  
### noFarClipping (disabled by default, 0 or 1)  
Performance hack, medium performance impact.  
Disables far plane polygon clipping.  
Can lead to polygons abruptly disappearing beyound maximum view distance.  
### noToonSplitting (disabled by default, 0 or 1)  
Performance hack, medium performance impact.  
Disables toon shading polygon splitting.  
Can lead to reduced toon shading quality.  
  
## Performance hacks related to various rasterization features:  
Performance hacks, high performance impact.  
### noLighting (disabled by default, 0 or 1)  
Disables vertex lighting. Also disables environment mapping due to technical reasons.  
### noEnvMapping (disabled by default, 0 or 1)  
Disables environment mapping.  
### noBlending (disabled by default, 0 or 1)  
Hides polygons and sprites with blending enabled.  
