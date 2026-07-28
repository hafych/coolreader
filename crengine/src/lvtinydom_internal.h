#ifndef LVTINYDOM_INTERNAL_H_INCLUDED
#define LVTINYDOM_INTERNAL_H_INCLUDED

#include "../include/lvtinydom.h"

#include <vector>

/// Native regression seam for cache codec ownership and recovery.
bool LVRunCacheFileCodecRegression(
        CacheCompressionType type,
        const std::vector<lUInt8> &input);

/// Native regression seam for cache index ownership and rollback.
bool LVRunCacheFileIndexRegression();

/// Native regression seam for DOM blob ownership and cache restoration.
bool LVRunBlobCacheRegression();

/// Native regression seam for DOM chunk ownership and cache transitions.
bool LVRunDomChunkStorageRegression();

/// Native regression seam for DOM node-part ownership and load rollback.
bool LVRunDomNodePartOwnershipRegression();

/// Native regression seam for bounded transactional style-index restore.
bool LVRunStyleIndexRestoreRegression();

#endif
