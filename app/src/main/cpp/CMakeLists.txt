cmake_minimum_required(VERSION 3.22.1)

project("monster_turbo_engine")

add_library(
    monster_turbo_engine
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    monster_turbo_engine
    ${log-lib}
)
