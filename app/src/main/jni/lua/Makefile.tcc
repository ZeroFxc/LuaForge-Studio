# Makefile for compiling tcc-generated C modules into DLLs for lxclua
# Usage: make SRC=xxx.c [OUT=xxx.dll]
# Example: make SRC=test_tcc_output.c

CC = gcc -std=gnu11
CFLAGS = -O2 -DLUA_LIB
INCLUDES = -Isrc/core -Isrc/compiler -Isrc/utils -Isrc/stdlib

LUA_LIB = liblua-import.a

SRC ?=
OUT ?= $(basename $(SRC)).dll

ifndef SRC
$(error Usage: make SRC=xxx.c [OUT=xxx.dll])
endif

all: $(OUT)

$(OUT): $(SRC) $(LUA_LIB)
	$(CC) -shared $(CFLAGS) $(INCLUDES) -o $@ $< $(LUA_LIB)

clean:
	rm -f *.dll *.o

.PHONY: all clean