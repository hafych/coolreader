/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2013 Vadim Lopatin <coolreader.org@gmail.com>           *
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

#ifndef LVQUEUE_H_INCLUDED
#define LVQUEUE_H_INCLUDED

#include <climits>
#include <list>
#include <stdexcept>
#include <utility>

template < typename T >
class LVQueue {
    typedef std::list<T> Storage;
    Storage items;

    void ensureRoom() const {
        if (items.size() >= static_cast<size_t>(INT_MAX))
            throw std::length_error("LVQueue size overflow");
    }

public:
    struct Iterator {
    private:
        LVQueue * queue;
        typename Storage::iterator current;
        bool valid;
    public:
        Iterator(const Iterator &) = default;
        explicit Iterator(LVQueue * _queue)
            : queue(_queue), current(_queue->items.end()), valid(false) {
        }
        T get() { return valid ? *current : T(); }
        void set(T value) {
            if (valid)
                *current = std::move(value);
        }
        bool next() {
            if (!valid)
                current = queue->items.begin();
            else
                ++current;
            valid = current != queue->items.end();
            return valid;
        }
        T remove() {
            if (!valid)
                return T();
            T res = std::move(*current);
            current = queue->items.erase(current);
            valid = current != queue->items.end();
            return res;
        }
        void moveToHead() {
            if (valid && current != queue->items.begin())
                queue->items.splice(queue->items.begin(),
                        queue->items, current);
        }
    };

    Iterator iterator() { return Iterator(this); }
    LVQueue() = default;
    LVQueue(const LVQueue &) = delete;
    LVQueue & operator=(const LVQueue &) = delete;
    LVQueue(LVQueue &&) = default;
    LVQueue & operator=(LVQueue &&) = default;
    ~LVQueue() = default;

    int length() const { return static_cast<int>(items.size()); }
    void pushBack(T item) {
        ensureRoom();
        items.push_back(std::move(item));
    }
    void pushFront(T item) {
        ensureRoom();
        items.push_front(std::move(item));
    }
    T popFront() {
        if (items.empty())
            return T();
        T res = std::move(items.front());
        items.pop_front();
        return res;
    }
    T popBack() {
        if (items.empty())
            return T();
        T res = std::move(items.back());
        items.pop_back();
        return res;
    }
    void clear() { items.clear(); }
};



#endif // LVQUEUE_H_INCLUDED
