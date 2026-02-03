#undef IF_TEX
#undef IFN_TEX
#ifdef TEX
	#define IF_TEX(x) x
	#define IFN_TEX(x)
#else
	#define IF_TEX(x)
	#define IFN_TEX(x) x
#endif

#undef IF_LIGHT
#undef IFN_LIGHT
#ifdef LIGHT
	#define IF_LIGHT(x) x
	#define IFN_LIGHT(x)
#else
	#define IF_LIGHT(x)
	#define IFN_LIGHT(x) x
#endif

#undef IF_ENVMAP
#undef IFN_ENVMAP
#ifdef ENVMAP
	#define IF_ENVMAP(x) x
	#define IFN_ENVMAP(x)
#else
	#define IF_ENVMAP(x)
	#define IFN_ENVMAP(x) x
#endif

#define CONC(x,y) x ## y
#define FN_NAME_COMBINED(x,y) CONC(x,y)

final static void  FUNC_NAME
(
	int[] frameBuffer, int fbWidth, int fbHeight,
	int clipX1, int clipY1, int clipX2, int clipY2,
	int ax, int ay, 
	int bx, int by, 
	int cx, int cy,
	
	#ifdef TEX
		int au, int av, int bu, int bv, int cu, int cv,
		Texture tex, boolean useColorKey,
	#else
		int polyColor,
	#endif
	
	#ifdef LIGHT
		int as, int bs, int cs,
	#else
		int shade,
	#endif
	
	#ifdef ENVMAP
		int aeu, int aev, int beu, int bev, int ceu, int cev,
		Texture envMap,
	#endif
	
	int blendMode
) {
	//Sorting vertices
	if (ay > by) {
		int t = ax; ax = bx; bx = t; t = ay; ay = by; by = t;
		IF_TEX(t = au; au = bu; bu = t; t = av; av = bv; bv = t;)
		IF_LIGHT(t = as; as = bs; bs = t;)
		IF_ENVMAP(t = aeu; aeu = beu; beu = t; t = aev; aev = bev; bev = t;)
	}
	if (by > cy) {
		int t = bx; bx = cx; cx = t; t = by; by = cy; cy = t;
		IF_TEX(t = bu; bu = cu; cu = t; t = bv; bv = cv; cv = t;)
		IF_LIGHT(t = bs; bs = cs; cs = t;)
		IF_ENVMAP(t = beu; beu = ceu; ceu = t; t = bev; bev = cev; cev = t;)
	}
	if (ay > by) {
		int t = ax; ax = bx; bx = t; t = ay; ay = by; by = t;
		IF_TEX(t = au; au = bu; bu = t; t = av; av = bv; bv = t;)
		IF_LIGHT(t = as; as = bs; bs = t;)
		IF_ENVMAP(t = aeu; aeu = beu; beu = t; t = aev; aev = bev; bev = t;)
	}
	if (cy == ay) return;
	if (cy <= clipY1 || ay >= clipY2) return;
	
	#ifdef TEX
		#ifndef LIGHT
			int shadeOffset = shade << 8;
		#endif
	#else
		#ifdef LIGHT
			final int colorRB = polyColor & 0x00ff00ff;
			final int colorG =  polyColor & 0x0000ff00;
		#else
			int colorRGB = 0xff000000;
			colorRGB |= (((polyColor & 0xff0000) * (shade + 1)) >> 5) & 0xff0000;
			colorRGB |= (((polyColor & 0x00ff00) * (shade + 1)) >> 5) & 0x00ff00;
			colorRGB |= (((polyColor & 0x0000ff) * (shade + 1)) >> 5) & 0x0000ff;
		#endif
	#endif

	int tempI = cy - ay;
	final int dx_start = ((cx - ax) << fp) / tempI;
	IF_TEX(final int du_start = ((cu - au) << fp) / tempI;)
	IF_TEX(final int dv_start = ((cv - av) << fp) / tempI;)
	IF_LIGHT(final int ds_start = ((cs - as) << fp) / tempI;)
	IF_ENVMAP(final int deu_start = ((ceu - aeu) << fp) / tempI;)
	IF_ENVMAP(final int dev_start = ((cev - aev) << fp) / tempI;)

	int dx_end = 0;
	IF_TEX(int du_end = 0; int dv_end = 0;)
	IF_LIGHT(int ds_end = 0;)
	IF_ENVMAP(int deu_end = 0; int dev_end = 0;)
	if(by != ay) {
		tempI = by - ay;
		dx_end = ((bx - ax) << fp) / tempI;
		IF_TEX(du_end = ((bu - au) << fp) / tempI;)
		IF_TEX(dv_end = ((bv - av) << fp) / tempI;)
		IF_LIGHT(ds_end = ((bs - as) << fp) / tempI;)
		IF_ENVMAP(deu_end = ((beu - aeu) << fp) / tempI;)
		IF_ENVMAP(dev_end = ((bev - aev) << fp) / tempI;)
	}
	
	int x_start, x_end;

	#if defined(TEX) || defined(LIGHT) || defined(ENVMAP)
		//Calculate scanline derivatives
		tempI = by - ay;
		x_start = (ax << fp) + dx_start * tempI;
		IF_TEX(int u_start = (au << fp) + du_start * tempI;)
		IF_TEX(int v_start = (av << fp) + dv_start * tempI;)
		IF_LIGHT(int s_start = (as << fp) + ds_start * tempI;)
		IF_ENVMAP(int eu_start = (aeu << fp) + deu_start * tempI;)
		IF_ENVMAP(int ev_start = (aev << fp) + dev_start * tempI;)

		x_end = bx << fp;
		IF_TEX(int u_end = bu << fp;)
		IF_TEX(int v_end = bv << fp;)
		IF_LIGHT(int s_end = bs << fp;)
		IF_ENVMAP(int eu_end = beu << fp;)
		IF_ENVMAP(int ev_end = bev << fp;)

		tempI = (x_start - x_end) >> fp;
		//Symmetric ceil
		if (tempI < 0) tempI--;
		else tempI++;
		IF_TEX(final int du = (u_start - u_end) / tempI;)
		IF_TEX(final int dv = (v_start - v_end) / tempI;)
		IF_LIGHT(final int ds = (s_start - s_end) / tempI;)
		IF_ENVMAP(final int deu = (eu_start - eu_end) / tempI;)
		IF_ENVMAP(final int dev = (ev_start - ev_end) / tempI;)
	#endif

	x_end = x_start = ax << fp;
	IF_TEX(u_end = u_start = au << fp;)
	IF_TEX(v_end = v_start = av << fp;)
	IF_LIGHT(s_end = s_start = as << fp;)
	IF_ENVMAP(eu_end = eu_start = aeu << fp;)
	IF_ENVMAP(ev_end = ev_start = aev << fp;)
	
	int y_start = ay;
	int y_end = by;
	
	for (int i = 0; i < 2; i++) {
		if(y_end > clipY1) {
			int x1, x2;
			IF_TEX(int u; int v;)
			IF_LIGHT(int s;)
			IF_ENVMAP(int eu; int ev;)
	
			int dx_left, dx_right;
			IF_TEX(int du_left; int dv_left;)
			IF_LIGHT(int ds_left;)
			IF_ENVMAP(int deu_left; int dev_left;)
	
			if(y_start < clipY1) {
				tempI = y_start - clipY1;
				
				x_start -= dx_start * tempI;
				IF_TEX(u_start -= du_start * tempI;)
				IF_TEX(v_start -= dv_start * tempI;)
				IF_LIGHT(s_start -= ds_start * tempI;)
				IF_ENVMAP(eu_start -= deu_start * tempI;)
				IF_ENVMAP(ev_start -= dev_start * tempI;)

				x_end -= dx_end * tempI;
				IF_TEX(u_end -= du_end * tempI;)
				IF_TEX(v_end -= dv_end * tempI;)
				IF_LIGHT(s_end -= ds_end * tempI;)
				IF_ENVMAP(eu_end -= deu_end * tempI;)
				IF_ENVMAP(ev_end -= dev_end * tempI;)
				
				y_start = clipY1;
			}
			
			if(x_start < x_end || (x_start == x_end && dx_start < dx_end)) {
				x1 = x_start;
				dx_left = dx_start;
				IF_TEX(u = u_start;)
				IF_TEX(v = v_start;)
				IF_TEX(du_left = du_start;)
				IF_TEX(dv_left = dv_start;)
				IF_LIGHT(s = s_start;)
				IF_LIGHT(ds_left = ds_start;)
				IF_ENVMAP(eu = eu_start;)
				IF_ENVMAP(ev = ev_start;)
				IF_ENVMAP(deu_left = deu_start;)
				IF_ENVMAP(dev_left = dev_start;)
				x2 = x_end;
				dx_right = dx_end;
			} else {
				x1 = x_end;
				dx_left = dx_end;
				IF_TEX(u = u_end;)
				IF_TEX(v = v_end;)
				IF_TEX(du_left = du_end;)
				IF_TEX(dv_left = dv_end;)
				IF_LIGHT(s = s_end;)
				IF_LIGHT(ds_left = ds_end;)
				IF_ENVMAP(eu = eu_end;)
				IF_ENVMAP(ev = ev_end;)
				IF_ENVMAP(deu_left = deu_end;)
				IF_ENVMAP(dev_left = dev_end;)
				x2 = x_start;
				dx_right = dx_start;
			}
			
			final int y_end_draw = y_end < clipY2 ? y_end : clipY2;
					
				switch (blendMode) {
					default:
						FN_NAME_COMBINED(FUNC_NAME, _replace) (
							frameBuffer, fbWidth, fbHeight,
							clipX1, clipX2,
							y_start, y_end_draw,
							#ifdef TEX
								u, du_left, du, v, dv_left, dv,
								tex, useColorKey,
								#ifndef LIGHT
									shadeOffset,
								#endif
							#else
								#ifdef LIGHT
									colorRB, colorG,
								#else
									colorRGB,
								#endif
							#endif
							
							#ifdef LIGHT
								s, ds_left, ds,
							#endif
							
							#ifdef ENVMAP
								eu, deu_left, deu, ev, dev_left, dev,
								envMap,
							#endif
								x1, dx_left, x2, dx_right
						);
						break;
					case 1:
						FN_NAME_COMBINED(FUNC_NAME, _half) (
							frameBuffer, fbWidth, fbHeight,
							clipX1, clipX2,
							y_start, y_end_draw,
							#ifdef TEX
								u, du_left, du, v, dv_left, dv,
								tex, useColorKey,
								#ifndef LIGHT
									shadeOffset,
								#endif
							#else
								#ifdef LIGHT
									colorRB, colorG,
								#else
									colorRGB,
								#endif
							#endif
							
							#ifdef LIGHT
								s, ds_left, ds,
							#endif
							
							#ifdef ENVMAP
								eu, deu_left, deu, ev, dev_left, dev,
								envMap,
							#endif
								x1, dx_left, x2, dx_right
						);
						break;
					case 2:
						FN_NAME_COMBINED(FUNC_NAME, _add) (
							frameBuffer, fbWidth, fbHeight,
							clipX1, clipX2,
							y_start, y_end_draw,
							#ifdef TEX
								u, du_left, du, v, dv_left, dv,
								tex, useColorKey,
								#ifndef LIGHT
									shadeOffset,
								#endif
							#else
								#ifdef LIGHT
									colorRB, colorG,
								#else
									colorRGB,
								#endif
							#endif
							
							#ifdef LIGHT
								s, ds_left, ds,
							#endif
							
							#ifdef ENVMAP
								eu, deu_left, deu, ev, dev_left, dev,
								envMap,
							#endif
								x1, dx_left, x2, dx_right
						);
						break;
					case 3:
						FN_NAME_COMBINED(FUNC_NAME, _sub) (
							frameBuffer, fbWidth, fbHeight,
							clipX1, clipX2,
							y_start, y_end_draw,
							#ifdef TEX
								u, du_left, du, v, dv_left, dv,
								tex, useColorKey,
								#ifndef LIGHT
									shadeOffset,
								#endif
							#else
								#ifdef LIGHT
									colorRB, colorG,
								#else
									colorRGB,
								#endif
							#endif
							
							#ifdef LIGHT
								s, ds_left, ds,
							#endif
							
							#ifdef ENVMAP
								eu, deu_left, deu, ev, dev_left, dev,
								envMap,
							#endif
								x1, dx_left, x2, dx_right
						);
						break;
				}
		}
		
		if(i == 1) return;
		if(by >= clipY2) return;
		if(cy == by) return;
		tempI = by - ay;
		x_start = (ax << fp) + dx_start * tempI;
		IF_TEX(u_start = (au << fp) + du_start * tempI;)
		IF_TEX(v_start = (av << fp) + dv_start * tempI;)
		IF_LIGHT(s_start = (as << fp) + ds_start * tempI;)
		IF_ENVMAP(eu_start = (aeu << fp) + deu_start * tempI;)
		IF_ENVMAP(ev_start = (aev << fp) + dev_start * tempI;)

		x_end = bx << fp;
		IF_TEX(u_end = bu << fp;)
		IF_TEX(v_end = bv << fp;)
		IF_LIGHT(s_end = bs << fp;)
		IF_ENVMAP(eu_end = beu << fp;)
		IF_ENVMAP(ev_end = bev << fp;)

		tempI = cy - by;
		dx_end = ((cx - bx) << fp) / tempI;
		IF_TEX(du_end = ((cu - bu) << fp) / tempI;)
		IF_TEX(dv_end = ((cv - bv) << fp) / tempI;)
		IF_LIGHT(ds_end = ((cs - bs) << fp) / tempI;)
		IF_ENVMAP(deu_end = ((ceu - beu) << fp) / tempI;)
		IF_ENVMAP(dev_end = ((cev - bev) << fp) / tempI;)
		
		y_start = by;
		y_end = cy;
	}
}

#undef BLEND_MODE
#define BLEND_MODE 0
private final static void FN_NAME_COMBINED(FUNC_NAME, _replace)
#include "ScanLineBody.java"

#undef BLEND_MODE
#define BLEND_MODE 1
private final static void FN_NAME_COMBINED(FUNC_NAME, _half)
#include "ScanLineBody.java"

#undef BLEND_MODE
#define BLEND_MODE 2
private final static void FN_NAME_COMBINED(FUNC_NAME, _add)
#include "ScanLineBody.java"

#undef BLEND_MODE
#define BLEND_MODE 3
private final static void FN_NAME_COMBINED(FUNC_NAME, _sub)
#include "ScanLineBody.java"