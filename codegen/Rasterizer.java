//MIT License
//Copyright (c) 2026 Roman Lahin

#include "codegen_warning.txt"
package com.mascotcapsule.micro3d.v3;

public class Rasterizer {
	static final int fp = 12, FP = 1 << fp;
	private static final int shadeFpShift = fp - 8;
	
	private Rasterizer() {}
	
	#define BLEND_HALF(col1, col2, res) \
		res = 0xff000000 | ((col1 & col2) + (((col1 ^ col2) >> 1) & 0x7F7F7F));
	
	#define BLEND_ADD(col1, col2, res) \
		{ \
			int tmp = ((((((col1 & col2) << 1) + ((col1 ^ col2) & 0xFEFEFE)) & 0x1010100) >> 8) + 0x7F7F7F) ^ 0x7F7F7F; \
			res = 0xff000000 | (col1 + col2 - tmp) | tmp; \
		}
	
	#define BLEND_SUB(col1, col2, res) \
		{ \
			int tmp = ((((((col2 ^ ~col1) & 0xFEFEFE) + ((col2 & ~col1) << 1)) >> 8) & 0x10101) + 0x7F7F7F) ^ 0x7F7F7F; \
			res = 0xff000000 | ((col1 | tmp) - (tmp | col2)); \
		}

	final static int blendPixel(int src, int dst, int mode) {
		switch (mode) {
			default:
				BLEND_HALF(dst, src, src)
				return src;
			case 2: {
				BLEND_ADD(dst, src, src)
				return src;
			}
			case 3: {
				BLEND_SUB(dst, src, src)
				return src;
			}
		}
	}
	
	static void drawLine(
			int[] frameBuffer, int fbWidth,
			int clipX1, int clipY1, int clipX2, int clipY2,
			int x0, int y0, int x1, int y1, 
			int color, int blendMode
	){
		int dx = x1 - x0; if (dx < 0) dx = -dx;
		int dy = y1 - y0; if (dy < 0) dy = -dy;
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;

		while (true) {
			if (x0 >= clipX1 && x0 < clipX2 && y0 >= clipY1 && y0 < clipY2) {
				int tmpCol = color;
				int fbPos = y0 * fbWidth + x0;
				
				if (blendMode != 0) tmpCol = blendPixel(tmpCol, frameBuffer[fbPos], blendMode);
				
				frameBuffer[fbPos] = tmpCol;
			}
			
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 > -dy) { err -= dy; x0 += sx; }
			if (e2 < dx) { err += dx; y0 += sy; }
		}
	}
	
	//Possible defines: 
	// TEX (textured triangle) 
	// LIGHT (smooth lighting)
	// ENVMAP (environment mapping)
	
	//Textured triangles
	#define TEX
	#undef LIGHT
	#undef ENVMAP
	#undef FUNC_NAME
	#define FUNC_NAME fillTriangleAffineT
	#include "TriangleRasterizerBody.java"
	
	#define TEX
	#define LIGHT
	#undef ENVMAP
	#undef FUNC_NAME
	#define FUNC_NAME fillTriangleAffineTL
	#include "TriangleRasterizerBody.java"
	
	#define TEX
	#undef LIGHT
	#define ENVMAP
	#undef FUNC_NAME
	#define FUNC_NAME fillTriangleAffineTE
	#include "TriangleRasterizerBody.java"
	
	#define TEX
	#define LIGHT
	#define ENVMAP
	#undef FUNC_NAME
	#define FUNC_NAME fillTriangleAffineTLE
	#include "TriangleRasterizerBody.java"
	
	//Color triangles
	#undef TEX
	#undef LIGHT
	#undef ENVMAP
	#undef FUNC_NAME
	#define FUNC_NAME fillTriangleAffineC
	#include "TriangleRasterizerBody.java"
	
	#undef TEX
	#define LIGHT
	#undef ENVMAP
	#undef FUNC_NAME
	#define FUNC_NAME fillTriangleAffineCL
	#include "TriangleRasterizerBody.java"
	
	#undef TEX
	#undef LIGHT
	#define ENVMAP
	#undef FUNC_NAME
	#define FUNC_NAME fillTriangleAffineCE
	#include "TriangleRasterizerBody.java"
	
	#undef TEX
	#define LIGHT
	#define ENVMAP
	#undef FUNC_NAME
	#define FUNC_NAME fillTriangleAffineCLE
	#include "TriangleRasterizerBody.java"
}