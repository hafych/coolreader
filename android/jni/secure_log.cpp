/*
 * CoolReader Android native logging.
 */

#include "secure_log.h"

#include "logredactor.h"

#include <android/log.h>
#include <cstdio>
#include <string>

namespace {

const std::size_t MAX_FORMATTED_LOG_BYTES = 4096;

std::string formatMessage(const char *format, va_list args)
{
    if (!format)
        return std::string();
    char buffer[MAX_FORMATTED_LOG_BYTES + 1];
    std::vsnprintf(buffer, sizeof(buffer), format, args);
    buffer[MAX_FORMATTED_LOG_BYTES] = '\0';
    return CRRedactLogMessage(buffer);
}

} // namespace

int cr3_secure_log_write(int priority, const char *tag, const char *message)
{
    std::string safe = CRRedactLogMessage(message);
    return __android_log_write(priority, tag ? tag : "cr3eng",
                               safe.c_str());
}

int cr3_secure_log_print(int priority, const char *tag,
                         const char *format, ...)
{
    va_list args;
    va_start(args, format);
    std::string safe = formatMessage(format, args);
    va_end(args);
    return __android_log_write(priority, tag ? tag : "cr3eng",
                               safe.c_str());
}

void cr3_secure_log_assert(const char *condition, const char *tag,
                           const char *format, ...)
{
    va_list args;
    va_start(args, format);
    std::string safe = formatMessage(format, args);
    va_end(args);
    __android_log_assert(condition ? condition : "native assertion",
                         tag ? tag : "cr3eng", "%s", safe.c_str());
}
