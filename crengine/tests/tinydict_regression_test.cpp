#include "tinydict.h"

#include <cstdio>
#include <cstring>
#include <memory>
#include <string>
#include <unistd.h>
#include <vector>

static int fail(const char *message)
{
    std::fprintf(stderr, "%s\n", message);
    return 1;
}

class TemporaryFile
{
    std::string path;

public:
    explicit TemporaryFile(const char *suffix = nullptr)
    {
        char candidate[] = "/tmp/coolreader-tinydict-XXXXXX";
        int descriptor = mkstemp(candidate);
        if (descriptor >= 0) {
            close(descriptor);
            path = candidate;
            if (suffix && *suffix) {
                std::string renamed = path + suffix;
                if (std::rename(path.c_str(), renamed.c_str()) != 0) {
                    unlink(path.c_str());
                    path.clear();
                } else {
                    path = renamed;
                }
            }
        }
    }

    ~TemporaryFile()
    {
        if (!path.empty())
            unlink(path.c_str());
    }

    bool valid() const
    {
        return !path.empty();
    }

    const char *c_str() const
    {
        return path.c_str();
    }
};

static bool writeFile(
        const char *path, const void *data, std::size_t size)
{
    FILE *file = std::fopen(path, "wb");
    if (!file)
        return false;
    bool written = std::fwrite(data, 1, size, file) == size;
    return std::fclose(file) == 0 && written;
}

static void appendU16(
        std::vector<unsigned char> &bytes, unsigned value)
{
    bytes.push_back(static_cast<unsigned char>(value));
    bytes.push_back(static_cast<unsigned char>(value >> 8));
}

static void appendU32(
        std::vector<unsigned char> &bytes, unsigned long value)
{
    bytes.push_back(static_cast<unsigned char>(value));
    bytes.push_back(static_cast<unsigned char>(value >> 8));
    bytes.push_back(static_cast<unsigned char>(value >> 16));
    bytes.push_back(static_cast<unsigned char>(value >> 24));
}

static bool compressChunks(
        const std::string &article, unsigned chunkLength,
        std::vector<std::vector<unsigned char> > &chunks,
        std::vector<unsigned char> &finalBytes)
{
    z_stream stream = {};
    if (deflateInit2(&stream, Z_DEFAULT_COMPRESSION, Z_DEFLATED,
                -15, 8, Z_DEFAULT_STRATEGY) != Z_OK)
        return false;

    bool complete = true;
    for (std::size_t start = 0; start < article.size();
            start += chunkLength) {
        std::size_t length = article.size() - start;
        if (length > chunkLength)
            length = chunkLength;
        std::vector<unsigned char> packed(
                static_cast<std::size_t>(compressBound(length)) + 16);
        stream.next_in = reinterpret_cast<Bytef *>(
                const_cast<char *>(article.data() + start));
        stream.avail_in = static_cast<uInt>(length);
        stream.next_out = packed.data();
        stream.avail_out = static_cast<uInt>(packed.size());
        int result = deflate(&stream, Z_FULL_FLUSH);
        if (result != Z_OK || stream.avail_in != 0) {
            complete = false;
            break;
        }
        packed.resize(packed.size() - stream.avail_out);
        if (packed.empty() || packed.size() > 0xffff) {
            complete = false;
            break;
        }
        chunks.push_back(packed);
    }

    finalBytes.resize(16);
    if (complete) {
        stream.next_in = nullptr;
        stream.avail_in = 0;
        stream.next_out = finalBytes.data();
        stream.avail_out = static_cast<uInt>(finalBytes.size());
        complete = deflate(&stream, Z_FINISH) == Z_STREAM_END;
        finalBytes.resize(finalBytes.size() - stream.avail_out);
    }
    bool ended = deflateEnd(&stream) == Z_OK;
    return complete && ended && !chunks.empty()
            && chunks.size() <= 0xffff && !finalBytes.empty();
}

static bool makeDictZip(
        const std::string &article, unsigned chunkLength,
        std::vector<unsigned char> &data)
{
    std::vector<std::vector<unsigned char> > chunks;
    std::vector<unsigned char> finalBytes;
    if (!compressChunks(
                article, chunkLength, chunks, finalBytes))
        return false;

    data = {
        0x1f, 0x8b, Z_DEFLATED, 0x04,
        0, 0, 0, 0, 0, 0xff
    };
    unsigned subfieldLength =
            6 + static_cast<unsigned>(chunks.size()) * 2;
    appendU16(data, subfieldLength + 4);
    data.push_back('R');
    data.push_back('A');
    appendU16(data, subfieldLength);
    appendU16(data, 1);
    appendU16(data, chunkLength);
    appendU16(data, static_cast<unsigned>(chunks.size()));
    for (const std::vector<unsigned char> &chunk : chunks)
        appendU16(data, static_cast<unsigned>(chunk.size()));
    for (const std::vector<unsigned char> &chunk : chunks)
        data.insert(data.end(), chunk.begin(), chunk.end());
    data.insert(data.end(), finalBytes.begin(), finalBytes.end());

    uLong checksum = crc32(
            0, reinterpret_cast<const Bytef *>(article.data()),
            static_cast<uInt>(article.size()));
    appendU32(data, checksum);
    appendU32(data, article.size());
    return true;
}

static std::string encodeIndexNumber(unsigned value)
{
    static const char digits[] =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    if (value == 0)
        return "A";
    std::string encoded;
    while (value) {
        encoded.insert(encoded.begin(), digits[value & 63]);
        value >>= 6;
    }
    return encoded;
}

static int testCompressedChunkOwnership()
{
    const std::string article =
            "first chunk data|second compressed chunk";
    const unsigned chunkLength = 16;
    std::vector<unsigned char> compressed;
    if (!makeDictZip(article, chunkLength, compressed))
        return fail("could not build the dictzip regression fixture");

    TemporaryFile indexFile(".index");
    TemporaryFile dataFile;
    TemporaryFile truncatedFile;
    if (!indexFile.valid() || !dataFile.valid()
            || !truncatedFile.valid())
        return fail("could not create temporary dictionary files");

    std::string index = "hello\tA\t"
            + encodeIndexNumber(static_cast<unsigned>(article.size()))
            + "\n";
    if (!writeFile(indexFile.c_str(), index.data(), index.size())
            || !writeFile(
                    dataFile.c_str(), compressed.data(), compressed.size()))
        return fail("could not write the dictzip regression fixture");

    gzFile gzip = gzopen(dataFile.c_str(), "rb");
    std::vector<char> gzipArticle(article.size());
    char trailingByte = 0;
    int gzipBytes = gzip
            ? gzread(gzip, gzipArticle.data(),
                    static_cast<unsigned>(gzipArticle.size()))
            : -1;
    int trailingBytes = gzip ? gzread(gzip, &trailingByte, 1) : -1;
    int gzipClose = gzip ? gzclose(gzip) : Z_ERRNO;
    if (gzipBytes != static_cast<int>(article.size())
            || trailingBytes != 0 || gzipClose != Z_OK
            || std::memcmp(gzipArticle.data(),
                    article.data(), article.size()) != 0)
        return fail("dictzip regression fixture is not GZIP-compatible");

    TinyDictionary dictionary;
    if (!dictionary.open(indexFile.c_str(), dataFile.c_str()))
        return fail("could not open a valid chunked dictionary");
    std::string expectedName(indexFile.c_str());
    expectedName.erase(0, expectedName.find_last_of("/\\") + 1);
    expectedName.resize(expectedName.find_last_of('.'));
    if (!dictionary.getDictionaryName()
            || expectedName != dictionary.getDictionaryName())
        return fail("dictionary name did not retain value ownership");
    std::unique_ptr<TinyDictWordList> words(dictionary.find("hello"));
    if (!words || words->length() != 1)
        return fail("chunked dictionary index lookup failed");
    const char *firstRead = words->getArticle(0);
    if (!firstRead)
        return fail("chunked dictionary article could not be read");
    if (std::strcmp(firstRead, article.c_str()) != 0) {
        std::fprintf(stderr, "expected '%s', received '%s'\n",
                article.c_str(), firstRead);
        return fail("chunked dictionary article was not reconstructed");
    }

    dictionary.compact();
    const char *secondRead = words->getArticle(0);
    if (!secondRead || std::strcmp(secondRead, article.c_str()) != 0)
        return fail("chunked dictionary did not recover after compacting");

    compressed.resize(compressed.size() - 6);
    if (!writeFile(truncatedFile.c_str(),
                compressed.data(), compressed.size()))
        return fail("could not write the truncated dictzip fixture");
    if (dictionary.open(indexFile.c_str(), truncatedFile.c_str()))
        return fail("truncated chunked dictionary was accepted");

    std::unique_ptr<TinyDictWordList> retainedWords(
            dictionary.find("hello"));
    const char *retainedArticle = retainedWords
            ? retainedWords->getArticle(0) : nullptr;
    if (!retainedArticle
            || std::strcmp(retainedArticle, article.c_str()) != 0)
        return fail("failed reopen replaced the working dictionary graph");
    if (!dictionary.getDictionaryName()
            || expectedName != dictionary.getDictionaryName())
        return fail("failed reopen replaced the dictionary name");

    TinyDictionaryList dictionaries;
    if (dictionaries.add(indexFile.c_str(), truncatedFile.c_str())
            || dictionaries.length() != 0)
        return fail("dictionary list adopted a failed candidate");
    if (!dictionaries.add(indexFile.c_str(), dataFile.c_str())
            || dictionaries.length() != 1)
        return fail("dictionary list did not adopt a valid candidate");
    TinyDictResultList results;
    if (!dictionaries.find(results, "hello")
            || results.length() != 1 || !results.get(0))
        return fail("dictionary result graph lookup failed");
    const char *listedArticle = results.get(0)->getArticle(0);
    if (!listedArticle
            || std::strcmp(listedArticle, article.c_str()) != 0)
        return fail("dictionary result graph lost its article owner");
    results.clear();
    dictionaries.clear();
    if (results.length() != 0 || dictionaries.length() != 0)
        return fail("dictionary owner graphs did not clear");
    return 0;
}

int main()
{
    return testCompressedChunkOwnership();
}
