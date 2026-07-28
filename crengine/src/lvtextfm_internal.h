#ifndef __LVTEXTFM_INTERNAL_H_INCLUDED__
#define __LVTEXTFM_INTERNAL_H_INCLUDED__

/// Native regression seam for formatter workspace ownership and static leases.
bool LVRunFormatterWorkspaceOwnershipRegression();

/// Native regression seam for formatted source/line/float graph ownership.
bool LVRunFormattedTextOwnershipRegression();

#endif
