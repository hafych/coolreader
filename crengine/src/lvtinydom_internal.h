#ifndef LVTINYDOM_INTERNAL_H_INCLUDED
#define LVTINYDOM_INTERNAL_H_INCLUDED

#include "../include/lvtinydom.h"

#include <vector>

/// Native regression seam for cache codec ownership and recovery.
bool LVRunCacheFileCodecRegression(
        CacheCompressionType type,
        const std::vector<lUInt8> &input);

#endif
