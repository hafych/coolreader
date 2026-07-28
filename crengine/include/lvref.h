/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2013 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2018 poire-z <poire-z@users.noreply.github.com>         *
 *   Copyright (C) 2018,2021 Aleksey Chernov <valexlin@gmail.com>          *
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
 * \file lvref.h
 * \brief smart pointer with reference counting template
 */

#ifndef __LVREF_H_INCLUDED__
#define __LVREF_H_INCLUDED__

#include "lvtypes.h"
#include "lvmemman.h"
#include "crlocks.h"
#include "lvautoptr.h"

#include <atomic>
#include <climits>
#include <memory>
#include <stdexcept>
#include <utility>

/// Reference counter structure
/**
    For internal usage in LVRef<> class
*/
class ref_count_rec_t {
public:
    int _refcount;
    void * _obj;
    static ref_count_rec_t null_ref;
    static ref_count_rec_t protected_null_ref;
    static std::atomic<bool> fail_next_allocation_for_regression;

    ref_count_rec_t( void * obj ) : _refcount(1), _obj(obj) { }
    static void failNextAllocationForRegression()
    {
        fail_next_allocation_for_regression.store(
                true, std::memory_order_release);
    }
    void * operator new( size_t size )
    {
        if (fail_next_allocation_for_regression.exchange(
                    false, std::memory_order_acq_rel))
            throw std::bad_alloc();
#if (LDOM_USE_OWN_MEM_MAN==1)
        (void)size;
        return ldomRefStorage().alloc();
#else
        return ::operator new(size);
#endif
    }
    void operator delete( void * p )
    {
#if (LDOM_USE_OWN_MEM_MAN==1)
        ldomRefStorage().free((ldomMemBlock *)p);
#else
        ::operator delete(p);
#endif
    }
};

/// sample ref counter implementation for LVFastRef
class LVRefCounter
{
    int refCount;
public:
    LVRefCounter() : refCount(0) { }
    void AddRef() { refCount++; }
    int Release() { return --refCount; }
    int getRefCount() { return refCount; }
};

/// Fast smart pointer with reference counting
/**
    Stores pointer to object and reference counter.
    Imitates usual pointer behavior, but deletes object 
    when there are no more references on it.
    On copy, increases reference counter.
    On destroy, decreases reference counter; deletes object if counter became 0.
    T should implement AddRef() and Release() methods.
    \param T class of stored object
 */
template <class T> class LVFastRef
{
private:
    T * _ptr;
    inline void Release()
    {
        if ( _ptr ) {
            if ( _ptr->Release()==0 ) {
                delete _ptr;
            }
            _ptr=NULL;
        }
    }
public:
    /// Default constructor.
    /** Initializes pointer to NULL */
    LVFastRef() : _ptr(NULL) { }

    /// Constructor by object pointer.
    /** Initializes pointer to given value 
    \param ptr is a pointer to object
     */
    explicit LVFastRef( T * ptr ) {
        _ptr = ptr;
        if ( _ptr )
            _ptr->AddRef();
    }

    /// Copy constructor.
    /** Creates copy of object pointer. Increments reference counter instead of real copy.
    \param ref is reference to copy
     */
    LVFastRef( const LVFastRef & ref )
    {
        _ptr = ref._ptr;
        if ( _ptr )
            _ptr->AddRef();
    }

    /// Destructor.
    /** Decrements reference counter; deletes object if counter became 0. */
    ~LVFastRef() { Release(); }

    /// Clears pointer.
    /** Sets object pointer to NULL. */
    void Clear() { Release(); }

    /// Copy operator.
    /** Duplicates a pointer from specified reference. 
    Increments counter instead of copying of object. 
    \param ref is reference to copy
     */
    LVFastRef & operator = ( const LVFastRef & ref )
    {
        if ( _ptr ) {
            if ( _ptr==ref._ptr )
                return *this;
            Release();
        }
        if ( ref._ptr )
            (_ptr = ref._ptr)->AddRef();
        return *this;
    }

    /// Object pointer assignment operator.
    /** Sets object pointer to the specified value. 
    Reference counter is being initialized to 1.
    \param obj pointer to object
     */
    LVFastRef & operator = ( T * obj )
    {
        if ( _ptr ) {
            if ( _ptr==obj )
                return *this;
            Release();
        }
        if ( obj )
            (_ptr = obj)->AddRef();
        return *this;
    }

    /// Returns stored pointer to object.
    /** Imitates usual pointer behavior. 
    Usual way to access object fields. 
     */
    T * operator -> () const { return _ptr; }

    /// Dereferences pointer to object.
    /** Imitates usual pointer behavior. */
    T & operator * () const { return *_ptr; }

    /// To check reference counter value.
    /** It might be useful in some cases. 
    \return reference counter value.
     */
    int getRefCount() const { return _ptr->getRefCount(); }

    /// Returns stored pointer to object.
    /** Usual way to get pointer value. 
    \return stored pointer to object.
     */
    T * get() const { return _ptr; }

    /// Checks whether pointer is NULL or not.
    /** \return true if pointer is NULL.
    \sa isNull() */
    bool operator ! () const { return !_ptr; }

    /// Checks whether pointer is NULL or not.
    /** \return true if pointer is NULL. 
    \sa operator !()
     */
    bool isNull() const { return (_ptr == NULL); }
};

/// Fast smart pointer with reference counting and protection by mutex
/**
    Stores pointer to object and reference counter.
    Imitates usual pointer behavior, but deletes object
    when there are no more references on it.
    On copy, increases reference counter.
    On destroy, decreases reference counter; deletes object if counter became 0.
    T should implement AddRef() and Release() methods.
    \param T class of stored object
 */
template <class T> class LVProtectedFastRef
{
private:
    T * _ptr;
    inline T * Release()
    {
        T * res = NULL;
        if ( _ptr ) {
            if ( _ptr->Release()==0 ) {
                res = _ptr;
            }
            _ptr=NULL;
        }
        return res;
    }
public:
    /// Default constructor.
    /** Initializes pointer to NULL */
    LVProtectedFastRef() : _ptr(NULL) { }

    /// Constructor by object pointer.
    /** Initializes pointer to given value
    \param ptr is a pointer to object
     */
    explicit LVProtectedFastRef( T * ptr ) {
        REF_GUARD
        _ptr = ptr;
        if ( _ptr )
            _ptr->AddRef();
    }

    /// Copy constructor.
    /** Creates copy of object pointer. Increments reference counter instead of real copy.
    \param ref is reference to copy
     */
    LVProtectedFastRef( const LVProtectedFastRef & ref )
    {
        REF_GUARD
        _ptr = ref._ptr;
        if ( _ptr )
            _ptr->AddRef();
    }

    /// Destructor.
    /** Decrements reference counter; deletes object if counter became 0. */
    ~LVProtectedFastRef() {
        T * removed = NULL;
        {
            REF_GUARD
            removed = Release();
        }
        if (removed)
            delete removed;
    }

    /// Clears pointer.
    /** Sets object pointer to NULL. */
    void Clear() {
        T * removed = NULL;
        {
            REF_GUARD
            removed = Release();
        }
        if (removed)
            delete removed;
    }

    /// Copy operator.
    /** Duplicates a pointer from specified reference.
    Increments counter instead of copying of object.
    \param ref is reference to copy
     */
    LVProtectedFastRef & operator = ( const LVProtectedFastRef & ref )
    {
        T * removed = NULL;
        {
            REF_GUARD
            if ( _ptr ) {
                if ( _ptr==ref._ptr )
                    return *this;
                removed = Release();
            }
            if ( ref._ptr )
                (_ptr = ref._ptr)->AddRef();
        }
        if (removed)
            delete removed;
        return *this;
    }

    /// Object pointer assignment operator.
    /** Sets object pointer to the specified value.
    Reference counter is being initialized to 1.
    \param obj pointer to object
     */
    LVProtectedFastRef & operator = ( T * obj )
    {
        T * removed = NULL;
        {
            REF_GUARD
            if ( _ptr ) {
                if ( _ptr==obj )
                    return *this;
                removed = Release();
            }
            if ( obj )
                (_ptr = obj)->AddRef();
        }
        if (removed)
            delete removed;
        return *this;
    }

    /// Returns stored pointer to object.
    /** Imitates usual pointer behavior.
    Usual way to access object fields.
     */
    T * operator -> () const { return _ptr; }

    /// Dereferences pointer to object.
    /** Imitates usual pointer behavior. */
    T & operator * () const { return *_ptr; }

    /// To check reference counter value.
    /** It might be useful in some cases.
    \return reference counter value.
     */
    int getRefCount() const { return _ptr->getRefCount(); }

    /// Returns stored pointer to object.
    /** Usual way to get pointer value.
    \return stored pointer to object.
     */
    T * get() const { return _ptr; }

    /// Checks whether pointer is NULL or not.
    /** \return true if pointer is NULL.
    \sa isNull() */
    bool operator ! () const { return !_ptr; }

    /// Checks whether pointer is NULL or not.
    /** \return true if pointer is NULL.
    \sa operator !()
     */
    bool isNull() const { return (_ptr == NULL); }
};



/// Smart pointer with reference counting
/**
    Stores pointer to object and reference counter.
    Imitates usual pointer behavior, but deletes object 
    when there are no more references on it.
    On copy, increases reference counter
    On destroy, decreases reference counter; deletes object if counter became 0.
    Separate counter object is used, so no counter support is required for T.
    \param T class of stored object
*/
template <class T> class LVRef
{
private:
    ref_count_rec_t * _ptr;
    //========================================
    ref_count_rec_t * AddRef() const { ++_ptr->_refcount; return _ptr; }
    //========================================
    void Release()
    { 
        if (--_ptr->_refcount == 0)
        {
            if (_ptr != &ref_count_rec_t::null_ref)
            {
                if ( _ptr->_obj )
                    delete (reinterpret_cast<T*>(_ptr->_obj));
                delete _ptr;
            }
        }
    }
    //========================================
public:

	/// creates reference to copy
	LVRef clone() const
	{
		if ( isNull() )
			return LVRef(NULL);
		return LVRef(new T(*get()));
	}

    /// Default constructor.
    /** Initializes pointer to NULL */
    LVRef() : _ptr(&ref_count_rec_t::null_ref) { ref_count_rec_t::null_ref._refcount++; }

    /// Constructor by object pointer.
    /** Initializes pointer to given value 
        \param ptr is a pointer to object
    */
    explicit LVRef( T * ptr ) {
        if (ptr)
        {
            std::unique_ptr<T> candidate(ptr);
            _ptr = new ref_count_rec_t(ptr);
            candidate.release();
        }
        else
        {
            ref_count_rec_t::null_ref._refcount++;
            _ptr = &ref_count_rec_t::null_ref;
        }
    }

    /// Copy constructor.
    /** Creates copy of object pointer. Increments reference counter instead of real copy.
        \param ref is reference to copy
    */
    LVRef( const LVRef & ref ) { _ptr = ref.AddRef(); }

    /// Destructor.
    /** Decrements reference counter; deletes object if counter became 0. */
    ~LVRef() { Release(); }

    /// Clears pointer.
    /** Sets object pointer to NULL. */
    void Clear() { Release(); _ptr = &ref_count_rec_t::null_ref; ++_ptr->_refcount; }

    /// Copy operator.
    /** Duplicates a pointer from specified reference. 
        Increments counter instead of copying of object. 
        \param ref is reference to copy
    */
    LVRef & operator = ( const LVRef & ref )
    {
        if (!ref._ptr->_obj)
        {
            Clear();
        }
        else
        {
            if (_ptr!=ref._ptr)
            {
                Release();
                _ptr = ref.AddRef(); 
            }
        }
        return *this;
    }

    /// Object pointer assignment operator.
    /** Sets object pointer to the specified value. 
        Reference counter is being initialized to 1.
        \param obj pointer to object
    */
    LVRef & operator = ( T * obj )
    {
        if ( !obj )
        {
            Clear();
        }
        else
        {
            if (_ptr->_obj!=obj)
            {
                std::unique_ptr<T> candidate(obj);
                std::unique_ptr<ref_count_rec_t> record(
                        new ref_count_rec_t(obj));
                Release();
                candidate.release();
                _ptr = record.release();
            }
        }
        return *this;
    }

    /// Returns stored pointer to object.
    /** Imitates usual pointer behavior. 
        Usual way to access object fields. 
    */
    T * operator -> () const { return reinterpret_cast<T*>(_ptr->_obj); }

    /// Dereferences pointer to object.
    /** Imitates usual pointer behavior. */
    T & operator * () const { return *(reinterpret_cast<T*>(_ptr->_obj)); }

    /// To check reference counter value.
    /** It might be useful in some cases. 
        \return reference counter value.
    */
    int getRefCount() const { return _ptr->_refcount; }

    /// Returns stored pointer to object.
    /** Usual way to get pointer value. 
        \return stored pointer to object.
    */
    T * get() const { return reinterpret_cast<T*>(_ptr->_obj); }

    /// Checks whether pointer is NULL or not.
    /** \return true if pointer is NULL.
        \sa isNull() */
    bool operator ! () const { return !_ptr->_obj; }

    /// Checks whether pointer is NULL or not.
    /** \return true if pointer is NULL. 
        \sa operator !()
    */
    bool isNull() const { return _ptr->_obj == NULL; }
};

template <typename T >
class LVRefVec
{
    std::unique_ptr<LVRef<T>[]> _array;
    int _size;
    int _count;

    void swap(LVRefVec &vector)
    {
        _array.swap(vector._array);
        std::swap(_size, vector._size);
        std::swap(_count, vector._count);
    }

public:
    /// default constructor
    LVRefVec() : _array(), _size(0), _count(0) {}

    /// creates array of given size
    LVRefVec( int len, LVRef<T> value )
        : _array(), _size(0), _count(0)
    {
        if (len <= 0)
            return;
        std::unique_ptr<LVRef<T>[]> storage(new LVRef<T>[len]);
        for (int i = 0; i < len; ++i)
            storage[i] = value;
        _array = std::move(storage);
        _size = _count = len;
    }

    LVRefVec( const LVRefVec & v )
        : _array(), _size(0), _count(0)
    {
        if (v._count <= 0)
            return;
        std::unique_ptr<LVRef<T>[]> storage(
                new LVRef<T>[v._count]);
        for (int i = 0; i < v._count; ++i)
            storage[i] = v._array[i];
        _array = std::move(storage);
        _size = _count = v._count;
    }

    LVRefVec & operator = ( const LVRefVec & v )
    {
        if (this != &v) {
            LVRefVec copy(v);
            swap(copy);
        }
        return *this;
    }

    /// retrieves item from specified position
    LVRef<T> operator [] ( int pos ) const { return _array[pos]; }
    /// retrieves item reference from specified position
    LVRef<T> & operator [] ( int pos ) { return _array[pos]; }
    /// ensures that size of vector is not less than specified value
    void reserve( int size )
    {
        if (size <= _size)
            return;
        std::unique_ptr<LVRef<T>[]> storage(new LVRef<T>[size]);
        for (int i = 0; i < _count; ++i)
            storage[i] = _array[i];
        _array = std::move(storage);
        _size = size;
    }

    /// sets item by index (extends vector if necessary)
    void set( int index, LVRef<T> item )
    {
        if (index < 0)
            return;
        if (index == INT_MAX)
            throw std::length_error("LVRefVec index overflow");
        reserve(index + 1);
        _array[index] = item;
        if (index >= _count)
            _count = index + 1;
    }

    /// returns size of buffer
    int size() const { return _size; }
    /// returns number of items in vector
    int length() const { return _count; }
    /// returns true if there are no items in vector
    bool empty() const { return _count==0; }
    /// clears all items
    void clear()
    {
        _array.reset();
        _size = 0;
        _count = 0;
    }

    /// copies range to beginning of array
    void trim( int pos, int count, int reserved )
    {
        if (pos < 0 || count < 0 || reserved < 0
                || pos > _count || count > _count - pos)
            return;
        int new_sz = count;
        if (new_sz < reserved)
            new_sz = reserved;
        std::unique_ptr<LVRef<T>[]> storage;
        if (new_sz > 0) {
            storage.reset(new LVRef<T>[new_sz]);
            for (int i = 0; i < count; ++i)
                storage[i] = _array[pos + i];
        }
        _array = std::move(storage);
        _count = count;
        _size = new_sz;
    }

    /// removes several items from vector
    void erase( int pos, int count )
    {
        if (pos < 0 || count <= 0
                || pos > _count || count > _count - pos)
            return;
        const int oldCount = _count;
        for (int i = pos + count; i < oldCount; ++i)
            _array[i - count] = _array[i];
        _count -= count;
        for (int i = _count; i < oldCount; ++i)
            _array[i] = LVRef<T>();
    }

    /// adds new item to end of vector
    void add( LVRef<T> item )
    { 
        insert( -1, item );
    }

    void add( LVRefVec<T> & list )
    {
        append(list.ptr(), list.length());
    }

    /// adds new item to end of vector
    void append( const LVRef<T> * items, int count )
    {
        if (!items || count <= 0)
            return;
        if (_count > INT_MAX - count)
            throw std::length_error("LVRefVec append overflow");
        std::unique_ptr<LVRef<T>[]> snapshot(new LVRef<T>[count]);
        for (int i = 0; i < count; ++i)
            snapshot[i] = items[i];
        reserve(_count + count);
        for (int i = 0; i < count; ++i)
            _array[_count + i] = snapshot[i];
        _count += count;
    }
    
    LVRef<T> * addSpace( int count )
    {
        if (count <= 0)
            return _array ? _array.get() + _count : NULL;
        if (_count > INT_MAX - count)
            throw std::length_error("LVRefVec growth overflow");
        reserve(_count + count);
        LVRef<T> * ptr = _array.get() + _count;
        _count += count;
        return ptr;
    }
    
    /// inserts new item to specified position
    void insert( int pos, LVRef<T> item )
    {
        if (pos<0 || pos>_count)
            pos = _count;
        if (_count == INT_MAX)
            throw std::length_error("LVRefVec insertion overflow");
        if (_count >= _size) {
            const long long desiredSize =
                    static_cast<long long>(_count) * 3 / 2 + 8;
            int grownSize = desiredSize > INT_MAX
                    ? INT_MAX
                    : static_cast<int>(desiredSize);
            if (grownSize < _count + 1)
                grownSize = _count + 1;
            reserve(grownSize);
        }
        for (int i=_count; i>pos; --i)
            _array[i] = _array[i-1];
        _array[pos] = item;
        _count++;
    }
    /// returns array pointer
    LVRef<T> * ptr() { return _array.get(); }
    /// destructor
    ~LVRefVec() = default;
};

#if 0
template <class T>
class LVProtectedRef {
private:
    ref_count_rec_t * _ptr;
    //========================================
    ref_count_rec_t * AddRef() const { ++_ptr->_refcount; return _ptr; }
    //========================================
    void Release()
    {
        if (--_ptr->_refcount == 0)
        {
            if ( _ptr->_obj )
                delete (reinterpret_cast<T*>(_ptr->_obj));
            delete _ptr;
        }
    }
    //========================================
public:

    /// creates reference to copy
    LVProtectedRef & clone()
    {
        REF_GUARD
        if ( isNull() )
            return LVProtectedRef();
        return LVProtectedRef( new T( *_ptr ) );
    }

    /// Default constructor.
    /** Initializes pointer to NULL */
    LVProtectedRef() : _ptr(&ref_count_rec_t::protected_null_ref) {
        REF_GUARD
        ref_count_rec_t::protected_null_ref._refcount++;
    }

    /// Constructor by object pointer.
    /** Initializes pointer to given value
        \param ptr is a pointer to object
    */
    explicit LVProtectedRef( T * ptr ) {
        REF_GUARD
        if (ptr)
        {
            _ptr = new ref_count_rec_t(ptr);
        }
        else
        {
            ref_count_rec_t::protected_null_ref._refcount++;
            _ptr = &ref_count_rec_t::protected_null_ref;
        }
    }

    /// Copy constructor.
    /** Creates copy of object pointer. Increments reference counter instead of real copy.
        \param ref is reference to copy
    */
    LVProtectedRef( const LVProtectedRef & ref ) {
        REF_GUARD
        _ptr = ref.AddRef();
    }

    /// Destructor.
    /** Decrements reference counter; deletes object if counter became 0. */
    ~LVProtectedRef() {
        REF_GUARD
        Release();
    }

    /// Clears pointer.
    /** Sets object pointer to NULL. */
    void Clear() {
        REF_GUARD
        Release();
        _ptr = &ref_count_rec_t::protected_null_ref;
        ++_ptr->_refcount;
    }

    /// Copy operator.
    /** Duplicates a pointer from specified reference.
        Increments counter instead of copying of object.
        \param ref is reference to copy
    */
    LVProtectedRef & operator = ( const LVProtectedRef & ref )
    {
        REF_GUARD
        if (!ref._ptr->_obj)
        {
            Clear();
        }
        else
        {
            if (_ptr!=ref._ptr)
            {
                Release();
                _ptr = ref.AddRef();
            }
        }
        return *this;
    }

    /// Object pointer assignment operator.
    /** Sets object pointer to the specified value.
        Reference counter is being initialized to 1.
        \param obj pointer to object
    */
    LVProtectedRef & operator = ( T * obj )
    {
        REF_GUARD
        if ( !obj )
        {
            Clear();
        }
        else
        {
            if (_ptr->_obj!=obj)
            {
                Release();
                _ptr = new ref_count_rec_t(obj);
            }
        }
        return *this;
    }

    /// Returns stored pointer to object.
    /** Imitates usual pointer behavior.
        Usual way to access object fields.
    */
    T * operator -> () const { return reinterpret_cast<T*>(_ptr->_obj); }

    /// Dereferences pointer to object.
    /** Imitates usual pointer behavior. */
    T & operator * () const { return *(reinterpret_cast<T*>(_ptr->_obj)); }

    /// To check reference counter value.
    /** It might be useful in some cases.
        \return reference counter value.
    */
    int getRefCount() const { return _ptr->_refcount; }

    /// Returns stored pointer  to object.
    /** Usual way to get pointer value.
        \return stored pointer to object.
    */
    T * get() const { return reinterpret_cast<T*>(_ptr->_obj); }

    /// Checks whether pointer is NULL or not.
    /** \return true if pointer is NULL.
        \sa isNull() */
    bool operator ! () const { return !_ptr->_obj; }

    /// Checks whether pointer is NULL or not.
    /** \return true if pointer is NULL.
        \sa operator !()
    */
    bool isNull() const { return _ptr->_obj == NULL; }
};
#endif


#endif
