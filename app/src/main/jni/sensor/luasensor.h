#ifndef luasensor_h
#define luasensor_h

#ifdef __cplusplus
extern "C"
{
#endif
  #include "src/core/lua.h"
  #include "src/core/lualib.h"
#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
#undef LUALIB_API
#define LUALIB_API extern "C"
#endif

LUALIB_API int (luaopen_sensor) (lua_State *L);

#endif
