/*
 * Copyright (C) 2014  Andrew Gunnerson <andrewgunnerson@gmail.com>
 *
 * This file is part of MultiBootPatcher
 *
 * MultiBootPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MultiBootPatcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MultiBootPatcher.  If not, see <http://www.gnu.org/licenses/>.
 */

#pragma once

#include "cwrapper/ctypes.h"

#ifdef __cplusplus
extern "C" {
#endif

CPatchInfo * mbp_patchinfo_create(void);
void mbp_patchinfo_destroy(CPatchInfo *info);

char * mbp_patchinfo_id(const CPatchInfo *info);
void mbp_patchinfo_set_id(CPatchInfo *info, const char *id);

char * mbp_patchinfo_name(const CPatchInfo *info);
void mbp_patchinfo_set_name(CPatchInfo *info, const char *name);

char ** mbp_patchinfo_regexes(const CPatchInfo *info);
void mbp_patchinfo_set_regexes(CPatchInfo *info, const char **regexes);

char ** mbp_patchinfo_exclude_regexes(const CPatchInfo *info);
void mbp_patchinfo_set_exclude_regexes(CPatchInfo *info, const char **regexes);

void mbp_patchinfo_add_autopatcher(CPatchInfo *info, const char *apName,
                                   CStringMap *args);
void mbp_patchinfo_remove_autopatcher(CPatchInfo *info, const char *apName);
char ** mbp_patchinfo_autopatchers(const CPatchInfo *info);
CStringMap * mbp_patchinfo_autopatcher_args(const CPatchInfo *info,
                                            const char *apName);

bool mbp_patchinfo_has_boot_image(const CPatchInfo *info);
void mbp_patchinfo_set_has_boot_image(CPatchInfo *info, bool hasBootImage);

bool mbp_patchinfo_autodetect_boot_images(const CPatchInfo *info);
void mbp_patchinfo_set_autodetect_boot_images(CPatchInfo *info,
                                              bool autoDetect);

char ** mbp_patchinfo_boot_images(const CPatchInfo *info);
void mbp_patchinfo_set_boot_images(CPatchInfo *info, const char **bootImages);

char * mbp_patchinfo_ramdisk(const CPatchInfo *info);
void mbp_patchinfo_set_ramdisk(CPatchInfo *info, const char *ramdisk);

bool mbp_patchinfo_device_check(const CPatchInfo *info);
void mbp_patchinfo_set_device_check(CPatchInfo *info, bool deviceCheck);

#ifdef __cplusplus
}
#endif
