/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2013,2014 Vadim Lopatin <coolreader.org@gmail.com>      *
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

#include <stdlib.h>
#include <memory>
#include <mutex>
#include <utility>
#include "crconcurrent.h"
#include "lvptrvec.h"
#include "lvstring.h"
#include "crlog.h"

CRMutex * _refMutex = NULL;
CRMutex * _fontMutex = NULL;
CRMutex * _fontManMutex = NULL;
CRMutex * _fontGlyphCacheMutex = NULL;
CRMutex * _fontLocalGlyphCacheMutex = NULL;
CRMutex * _crengineMutex = NULL;

CRConcurrencyProvider * concurrencyProvider = NULL;

namespace {

class CRStdMutex : public CRMutex {
private:
    std::recursive_mutex m_mutex;

public:
    void acquire() override
    {
        m_mutex.lock();
    }

    void release() override
    {
        m_mutex.unlock();
    }
};

std::mutex g_concurrencyLifecycleMutex;
std::unique_ptr<CRMutex> g_refMutexOwner;
std::unique_ptr<CRMutex> g_fontMutexOwner;
std::unique_ptr<CRMutex> g_fontManMutexOwner;
std::unique_ptr<CRMutex> g_fontGlyphCacheMutexOwner;
std::unique_ptr<CRMutex> g_fontLocalGlyphCacheMutexOwner;
std::unique_ptr<CRMutex> g_crengineMutexOwner;
bool g_usingFallbackMutexes = false;

bool engineMutexesReady()
{
    return g_refMutexOwner
            && g_fontMutexOwner
            && g_fontManMutexOwner
            && g_fontGlyphCacheMutexOwner
            && g_fontLocalGlyphCacheMutexOwner
            && g_crengineMutexOwner;
}

void clearEngineMutexViews()
{
    _refMutex = NULL;
    _fontMutex = NULL;
    _fontManMutex = NULL;
    _fontGlyphCacheMutex = NULL;
    _fontLocalGlyphCacheMutex = NULL;
    _crengineMutex = NULL;
}

void publishEngineMutexViews()
{
    _refMutex = g_refMutexOwner.get();
    _fontMutex = g_fontMutexOwner.get();
    _fontManMutex = g_fontManMutexOwner.get();
    _fontGlyphCacheMutex = g_fontGlyphCacheMutexOwner.get();
    _fontLocalGlyphCacheMutex = g_fontLocalGlyphCacheMutexOwner.get();
    _crengineMutex = g_crengineMutexOwner.get();
}

void createFallbackEngineMutexes()
{
    g_refMutexOwner.reset(new CRStdMutex());
    g_fontMutexOwner.reset(new CRStdMutex());
    g_fontManMutexOwner.reset(new CRStdMutex());
    g_fontGlyphCacheMutexOwner.reset(new CRStdMutex());
    g_fontLocalGlyphCacheMutexOwner.reset(new CRStdMutex());
    g_crengineMutexOwner.reset(new CRStdMutex());
    g_usingFallbackMutexes = true;
    publishEngineMutexViews();
}

class EngineMutexBootstrap {
public:
    EngineMutexBootstrap()
    {
        createFallbackEngineMutexes();
    }

    ~EngineMutexBootstrap()
    {
        clearEngineMutexViews();
    }
};

EngineMutexBootstrap g_engineMutexBootstrap;

} // namespace

void CRSetupEngineConcurrency()
{
    CRConcurrencyProvider *provider = NULL;
    {
        std::lock_guard<std::mutex> guard(g_concurrencyLifecycleMutex);
        if (engineMutexesReady()
                && (!g_usingFallbackMutexes || !concurrencyProvider))
            return;
        provider = concurrencyProvider;
    }

    std::unique_ptr<CRMutex> refMutex(
            provider ? provider->createMutex() : new CRStdMutex());
    std::unique_ptr<CRMutex> fontMutex(
            provider ? provider->createMutex() : new CRStdMutex());
    std::unique_ptr<CRMutex> fontManMutex(
            provider ? provider->createMutex() : new CRStdMutex());
    std::unique_ptr<CRMutex> fontGlyphCacheMutex(
            provider ? provider->createMutex() : new CRStdMutex());
    std::unique_ptr<CRMutex> fontLocalGlyphCacheMutex(provider
            ? provider->createMutex() : new CRStdMutex());
    std::unique_ptr<CRMutex> crengineMutex(
            provider ? provider->createMutex() : new CRStdMutex());
    if (!refMutex || !fontMutex || !fontManMutex
            || !fontGlyphCacheMutex || !fontLocalGlyphCacheMutex
            || !crengineMutex) {
        CRLog::error(
                "CRSetupEngineConcurrency() : Cannot create engine mutexes");
        return;
    }

    std::lock_guard<std::mutex> guard(g_concurrencyLifecycleMutex);
    if (engineMutexesReady() && !g_usingFallbackMutexes)
        return;
    if (provider != concurrencyProvider) {
        CRLog::error(
                "CRSetupEngineConcurrency() : Provider changed during setup");
        return;
    }
    clearEngineMutexViews();
    g_refMutexOwner = std::move(refMutex);
    g_fontMutexOwner = std::move(fontMutex);
    g_fontManMutexOwner = std::move(fontManMutex);
    g_fontGlyphCacheMutexOwner = std::move(fontGlyphCacheMutex);
    g_fontLocalGlyphCacheMutexOwner =
            std::move(fontLocalGlyphCacheMutex);
    g_crengineMutexOwner = std::move(crengineMutex);
    g_usingFallbackMutexes = provider == NULL;
    publishEngineMutexViews();
}

void CRShutdownEngineConcurrency()
{
    std::unique_ptr<CRMutex> refMutex;
    std::unique_ptr<CRMutex> fontMutex;
    std::unique_ptr<CRMutex> fontManMutex;
    std::unique_ptr<CRMutex> fontGlyphCacheMutex;
    std::unique_ptr<CRMutex> fontLocalGlyphCacheMutex;
    std::unique_ptr<CRMutex> crengineMutex;
    {
        std::lock_guard<std::mutex> guard(g_concurrencyLifecycleMutex);
        clearEngineMutexViews();
        refMutex = std::move(g_refMutexOwner);
        fontMutex = std::move(g_fontMutexOwner);
        fontManMutex = std::move(g_fontManMutexOwner);
        fontGlyphCacheMutex = std::move(g_fontGlyphCacheMutexOwner);
        fontLocalGlyphCacheMutex =
                std::move(g_fontLocalGlyphCacheMutexOwner);
        crengineMutex = std::move(g_crengineMutexOwner);
        g_usingFallbackMutexes = false;
    }
    refMutex.reset();
    fontMutex.reset();
    fontManMutex.reset();
    fontGlyphCacheMutex.reset();
    fontLocalGlyphCacheMutex.reset();
    crengineMutex.reset();
}

CRThreadExecutor::CRThreadExecutor() : _stopped(false) {
    _monitor.reset(concurrencyProvider->createMonitor());
    _thread.reset(concurrencyProvider->createThread(this));
    _thread->start();
}

CRThreadExecutor::~CRThreadExecutor() {
    if (!_stopped.load(std::memory_order_acquire))
        stop();
}

void CRThreadExecutor::run() {
    CRLog::trace("Starting thread executor");
    for (;;) {
        if (_stopped.load(std::memory_order_acquire))
            break;
        std::unique_ptr<CRRunnable> task;
        {
            CRGuard guard(_monitor.get());
            CR_UNUSED(guard);
            if (_queue.length() == 0)
                _monitor->wait();
            if (_stopped.load(std::memory_order_acquire))
                break;
            task = _queue.popFront();
        }
        // process next event
        if (task) {
            task->run();
        }
    }
    CRLog::trace("Exiting thread executor");
}

void CRThreadExecutor::execute(CRRunnable * task) {
    std::unique_ptr<CRRunnable> ownedTask(task);
    CRGuard guard(_monitor.get());
    CR_UNUSED(guard);
    if (_stopped.load(std::memory_order_acquire)) {
        CRLog::error("Ignoring new task since executor is stopped");
        return;
    }
    _queue.pushBack(std::move(ownedTask));
    _monitor->notify();
}

void CRThreadExecutor::stop() {
    bool shouldJoin = false;
    {
        CRGuard guard(_monitor.get());
        CR_UNUSED(guard);
        if (!_stopped.exchange(true, std::memory_order_acq_rel)) {
            _queue.clear();
            _monitor->notify();
            shouldJoin = true;
        }
    }
    if (shouldJoin)
        _thread->join();
}
