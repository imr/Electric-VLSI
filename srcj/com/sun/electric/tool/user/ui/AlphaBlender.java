/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AlphaBlender.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.user.ui;

import com.sun.electric.technology.Layer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to perform alpha blending for LayerDrawing.
 */
class AlphaBlender {
    
    private Color background;
    private int[] opaqueData;
    
    /** Creates a new instance of AlphaBlender */
    public AlphaBlender() {
    }
    
    private static final int SCALE_SH = 8;
    private static final int SCALE = 1 << SCALE_SH;
        
    private AlphaBlendGroup[] groups;
    private int[][] layerBits;
//    private int m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, m16, m17, m18, m19, m20, m21, m22, m23, m24, m25, m26, m27, m28, m29, m30, m31;
    
    private int r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15, r16, r17, r18, r19, r20, r21, r22, r23, r24, r25, r26, r27, r28, r29, r30, r31;
    private int g0, g1, g2, g3, g4, g5, g6, g7, g8, g9, g10, g11, g12, g13, g14, g15, g16, g17, g18, g19, g20, g21, g22, g23, g24, g25, g26, g27, g28, g29, g30, g31;
    private int b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15, b16, b17, b18, b19, b20, b21, b22, b23, b24, b25, b26, b27, b28, b29, b30, b31;
    
    void init(int backgroundValue, List<EditWindow.LayerColor> blendingOrder, Map<Layer,int[]> layerBits) {
        background = new Color(backgroundValue);
        ArrayList<int[]> bits = new ArrayList<int[]>();
        ArrayList<EditWindow.LayerColor> colors = new ArrayList<EditWindow.LayerColor>();
        for (EditWindow.LayerColor layerColor: blendingOrder) {
            int[] b = layerBits.get(layerColor.layer);
            if (b == null) continue;
            bits.add(b);
            colors.add(layerColor);
        }
        this.layerBits = bits.toArray(new int[bits.size()][]);
        
        groups = new AlphaBlendGroup[bits.size()/13 + 1];
        int k = 0;
        for (int i = 0; i < groups.length; i++) {
            int l = (bits.size() - k) / (groups.length - i);
            groups[i] = new AlphaBlendGroup(this.layerBits, colors, k, l, i == 0);
            k += l;
        }
    }
    
//    private void unpackBytes(int index) {
//        // unpack pixels
//        int pixel0 = 0, pixel1 = 0, pixel2 = 0, pixel3 = 0, pixel4 = 0, pixel5 = 0, pixel6 = 0, pixel7 = 0;
//        int pixel8 = 0, pixel9 = 0, pixel10 = 0, pixel11 = 0, pixel12 = 0, pixel13 = 0, pixel14 = 0, pixel15 = 0;
//        int pixel16 = 0, pixel17 = 0, pixel18 = 0, pixel19 = 0, pixel20 = 0, pixel21 = 0, pixel22 = 0, pixel23 = 0;
//        int pixel24 = 0, pixel25 = 0, pixel26 = 0, pixel27 = 0, pixel28 = 0, pixel29 = 0, pixel30 = 0, pixel31 = 0;
//        for (int i = 0; i < layerBits.length; i++) {
//            int value = layerBits[i][index];
//            if (value == 0) continue;
//            pixel0 |= (value & 1) << i;
//            pixel1 |= ((value >> 1) & 1) << i;
//            pixel2 |= ((value >> 2) & 1) << i;
//            pixel3 |= ((value >> 3) & 1) << i;
//            pixel4 |= ((value >> 4) & 1) << i;
//            pixel5 |= ((value >> 5) & 1) << i;
//            pixel6 |= ((value >> 6) & 1) << i;
//            pixel7 |= ((value >> 7) & 1) << i;
//            pixel8 |= ((value >> 8) & 1) << i;
//            pixel9 |= ((value >> 9) & 1) << i;
//            pixel10 |= ((value >> 10) & 1) << i;
//            pixel11 |= ((value >> 11) & 1) << i;
//            pixel12 |= ((value >> 12) & 1) << i;
//            pixel13 |= ((value >> 13) & 1) << i;
//            pixel14 |= ((value >> 14) & 1) << i;
//            pixel15 |= ((value >> 15) & 1) << i;
//            pixel16 |= ((value >> 16) & 1) << i;
//            pixel17 |= ((value >> 17) & 1) << i;
//            pixel18 |= ((value >> 18) & 1) << i;
//            pixel19 |= ((value >> 19) & 1) << i;
//            pixel20 |= ((value >> 20) & 1) << i;
//            pixel21 |= ((value >> 21) & 1) << i;
//            pixel22 |= ((value >> 22) & 1) << i;
//            pixel23 |= ((value >> 23) & 1) << i;
//            pixel24 |= ((value >> 24) & 1) << i;
//            pixel25 |= ((value >> 25) & 1) << i;
//            pixel26 |= ((value >> 26) & 1) << i;
//            pixel27 |= ((value >> 27) & 1) << i;
//            pixel28 |= ((value >> 28) & 1) << i;
//            pixel29 |= ((value >> 29) & 1) << i;
//            pixel30 |= ((value >> 30) & 1) << i;
//            pixel31 |= ((value >> 31) & 1) << i;
//        }
//        m0 = pixel0;
//        m1 = pixel1;
//        m2 = pixel2;
//        m3 = pixel3;
//        m4 = pixel4;
//        m5 = pixel5;
//        m6 = pixel6;
//        m7 = pixel7;
//        m8 = pixel8;
//        m9 = pixel9;
//        m10 = pixel10;
//        m11 = pixel11;
//        m12 = pixel12;
//        m13 = pixel13;
//        m14 = pixel14;
//        m15 = pixel15;
//        m16 = pixel16;
//        m17 = pixel17;
//        m18 = pixel18;
//        m19 = pixel19;
//        m20 = pixel20;
//        m21 = pixel21;
//        m22 = pixel22;
//        m23 = pixel23;
//        m24 = pixel24;
//        m25 = pixel25;
//        m26 = pixel26;
//        m27 = pixel27;
//        m28 = pixel28;
//        m29 = pixel29;
//        m30 = pixel30;
//        m31 = pixel31;
//    }
    
    void composeLine(int inputOffset, int minX, int maxX, int[] opaqueData, int opaqueOffset) {
        int minInt = minX>>5;
        int maxInt = maxX>>5;
        this.opaqueData = opaqueData;
        if (minInt == maxInt) {
            int mask = (1 << ((maxX&31) + 1)) - (1 << (minX&31));
            composeBits(inputOffset + minInt, mask, opaqueOffset + (minInt<<5));
        } else {
            if ((minX&31) != 0) {
                int headMask = -(1 << (minX&31));
                composeBits(inputOffset + minInt, headMask, opaqueOffset + (minInt<<5));
                minInt++;
            }
            if ((maxX&31) != 31) {
                int tailMask = (1 << ((maxX&31) + 1)) - 1;
                composeBits(inputOffset + maxInt, tailMask, opaqueOffset + (maxInt<<5));
                maxInt--;
            }
        }
        opaqueOffset += minInt<<5;
        for (int index = minInt; index <= maxInt; index++) {
            for (AlphaBlendGroup group: groups)
                group.unpackBytes(inputOffset + index);
            opaqueOffset = storeRGB32(opaqueOffset);
        }
    }
    
    void composeBits(int inputOffset, int mask, int outputOffset) {
        for (AlphaBlendGroup group: groups)
            group.unpackBytes(inputOffset);
        if ((mask & (1 << 0)) != 0)
            storeRGB(outputOffset + 0, r0, g0, b0);
        if ((mask & (1 << 1)) != 0)
            storeRGB(outputOffset + 1, r1, g1, b1);
        if ((mask & (1 << 2)) != 0)
            storeRGB(outputOffset + 2, r2, g2, b2);
        if ((mask & (1 << 3)) != 0)
            storeRGB(outputOffset + 3, r3, g3, b3);
        if ((mask & (1 << 4)) != 0)
            storeRGB(outputOffset + 4, r4, g4, b4);
        if ((mask & (1 << 5)) != 0)
            storeRGB(outputOffset + 5, r5, g5, b5);
        if ((mask & (1 << 6)) != 0)
            storeRGB(outputOffset + 6, r6, g6, b6);
        if ((mask & (1 << 7)) != 0)
            storeRGB(outputOffset + 7, r7, g7, b7);
        if ((mask & (1 << 8)) != 0)
            storeRGB(outputOffset + 8, r8, g8, b8);
        if ((mask & (1 << 9)) != 0)
            storeRGB(outputOffset + 9, r9, g9, b9);
        if ((mask & (1 << 10)) != 0)
            storeRGB(outputOffset + 10, r10, g10, b10);
        if ((mask & (1 << 11)) != 0)
            storeRGB(outputOffset + 11, r11, g11, b11);
        if ((mask & (1 << 12)) != 0)
            storeRGB(outputOffset + 12, r12, g12, b12);
        if ((mask & (1 << 13)) != 0)
            storeRGB(outputOffset + 13, r13, g13, b13);
        if ((mask & (1 << 14)) != 0)
            storeRGB(outputOffset + 14, r14, g14, b14);
        if ((mask & (1 << 15)) != 0)
            storeRGB(outputOffset + 15, r15, g15, b15);
        if ((mask & (1 << 16)) != 0)
            storeRGB(outputOffset + 16, r16, g16, b16);
        if ((mask & (1 << 17)) != 0)
            storeRGB(outputOffset + 17, r17, g17, b17);
        if ((mask & (1 << 18)) != 0)
            storeRGB(outputOffset + 18, r18, g18, b18);
        if ((mask & (1 << 19)) != 0)
            storeRGB(outputOffset + 19, r19, g19, b19);
        if ((mask & (1 << 20)) != 0)
            storeRGB(outputOffset + 20, r20, g20, b20);
        if ((mask & (1 << 21)) != 0)
            storeRGB(outputOffset + 21, r21, g21, b21);
        if ((mask & (1 << 22)) != 0)
            storeRGB(outputOffset + 22, r22, g22, b22);
        if ((mask & (1 << 23)) != 0)
            storeRGB(outputOffset + 23, r23, g23, b23);
        if ((mask & (1 << 24)) != 0)
            storeRGB(outputOffset + 24, r24, g24, b24);
        if ((mask & (1 << 25)) != 0)
            storeRGB(outputOffset + 25, r25, g25, b25);
        if ((mask & (1 << 26)) != 0)
            storeRGB(outputOffset + 26, r26, g26, b26);
        if ((mask & (1 << 27)) != 0)
            storeRGB(outputOffset + 27, r27, g27, b27);
        if ((mask & (1 << 28)) != 0)
            storeRGB(outputOffset + 28, r28, g28, b28);
        if ((mask & (1 << 29)) != 0)
            storeRGB(outputOffset + 29, r29, g29, b29);
        if ((mask & (1 << 30)) != 0)
            storeRGB(outputOffset + 30, r30, g30, b30);
        if ((mask & (1 << 31)) != 0)
            storeRGB(outputOffset + 31, r31, g31, b31);
    }
    
//    private int storeRGB32(int baseIndex) {
//        storeRGB_(baseIndex++, m0);
//        storeRGB_(baseIndex++, m1);
//        storeRGB_(baseIndex++, m2);
//        storeRGB_(baseIndex++, m3);
//        storeRGB_(baseIndex++, m4);
//        storeRGB_(baseIndex++, m5);
//        storeRGB_(baseIndex++, m6);
//        storeRGB_(baseIndex++, m7);
//        storeRGB_(baseIndex++, m8);
//        storeRGB_(baseIndex++, m9);
//        storeRGB_(baseIndex++, m10);
//        storeRGB_(baseIndex++, m11);
//        storeRGB_(baseIndex++, m12);
//        storeRGB_(baseIndex++, m13);
//        storeRGB_(baseIndex++, m14);
//        storeRGB_(baseIndex++, m15);
//        storeRGB_(baseIndex++, m16);
//        storeRGB_(baseIndex++, m17);
//        storeRGB_(baseIndex++, m18);
//        storeRGB_(baseIndex++, m19);
//        storeRGB_(baseIndex++, m20);
//        storeRGB_(baseIndex++, m21);
//        storeRGB_(baseIndex++, m22);
//        storeRGB_(baseIndex++, m23);
//        storeRGB_(baseIndex++, m24);
//        storeRGB_(baseIndex++, m25);
//        storeRGB_(baseIndex++, m26);
//        storeRGB_(baseIndex++, m27);
//        storeRGB_(baseIndex++, m28);
//        storeRGB_(baseIndex++, m29);
//        storeRGB_(baseIndex++, m30);
//        storeRGB_(baseIndex++, m31);
//        return baseIndex;
//    }
    
    private int storeRGB32(int baseIndex) {
        storeRGB(baseIndex++, r0, g0, b0);
        storeRGB(baseIndex++, r1, g1, b1);
        storeRGB(baseIndex++, r2, g2, b2);
        storeRGB(baseIndex++, r3, g3, b3);
        storeRGB(baseIndex++, r4, g4, b4);
        storeRGB(baseIndex++, r5, g5, b5);
        storeRGB(baseIndex++, r6, g6, b6);
        storeRGB(baseIndex++, r7, g7, b7);
        storeRGB(baseIndex++, r8, g8, b8);
        storeRGB(baseIndex++, r9, g9, b9);
        storeRGB(baseIndex++, r10, g10, b10);
        storeRGB(baseIndex++, r11, g11, b11);
        storeRGB(baseIndex++, r12, g12, b12);
        storeRGB(baseIndex++, r13, g13, b13);
        storeRGB(baseIndex++, r14, g14, b14);
        storeRGB(baseIndex++, r15, g15, b15);
        storeRGB(baseIndex++, r16, g16, b16);
        storeRGB(baseIndex++, r17, g17, b17);
        storeRGB(baseIndex++, r18, g18, b18);
        storeRGB(baseIndex++, r19, g19, b19);
        storeRGB(baseIndex++, r20, g20, b20);
        storeRGB(baseIndex++, r21, g21, b21);
        storeRGB(baseIndex++, r22, g22, b22);
        storeRGB(baseIndex++, r23, g23, b23);
        storeRGB(baseIndex++, r24, g24, b24);
        storeRGB(baseIndex++, r25, g25, b25);
        storeRGB(baseIndex++, r26, g26, b26);
        storeRGB(baseIndex++, r27, g27, b27);
        storeRGB(baseIndex++, r28, g28, b28);
        storeRGB(baseIndex++, r29, g29, b29);
        storeRGB(baseIndex++, r30, g30, b30);
        storeRGB(baseIndex++, r31, g31, b31);
        return baseIndex;
    }
    
//     private void storeRGB_(int baseIndex, int m) {
//        int r = 0, g = 0, b = 0;
//        for (int i = 0; i < groups.length; i++) {
//            AlphaBlendGroup group = groups[i];
//            int bits = (m >> group.groupShift) & group.groupMask;
//            int red = group.redMap[bits];
//            int green = group.greenMap[bits];
//            int blue = group.blueMap[bits];
//            int ia = group.inverseAlphaMap[bits];
//            if (ia == 0) {
//                r = red >> SCALE_SH;
//                g = green >> SCALE_SH;
//                b = blue >> SCALE_SH;
//            } else if (ia != SCALE) {
//                r = (red + r * ia) >> SCALE_SH;
//                g = (green + g * ia) >> SCALE_SH;
//                b = (blue + b * ia) >> SCALE_SH;
//            }
//        }
//        assert 0 <= r && r <= 255;
//        assert 0 <= g && g <= 255;
//        assert 0 <= b && b <= 255;
//        int color = (r << 16) | (g << 8) + b;
//        opaqueData[baseIndex] = color;
//    }
    
    private void storeRGB(int baseIndex, int red, int green, int blue) {
        int color;
        if (((red | green | blue) & ~0xFF) == 0) {
            color = (red << 16) | (green << 8) | blue;
        } else {
            color = normalizeRgbDim(red, green, blue);
        }
        opaqueData[baseIndex] = color;
    }
    
    private int normalizeRgbHighlight(int red, int green, int blue) {
        return 0xFFFFFF;
    }
    
    private int normalizeRgbClip(int red, int green, int blue) {
        red = Math.max(0, Math.min(0xFF, red));
        green = Math.max(0, Math.min(0xFF, red));
        blue = Math.max(0, Math.min(0xFF, red));
        return (red << 16) | (green << 8) | blue;
    }
    
    private int normalizeRgbDim(int red, int green, int blue) {
        int min, max;
        if (red <= green) {
            min = red;
            max = green;
        } else {
            min = green;
            max = red;
        }
        if (blue > max)
            max = blue;
        if (blue < min)
            min = blue;
        
        if (max - min <= 255) {
            int dec = min < 0 ? min : max - 255;
            red -= dec;
            green -= dec;
            blue -= dec;
        } else {
            int dec = max - 255;
            red = Math.max(0, red - dec);
            green = Math.max(0, green - dec);
            blue = Math.max(0, blue - dec);
        }
        return (red << 16) | (green << 8) | blue;
    }
    
    //********************************************************************
    
    private class AlphaBlendGroup {
        int groupShift;
        int groupMask;
        int[] redMap;
        int[] greenMap;
        int[] blueMap;
        int[] inverseAlphaMap;
        int[][] bits;
        
        AlphaBlendGroup(int[][] bits, List<EditWindow.LayerColor> cols, int offset, int len, boolean fillBackround) {
            groupShift = offset;
            groupMask = (1 << len) - 1;
            int mapLen = 1 << len;
            redMap = new int[mapLen];
            greenMap = new int[mapLen];
            blueMap = new int[mapLen];
            inverseAlphaMap = new int[mapLen];
            this.bits = new int[len][];
            
            for (int i = 0; i < len; i++) {
                this.bits[i] = bits[offset + i];
            }
            
            float[] backgroundComps = background.getRGBColorComponents(null);
            float bRed = backgroundComps[0];
            float bGreen = backgroundComps[1];
            float bBlue = backgroundComps[2];
            for (int k = 0; k < mapLen; k++) {
                double red = 0, green = 0, blue = 0, ia = 1.0;
                if (fillBackround) {
                    red = bRed;
                    green = bGreen;
                    blue = bBlue;
                    ia = 0.0;
                }
                for (int i = 0; i < len; i++) {
                    if ((k & (1 << i)) == 0) continue;
                    EditWindow.LayerColor lc = cols.get(offset + i);
                    double iAlpha = lc.inverseAlpha;
                    red *= iAlpha;
                    green *= iAlpha;
                    blue *= iAlpha;
                    ia *= iAlpha;
                    red += lc.premultipliedRed;
                    green += lc.premultipliedGreen;
                    blue += lc.premultipliedBlue;
                }
                redMap[k] = (int)(red*SCALE*255);
                greenMap[k] = (int)(green*SCALE*255);
                blueMap[k] = (int)(blue*SCALE*255);
                inverseAlphaMap[k] = (int)(ia*SCALE);
            }
        }
        
        private void unpackBytes(int index) {
            // unpack pixels
            int pixel0 = 0, pixel1 = 0, pixel2 = 0, pixel3 = 0, pixel4 = 0, pixel5 = 0, pixel6 = 0, pixel7 = 0;
            int pixel8 = 0, pixel9 = 0, pixel10 = 0, pixel11 = 0, pixel12 = 0, pixel13 = 0, pixel14 = 0, pixel15 = 0;
            int pixel16 = 0, pixel17 = 0, pixel18 = 0, pixel19 = 0, pixel20 = 0, pixel21 = 0, pixel22 = 0, pixel23 = 0;
            int pixel24 = 0, pixel25 = 0, pixel26 = 0, pixel27 = 0, pixel28 = 0, pixel29 = 0, pixel30 = 0, pixel31 = 0;
            for (int i = 0; i < bits.length; i++) {
                int value = bits[i][index];
                if (value == 0) continue;
                pixel0 |= (value & 1) << i;
                pixel1 |= ((value >> 1) & 1) << i;
                pixel2 |= ((value >> 2) & 1) << i;
                pixel3 |= ((value >> 3) & 1) << i;
                pixel4 |= ((value >> 4) & 1) << i;
                pixel5 |= ((value >> 5) & 1) << i;
                pixel6 |= ((value >> 6) & 1) << i;
                pixel7 |= ((value >> 7) & 1) << i;
                pixel8 |= ((value >> 8) & 1) << i;
                pixel9 |= ((value >> 9) & 1) << i;
                pixel10 |= ((value >> 10) & 1) << i;
                pixel11 |= ((value >> 11) & 1) << i;
                pixel12 |= ((value >> 12) & 1) << i;
                pixel13 |= ((value >> 13) & 1) << i;
                pixel14 |= ((value >> 14) & 1) << i;
                pixel15 |= ((value >> 15) & 1) << i;
                pixel16 |= ((value >> 16) & 1) << i;
                pixel17 |= ((value >> 17) & 1) << i;
                pixel18 |= ((value >> 18) & 1) << i;
                pixel19 |= ((value >> 19) & 1) << i;
                pixel20 |= ((value >> 20) & 1) << i;
                pixel21 |= ((value >> 21) & 1) << i;
                pixel22 |= ((value >> 22) & 1) << i;
                pixel23 |= ((value >> 23) & 1) << i;
                pixel24 |= ((value >> 24) & 1) << i;
                pixel25 |= ((value >> 25) & 1) << i;
                pixel26 |= ((value >> 26) & 1) << i;
                pixel27 |= ((value >> 27) & 1) << i;
                pixel28 |= ((value >> 28) & 1) << i;
                pixel29 |= ((value >> 29) & 1) << i;
                pixel30 |= ((value >> 30) & 1) << i;
                pixel31 |= ((value >> 31) & 1) << i;
            }
            
            // 
            int red, green, blue, ia;
            
            red = redMap[pixel0];
            green = greenMap[pixel0];
            blue = blueMap[pixel0];
            ia = inverseAlphaMap[pixel0];
            if (ia == 0) {
                r0 = red >> SCALE_SH;
                g0 = green >> SCALE_SH;
                b0 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r0 = (red + r0 * ia) >> SCALE_SH;
                g0 = (green + g0 * ia) >> SCALE_SH;
                b0 = (blue + b0 * ia) >> SCALE_SH;
            }

            red = redMap[pixel1];
            green = greenMap[pixel1];
            blue = blueMap[pixel1];
            ia = inverseAlphaMap[pixel1];
            if (ia == 0) {
                r1 = red >> SCALE_SH;
                g1 = green >> SCALE_SH;
                b1 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r1 = (red + r1 * ia) >> SCALE_SH;
                g1 = (green + g1 * ia) >> SCALE_SH;
                b1 = (blue + b1 * ia) >> SCALE_SH;
            }

            red = redMap[pixel2];
            green = greenMap[pixel2];
            blue = blueMap[pixel2];
            ia = inverseAlphaMap[pixel2];
            if (ia == 0) {
                r2 = red >> SCALE_SH;
                g2 = green >> SCALE_SH;
                b2 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r2 = (red + r2 * ia) >> SCALE_SH;
                g2 = (green + g2 * ia) >> SCALE_SH;
                b2 = (blue + b2 * ia) >> SCALE_SH;
            }

            red = redMap[pixel3];
            green = greenMap[pixel3];
            blue = blueMap[pixel3];
            ia = inverseAlphaMap[pixel3];
            if (ia == 0) {
                r3 = red >> SCALE_SH;
                g3 = green >> SCALE_SH;
                b3 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r3 = (red + r3 * ia) >> SCALE_SH;
                g3 = (green + g3 * ia) >> SCALE_SH;
                b3 = (blue + b3 * ia) >> SCALE_SH;
            }

            red = redMap[pixel4];
            green = greenMap[pixel4];
            blue = blueMap[pixel4];
            ia = inverseAlphaMap[pixel4];
            if (ia == 0) {
                r4 = red >> SCALE_SH;
                g4 = green >> SCALE_SH;
                b4 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r4 = (red + r4 * ia) >> SCALE_SH;
                g4 = (green + g4 * ia) >> SCALE_SH;
                b4 = (blue + b4 * ia) >> SCALE_SH;
            }

            red = redMap[pixel5];
            green = greenMap[pixel5];
            blue = blueMap[pixel5];
            ia = inverseAlphaMap[pixel5];
            if (ia == 0) {
                r5 = red >> SCALE_SH;
                g5 = green >> SCALE_SH;
                b5 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r5 = (red + r5 * ia) >> SCALE_SH;
                g5 = (green + g5 * ia) >> SCALE_SH;
                b5 = (blue + b5 * ia) >> SCALE_SH;
            }

            red = redMap[pixel6];
            green = greenMap[pixel6];
            blue = blueMap[pixel6];
            ia = inverseAlphaMap[pixel6];
            if (ia == 0) {
                r6 = red >> SCALE_SH;
                g6 = green >> SCALE_SH;
                b6 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r6 = (red + r6 * ia) >> SCALE_SH;
                g6 = (green + g6 * ia) >> SCALE_SH;
                b6 = (blue + b6 * ia) >> SCALE_SH;
            }

            red = redMap[pixel7];
            green = greenMap[pixel7];
            blue = blueMap[pixel7];
            ia = inverseAlphaMap[pixel7];
            if (ia == 0) {
                r7 = red >> SCALE_SH;
                g7 = green >> SCALE_SH;
                b7 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r7 = (red + r7 * ia) >> SCALE_SH;
                g7 = (green + g7 * ia) >> SCALE_SH;
                b7 = (blue + b7 * ia) >> SCALE_SH;
            }

            red = redMap[pixel8];
            green = greenMap[pixel8];
            blue = blueMap[pixel8];
            ia = inverseAlphaMap[pixel8];
            if (ia == 0) {
                r8 = red >> SCALE_SH;
                g8 = green >> SCALE_SH;
                b8 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r8 = (red + r8 * ia) >> SCALE_SH;
                g8 = (green + g8 * ia) >> SCALE_SH;
                b8 = (blue + b8 * ia) >> SCALE_SH;
            }

            red = redMap[pixel9];
            green = greenMap[pixel9];
            blue = blueMap[pixel9];
            ia = inverseAlphaMap[pixel9];
            if (ia == 0) {
                r9 = red >> SCALE_SH;
                g9 = green >> SCALE_SH;
                b9 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r9 = (red + r9 * ia) >> SCALE_SH;
                g9 = (green + g9 * ia) >> SCALE_SH;
                b9 = (blue + b9 * ia) >> SCALE_SH;
            }

            red = redMap[pixel10];
            green = greenMap[pixel10];
            blue = blueMap[pixel10];
            ia = inverseAlphaMap[pixel10];
            if (ia == 0) {
                r10 = red >> SCALE_SH;
                g10 = green >> SCALE_SH;
                b10 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r10 = (red + r10 * ia) >> SCALE_SH;
                g10 = (green + g10 * ia) >> SCALE_SH;
                b10 = (blue + b10 * ia) >> SCALE_SH;
            }

            red = redMap[pixel11];
            green = greenMap[pixel11];
            blue = blueMap[pixel11];
            ia = inverseAlphaMap[pixel11];
            if (ia == 0) {
                r11 = red >> SCALE_SH;
                g11 = green >> SCALE_SH;
                b11 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r11 = (red + r11 * ia) >> SCALE_SH;
                g11 = (green + g11 * ia) >> SCALE_SH;
                b11 = (blue + b11 * ia) >> SCALE_SH;
            }

            red = redMap[pixel12];
            green = greenMap[pixel12];
            blue = blueMap[pixel12];
            ia = inverseAlphaMap[pixel12];
            if (ia == 0) {
                r12 = red >> SCALE_SH;
                g12 = green >> SCALE_SH;
                b12 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r12 = (red + r12 * ia) >> SCALE_SH;
                g12 = (green + g12 * ia) >> SCALE_SH;
                b12 = (blue + b12 * ia) >> SCALE_SH;
            }

            red = redMap[pixel13];
            green = greenMap[pixel13];
            blue = blueMap[pixel13];
            ia = inverseAlphaMap[pixel13];
            if (ia == 0) {
                r13 = red >> SCALE_SH;
                g13 = green >> SCALE_SH;
                b13 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r13 = (red + r13 * ia) >> SCALE_SH;
                g13 = (green + g13 * ia) >> SCALE_SH;
                b13 = (blue + b13 * ia) >> SCALE_SH;
            }

            red = redMap[pixel14];
            green = greenMap[pixel14];
            blue = blueMap[pixel14];
            ia = inverseAlphaMap[pixel14];
            if (ia == 0) {
                r14 = red >> SCALE_SH;
                g14 = green >> SCALE_SH;
                b14 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r14 = (red + r14 * ia) >> SCALE_SH;
                g14 = (green + g14 * ia) >> SCALE_SH;
                b14 = (blue + b14 * ia) >> SCALE_SH;
            }

            red = redMap[pixel15];
            green = greenMap[pixel15];
            blue = blueMap[pixel15];
            ia = inverseAlphaMap[pixel15];
            if (ia == 0) {
                r15 = red >> SCALE_SH;
                g15 = green >> SCALE_SH;
                b15 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r15 = (red + r15 * ia) >> SCALE_SH;
                g15 = (green + g15 * ia) >> SCALE_SH;
                b15 = (blue + b15 * ia) >> SCALE_SH;
            }

            red = redMap[pixel16];
            green = greenMap[pixel16];
            blue = blueMap[pixel16];
            ia = inverseAlphaMap[pixel16];
            if (ia == 0) {
                r16 = red >> SCALE_SH;
                g16 = green >> SCALE_SH;
                b16 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r16 = (red + r16 * ia) >> SCALE_SH;
                g16 = (green + g16 * ia) >> SCALE_SH;
                b16 = (blue + b16 * ia) >> SCALE_SH;
            }

            red = redMap[pixel17];
            green = greenMap[pixel17];
            blue = blueMap[pixel17];
            ia = inverseAlphaMap[pixel17];
            if (ia == 0) {
                r17 = red >> SCALE_SH;
                g17 = green >> SCALE_SH;
                b17 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r17 = (red + r17 * ia) >> SCALE_SH;
                g17 = (green + g17 * ia) >> SCALE_SH;
                b17 = (blue + b17 * ia) >> SCALE_SH;
            }

            red = redMap[pixel18];
            green = greenMap[pixel18];
            blue = blueMap[pixel18];
            ia = inverseAlphaMap[pixel18];
            if (ia == 0) {
                r18 = red >> SCALE_SH;
                g18 = green >> SCALE_SH;
                b18 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r18 = (red + r18 * ia) >> SCALE_SH;
                g18 = (green + g18 * ia) >> SCALE_SH;
                b18 = (blue + b18 * ia) >> SCALE_SH;
            }

            red = redMap[pixel19];
            green = greenMap[pixel19];
            blue = blueMap[pixel19];
            ia = inverseAlphaMap[pixel19];
            if (ia == 0) {
                r19 = red >> SCALE_SH;
                g19 = green >> SCALE_SH;
                b19 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r19 = (red + r19 * ia) >> SCALE_SH;
                g19 = (green + g19 * ia) >> SCALE_SH;
                b19 = (blue + b19 * ia) >> SCALE_SH;
            }

            red = redMap[pixel20];
            green = greenMap[pixel20];
            blue = blueMap[pixel20];
            ia = inverseAlphaMap[pixel20];
            if (ia == 0) {
                r20 = red >> SCALE_SH;
                g20 = green >> SCALE_SH;
                b20 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r20 = (red + r20 * ia) >> SCALE_SH;
                g20 = (green + g20 * ia) >> SCALE_SH;
                b20 = (blue + b20 * ia) >> SCALE_SH;
            }

            red = redMap[pixel21];
            green = greenMap[pixel21];
            blue = blueMap[pixel21];
            ia = inverseAlphaMap[pixel21];
            if (ia == 0) {
                r21 = red >> SCALE_SH;
                g21 = green >> SCALE_SH;
                b21 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r21 = (red + r21 * ia) >> SCALE_SH;
                g21 = (green + g21 * ia) >> SCALE_SH;
                b21 = (blue + b21 * ia) >> SCALE_SH;
            }

            red = redMap[pixel22];
            green = greenMap[pixel22];
            blue = blueMap[pixel22];
            ia = inverseAlphaMap[pixel22];
            if (ia == 0) {
                r22 = red >> SCALE_SH;
                g22 = green >> SCALE_SH;
                b22 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r22 = (red + r22 * ia) >> SCALE_SH;
                g22 = (green + g22 * ia) >> SCALE_SH;
                b22 = (blue + b22 * ia) >> SCALE_SH;
            }

            red = redMap[pixel23];
            green = greenMap[pixel23];
            blue = blueMap[pixel23];
            ia = inverseAlphaMap[pixel23];
            if (ia == 0) {
                r23 = red >> SCALE_SH;
                g23 = green >> SCALE_SH;
                b23 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r23 = (red + r23 * ia) >> SCALE_SH;
                g23 = (green + g23 * ia) >> SCALE_SH;
                b23 = (blue + b23 * ia) >> SCALE_SH;
            }

            red = redMap[pixel24];
            green = greenMap[pixel24];
            blue = blueMap[pixel24];
            ia = inverseAlphaMap[pixel24];
            if (ia == 0) {
                r24 = red >> SCALE_SH;
                g24 = green >> SCALE_SH;
                b24 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r24 = (red + r24 * ia) >> SCALE_SH;
                g24 = (green + g24 * ia) >> SCALE_SH;
                b24 = (blue + b24 * ia) >> SCALE_SH;
            }

            red = redMap[pixel25];
            green = greenMap[pixel25];
            blue = blueMap[pixel25];
            ia = inverseAlphaMap[pixel25];
            if (ia == 0) {
                r25 = red >> SCALE_SH;
                g25 = green >> SCALE_SH;
                b25 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r25 = (red + r25 * ia) >> SCALE_SH;
                g25 = (green + g25 * ia) >> SCALE_SH;
                b25 = (blue + b25 * ia) >> SCALE_SH;
            }

            red = redMap[pixel26];
            green = greenMap[pixel26];
            blue = blueMap[pixel26];
            ia = inverseAlphaMap[pixel26];
            if (ia == 0) {
                r26 = red >> SCALE_SH;
                g26 = green >> SCALE_SH;
                b26 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r26 = (red + r26 * ia) >> SCALE_SH;
                g26 = (green + g26 * ia) >> SCALE_SH;
                b26 = (blue + b26 * ia) >> SCALE_SH;
            }

            red = redMap[pixel27];
            green = greenMap[pixel27];
            blue = blueMap[pixel27];
            ia = inverseAlphaMap[pixel27];
            if (ia == 0) {
                r27 = red >> SCALE_SH;
                g27 = green >> SCALE_SH;
                b27 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r27 = (red + r27 * ia) >> SCALE_SH;
                g27 = (green + g27 * ia) >> SCALE_SH;
                b27 = (blue + b27 * ia) >> SCALE_SH;
            }

            red = redMap[pixel28];
            green = greenMap[pixel28];
            blue = blueMap[pixel28];
            ia = inverseAlphaMap[pixel28];
            if (ia == 0) {
                r28 = red >> SCALE_SH;
                g28 = green >> SCALE_SH;
                b28 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r28 = (red + r28 * ia) >> SCALE_SH;
                g28 = (green + g28 * ia) >> SCALE_SH;
                b28 = (blue + b28 * ia) >> SCALE_SH;
            }

            red = redMap[pixel29];
            green = greenMap[pixel29];
            blue = blueMap[pixel29];
            ia = inverseAlphaMap[pixel29];
            if (ia == 0) {
                r29 = red >> SCALE_SH;
                g29 = green >> SCALE_SH;
                b29 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r29 = (red + r29 * ia) >> SCALE_SH;
                g29 = (green + g29 * ia) >> SCALE_SH;
                b29 = (blue + b29 * ia) >> SCALE_SH;
            }

            red = redMap[pixel30];
            green = greenMap[pixel30];
            blue = blueMap[pixel30];
            ia = inverseAlphaMap[pixel30];
            if (ia == 0) {
                r30 = red >> SCALE_SH;
                g30 = green >> SCALE_SH;
                b30 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r30 = (red + r30 * ia) >> SCALE_SH;
                g30 = (green + g30 * ia) >> SCALE_SH;
                b30 = (blue + b30 * ia) >> SCALE_SH;
            }

            red = redMap[pixel31];
            green = greenMap[pixel31];
            blue = blueMap[pixel31];
            ia = inverseAlphaMap[pixel31];
            if (ia == 0) {
                r31 = red >> SCALE_SH;
                g31 = green >> SCALE_SH;
                b31 = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r31 = (red + r31 * ia) >> SCALE_SH;
                g31 = (green + g31 * ia) >> SCALE_SH;
                b31 = (blue + b31 * ia) >> SCALE_SH;
            }
        }
    }
}
