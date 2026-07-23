/*
 * CoolReader Engine
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

#include "logredactor.h"

#include <algorithm>
#include <cctype>
#include <cstring>

namespace {

const char * const REDACTED_NATIVE_DIAGNOSTIC =
        "[redacted native diagnostic]";
const std::size_t MAX_NATIVE_LOG_BYTES = 4000;

bool isWordCharacter(char c)
{
    unsigned char value = static_cast<unsigned char>(c);
    return std::isalnum(value) || c == '_';
}

bool containsWord(const std::string &text, const char *word)
{
    std::size_t length = std::strlen(word);
    std::size_t position = 0;
    while ((position = text.find(word, position)) != std::string::npos) {
        bool startsAtBoundary = position == 0
                || !isWordCharacter(text[position - 1]);
        std::size_t end = position + length;
        bool endsAtBoundary = end == text.length()
                || !isWordCharacter(text[end]);
        if (startsAtBoundary && endsAtBoundary)
            return true;
        position = end;
    }
    return false;
}

bool containsSensitiveData(const std::string &text)
{
    if (text.find('/') != std::string::npos
            || text.find('\\') != std::string::npos
            || text.find('?') != std::string::npos
            || text.find('#') != std::string::npos)
        return true;

    static const char * const sensitiveWords[] = {
        "password", "passwd", "pwd", "token", "authorization", "auth",
        "access_token", "access-token", "refresh_token", "refresh-token",
        "api_key", "api-key", "apikey", "secret", "session", "cookie",
        "bearer", "basic"
    };
    for (const char *word : sensitiveWords) {
        if (containsWord(text, word))
            return true;
    }

    static const char * const privateExtensions[] = {
        ".azw", ".azw3", ".chm", ".djvu", ".doc", ".docx", ".epub",
        ".fb2", ".fb3", ".html", ".mobi", ".odt", ".pdb", ".pdf",
        ".pml", ".prc", ".rtf", ".tcr", ".txt", ".xhtml", ".zip"
    };
    for (const char *extension : privateExtensions) {
        if (text.find(extension) != std::string::npos)
            return true;
    }
    return false;
}

} // namespace

std::string CRRedactLogMessage(const char *message)
{
    if (!message)
        return std::string();
    std::string safe(message);
    std::transform(safe.begin(), safe.end(), safe.begin(),
            [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    if (containsSensitiveData(safe))
        return REDACTED_NATIVE_DIAGNOSTIC;

    safe.assign(message);
    for (char &c : safe) {
        unsigned char value = static_cast<unsigned char>(c);
        if (value < 0x20 || value == 0x7f)
            c = ' ';
    }
    if (safe.length() > MAX_NATIVE_LOG_BYTES) {
        safe.resize(MAX_NATIVE_LOG_BYTES);
        safe.append(" [truncated]");
    }
    return safe;
}
