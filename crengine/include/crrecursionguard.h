#ifndef __CR_RECURSION_GUARD_H_INCLUDED__
#define __CR_RECURSION_GUARD_H_INCLUDED__

/// Bounds recursive work independently on every calling thread.
class CRThreadLocalRecursionLimit {
private:
    static int & depth() {
        static thread_local int value = 0;
        return value;
    }

public:
    CRThreadLocalRecursionLimit() {
        ++depth();
    }

    ~CRThreadLocalRecursionLimit() {
        --depth();
    }

    bool test(int limit = 15) const {
        return depth() < limit;
    }

    CRThreadLocalRecursionLimit(
            const CRThreadLocalRecursionLimit &) = delete;
    CRThreadLocalRecursionLimit & operator=(
            const CRThreadLocalRecursionLimit &) = delete;
};

#endif
