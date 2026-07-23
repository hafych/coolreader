/*
 * CoolReader Engine
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

#ifndef CR_LOG_REDACTOR_H_INCLUDED
#define CR_LOG_REDACTOR_H_INCLUDED

#include <string>

/// Redacts a native diagnostic if it can contain a credential or user path.
std::string CRRedactLogMessage(const char *message);

#endif
