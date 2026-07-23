/*
 * CoolReader Android native logging.
 */

#ifndef CR3_SECURE_LOG_H
#define CR3_SECURE_LOG_H

#include <stdarg.h>

int cr3_secure_log_write(int priority, const char *tag, const char *message);
int cr3_secure_log_print(int priority, const char *tag,
                         const char *format, ...);
void cr3_secure_log_assert(const char *condition, const char *tag,
                           const char *format, ...);

#endif
