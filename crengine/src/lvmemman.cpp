/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2009,2011-2015 Vadim Lopatin <coolreader.org@gmail.com>
 *   Copyright (C) 2020 poire-z <poire-z@users.noreply.github.com>         *
 *   Copyright (C) 2020,2021 Aleksey Chernov <valexlin@gmail.com>          *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or         *
 *   modify it under the terms of the GNU General Public License           *
 *   as published by the Free Software Foundation; either version 2        *
 *   of the License, or (at your option) any later version.                *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the Free Software           *
 *   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,            *
 *   MA 02110-1301, USA.                                                   *
 ***************************************************************************/

/**
 * \file lvmemman.cpp
 * \brief memory manager implementation
 */

#include <cstring>
#include <stdlib.h>
#include "../include/lvmemman.h"
#include "../include/lvref.h"
#include "../include/lvtinydom.h"
#include "../include/lvstreamutils.h"
#include "../include/crlog.h"

#ifdef _LINUX
#ifndef _XOPEN_SOURCE
#define _XOPEN_SOURCE
#endif
#include <signal.h>
#include <unistd.h>
#endif

#ifdef _DEBUG
#include <stdexcept>
#include <string>
#endif

static char file_to_remove_on_crash[2048] = "";

void crSetFileToRemoveOnFatalError(const char * filename) {
	strcpy(file_to_remove_on_crash, filename == NULL ? "" : filename); // NOLINT
}

#ifdef _LINUX
static struct sigaction old_sa[NSIG];

#ifdef ANDROID
//#define ANDROID_BACKTRACE
#endif

#ifdef ANDROID_BACKTRACE
#include <unwind.h>
#include <dlfcn.h>

namespace {

struct BacktraceState
{
    void** current;
    void** end;
};

static _Unwind_Reason_Code unwindCallback(struct _Unwind_Context* context, void* arg)
{
    BacktraceState* state = static_cast<BacktraceState*>(arg);
    uintptr_t pc = _Unwind_GetIP(context);
    if (pc) {
        if (state->current == state->end) {
            return _URC_END_OF_STACK;
        } else {
            *state->current++ = reinterpret_cast<void*>(pc);
        }
    }
    return _URC_NO_REASON;
}

}

size_t captureBacktrace(void** buffer, size_t max)
{
    BacktraceState state = {buffer, buffer + max};
    _Unwind_Backtrace(unwindCallback, &state);

    return state.current - buffer;
}

void dumpBacktrace(void** addrs, size_t count)
{
    for (size_t idx = 0; idx < count; ++idx) {
        const void* addr = addrs[idx];
        const char* symbol = "";

        Dl_info info;
        if (dladdr(addr, &info) && info.dli_sname) {
            symbol = info.dli_sname;
        }

        CRLog::trace("   # %02d : 0x%08x   %s", idx, addr, symbol);

    }
}

#endif // ANDROID_BACKTRACE



void cr_sigaction(int signal, siginfo_t *info, void *reserved)
{
    CR_UNUSED2(info, reserved);
	if (file_to_remove_on_crash[0])
		unlink(file_to_remove_on_crash);
	CRLog::error("cr_sigaction(%d)", signal);

#ifdef ANDROID_BACKTRACE
    void* buffer[50];
	dumpBacktrace(buffer, captureBacktrace(buffer, 50));
#endif

	old_sa[signal].sa_handler(signal);
}
#endif

static bool signals_are_set = false;
void crSetSignalHandler()
{
#ifdef _LINUX
	if (signals_are_set)
		return;
	signals_are_set = true;
	struct sigaction handler = {};
	handler.sa_sigaction = cr_sigaction;
	handler.sa_flags = SA_RESETHAND;
#define CATCHSIG(X) sigaction(X, &handler, &old_sa[X])
	CATCHSIG(SIGILL);
	CATCHSIG(SIGABRT);
	CATCHSIG(SIGBUS);
	CATCHSIG(SIGFPE);
	CATCHSIG(SIGSEGV);
//	CATCHSIG(SIGSTKFLT);
	CATCHSIG(SIGPIPE);
#endif
}

/// default fatal error handler: uses exit()
void lvDefFatalErrorHandler (int errorCode, const char * errorText )
{
    char strbuff[10];
    snprintf(strbuff, sizeof(strbuff), "%d", errorCode);
    fprintf( stderr, "FATAL ERROR #%s: %s\n", strbuff, errorText );
#ifdef _DEBUG
    std::string errstr = std::string("FATAL ERROR #") + std::string(strbuff) + std::string(": ") + std::string(errorText);
    throw std::runtime_error(errstr);
#endif
    exit( errorCode );
}

lv_FatalErrorHandler_t * lvFatalErrorHandler = &lvDefFatalErrorHandler;

void crFatalError( int code, const char * errorText )
{
	if (file_to_remove_on_crash[0])
		LVDeleteFile(Utf8ToUnicode(lString8(file_to_remove_on_crash)));
    lvFatalErrorHandler( code, errorText );
}

/// set fatal error handler
void crSetFatalErrorHandler( lv_FatalErrorHandler_t * handler )
{
    lvFatalErrorHandler = handler;
}

ref_count_rec_t ref_count_rec_t::null_ref(NULL);
ref_count_rec_t ref_count_rec_t::protected_null_ref(NULL);
std::atomic<bool>
        ref_count_rec_t::fail_next_allocation_for_regression(false);


#if (LDOM_USE_OWN_MEM_MAN==1)
namespace {

using ldomBlockStorageOwners = std::array<
        std::unique_ptr<ldomMemManStorage>,
        LOCAL_STORAGE_COUNT>;

ldomBlockStorageOwners &blockStorageOwners()
{
    static ldomBlockStorageOwners owners;
    return owners;
}

size_t blockSizeToStorageIndex(size_t byteCount)
{
    if (byteCount == 0)
        return 0;
    return (byteCount - 1) >> BLOCK_SIZE_GRANULARITY;
}

size_t storageIndexToBlockSize(size_t storageIndex)
{
    return (storageIndex + 1) << BLOCK_SIZE_GRANULARITY;
}

void clearBlockStorageOwners()
{
    ldomBlockStorageOwners &owners = blockStorageOwners();
    for (std::unique_ptr<ldomMemManStorage> &owner : owners)
        owner.reset();
}

} // namespace

ldomMemManStorage &ldomRefStorage()
{
    static ldomMemManStorage storage(sizeof(ref_count_rec_t));
    return storage;
}

void * ldomAlloc( size_t byteCount )
{
    const size_t storageIndex = blockSizeToStorageIndex(byteCount);
    if (storageIndex < LOCAL_STORAGE_COUNT)
    {
        std::unique_ptr<ldomMemManStorage> &storage =
                blockStorageOwners()[storageIndex];
        if (!storage)
            storage = std::make_unique<ldomMemManStorage>(
                    storageIndexToBlockSize(storageIndex));
        return storage->alloc();
    }
    return std::malloc(byteCount);
}

void ldomFree( void * p, size_t byteCount )
{
    if (p == NULL)
        return;
    const size_t storageIndex = blockSizeToStorageIndex(byteCount);
    if (storageIndex < LOCAL_STORAGE_COUNT)
    {
        std::unique_ptr<ldomMemManStorage> &storage =
                blockStorageOwners()[storageIndex];
        if (!storage)
            crFatalError();
        storage->free(static_cast<ldomMemBlock *>(p));
    }
    else
        std::free(p);
}

void ldomFreeStorage()
{
    clearBlockStorageOwners();
    ldomRefStorage().clear();
}

bool LVRunDomBlockStorageOwnershipRegression()
{
    clearBlockStorageOwners();
    try {
        ldomMemManStorage localStorage(5);
        if (localStorage.block_size < 5
                || localStorage.block_size % alignof(ldomMemBlock) != 0
                || localStorage.slice_count != 1)
            return false;

        std::array<void *, FIRST_SLICE_SIZE + 1> blocks;
        for (void *&block : blocks) {
            block = localStorage.alloc();
            if (block == NULL)
                return false;
            std::memset(block, 0xA5, 5);
        }
        if (localStorage.slice_count != 2)
            return false;
        for (void *block : blocks)
            localStorage.free(static_cast<ldomMemBlock *>(block));
        for (size_t i = 0; i < localStorage.slice_count; ++i) {
            if (localStorage.slices[i]->blocks_used != 0)
                return false;
        }

        void *reused = localStorage.alloc();
        if (reused != blocks.back())
            return false;
        localStorage.free(static_cast<ldomMemBlock *>(reused));

        localStorage.clear();
        localStorage.clear();
        if (localStorage.slice_count != 0)
            return false;
        void *reinitialized = localStorage.alloc();
        if (reinitialized == NULL || localStorage.slice_count != 1)
            return false;
        localStorage.free(static_cast<ldomMemBlock *>(reinitialized));

        const std::array<size_t, 11> requestSizes = {
            0, 1, 4, 5, 8, 9, 60, 61, 64, 65, 128
        };
        for (size_t requestSize : requestSizes) {
            void *allocation = ldomAlloc(requestSize);
            if (allocation == NULL)
                return false;
            if (requestSize != 0)
                std::memset(allocation, 0x5A, requestSize);
            ldomFree(allocation, requestSize);
        }

        void *first = ldomAlloc(5);
        if (first == NULL)
            return false;
        ldomFree(first, 5);
        void *second = ldomAlloc(5);
        if (second != first)
            return false;
        ldomFree(second, 5);

        clearBlockStorageOwners();
        void *afterClear = ldomAlloc(64);
        if (afterClear == NULL)
            return false;
        std::memset(afterClear, 0x3C, 64);
        ldomFree(afterClear, 64);
        clearBlockStorageOwners();
        return true;
    } catch (const std::bad_alloc &) {
        clearBlockStorageOwners();
        return false;
    }
}
#endif
