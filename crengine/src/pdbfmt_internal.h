#ifndef PDBFMT_INTERNAL_H
#define PDBFMT_INTERNAL_H

#include "../include/lvtypes.h"

#include <cstddef>
#include <vector>

bool LVInflatePDBBuffer(const lUInt8 *compressed,
        size_t compressedSize,
        std::vector<lUInt8> &uncompressed);

#endif
