/*
 * Copyright (C) 2014  Andrew Gunnerson <andrewgunnerson@gmail.com>
 *
 * This file is part of MultiBootPatcher
 *
 * MultiBootPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MultiBootPatcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MultiBootPatcher.  If not, see <http://www.gnu.org/licenses/>.
 */

#pragma once

#include <memory>
#include <string>

#include <cppformat/format.h>

#if 0
#define LOGE(msg) mb::util::log(mb::util::LogLevel::ERROR, msg)
#define LOGW(msg) mb::util::log(mb::util::LogLevel::WARNING, msg)
#define LOGI(msg) mb::util::log(mb::util::LogLevel::INFO, msg)
#define LOGD(msg) mb::util::log(mb::util::LogLevel::DEBUG, msg)
#define LOGV(msg) mb::util::log(mb::util::LogLevel::VERBOSE, msg)
#define FLOGE(...) mb::util::log(mb::util::LogLevel::ERROR, fmt::format(__VA_ARGS__))
#define FLOGW(...) mb::util::log(mb::util::LogLevel::WARNING, fmt::format(__VA_ARGS__))
#define FLOGI(...) mb::util::log(mb::util::LogLevel::INFO, fmt::format(__VA_ARGS__))
#define FLOGD(...) mb::util::log(mb::util::LogLevel::DEBUG, fmt::format(__VA_ARGS__))
#define FLOGV(...) mb::util::log(mb::util::LogLevel::VERBOSE, fmt::format(__VA_ARGS__))
#else
#define LOGE(...) mb::util::log(mb::util::LogLevel::ERROR, fmt::format(__VA_ARGS__))
#define LOGW(...) mb::util::log(mb::util::LogLevel::WARNING, fmt::format(__VA_ARGS__))
#define LOGI(...) mb::util::log(mb::util::LogLevel::INFO, fmt::format(__VA_ARGS__))
#define LOGD(...) mb::util::log(mb::util::LogLevel::DEBUG, fmt::format(__VA_ARGS__))
#define LOGV(...) mb::util::log(mb::util::LogLevel::VERBOSE, fmt::format(__VA_ARGS__))
#endif

namespace mb
{
namespace util
{


enum class LogLevel {
    ERROR,
    WARNING,
    INFO,
    DEBUG,
    VERBOSE
};


// All loggers should be a subclass of BaseLogger
class BaseLogger
{
public:
    virtual void log(LogLevel prio, const std::string &msg) = 0;
};


// Logger for <stdio.h> streams
class StdioLogger : public BaseLogger
{
public:
    StdioLogger(std::FILE *stream);

    virtual void log(LogLevel prio, const std::string &msg) override;

private:
    std::FILE *_stream;
};


// Logger for the kernel log
class KmsgLogger : public BaseLogger
{
public:
    KmsgLogger();

    virtual ~KmsgLogger();

    virtual void log(LogLevel prio, const std::string &msg) override;

private:
    int _fd;
#define KMSG_BUF_SIZE 512
    char _buf[KMSG_BUF_SIZE];
};


// Logger for Android's logcat (ifdef'd since usually, we can't link to liblog)
#ifdef USE_ANDROID_LOG
class AndroidLogger : public BaseLogger
{
public:
    virtual void log(LogLevel prio, const std::string &msg) override;
};
#endif


void log_set_logger(std::shared_ptr<BaseLogger> logger);
void log(LogLevel prio, const std::string &msg);

}
}