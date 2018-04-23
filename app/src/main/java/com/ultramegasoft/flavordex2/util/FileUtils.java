/*
 * The MIT License (MIT)
 * Copyright © 2018 Steve Guidetti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ultramegasoft.flavordex2.util;

import android.support.annotation.NonNull;

import java.io.File;

/**
 * Helpers for dealing with files.
 *
 * @author Steve Guidetti
 */
public class FileUtils {
    /**
     * File extensions
     */
    public static final String EXT_CSV = ".csv";
    public static final String EXT_ZIP = ".zip";

    /**
     * Get a unique file name based on a given name.
     *
     * @param basePath  The path to the directory
     * @param baseName  The base name of the file without extension
     * @param extension The file extension
     * @return The unique file name
     */
    @NonNull
    public static String getUniqueFileName(@NonNull String basePath, @NonNull String baseName,
                                           @NonNull String extension) {
        String newName = baseName + extension;
        int i = 1;
        while(new File(basePath, newName).exists()) {
            newName = baseName + " (" + i++ + ")" + extension;
        }
        return newName;
    }
}
