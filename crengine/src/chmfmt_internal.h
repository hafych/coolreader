#ifndef CHMFMT_INTERNAL_H
#define CHMFMT_INTERNAL_H

#include "../include/crsetup.h"
#include "../include/lvtinydom.h"

#if CHM_SUPPORT_ENABLED==1

/// Parses one CHM HTML resource and transfers the resulting document.
ldomDocument *LVParseCHMHTMLStream(
        LVStreamRef stream,
        lString32 defaultEncodingName,
        ldomDocument *parentDocument);

/// Native regression seam for CHM metadata factory ownership.
bool LVRunChmMetadataOwnershipRegression();

#endif

#endif
