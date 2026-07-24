/***************************************************************************
 *   CoolReader engine                                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or         *
 *   modify it under the terms of the GNU General Public License           *
 *   as published by the Free Software Foundation; either version 2        *
 *   of the License, or (at your option) any later version.                *
 ***************************************************************************/

#ifndef __LV_CACHE_H_INCLUDED__
#define __LV_CACHE_H_INCLUDED__

#include "lvtypes.h"

/// Common observable counters for bounded native caches.
struct LVCacheStats {
    int capacity;
    int size;
    lUInt64 hits;
    lUInt64 misses;
    lUInt64 evictions;

    LVCacheStats(int capacityBytes = 0, int sizeBytes = 0,
            lUInt64 hitCount = 0, lUInt64 missCount = 0,
            lUInt64 evictionCount = 0)
        : capacity(capacityBytes), size(sizeBytes), hits(hitCount),
          misses(missCount), evictions(evictionCount) {
    }
};

#endif //__LV_CACHE_H_INCLUDED__
