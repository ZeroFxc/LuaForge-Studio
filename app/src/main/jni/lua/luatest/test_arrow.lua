-- ==========================================
-- 箭头函数边界测试
-- ==========================================

-- ==========================================
-- Test A1: -> 语句形式，有参数
-- ==========================================
local arrow_stmt = ->(a, b){ print("Test A1 (-> stmt):", a + b) }
arrow_stmt(10, 20)

-- ==========================================
-- Test A2: -> 无参数
-- ==========================================
local arrow_stmt_noarg = ->{ print("Test A2 (-> stmt no arg): hello") }
arrow_stmt_noarg()

-- ==========================================
-- Test A3: => 表达式，有参数
-- ==========================================
local arrow_expr = =>(x, y){ x * y }
print("Test A3 (=> expr):", arrow_expr(5, 6))

-- ==========================================
-- Test A4: => 表达式，无参数
-- ==========================================
local arrow_expr_noarg = =>{ 42 }
print("Test A4 (=> expr no arg):", arrow_expr_noarg())

-- ==========================================
-- Test A5: => 简单表达式
-- ==========================================
local adder = =>(n){ 1 + n }
print("Test A5 (=> simple):", adder(99))

-- ==========================================
-- Test A6: => 作为函数参数
-- ==========================================
local function apply(f, x) return f(x) end
print("Test A6 (=> as arg):", apply(=>(v){ v * 2 }, 7))

-- ==========================================
-- Test A7: -> 作为回调
-- ==========================================
local nums = {10, 20, 30}
local sum = 0
local cb = ->(n){ sum = sum + n }
for _, n in ipairs(nums) do cb(n) end
print("Test A7 (-> callback):", sum)

-- ==========================================
-- Test A8: => 嵌套（返回 =>）
-- ==========================================
local nested = =>(a){ =>(b){ a + b } }
local inner = nested(10)
print("Test A8 (nested =>):", inner(5))

-- ==========================================
-- Test A9: => vararg
-- ==========================================
local vararg_fn = =>(...){ 1 + select(1, ...) }
print("Test A9 (=> vararg):", vararg_fn(3))

-- ==========================================
-- Test A10: -> 多语句体
-- ==========================================
local multi_stmt = ->(a){
  local r = a * 2
  r = r + 1
  print("Test A10 (-> multi stmt): inner value =", r)
}
multi_stmt(5)

-- ==========================================
-- Test A11: => 原地立即调用（IIFE）
-- ==========================================
print("Test A11 (IIFE =>):", (=>(x){ x * x })(7))

-- ==========================================
-- Test A12: -> 无参数，多语句
-- ==========================================
local counter = 0
local inc = ->{
  counter = counter + 1
  print("Test A12 (-> no arg multi): counter =", counter)
}
inc()
inc()

-- ==========================================
-- Test A13: => 字符串拼接
-- ==========================================
local greet = =>(name){ "Hello, " .. name .. "!" }
print("Test A13 (=> string):", greet("World"))

-- ==========================================
-- Test A14: => 表构造
-- ==========================================
local make_pair = =>(k, v){ {key = k, value = v} }
local pair = make_pair("x", 100)
print("Test A14 (=> table):", pair.key, pair.value)

-- ==========================================
-- Test A15: => 条件表达式
-- ==========================================
local abs = =>(n){ if n >= 0 then n else -n end }
print("Test A15 (=> cond):", abs(-5), abs(3))

-- ==========================================
-- Test A16: => 嵌套三层
-- ==========================================
local triple = =>(a){ =>(b){ =>(c){ a + b + c } } }
print("Test A16 (triple =>):", triple(1)(2)(3))

-- ==========================================
-- Test A17: -> 和 => 混合使用
-- ==========================================
local result = 0
local proc = ->(x){ result = (=>(y){ x + y })(10) }
proc(5)
print("Test A17 (mixed):", result)

-- ==========================================
-- Test A18: => 返回 nil
-- ==========================================
local return_nil = =>{ nil }
print("Test A18 (=> nil):", return_nil())

-- ==========================================
-- Test A19: => 返回布尔值
-- ==========================================
local is_even = =>(n){ n % 2 == 0 }
print("Test A19 (=> bool):", is_even(4), is_even(7))

-- ==========================================
-- Test A20: => 复杂表达式（非语句）
-- ==========================================
local with_expr = =>(n){ (n + 1) * 3 }
print("Test A20 (=> complex expr):", with_expr(4))

-- ==========================================
-- Test A21: => 在表内作为方法值
-- ==========================================
local obj = {
  name = "test"
}
obj.get_name = =>{ obj.name }
print("Test A21 (=> table method):", obj.get_name())

-- ==========================================
-- Test A22: -> 操作外部局部变量
-- ==========================================
local captured = 0
local capture_fn = ->(n){
  captured = captured + n
}
capture_fn(3)
capture_fn(5)
print("Test A22 (-> capture):", captured)

-- ==========================================
-- Test A23: => 多参数（5个）
-- ==========================================
local sum5 = =>(a, b, c, d, e){ a + b + c + d + e }
print("Test A23 (=> 5 args):", sum5(1, 2, 3, 4, 5))

-- ==========================================
-- Test A24: => 链式调用
-- ==========================================
local chain = =>(x){ =>(y){ x * y } }
local double = chain(2)
local triple2 = chain(3)
print("Test A24 (=> chain):", double(10), triple2(10))

-- ==========================================
-- Test A25: 箭头函数与 switch 表达式混合
-- ==========================================
local classify = =>(n){
  switch(n)
    case 1 => "one"
    case 2 => "two"
    case 3 => "three"
    default => "many"
  end
}
print("Test A25 (=> + switch):", classify(2))

-- ==========================================
-- Test A26: -> 语句体内使用 switch 表达式
-- ==========================================
local stmt_with_switch = ->(n){
  local r = switch(n)
    case 1 => "first"
    case 2 => "second"
    default => "other"
  end
  print("Test A26 (-> + switch): n=" .. n .. " result=" .. r)
}
stmt_with_switch(1)
stmt_with_switch(5)