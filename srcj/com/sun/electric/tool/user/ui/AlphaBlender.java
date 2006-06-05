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
    
    private int backgroundValue;
    private int[] opaqueData;
    
    /** Creates a new instance of AlphaBlender */
    public AlphaBlender() {
    }
    
    private static final int SCALE_SH = 8;
    private static final int SCALE = 1 << SCALE_SH;
        
    private AlphaBlendGroup[] groups;
    private byte[][] layerBytes;
    private int m0, m1, m2, m3, m4, m5, m6, m7;
    
    private int r0, r1, r2, r3, r4, r5, r6, r7;
    private int g0, g1, g2, g3, g4, g5, g6, g7;
    private int b0, b1, b2, b3, b4, b5, b6, b7;
    
    void init(int backgroundValue, List<EditWindow.LayerColor> blendingOrder, Map<Layer,byte[]> layerBytes) {
        this.backgroundValue = backgroundValue;
        Color background = new Color(backgroundValue);
        ArrayList<byte[]> bytes = new ArrayList<byte[]>();
        ArrayList<Color> colors = new ArrayList<Color>();
        for (EditWindow.LayerColor layerColor: blendingOrder) {
            byte[] b = layerBytes.get(layerColor.layer);
            if (b == null) continue;
            if (layerColor.color.getAlpha() == 0) continue;
            bytes.add(b);
            colors.add(layerColor.color);
        }
        this.layerBytes = bytes.toArray(new byte[bytes.size()][]);
        
        groups = new AlphaBlendGroup[bytes.size()/13 + 1];
        int k = 0;
        for (int i = 0; i < groups.length; i++) {
            int l = (bytes.size() - k) / (groups.length - i);
            groups[i] = new AlphaBlendGroup(this.layerBytes, colors, k, l, background);
            background = null;
            k += l;
        }
    }
    
    private void unpackBytes(int index) {
        // unpack pixels
        int pixel0 = 0, pixel1 = 0, pixel2 = 0, pixel3 = 0, pixel4 = 0, pixel5 = 0, pixel6 = 0, pixel7 = 0;
        for (int i = 0; i < layerBytes.length; i++) {
            int value = layerBytes[i][index];
            if (value == 0) continue;
            pixel0 |= (value & 1) << i;
            pixel1 |= ((value >> 1) & 1) << i;
            pixel2 |= ((value >> 2) & 1) << i;
            pixel3 |= ((value >> 3) & 1) << i;
            pixel4 |= ((value >> 4) & 1) << i;
            pixel5 |= ((value >> 5) & 1) << i;
            pixel6 |= ((value >> 6) & 1) << i;
            pixel7 |= ((value >> 7) & 1) << i;
        }
        m0 = pixel0;
        m1 = pixel1;
        m2 = pixel2;
        m3 = pixel3;
        m4 = pixel4;
        m5 = pixel5;
        m6 = pixel6;
        m7 = pixel7;
    }
    
    int composeBytes(int inputOffset, int numBytes, int[] opaqueData, int outputOffset) {
        int s = 0;
        this.opaqueData = opaqueData;
        for (int index = 0; index < numBytes; index++) {
            unpackBytes(inputOffset);
            inputOffset++;
            outputOffset = storeRGB8_(outputOffset);
        }
        return s;
    }
    
    void composeBytes0(int inputOffset, int numBytes, int[] opaqueData, int outputOffset) {
        this.opaqueData = opaqueData;
        for (int index = 0; index < numBytes; index++) {
            for (AlphaBlendGroup group: groups)
                group.unpackBytes(inputOffset);
            inputOffset++;
            outputOffset = storeRGB8(outputOffset);
        }
    }
    
    void composeBits(int inputOffset, byte bitMask, int[] opaqueData, int outputOffset) {
        this.opaqueData = opaqueData;
        for (AlphaBlendGroup group: groups)
            group.unpackBytes(inputOffset);
        if ((bitMask & (1 << 0)) != 0)
            storeRGB(outputOffset + 0, r0, g0, b0);
        if ((bitMask & (1 << 1)) != 0)
            storeRGB(outputOffset + 1, r1, g1, b1);
        if ((bitMask & (1 << 2)) != 0)
            storeRGB(outputOffset + 2, r2, g2, b2);
        if ((bitMask & (1 << 3)) != 0)
            storeRGB(outputOffset + 3, r3, g3, b3);
        if ((bitMask & (1 << 4)) != 0)
            storeRGB(outputOffset + 4, r4, g4, b4);
        if ((bitMask & (1 << 5)) != 0)
            storeRGB(outputOffset + 5, r5, g5, b5);
        if ((bitMask & (1 << 6)) != 0)
            storeRGB(outputOffset + 6, r6, g6, b6);
        if ((bitMask & (1 << 7)) != 0)
            storeRGB(outputOffset + 7, r7, g7, b7);
    }
    
    private int storeRGB8_(int baseIndex) {
        storeRGB_(baseIndex++, m0);
        storeRGB_(baseIndex++, m1);
        storeRGB_(baseIndex++, m2);
        storeRGB_(baseIndex++, m3);
        storeRGB_(baseIndex++, m4);
        storeRGB_(baseIndex++, m5);
        storeRGB_(baseIndex++, m6);
        storeRGB_(baseIndex++, m7);
        return baseIndex;
    }
    
    private int storeRGB8(int baseIndex) {
        storeRGB(baseIndex++, r0, g0, b0);
        storeRGB(baseIndex++, r1, g1, b1);
        storeRGB(baseIndex++, r2, g2, b2);
        storeRGB(baseIndex++, r3, g3, b3);
        storeRGB(baseIndex++, r4, g4, b4);
        storeRGB(baseIndex++, r5, g5, b5);
        storeRGB(baseIndex++, r6, g6, b6);
        storeRGB(baseIndex++, r7, g7, b7);
        return baseIndex;
    }
    
     private void storeRGB_(int baseIndex, int m) {
        int r = 0, g = 0, b = 0;
        for (int i = 0; i < groups.length; i++) {
            AlphaBlendGroup group = groups[i];
            int bits = (m >> group.groupShift) & group.groupMask;
            int red = group.redMap[bits];
            int green = group.greenMap[bits];
            int blue = group.blueMap[bits];
            int ia = group.inverseAlphaMap[bits];
            if (ia == 0) {
                r = red >> SCALE_SH;
                g = green >> SCALE_SH;
                b = blue >> SCALE_SH;
            } else if (ia != SCALE) {
                r = (red + r * ia) >> SCALE_SH;
                g = (green + g * ia) >> SCALE_SH;
                b = (blue + b * ia) >> SCALE_SH;
            }
        }
        assert 0 <= r && r <= 255;
        assert 0 <= g && g <= 255;
        assert 0 <= b && b <= 255;
        int color = (r << 16) | (g << 8) + b;
        int pixelValue = opaqueData[baseIndex];
        if (pixelValue != backgroundValue) {
            int pixelAlpha = (pixelValue >> 24) & 0xFF;
            if (pixelAlpha == 0xFF || pixelAlpha == 0)
                color = pixelValue;
            else if (pixelAlpha != 0)
                color = alphaBlend(pixelValue, color, pixelAlpha);
        }
        opaqueData[baseIndex] = color;
    }
    
     private void storeRGB(int baseIndex, int red, int green, int blue) {
        assert 0 <= red && red <= 255;
        assert 0 <= green && green <= 255;
        assert 0 <= blue && blue <= 255;
        int color = (red << 16) | (green << 8) + blue;
        int pixelValue = opaqueData[baseIndex];
        if (pixelValue != backgroundValue) {
            int pixelAlpha = (pixelValue >> 24) & 0xFF;
            if (pixelAlpha == 0xFF || pixelAlpha == 0)
                color = pixelValue;
            else if (pixelAlpha != 0)
                color = alphaBlend(pixelValue, color, pixelAlpha);
        }
        opaqueData[baseIndex] = color;
    }
    
	private static int alphaBlend(int color, int backgroundColor, int alpha)
	{
		int red = (color >> 16) & 0xFF;
		int green = (color >> 8) & 0xFF;
		int blue = color & 0xFF;
		int inverseAlpha = 254 - alpha;
		int redBack = (backgroundColor >> 16) & 0xFF;
		int greenBack = (backgroundColor >> 8) & 0xFF;
		int blueBack = backgroundColor & 0xFF;
		red = ((red * alpha) + (redBack * inverseAlpha)) / 255;
		green = ((green * alpha) + (greenBack * inverseAlpha)) / 255;
		blue = ((blue * alpha) + (blueBack * inverseAlpha)) / 255;
		color = (red << 16) | (green << 8) + blue;
		return color;
	}

    //********************************************************************
    
    private class AlphaBlendGroup {
        int groupShift;
        int groupMask;
        int[] redMap;
        int[] greenMap;
        int[] blueMap;
        int[] inverseAlphaMap;
        byte[][] bytes;
        
        AlphaBlendGroup(byte[][] bytes, List<Color> cols, int offset, int len, Color background) {
            groupShift = offset;
            groupMask = (1 << len) - 1;
            int mapLen = 1 << len;
            redMap = new int[mapLen];
            greenMap = new int[mapLen];
            blueMap = new int[mapLen];
            inverseAlphaMap = new int[mapLen];
            this.bytes = new byte[len][];
            
            float[][] compArrays = new float[len][];
            
            for (int i = 0; i < len; i++) {
                this.bytes[i] = bytes[offset + i];
                Color color = cols.get(offset + i);
                compArrays[i] = color.getRGBComponents(null);
            }
            
            for (int k = 0; k < mapLen; k++) {
                double red = 0, green = 0, blue = 0, ia = 1.0;
                if (background != null) {
                    float compArray[] = background.getRGBColorComponents(null);
                    red = compArray[0];
                    green = compArray[1];
                    blue = compArray[2];
                    ia = 0.0;
                }
                for (int i = 0; i < len; i++) {
                    if ((k & (1 << i)) == 0) continue;
                    float[] compArray = compArrays[i];
                    double alpha = compArray[3];
                    red = compArray[0]*alpha + red*(1 - alpha);
                    green = compArray[1]*alpha + green*(1 - alpha);
                    blue = compArray[2]*alpha + blue*(1 - alpha);
                    ia = ia*(1 - alpha);
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
            for (int i = 0; i < bytes.length; i++) {
                int value = bytes[i][index];
                if (value == 0) continue;
                pixel0 |= (value & 1) << i;
                pixel1 |= ((value >> 1) & 1) << i;
                pixel2 |= ((value >> 2) & 1) << i;
                pixel3 |= ((value >> 3) & 1) << i;
                pixel4 |= ((value >> 4) & 1) << i;
                pixel5 |= ((value >> 5) & 1) << i;
                pixel6 |= ((value >> 6) & 1) << i;
                pixel7 |= ((value >> 7) & 1) << i;
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

        }
    }
}
