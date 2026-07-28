/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2009,2012 Vadim Lopatin <coolreader.org@gmail.com> *
 *   Copyright (C) 2020 Aleksey Chernov <valexlin@gmail.com>               *
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

#include "lvdirectorycontainer.h"
#include "lvstreamutils.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>

#include <cerrno>
#include <memory>

namespace {

void addDirectoryItem(
        LVDirectoryContainer &directory,
        const lString32 &name,
        lvsize_t size,
        lUInt32 flags,
        bool isContainer)
{
    std::unique_ptr<LVDirectoryContainerItemInfo> item(
            new LVDirectoryContainerItemInfo());
    item->SetItemInfo(name, size, flags, isContainer);
    directory.Add(item.release());
}

#if !defined(__SYMBIAN32__) && defined(_WIN32)
class ScopedFindHandle
{
    HANDLE _handle;
public:
    explicit ScopedFindHandle(HANDLE handle)
        : _handle(handle)
    {
    }

    ~ScopedFindHandle()
    {
        if (_handle != NULL && _handle != INVALID_HANDLE_VALUE)
            FindClose(_handle);
    }

    ScopedFindHandle(const ScopedFindHandle &) = delete;
    ScopedFindHandle &operator=(const ScopedFindHandle &) = delete;

    HANDLE get() const { return _handle; }
};
#else
struct DirectoryCloser
{
    void operator()(DIR *directory) const
    {
        if (directory)
            closedir(directory);
    }
};

using ScopedDirectory = std::unique_ptr<DIR, DirectoryCloser>;
#endif

} // namespace

LVStreamRef LVDirectoryContainer::OpenStream(const char32_t *fname, lvopen_mode_t mode)
{
    int found_index = -1;
    for (int i=0; i<m_list.length(); i++) {
        if ( !lStr_cmp( fname, m_list[i]->GetName() ) ) {
            if ( m_list[i]->IsContainer() ) {
                // found directory with same name!!!
                return LVStreamRef();
            }
            found_index = i;
            break;
        }
    }
    // make filename
    lString32 fn = m_fname;
    fn << fname;
    //const char * fb8 = UnicodeToUtf8( fn ).c_str();
    //printf("Opening directory container file %s : %s fname=%s path=%s\n", UnicodeToUtf8( lString32(fname) ).c_str(), UnicodeToUtf8( fn ).c_str(), UnicodeToUtf8( m_fname ).c_str(), UnicodeToUtf8( m_path ).c_str());
    LVStreamRef stream( LVOpenFileStream( fn.c_str(), mode ) );
    if (!stream) {
        return stream;
    }
    //stream->m_parent = this;
    if (found_index<0) {
        lvsize_t size = 0;
        stream->GetSize(&size);
        addDirectoryItem(*this, lString32(fname), size, 0, false);
    }
    return stream;
}

lverror_t LVDirectoryContainer::GetSize(lvsize_t *pSize)
{
    if (m_fname.empty())
        return LVERR_FAIL;
    *pSize = GetObjectCount();
    return LVERR_OK;
}

LVDirectoryContainer::LVDirectoryContainer() : m_parent(NULL)
{
}

LVDirectoryContainer::~LVDirectoryContainer()
{
    SetName(NULL);
    Clear();
}

std::unique_ptr<LVDirectoryContainer> LVDirectoryContainer::OpenDirectory(
        const char32_t *path, const char32_t *mask)
{
    if (!path || !path[0])
        return std::unique_ptr<LVDirectoryContainer>();
    
    
    // container object
    std::unique_ptr<LVDirectoryContainer> dir(new LVDirectoryContainer());
    
    // make filename
    lString32 fn( path );
    lChar32 lastch = 0;
    if ( !fn.empty() )
        lastch = fn[fn.length()-1];
    if ( lastch!='\\' && lastch!='/' )
        fn << dir->m_path_separator;
    
    dir->SetName(fn.c_str());
    
#if !defined(__SYMBIAN32__) && defined(_WIN32)
    // WIN32 API
    fn << (mask ? mask : U"*.*");
    WIN32_FIND_DATAW data = { 0 };
    WIN32_FIND_DATAA dataa = { 0 };
    //lString8 bs = DOMString(path).ToAnsiString();
    lString16 fn16 = UnicodeToUtf16(fn);
    HANDLE hFind = FindFirstFileW(fn16.c_str(), &data);
    bool unicode=true;
    if (hFind == INVALID_HANDLE_VALUE || !hFind) {
        lUInt32 err=GetLastError();
        if (err == ERROR_CALL_NOT_IMPLEMENTED) {
            hFind = FindFirstFileA(UnicodeToLocal(fn).c_str(), &dataa);
            unicode=false;
            if (hFind == INVALID_HANDLE_VALUE || !hFind)
                return std::unique_ptr<LVDirectoryContainer>();
        } else {
            return std::unique_ptr<LVDirectoryContainer>();
        }
    }
    ScopedFindHandle findHandle(hFind);
    
    if (unicode) {
        // unicode
        for (;;) {
            lUInt32 dwAttrs = data.dwFileAttributes;
            wchar_t * pfn = data.cFileName;
            for (int i=0; data.cFileName[i]; i++) {
                if (data.cFileName[i]=='/' || data.cFileName[i]=='\\')
                    pfn = data.cFileName + i + 1;
            }
            
            if ( (dwAttrs & FILE_ATTRIBUTE_DIRECTORY) ) {
                // directory
                if (!lStr_cmp(pfn, L"..") || !lStr_cmp(pfn, L".")) {
                    // .. or .
                } else {
                    // normal directory
                    addDirectoryItem(
                            *dir, Utf16ToUnicode(pfn), 0,
                            data.dwFileAttributes, true);
                }
            } else {
                // file
                lvsize_t size = data.nFileSizeLow;
#if LVLONG_FILE_SUPPORT
                size |= static_cast<lvsize_t>(data.nFileSizeHigh) << 32;
#endif
                addDirectoryItem(
                        *dir, Utf16ToUnicode(pfn), size,
                        data.dwFileAttributes, false);
            }
            
            if (!FindNextFileW(findHandle.get(), &data)) {
                if (GetLastError() != ERROR_NO_MORE_FILES)
                    return std::unique_ptr<LVDirectoryContainer>();
                break;
            }
            
        }
    } else {
        // ANSI
        for (;;) {
            lUInt32 dwAttrs = dataa.dwFileAttributes;
            char * pfn = dataa.cFileName;
            for (int i=0; dataa.cFileName[i]; i++) {
                if (dataa.cFileName[i]=='/' || dataa.cFileName[i]=='\\')
                    pfn = dataa.cFileName + i + 1;
            }
            
            if ( (dwAttrs & FILE_ATTRIBUTE_DIRECTORY) ) {
                // directory
                if (!strcmp(pfn, "..") || !strcmp(pfn, ".")) {
                    // .. or .
                } else {
                    // normal directory
                    addDirectoryItem(
                            *dir, LocalToUnicode(lString8(pfn)), 0,
                            dataa.dwFileAttributes, true);
                }
            } else {
                // file
                lvsize_t size = dataa.nFileSizeLow;
#if LVLONG_FILE_SUPPORT
                size |= static_cast<lvsize_t>(dataa.nFileSizeHigh) << 32;
#endif
                addDirectoryItem(
                        *dir, LocalToUnicode(lString8(pfn)), size,
                        dataa.dwFileAttributes, false);
            }
            
            if (!FindNextFileA(findHandle.get(), &dataa)) {
                if (GetLastError() != ERROR_NO_MORE_FILES)
                    return std::unique_ptr<LVDirectoryContainer>();
                break;
            }
            
        }
    }
#else
    // POSIX
    CR_UNUSED(mask);
    lString32 p( fn );
    p.erase( p.length()-1, 1 );
    lString8 p8 = UnicodeToLocal( p );
    if ( p8.empty() )
        p8 = ".";
    const char * p8s = p8.c_str();
    ScopedDirectory directory(opendir(p8s));
    if (!directory)
        return std::unique_ptr<LVDirectoryContainer>();
    for (;;) {
        errno = 0;
        struct dirent *pde = readdir(directory.get());
        if (!pde) {
            if (errno != 0)
                return std::unique_ptr<LVDirectoryContainer>();
            break;
        }
        lString8 fpath = p8 + "/" + pde->d_name;
        struct stat st;
        if (stat(fpath.c_str(), &st) != 0)
            continue;
        if (S_ISDIR(st.st_mode)) {
            if (strcmp(pde->d_name, ".")
                    && strcmp(pde->d_name, "..")) {
                addDirectoryItem(
                        *dir,
                        LocalToUnicode(lString8(pde->d_name)),
                        0,
                        static_cast<lUInt32>(st.st_mode),
                        true);
            }
        } else if (S_ISREG(st.st_mode)) {
            addDirectoryItem(
                    *dir,
                    LocalToUnicode(lString8(pde->d_name)),
                    static_cast<lvsize_t>(st.st_size),
                    static_cast<lUInt32>(st.st_mode),
                    false);
        }
    }
#endif
    return dir;
}
