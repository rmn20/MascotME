(
	int[] frameBuffer, int fbWidth,
	int clipX1, int clipX2,
	int y_start, int y_end,
	#ifdef TEX
		int u_start, int du_start, int du, int v_start, int dv_start, int dv,
		Texture tex,
		#ifndef FAST_PATH
			boolean useColorKey,
			#ifndef LIGHT
				int shadeOffset,
			#endif
		#endif
	#else
		#ifdef LIGHT
			int colorRB, int colorG,
		#else
			int colorRGB,
		#endif
	#endif
	
	#ifdef LIGHT
		int s_start, int ds_start, int ds,
	#endif
	
	#ifdef ENVMAP
		int eu_start, int deu_start, int deu, int ev_start, int dev_start, int dev,
		Texture envMap,
	#endif
		int x_start, int dx_start, int x_end, int dx_end
) {
	#ifdef TEX
		final byte[] texBitmap = tex.bitmapData;
		final int texWBit = tex.widthBit;
		final int texLenMask = texBitmap.length - 1;
		
		#ifdef FAST_PATH
			final int[] texPal = tex.origPalette;
		#else
			final int[] texPal = tex.palette;
			int colorKeyIdx = useColorKey ? 0 : 256;
			#if BLEND_MODE >= 2
				if (tex.firstColorIsBlack) colorKeyIdx = 0;
			#endif
		#endif
	#endif
	
	#ifdef ENVMAP
		final int[] envTexBitmap = envMap.envmapData;
		final int envTexWBit = envMap.widthBit;
		final int envTexLenMask = envTexBitmap.length - 1;
	#endif
	
	#if defined(LIGHT) && !defined(TEX)
		final int rbMask = 0x00ff00ff << 5;
		final int gMask =  0x0000ff00 << 5;
	#endif
	
	//Corrected rounding to avoid texture seams
	IF_TEX(if (du_start != 0) u_start += du_start > 0 ? 1 : -1;)
	IF_TEX(if (dv_start != 0) v_start += dv_start > 0 ? 1 : -1;)
	IF_LIGHT(if (ds_start != 0) s_start += ds_start > 0 ? 1 : -1;)
	IF_ENVMAP(if (deu_start != 0) eu_start += deu_start > 0 ? 1 : -1;)
	IF_ENVMAP(if (dev_start != 0) ev_start += dev_start > 0 ? 1 : -1;)
		
	y_start *= fbWidth;
	y_end *= fbWidth;
		
	for(; y_start < y_end;
		y_start += fbWidth, 
		
		#ifdef TEX
			u_start += du_start, v_start += dv_start, 
		#endif
		
		#ifdef LIGHT
			s_start += ds_start,
		#endif
		
		#ifdef ENVMAP
			eu_start += deu_start, ev_start += dev_start, 
		#endif
		
		x_start += dx_start, x_end += dx_end
	) {
		int x1 = x_start >> fp;
		int x2 = x_end >> fp;
		
		//Subpixel precision, ceil rounding
		int tempI = FP - (x_start & (FP - 1));
		IF_TEX(int u = u_start + ((du * tempI) >> fp);)
		IF_TEX(int v = v_start + ((dv * tempI) >> fp);)
		IF_LIGHT(int s = s_start + ((ds * tempI) >> fp);)
		IF_ENVMAP(int eu = eu_start + ((deu * tempI) >> fp);)
		IF_ENVMAP(int ev = ev_start + ((dev * tempI) >> fp);)

		if(x1 < clipX1) {
			tempI = x1 - clipX1;
			IF_TEX(u -= du * tempI;)
			IF_TEX(v -= dv * tempI;)
			IF_LIGHT(s -= ds * tempI;)
			IF_ENVMAP(eu -= deu * tempI;)
			IF_ENVMAP(ev -= dev * tempI;)
			x1 = clipX1;
		}

		if(x2 > clipX2) x2 = clipX2;

		x1 += y_start;
		x2 += y_start;
		
		#undef LINE_BODY
		#undef LINE_ENV
		
		#ifdef TEX
			#ifndef FAST_PATH
				#define LINE_BODY \
					int color = texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask]; \
					\
					if (color != colorKeyIdx) { \
						IF_LIGHT( color = texPal[((s >> shadeFpShift) & 0x1f00) | (color & 0xFF)];) \
						IFN_LIGHT(color = texPal[shadeOffset | (color & 0xFF)];)
			#else
				#define LINE_BODY \
					int color = texPal[texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask] & 0xFF];
			#endif
		#else
			#ifdef LIGHT
				#define LINE_BODY \
					int shadeLevel = s >> fp; \
					int color = \
							0xff000000 | \
							((((colorRB * shadeLevel) & rbMask) | \
							((colorG * shadeLevel) & gMask)) >> 5);
			#else
				#define LINE_BODY \
					int color = colorRGB;
			#endif
		#endif
		
		#ifdef ENVMAP
			#define LINE_ENV \
				int envPx = envTexBitmap[(((ev >> fp) << envTexWBit) | (eu >> fp)) & envTexLenMask]; \
				if (envPx != 0) BLEND_ADD(color, envPx, color)
		#else
			#define LINE_ENV
		#endif
		
		#undef FB_WRITE
		#undef FB_WRITE_2
		
		#if BLEND_MODE == 0
			#define FB_WRITE(fb, color) fb = color;
			#define FB_WRITE_2(fb, color, colorBuf) fb = color;
		#elif BLEND_MODE == 1
			#define FB_WRITE(fb, color) \
				int dst = fb; \
				BLEND_HALF(dst, color, fb)
			#define FB_WRITE_2(fb, color, colorBuf) \
				colorBuf = color; \
				int dst = fb; \
				BLEND_HALF(dst, colorBuf, fb)
		#elif BLEND_MODE == 2
			#define FB_WRITE(fb, color) \
				int dst = fb; \
				BLEND_ADD(dst, color, fb)
			#define FB_WRITE_2(fb, color, colorBuf) \
				colorBuf = color; \
				int dst = fb; \
				BLEND_ADD(dst, colorBuf, fb)
		#else
			#define FB_WRITE(fb, color) \
				int dst = fb; \
				BLEND_SUB(dst, color, fb)
			#define FB_WRITE_2(fb, color, colorBuf) \
				colorBuf = color; \
				int dst = fb; \
				BLEND_SUB(dst, colorBuf, fb)
		#endif
		
		#if defined(TEX) && !defined(LIGHT) && !defined(ENVMAP)
			//Slight speedup for most used functions
			
			#ifdef FAST_PATH
				while(x2 - x1 >= 6) {
					FB_WRITE(frameBuffer[x1    ], texPal[texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask] & 0xFF])
					u += du; v += dv;
					FB_WRITE(frameBuffer[x1 + 1], texPal[texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask] & 0xFF])
					u += du; v += dv;
					FB_WRITE(frameBuffer[x1 + 2], texPal[texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask] & 0xFF])
					u += du; v += dv;
					FB_WRITE(frameBuffer[x1 + 3], texPal[texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask] & 0xFF])
					u += du; v += dv;
					FB_WRITE(frameBuffer[x1 + 4], texPal[texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask] & 0xFF])
					u += du; v += dv;
					FB_WRITE(frameBuffer[x1 + 5], texPal[texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask] & 0xFF])
					u += du; v += dv;
					
					x1 += 6;
				}
			#else
				while(x2 - x1 >= 6) {
					int texIdx = texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask];
					u += du; v += dv;
					int texIdx2 = texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask];
					u += du; v += dv;
					int texIdx3 = texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask];
					u += du; v += dv;
					int texIdx4 = texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask];
					u += du; v += dv;
					int texIdx5 = texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask];
					u += du; v += dv;
					int texIdx6 = texBitmap[(((v >> fp) << texWBit) | (u >> fp)) & texLenMask];
					u += du; v += dv;
					
					if(texIdx != colorKeyIdx) {FB_WRITE_2(frameBuffer[x1], texPal[shadeOffset | (texIdx & 0xFF)], texIdx)}
					if(texIdx2 != colorKeyIdx) {FB_WRITE_2(frameBuffer[x1 + 1], texPal[shadeOffset | (texIdx2 & 0xFF)], texIdx2)}
					if(texIdx3 != colorKeyIdx) {FB_WRITE_2(frameBuffer[x1 + 2], texPal[shadeOffset | (texIdx3 & 0xFF)], texIdx3)}
					if(texIdx4 != colorKeyIdx) {FB_WRITE_2(frameBuffer[x1 + 3], texPal[shadeOffset | (texIdx4 & 0xFF)], texIdx4)}
					if(texIdx5 != colorKeyIdx) {FB_WRITE_2(frameBuffer[x1 + 4], texPal[shadeOffset | (texIdx5 & 0xFF)], texIdx5)}
					if(texIdx6 != colorKeyIdx) {FB_WRITE_2(frameBuffer[x1 + 5], texPal[shadeOffset | (texIdx6 & 0xFF)], texIdx6)}
					x1 += 6;
				}
			#endif
		#endif
		
		for(; x1 < x2; 
			#ifdef TEX
				u += du, v += dv, 
			#endif
			#ifdef LIGHT
				s += ds, 
			#endif
			#ifdef ENVMAP
				eu += deu, ev += dev, 
			#endif
			x1++
		) {
			LINE_BODY
			LINE_ENV
			FB_WRITE(frameBuffer[x1], color)
			#if defined(TEX) && !defined(FAST_PATH)
				}
			#endif
		}
	}
}