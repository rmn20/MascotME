# MascotME integration notes  
Since MascotME works as a library on top of MIDP, several compromises had to be made.  
There's no reliable and fast way to access lcdui.Graphics contents, no reliable way to get lcdui.Graphics resolution.  

When integrated into custom MIDP implementations, these issues can be resolved.  
## Framebuffer resolution  
To properly setup projection parameters MascotME needs lcdui.Graphics resolution.  
Just replace fbWidth and fbHeight assignment in Graphics3D.bind with something like graphics.getWidth() and graphics.getHeight() if possible.  
## Framebuffer access  
To properly render semitransparent geometry on top of 2D lcdui graphics and mix 3D and 2D graphical elements MascotME needs access to lcdui.Graphics framebuffer contents.  
If your MIDP implementation uses int[] framebuffer, just pass your framebuffer to MascotME in Graphics3D.bind.  
If other framebuffer format is used, you can replace Graphics3D.clearFB and Graphics3D.clearFBAlpha calls with framebuffer contents copying. You may also need to disable alpha blending in Graphics3D.drawFB drawRGB call in some cases.
## Hacks  
If you solved some of the aforementioned issues, please remove support of corresponding hacks, such as fbClearColor, fbSizeWorkaround, doNotClear, useArrayCopyClear, or other.  
Be aware that no2DInbetween still can be useful performance hack when framebuffer copying is used.  
Remove or add hacks at your discretion. You may also need to remove mascotme.ini parsing to avoid unexpected issues.