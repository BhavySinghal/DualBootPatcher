/*
 * Copyright (C) 2014  Andrew Gunnerson <andrewgunnerson@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.chenxiaolong.multibootpatcher.socket;

import android.content.Context;

import com.github.chenxiaolong.dualbootpatcher.BuildConfig;
import com.github.chenxiaolong.dualbootpatcher.SystemPropertiesProxy;
import com.github.chenxiaolong.multibootpatcher.Version;

import java.util.HashMap;

public class MbtoolUtils {
    private static HashMap<Feature, Version> sMinVersionMap = new HashMap<>();

    static {
        if (BuildConfig.BUILD_TYPE.equals("ci")) {
            // Snapshot builds
            sMinVersionMap.put(Feature.DAEMON, Version.from("8.0.0.r1025"));
            sMinVersionMap.put(Feature.APP_SHARING, Version.from("8.0.0.r991"));
            sMinVersionMap.put(Feature.IN_APP_INSTALLATION, Version.from("8.0.0.r1045"));
        } else {
            // Debug/release builds
            sMinVersionMap.put(Feature.DAEMON, Version.from("8.99.6"));
            sMinVersionMap.put(Feature.APP_SHARING, Version.from("8.99.6"));
            sMinVersionMap.put(Feature.IN_APP_INSTALLATION, Version.from("8.99.6"));
        }
    }

    public static Version getSystemMbtoolVersion(Context context) {
        try {
            return new Version(SystemPropertiesProxy.get(context, "ro.multiboot.version"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Version.from("0.0.0");
    }

    public static Version getMbtoolVersion(Context context) {
        try {
            return new Version(MbtoolSocket.getInstance().version(context));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Version.from("0.0.0");
    }

    public static Version getMinimumRequiredVersion(Feature feature) {
        return sMinVersionMap.get(feature);
    }

    public enum Feature {
        DAEMON,
        APP_SHARING,
        IN_APP_INSTALLATION
    }
}
