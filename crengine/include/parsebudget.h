/***************************************************************************
 *   CoolReader engine                                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or         *
 *   modify it under the terms of the GNU General Public License           *
 *   as published by the Free Software Foundation; either version 2        *
 *   of the License, or (at your option) any later version.                *
 ***************************************************************************/

#ifndef __PARSEBUDGET_H_INCLUDED__
#define __PARSEBUDGET_H_INCLUDED__

#include "lvtypes.h"

/// Stable failure codes for resource limits enforced while parsing content.
enum ParseBudgetErrorCode {
    PARSE_BUDGET_OK = 0,
    PARSE_BUDGET_INPUT_BYTES = 1001,
    PARSE_BUDGET_TEXT_CHARACTERS = 1002,
    PARSE_BUDGET_XML_DEPTH = 1003,
    PARSE_BUDGET_ARCHIVE_ENTRY_COUNT = 1101,
    PARSE_BUDGET_ARCHIVE_ENTRY_BYTES = 1102,
    PARSE_BUDGET_ARCHIVE_TOTAL_BYTES = 1103,
    PARSE_BUDGET_ARCHIVE_COMPRESSION_RATIO = 1104,
    PARSE_BUDGET_ARCHIVE_PATH_DEPTH = 1105,
    PARSE_BUDGET_CONTAINER_DEPTH = 1106,
    PARSE_BUDGET_ARCHIVE_PATH = 1107,
    PARSE_BUDGET_ARCHIVE_DUPLICATE_ENTRY = 1108,
    PARSE_BUDGET_IMAGE_DIMENSIONS = 1201
};

inline const char *parseBudgetErrorName(ParseBudgetErrorCode code)
{
    switch (code) {
    case PARSE_BUDGET_OK:
        return "ok";
    case PARSE_BUDGET_INPUT_BYTES:
        return "input-bytes";
    case PARSE_BUDGET_TEXT_CHARACTERS:
        return "text-characters";
    case PARSE_BUDGET_XML_DEPTH:
        return "xml-depth";
    case PARSE_BUDGET_ARCHIVE_ENTRY_COUNT:
        return "archive-entry-count";
    case PARSE_BUDGET_ARCHIVE_ENTRY_BYTES:
        return "archive-entry-bytes";
    case PARSE_BUDGET_ARCHIVE_TOTAL_BYTES:
        return "archive-total-bytes";
    case PARSE_BUDGET_ARCHIVE_COMPRESSION_RATIO:
        return "archive-compression-ratio";
    case PARSE_BUDGET_ARCHIVE_PATH_DEPTH:
        return "archive-path-depth";
    case PARSE_BUDGET_CONTAINER_DEPTH:
        return "container-depth";
    case PARSE_BUDGET_ARCHIVE_PATH:
        return "archive-path";
    case PARSE_BUDGET_ARCHIVE_DUPLICATE_ENTRY:
        return "archive-duplicate-entry";
    case PARSE_BUDGET_IMAGE_DIMENSIONS:
        return "image-dimensions";
    }
    return "unknown";
}

struct ParseBudgetLimits {
    lUInt64 maxInputBytes;
    lUInt64 maxDecodedTextCharacters;
    unsigned maxXmlDepth;
    unsigned maxArchiveEntries;
    lUInt64 maxArchiveEntryBytes;
    lUInt64 maxArchiveTotalBytes;
    lUInt64 archiveRatioMinimumBytes;
    unsigned maxArchiveCompressionRatio;
    unsigned maxArchivePathDepth;
    unsigned maxContainerDepth;
    unsigned maxImageDimension;
    lUInt64 maxImagePixels;

    static ParseBudgetLimits defaults()
    {
        ParseBudgetLimits limits;
        limits.maxInputBytes = 512ULL * 1024ULL * 1024ULL;
        limits.maxDecodedTextCharacters = 128ULL * 1024ULL * 1024ULL;
        limits.maxXmlDepth = 256;
        limits.maxArchiveEntries = 10000;
        limits.maxArchiveEntryBytes = 256ULL * 1024ULL * 1024ULL;
        limits.maxArchiveTotalBytes = 1024ULL * 1024ULL * 1024ULL;
        limits.archiveRatioMinimumBytes = 1024ULL * 1024ULL;
        limits.maxArchiveCompressionRatio = 200;
        limits.maxArchivePathDepth = 64;
        limits.maxContainerDepth = 4;
        limits.maxImageDimension = 16384;
        limits.maxImagePixels = 64ULL * 1024ULL * 1024ULL;
        return limits;
    }
};

/// Per-operation counters backed by one shared set of parsing limits.
class ParseBudget {
private:
    ParseBudgetLimits m_limits;
    ParseBudgetErrorCode m_error;
    lUInt64 m_textCharacters;
    unsigned m_xmlDepth;
    unsigned m_archiveEntries;
    lUInt64 m_archiveBytes;

    bool fail(ParseBudgetErrorCode error)
    {
        if (m_error == PARSE_BUDGET_OK)
            m_error = error;
        return false;
    }

public:
    explicit ParseBudget(
            const ParseBudgetLimits &limits = ParseBudgetLimits::defaults())
        : m_limits(limits)
    {
        reset();
    }

    void reset()
    {
        m_error = PARSE_BUDGET_OK;
        m_textCharacters = 0;
        m_xmlDepth = 0;
        m_archiveEntries = 0;
        m_archiveBytes = 0;
    }

    const ParseBudgetLimits &limits() const
    {
        return m_limits;
    }

    ParseBudgetErrorCode error() const
    {
        return m_error;
    }

    bool failed() const
    {
        return m_error != PARSE_BUDGET_OK;
    }

    bool reject(ParseBudgetErrorCode error)
    {
        return fail(error);
    }

    bool checkInputBytes(lUInt64 bytes)
    {
        return bytes <= m_limits.maxInputBytes
                || fail(PARSE_BUDGET_INPUT_BYTES);
    }

    bool consumeTextCharacters(lUInt64 characters)
    {
        if (characters > m_limits.maxDecodedTextCharacters - m_textCharacters)
            return fail(PARSE_BUDGET_TEXT_CHARACTERS);
        m_textCharacters += characters;
        return true;
    }

    bool enterXmlElement()
    {
        if (m_xmlDepth >= m_limits.maxXmlDepth)
            return fail(PARSE_BUDGET_XML_DEPTH);
        ++m_xmlDepth;
        return true;
    }

    bool checkXmlDepth(unsigned depth)
    {
        return depth <= m_limits.maxXmlDepth
                || fail(PARSE_BUDGET_XML_DEPTH);
    }

    void leaveXmlElement()
    {
        if (m_xmlDepth > 0)
            --m_xmlDepth;
    }

    bool consumeArchiveEntry(lUInt64 unpackedBytes)
    {
        if (m_archiveEntries >= m_limits.maxArchiveEntries)
            return fail(PARSE_BUDGET_ARCHIVE_ENTRY_COUNT);
        if (unpackedBytes > m_limits.maxArchiveEntryBytes)
            return fail(PARSE_BUDGET_ARCHIVE_ENTRY_BYTES);
        if (unpackedBytes > m_limits.maxArchiveTotalBytes - m_archiveBytes)
            return fail(PARSE_BUDGET_ARCHIVE_TOTAL_BYTES);
        ++m_archiveEntries;
        m_archiveBytes += unpackedBytes;
        return true;
    }

    bool checkArchiveCompression(lUInt64 packedBytes, lUInt64 unpackedBytes)
    {
        if (unpackedBytes <= m_limits.archiveRatioMinimumBytes)
            return true;
        if (packedBytes == 0)
            return fail(PARSE_BUDGET_ARCHIVE_COMPRESSION_RATIO);
        const lUInt64 ratio = m_limits.maxArchiveCompressionRatio;
        if (ratio == 0)
            return fail(PARSE_BUDGET_ARCHIVE_COMPRESSION_RATIO);
        if (packedBytes > (~static_cast<lUInt64>(0)) / ratio)
            return true;
        return unpackedBytes <= packedBytes * ratio
                || fail(PARSE_BUDGET_ARCHIVE_COMPRESSION_RATIO);
    }

    bool checkArchivePathDepth(unsigned depth)
    {
        return depth <= m_limits.maxArchivePathDepth
                || fail(PARSE_BUDGET_ARCHIVE_PATH_DEPTH);
    }

    bool checkContainerDepth(unsigned depth)
    {
        return depth <= m_limits.maxContainerDepth
                || fail(PARSE_BUDGET_CONTAINER_DEPTH);
    }

    bool checkImageDimensions(unsigned width, unsigned height)
    {
        if (width == 0 || height == 0
                || width > m_limits.maxImageDimension
                || height > m_limits.maxImageDimension)
            return fail(PARSE_BUDGET_IMAGE_DIMENSIONS);
        return static_cast<lUInt64>(width) * static_cast<lUInt64>(height)
                        <= m_limits.maxImagePixels
                || fail(PARSE_BUDGET_IMAGE_DIMENSIONS);
    }
};

#endif // __PARSEBUDGET_H_INCLUDED__
